package mx.unam.fc.icat.funz.ui.ejercicios;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.model.Ecuacion;
import mx.unam.fc.icat.funz.model.Termino;
import mx.unam.fc.icat.funz.utils.AlgebraTokens;
import mx.unam.fc.icat.funz.viewmodel.ExerciseViewModel;
import mx.unam.fc.icat.funz.data.AppState;

public class BalanzaFragment extends Fragment {

    private static final String DRAG_LABEL_BALANZA_TILE = "balanza_tile";
    private static final String DRAG_PAYLOAD_SEPARATOR = "|";
    private static final String DRAG_SIDE_SOURCE = "S";
    private static final String DRAG_SIDE_LEFT = "L";
    private static final String DRAG_SIDE_RIGHT = "R";
    private static final int MAX_TILES_PER_SIDE = 12;
    private static final int GRID_COLUMN_COUNT = 3; // Reducido para que quepan en el platillo (130dp)

    private ExerciseViewModel vm;
    private final Point lastTouchPoint = new Point();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_exercise_balanza, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(ExerciseViewModel.class);
        vm.setAnswerBoxVisible(true);
        TextView tvEq = view.findViewById(R.id.tv_balanza_equation);
        ImageView ivBase = view.findViewById(R.id.iv_balanza_base);
        RelativeLayout rlArm = view.findViewById(R.id.rl_balanza_arm);
        View plateL = view.findViewById(R.id.container_lhs);
        View plateR = view.findViewById(R.id.container_rhs);
        GridLayout gridL = view.findViewById(R.id.grid_lhs);
        GridLayout gridR = view.findViewById(R.id.grid_rhs);
        TextView tvStatus = view.findViewById(R.id.tv_balance_status);
        LinearLayout llSource = view.findViewById(R.id.ll_balanza_source_tiles);
        ImageView ivTrash = view.findViewById(R.id.iv_trash_bin);

        vm.lhsExpr.observe(getViewLifecycleOwner(), lhs -> tvEq.setText(formatEquation(lhs, vm.rhsExpr.getValue())));
        vm.rhsExpr.observe(getViewLifecycleOwner(), rhs -> tvEq.setText(formatEquation(vm.lhsExpr.getValue(), rhs)));

        vm.balanced.observe(getViewLifecycleOwner(), balanced -> {
            int color = ContextCompat.getColor(requireContext(), balanced ? R.color.success : R.color.warning);
            ivBase.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            if (balanced) {
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green));
                tvStatus.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
                        .withEndAction(() -> tvStatus.animate().scaleX(1f).scaleY(1f).start()).start();
            } else {
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            }
        });

        vm.tilt.observe(getViewLifecycleOwner(), angle -> {
            rlArm.animate().rotation(angle).setDuration(1000)
                    .setInterpolator(new OvershootInterpolator(1.5f)).start();
            plateL.animate().rotation(-angle).setDuration(1000).start();
            plateR.animate().rotation(-angle).setDuration(1000).start();
        });

        vm.ecuacion.observe(getViewLifecycleOwner(), ec -> {
            if (ec != null) {
                renderBalanzaWeights(ec.getLadoIzquierdo(), gridL, plateL, true);
                renderBalanzaWeights(ec.getLadoDerecho(), gridR, plateR, false);
            }
        });

        vm.statusMessage.observe(getViewLifecycleOwner(), tvStatus::setText);

        vm.ops.observe(getViewLifecycleOwner(), opList -> {
            if (llSource != null) {
                llSource.removeAllViews();
                List<String> labels = new ArrayList<>();
                labels.add(AlgebraTokens.X);
                labels.add(AlgebraTokens.NEG_X);
                labels.add(AlgebraTokens.POS_ONE);
                labels.add(AlgebraTokens.NEG_ONE);
                for (String op : opList) {
                    String clean = op.replace(" ", "")
                            .replace(AlgebraTokens.MINUS_SIGN, AlgebraTokens.MINUS)
                            .replace(AlgebraTokens.EN_DASH, AlgebraTokens.MINUS);
                    if (!labels.contains(clean)) labels.add(clean);
                }
                for (String label : labels) llSource.addView(makeBalanzaSourceTile(label));
            }
        });

        setupBalanzaInteraction(plateL, plateR, ivTrash);
    }

    private void setupBalanzaInteraction(View plateL, View plateR, View ivTrash) {
        View.OnDragListener balanzaDragListener = (v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            && DRAG_LABEL_BALANZA_TILE.equals(event.getClipDescription().getLabel());
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                    if (v.getBackground() != null) v.getBackground().setColorFilter(0x60FFFFFF, PorterDuff.Mode.SRC_ATOP);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    if (v.getBackground() != null) v.getBackground().clearColorFilter();
                    return true;
                case DragEvent.ACTION_DROP:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    if (v.getBackground() != null) v.getBackground().clearColorFilter();

                    ClipData data = event.getClipData();
                    if (data != null && data.getItemCount() > 0) {
                        String raw = data.getItemAt(0).getText().toString();
                        String label = raw.split("\\|", 2)[0];

                        if (v.getId() == R.id.container_lhs || v.getId() == R.id.container_rhs) {
                            Ecuacion currentEc = vm.ecuacion.getValue();
                            if (currentEc != null) {
                                boolean isLeft = (v.getId() == R.id.container_lhs);
                                List<Termino> ladoActual = isLeft ? currentEc.getLadoIzquierdo() : currentEc.getLadoDerecho();

                                if (label.contains("/") || label.contains(AlgebraTokens.DIV_SYMBOL)) {
                                    String numericPart = label.replaceAll("[^0-9]", "");
                                    if (!numericPart.isEmpty()) {
                                        int incomingDivisor = Integer.parseInt(numericPart);
                                        for (Termino t : ladoActual) {
                                            if (t.getDivisor() * incomingDivisor > 27) {
                                                showDenominatorLimitWarning(v);
                                                return true;
                                            }
                                        }
                                    }
                                }

                                int totalTiles = 0;
                                for (Termino t : ladoActual) {
                                    if (t.getDivisor() > 1) totalTiles += 1;
                                    else if (t.esVariable()) totalTiles += Math.abs(t.getCoeficiente());
                                    else if (t.esConstante()) totalTiles += Math.abs(t.getValor());
                                }

                                String opClean = label.replace(" ", "");
                                boolean isIncreasingScaling = opClean.contains("*") || opClean.contains("×");
                                boolean isDecreasingScaling = opClean.contains("/") || opClean.contains("÷");

                                if (totalTiles >= MAX_TILES_PER_SIDE || isIncreasingScaling) {
                                    boolean simplifies = false;
                                    if (opClean.equals(AlgebraTokens.X) || opClean.equals(AlgebraTokens.POS_X)) {
                                        for (Termino t : ladoActual) if (t.esVariable() && t.getCoeficiente() < 0) { simplifies = true; break; }
                                    } else if (opClean.equals(AlgebraTokens.NEG_X)) {
                                        for (Termino t : ladoActual) if (t.esVariable() && t.getCoeficiente() > 0) { simplifies = true; break; }
                                    } else if (opClean.equals(AlgebraTokens.POS_ONE) || opClean.equals("1") || opClean.equals("+1")) {
                                        for (Termino t : ladoActual) if (t.esConstante() && t.getValor() < 0) { simplifies = true; break; }
                                    } else if (opClean.equals(AlgebraTokens.NEG_ONE) || opClean.equals("-1")) {
                                        for (Termino t : ladoActual) if (t.esConstante() && t.getValor() > 0) { simplifies = true; break; }
                                    }

                                    if (!simplifies) {
                                        if (isIncreasingScaling) {
                                            String numericPart = opClean.replaceAll("[^0-9]", "");
                                            if (!numericPart.isEmpty()) {
                                                int factor = Integer.parseInt(numericPart);
                                                if (totalTiles * factor > MAX_TILES_PER_SIDE + 6) {
                                                    showFullPlateWarning(v);
                                                    return true;
                                                }
                                            }
                                        } else if (!isDecreasingScaling && totalTiles >= MAX_TILES_PER_SIDE) {
                                            showFullPlateWarning(v);
                                            return true;
                                        }
                                    }
                                }
                            }
                        }

                        if (v.getId() == R.id.iv_trash_bin) {
                            if (raw.contains(DRAG_PAYLOAD_SEPARATOR)) {
                                String inverseOp;
                                if (label.equals(AlgebraTokens.X)) inverseOp = AlgebraTokens.NEG_X;
                                else if (label.startsWith("+")) inverseOp = "-" + label.substring(1);
                                else if (label.startsWith("-")) inverseOp = "+" + label.substring(1);
                                else if (label.contains("/") || label.contains(AlgebraTokens.DIV_SYMBOL)) {
                                    inverseOp = "*" + (label.contains("/") ? label.substring(label.indexOf("/") + 1) : label.substring(label.indexOf(AlgebraTokens.DIV_SYMBOL) + 1));
                                }
                                else inverseOp = "-" + label;
                                vm.applyOp(inverseOp);
                            }
                        } else {
                            String op = label;
                            if (label.equals(AlgebraTokens.X)) op = AlgebraTokens.POS_X;
                            else if (!label.startsWith("+") && !label.startsWith("-") && !label.contains("/") && !label.contains("*") && !label.contains(AlgebraTokens.DIV_SYMBOL) && !label.contains(AlgebraTokens.MUL_SYMBOL)) op = "+" + label;
                            vm.applyOp(op);
                        }
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    if (v.getBackground() != null) v.getBackground().clearColorFilter();

                    View source = (View) event.getLocalState();
                    if (source != null) {
                        source.setAlpha(1.0f);
                        if (source.getBackground() != null) source.getBackground().clearColorFilter();
                    }
                    return true;
            }
            return false;
        };

        if (plateL != null) plateL.setOnDragListener(balanzaDragListener);
        if (plateR != null) plateR.setOnDragListener(balanzaDragListener);
        if (ivTrash != null) ivTrash.setOnDragListener(balanzaDragListener);
    }

    private void renderBalanzaWeights(List<Termino> terminos, GridLayout grid, View plate, boolean isLeft) {
        grid.removeAllViews();
        grid.setColumnCount(GRID_COLUMN_COUNT);
        int sizeX = getResources().getDimensionPixelSize(R.dimen.balanza_tile_size_x);
        int sizeUnit = getResources().getDimensionPixelSize(R.dimen.balanza_tile_size_unit);
        boolean hasTiles = false;
        for (Termino t : terminos) {
            if (t.esVariable()) {
                hasTiles = true;
                int bg = t.getCoeficiente() > 0 ? R.drawable.bg_tile_x : R.drawable.bg_tile_negative;
                if (t.getDivisor() > 1) addBalanzaWeightIcon(grid, bg, sizeX, t.getSimbolo(), isLeft, t.getSimbolo());
                else {
                    int count = Math.abs(t.getCoeficiente());
                    String label = t.getCoeficiente() > 0 ? AlgebraTokens.X : AlgebraTokens.NEG_X;
                    for (int i = 0; i < count; i++) addBalanzaWeightIcon(grid, bg, sizeX, label, isLeft, label);
                }
            } else if (t.esConstante()) {
                int val = t.getValor();
                if (val == 0) continue;
                hasTiles = true;
                int bg = val > 0 ? R.drawable.bg_tile_positive : R.drawable.bg_tile_negative;
                if (t.getDivisor() > 1) addBalanzaWeightIcon(grid, bg, sizeUnit, t.getSimbolo(), isLeft, t.getSimbolo());
                else {
                    int absVal = Math.abs(val);
                    String unitLabel = val > 0 ? AlgebraTokens.POS_ONE : AlgebraTokens.NEG_ONE;
                    for (int i = 0; i < absVal; i++) addBalanzaWeightIcon(grid, bg, sizeUnit, unitLabel, isLeft, unitLabel);
                }
            }
        }
        if (plate != null && plate.getBackground() != null) {
            if (hasTiles) plate.getBackground().setColorFilter(0x40000000, PorterDuff.Mode.SRC_ATOP);
            else plate.getBackground().clearColorFilter();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addBalanzaWeightIcon(GridLayout grid, int bgRes, int sizePx, String label, boolean isLeft, String displayText) {
        TextView tv = new TextView(requireContext());
        tv.setBackgroundResource(bgRes);
        tv.setText(displayText);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(displayText.length() > 2 ? R.dimen.text_size_micro : R.dimen.text_size_tiny));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = sizePx; lp.height = sizePx;
        lp.setMargins(dimenPx(R.dimen.margin_xsmall), dimenPx(R.dimen.margin_xsmall), dimenPx(R.dimen.margin_xsmall), dimenPx(R.dimen.margin_xsmall));
        tv.setLayoutParams(lp);
        grid.addView(tv);
        tv.setHapticFeedbackEnabled(AppState.getInstance().isHapticFeedbackEnabled());
        tv.setClickable(true);
        tv.setOnTouchListener((v, event) -> { if (event.getAction() == MotionEvent.ACTION_DOWN) lastTouchPoint.set((int) event.getX(), (int) event.getY()); return false; });
        tv.setOnLongClickListener(v -> {
            v.setPressed(false);
            View.DragShadowBuilder shadowBuilder = new BalanzaDragShadowBuilder(v, lastTouchPoint);
            ClipData cd = ClipData.newPlainText(DRAG_LABEL_BALANZA_TILE, label + DRAG_PAYLOAD_SEPARATOR + (isLeft ? DRAG_SIDE_LEFT : DRAG_SIDE_RIGHT));
            v.startDragAndDrop(cd, shadowBuilder, v, 0);
            return true;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private View makeBalanzaSourceTile(String label) {
        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_body));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setSingleLine(true);
        int bg = (label.startsWith(AlgebraTokens.MINUS) || label.startsWith(AlgebraTokens.MINUS_SIGN)) ? R.drawable.bg_tile_negative : (label.contains(AlgebraTokens.X_SYMBOL) || label.contains("/") || label.contains("*") || label.contains(AlgebraTokens.DIV_SYMBOL) || label.contains(AlgebraTokens.MUL_SYMBOL) ? R.drawable.bg_tile_x : R.drawable.bg_tile_positive);
        tv.setBackgroundResource(bg);
        int height = getResources().getDimensionPixelSize(R.dimen.balanza_tile_size_x);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
        lp.setMargins(dimenPx(R.dimen.margin_tiny), dimenPx(R.dimen.margin_tiny), dimenPx(R.dimen.margin_tiny), dimenPx(R.dimen.margin_tiny));
        tv.setLayoutParams(lp);
        tv.setClickable(true);
        tv.setOnTouchListener((v, event) -> { if (event.getAction() == MotionEvent.ACTION_DOWN) lastTouchPoint.set((int) event.getX(), (int) event.getY()); return false; });
        tv.setOnLongClickListener(v -> {
            v.setPressed(false);
            View.DragShadowBuilder shadowBuilder = new BalanzaDragShadowBuilder(v, lastTouchPoint);
            ClipData cd = ClipData.newPlainText(DRAG_LABEL_BALANZA_TILE, label + DRAG_PAYLOAD_SEPARATOR + DRAG_SIDE_SOURCE);
            v.startDragAndDrop(cd, shadowBuilder, v, 0);
            return true;
        });
        return tv;
    }

    private void showFullPlateWarning(View plate) {
        plate.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        plate.animate().translationX(15f).setDuration(50).withEndAction(() -> plate.animate().translationX(-15f).setDuration(50).withEndAction(() -> plate.animate().translationX(0f).setDuration(50).start()).start()).start();
        Toast.makeText(requireContext(), "¡El plato está muy pesado! Simplifica antes de añadir más.", Toast.LENGTH_SHORT).show();
    }

    private void showDenominatorLimitWarning(View plate) {
        plate.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        plate.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction(() -> plate.animate().scaleX(1.0f).scaleY(1.0f).start()).start();
        Toast.makeText(requireContext(), "¡Denominador demasiado grande! Intenta simplificar u operar de otra forma.", Toast.LENGTH_SHORT).show();
    }

    private int dimenPx(int resId) { return getResources().getDimensionPixelSize(resId); }

    private String formatEquation(String lhs, String rhs) {
        String left = lhs == null || lhs.trim().isEmpty() ? AlgebraTokens.ZERO : lhs.trim();
        String right = rhs == null || rhs.trim().isEmpty() ? AlgebraTokens.ZERO : rhs.trim();
        return left + " " + AlgebraTokens.EQUALS + " " + right;
    }

    private static class BalanzaDragShadowBuilder extends View.DragShadowBuilder {
        private final Point touchPoint;
        private final float scale = 1.4f;
        private Bitmap shadowBitmap;
        public BalanzaDragShadowBuilder(View v, Point touchPoint) {
            super(v);
            this.touchPoint = new Point(touchPoint.x, touchPoint.y);
            try {
                if (v.getWidth() > 0 && v.getHeight() > 0) {
                    shadowBitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(shadowBitmap);
                    float originalAlpha = v.getAlpha();
                    v.setAlpha(1.0f);
                    v.draw(c);
                    v.setAlpha(originalAlpha);
                }
            } catch (Exception e) { shadowBitmap = null; }
        }
        @Override public void onProvideShadowMetrics(Point size, Point touch) {
            int width = (int) (getView().getWidth() * scale);
            int height = (int) (getView().getHeight() * scale);
            size.set(width, height);
            touch.set((int) (touchPoint.x * scale), (int) (touchPoint.y * scale));
        }
        @Override public void onDrawShadow(Canvas canvas) {
            if (shadowBitmap != null && !shadowBitmap.isRecycled()) {
                Rect src = new Rect(0, 0, shadowBitmap.getWidth(), shadowBitmap.getHeight());
                Rect dst = new Rect(0, 0, (int)(shadowBitmap.getWidth() * scale), (int)(shadowBitmap.getHeight() * scale));
                Paint paint = new Paint();
                paint.setFilterBitmap(true);
                canvas.drawBitmap(shadowBitmap, src, dst, paint);
            } else {
                canvas.scale(scale, scale);
                super.onDrawShadow(canvas);
            }
        }
    }
}
