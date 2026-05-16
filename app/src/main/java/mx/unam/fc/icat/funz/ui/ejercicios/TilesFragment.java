package mx.unam.fc.icat.funz.ui.ejercicios;

import android.content.ClipData;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.utils.AlgebraTokens;
import mx.unam.fc.icat.funz.viewmodel.ExerciseViewModel;

public class TilesFragment extends Fragment {

    private static final String DRAG_LABEL_TILE = "tile";
    private static final String DRAG_LABEL_OP = "op";
    private ExerciseViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_exercise_tiles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(ExerciseViewModel.class);
        vm.setAnswerBoxVisible(true);
        LinearLayout llLeft = view.findViewById(R.id.ll_tiles_left);
        LinearLayout llRight = view.findViewById(R.id.ll_tiles_right);
        TextView tvSt = view.findViewById(R.id.tv_tiles_status);
        TextView tvEq = view.findViewById(R.id.tv_tiles_equation);
        TextView tvDrop = view.findViewById(R.id.tv_drop_hint);
        LinearLayout dropZone = view.findViewById(R.id.operation_drop_zone);
        LinearLayout llOps = view.findViewById(R.id.ll_tiles_ops_bottom);

        vm.statusMessage.observe(getViewLifecycleOwner(), tvSt::setText);
        vm.statusPositive.observe(getViewLifecycleOwner(), pos -> {
            if (pos == null) {
                tvSt.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            } else {
                tvSt.setTextColor(pos ? ContextCompat.getColor(requireContext(), R.color.accent_green) : resolveThemeColor(R.attr.colorWarnChipText));
            }
        });

        vm.lhsExpr.observe(getViewLifecycleOwner(), lhs -> tvEq.setText(formatEquation(lhs, vm.rhsExpr.getValue())));
        vm.rhsExpr.observe(getViewLifecycleOwner(), rhs -> tvEq.setText(formatEquation(vm.lhsExpr.getValue(), rhs)));

        vm.ops.observe(getViewLifecycleOwner(), opList -> {
            llOps.removeAllViews();
            for (String op : opList) llOps.addView(makeOpView(op));
        });

        setupOperationDropZone(dropZone, tvDrop);
        vm.leftTilesLd.observe(getViewLifecycleOwner(), tiles -> renderTiles(tiles, llLeft));
        vm.rightTilesLd.observe(getViewLifecycleOwner(), tiles -> renderTiles(tiles, llRight));
    }

    private void renderTiles(List<String> tiles, LinearLayout container) {
        container.removeAllViews();
        List<String> compactTiles = compactTilesForDisplay(tiles);
        for (String label : compactTiles) {
            container.addView(makeTileView(label));
        }
    }

    private View makeTileView(String label) {
        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_title));
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setIncludeFontPadding(false);

        if (label.startsWith("-")) {
            tv.setBackgroundResource(R.drawable.bg_tile_negative);
        } else {
            boolean isX = label.endsWith(AlgebraTokens.X) || label.contains("/");
            if (isX) tv.setBackgroundResource(R.drawable.bg_tile_x);
            else tv.setBackgroundResource(R.drawable.bg_tile_positive);
        }

        tv.setClickable(true);
        tv.setLongClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dimenPx(R.dimen.tile_item_height));
        lp.setMargins(dimenPx(R.dimen.tile_margin_h), dimenPx(R.dimen.tile_margin_v), dimenPx(R.dimen.tile_margin_h), dimenPx(R.dimen.tile_margin_v));
        tv.setLayoutParams(lp);
        tv.setHapticFeedbackEnabled(AppState.getInstance().isHapticFeedbackEnabled());

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText(DRAG_LABEL_TILE, label);
            v.startDragAndDrop(cd, new View.DragShadowBuilder(v), v, 0);
            return true;
        });
        return tv;
    }

    private View makeOpView(String op) {
        TextView tv = new TextView(requireContext());
        tv.setText(op);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_op));
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dimenPx(R.dimen.margin_tiny), dimenPx(R.dimen.margin_small), dimenPx(R.dimen.margin_tiny), dimenPx(R.dimen.margin_small));
        tv.setSingleLine(true);
        tv.setIncludeFontPadding(false);
        tv.setClickable(true);
        tv.setLongClickable(true);
        if (op.startsWith(AlgebraTokens.MINUS) || op.startsWith(AlgebraTokens.MINUS_SIGN)) tv.setBackgroundResource(R.drawable.bg_tile_negative);
        else if (op.contains(AlgebraTokens.DIV_SYMBOL) || op.contains(AlgebraTokens.MUL_SYMBOL) || op.contains(AlgebraTokens.X)) tv.setBackgroundResource(R.drawable.bg_tile_x);
        else tv.setBackgroundResource(R.drawable.bg_tile_positive);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dimenPx(R.dimen.tile_item_height), 1f);
        lp.setMargins(dimenPx(R.dimen.tile_margin_v), dimenPx(R.dimen.margin_tiny), dimenPx(R.dimen.tile_margin_v), dimenPx(R.dimen.margin_tiny));
        tv.setLayoutParams(lp);
        tv.setHapticFeedbackEnabled(AppState.getInstance().isHapticFeedbackEnabled());

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText(DRAG_LABEL_OP, op);
            v.startDragAndDrop(cd, new View.DragShadowBuilder(v), v, 0);
            return true;
        });
        return tv;
    }

    private void setupOperationDropZone(LinearLayout dropZone, TextView dropHint) {
        if (dropZone == null) return;
        dropZone.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    if (dropHint != null) dropHint.setText(R.string.drop_hint_release);
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    dropZone.setScaleX(1.05f); dropZone.setScaleY(1.05f);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    dropZone.setScaleX(1f); dropZone.setScaleY(1f);
                    return true;
                case DragEvent.ACTION_DROP:
                    if (event.getClipData() != null && event.getClipData().getItemCount() > 0) {
                        String op = event.getClipData().getItemAt(0).getText().toString();
                        vm.applyTileOperation(op);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    dropZone.setScaleX(1f); dropZone.setScaleY(1f);
                    if (dropHint != null) dropHint.setText(R.string.drop_hint_touch);
                    return true;
                default: return true;
            }
        });
    }

    private List<String> compactTilesForDisplay(List<String> source) {
        int xHalfUnits = 0, units = 0;
        for (String tile : source) {
            if (AlgebraTokens.X.equals(tile)) xHalfUnits += 2;
            else if (AlgebraTokens.NEG_X.equals(tile)) xHalfUnits -= 2;
            else if (AlgebraTokens.HALF_X.equals(tile)) xHalfUnits += 1;
            else if (AlgebraTokens.NEG_HALF_X.equals(tile)) xHalfUnits -= 1;
            else if (AlgebraTokens.POS_ONE.equals(tile) || AlgebraTokens.ONE.equals(tile)) units++;
            else if (AlgebraTokens.NEG_ONE.equals(tile)) units--;
        }
        List<String> compact = new ArrayList<>();
        if (xHalfUnits != 0) {
            if (xHalfUnits % 2 == 0) {
                int coef = xHalfUnits / 2;
                if (coef == 1) compact.add(AlgebraTokens.X);
                else if (coef == -1) compact.add(AlgebraTokens.NEG_X);
                else compact.add(coef + AlgebraTokens.X);
            } else {
                if (xHalfUnits == 1) compact.add(AlgebraTokens.HALF_X);
                else if (xHalfUnits == -1) compact.add(AlgebraTokens.NEG_HALF_X);
                else compact.add(xHalfUnits + AlgebraTokens.HALF_X);
            }
        }
        if (units > 0) compact.add(AlgebraTokens.PLUS + units);
        else if (units < 0) compact.add(String.valueOf(units));

        if (compact.isEmpty()) compact.add(AlgebraTokens.ZERO);
        return compact;
    }

    private String formatEquation(String lhs, String rhs) {
        String left = lhs == null || lhs.trim().isEmpty() ? AlgebraTokens.ZERO : lhs.trim();
        String right = rhs == null || rhs.trim().isEmpty() ? AlgebraTokens.ZERO : rhs.trim();
        return left + " " + AlgebraTokens.EQUALS + " " + right;
    }

    private int dimenPx(int dimenRes) { return getResources().getDimensionPixelSize(dimenRes); }
    private int resolveThemeColor(int attr) { TypedValue tv = new TypedValue(); requireActivity().getTheme().resolveAttribute(attr, tv, true); return tv.data; }
}
