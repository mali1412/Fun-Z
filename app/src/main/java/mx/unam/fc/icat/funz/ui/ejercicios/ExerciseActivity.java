package mx.unam.fc.icat.funz.ui.ejercicios;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import mx.unam.fc.icat.funz.db.Exercise;
import mx.unam.fc.icat.funz.viewmodel.ExerciseViewModel;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.ui.temas.InfoEjemplosActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.R;

/**
 * ExerciseActivity — Activity genérica para cualquier tipo de ejercicio.
 */
public class ExerciseActivity extends AppCompatActivity {

    private ExerciseViewModel vm;
    private Chip         chipTimer;
    private EditText     etAnswer;
    private LinearLayout drawerMenu;
    private FrameLayout  panelContainer;
    private View         loadingView;
    private View         currentPanel;
    private int moduleId;
    private int stepOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_exercise);

        moduleId  = getIntent().getIntExtra("module_id",  1);
        stepOrder = getIntent().getIntExtra("step_order", 1);

        boolean sessionContinue = getIntent().getBooleanExtra("session_continue", false);
        if (!sessionContinue) {
            AppState.getInstance().resetSession();
        }

        vm = new ViewModelProvider(this).get(ExerciseViewModel.class);

        bindCommonViews();
        observeViewModel();
        setupHamburger();

        vm.loadExercise(moduleId, stepOrder);
    }

    private void bindCommonViews() {
        chipTimer      = findViewById(R.id.tv_timer);
        etAnswer       = findViewById(R.id.et_answer);
        panelContainer = findViewById(R.id.panel_container);
        loadingView    = findViewById(R.id.loading_view);
        drawerMenu     = findViewById(R.id.drawer_menu);
        drawerMenu.setVisibility(View.GONE);

        findViewById(R.id.btn_back).setOnClickListener(v -> { vm.cancelTimer(); finish(); });
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        findViewById(R.id.btn_verify).setOnClickListener(v ->
                vm.verify(etAnswer.getText().toString()));
    }

    private void observeViewModel() {
        vm.loading.observe(this, loading ->
                loadingView.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        vm.exercise.observe(this, exercise -> {
            if (exercise == null) return;
            updateToolbarTitle(exercise);
            showPanelForType(exercise);
        });

        vm.timerDisplay.observe(this, chipTimer::setText);
        vm.timerUrgent.observe(this, urgent -> {
            if (Boolean.TRUE.equals(urgent)) {
                chipTimer.setChipBackgroundColorResource(R.color.error_bg);
                chipTimer.setTextColor(getColor(R.color.error));
            }
        });
        vm.autoAnswer.observe(this, answer -> {
            if (answer != null && !answer.isEmpty()) etAnswer.setText(answer);
        });
        vm.timerFinished.observe(this, unused -> showTimeUpDialog());
        vm.exerciseResult.observe(this, this::handleResult);
    }

    private void showPanelForType(Exercise exercise) {
        panelContainer.removeAllViews();
        int layoutRes;
        switch (exercise.type) {
            case Exercise.TYPE_BALANZA: layoutRes = R.layout.view_exercise_balanza; break;
            case Exercise.TYPE_CLASICO: layoutRes = R.layout.view_exercise_clasico; break;
            case Exercise.TYPE_TILES:   layoutRes = R.layout.view_exercise_tiles;   break;
            default: return;
        }
        currentPanel = getLayoutInflater().inflate(layoutRes, panelContainer, false);
        panelContainer.addView(currentPanel);
        bindTypePanel(exercise);
    }

    private void bindTypePanel(Exercise exercise) {
        switch (exercise.type) {
            case Exercise.TYPE_BALANZA: bindBalanzaPanel(exercise); break;
            case Exercise.TYPE_CLASICO: bindClasicoPanel(exercise); break;
            case Exercise.TYPE_TILES:   bindTilesPanel(exercise);   break;
        }
    }

    private void bindBalanzaPanel(Exercise exercise) {
        TextView  tvLhs     = currentPanel.findViewById(R.id.tv_lhs);
        TextView  tvRhs     = currentPanel.findViewById(R.id.tv_rhs);
        TextView  tvStatus  = currentPanel.findViewById(R.id.tv_balance_status);
        ImageView ivBalanza = currentPanel.findViewById(R.id.iv_balanza);
        LinearLayout llOps  = currentPanel.findViewById(R.id.ll_op_buttons);

        vm.lhsExpr.observe(this, tvLhs::setText);
        vm.rhsExpr.observe(this, tvRhs::setText);
        vm.statusMessage.observe(this, tvStatus::setText);
        vm.statusPositive.observe(this, pos -> {
            if (pos == null) {
                tvStatus.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            } else {
                tvStatus.setTextColor(pos ? getColor(R.color.accent_green) : resolveThemeColor(R.attr.colorWarnChipText));
            }
        });
        
        vm.balanced.observe(this, balanced ->
                ivBalanza.setColorFilter(Boolean.TRUE.equals(balanced) ? getColor(R.color.accent_green) : resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)));

        // Lógica de inclinación dinámica con animación
        vm.tilt.observe(this, angle -> {
            ivBalanza.animate()
                .rotation(angle)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator())
                .start();
        });

        vm.ops.observe(this, opList -> {
            llOps.removeAllViews();
            for (String op : opList) {
                Button btn = new Button(this, null, com.google.android.material.R.style.Widget_Material3_Button_OutlinedButton);
                btn.setText(op);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(4, 0, 4, 0);
                btn.setLayoutParams(lp);
                btn.setOnClickListener(v -> vm.applyOp(op));
                llOps.addView(btn);
            }
        });
    }

    private void bindClasicoPanel(Exercise exercise) {
        TextView tvEquation = currentPanel.findViewById(R.id.tv_equation_display);
        LinearLayout llSteps = currentPanel.findViewById(R.id.ll_solution_steps);
        tvEquation.setText(exercise.equation);
        List<String> steps = ExerciseViewModel.parseJson(exercise.solutionSteps);
        llSteps.removeAllViews();
        for (String step : steps) {
            TextView tv = new TextView(this);
            tv.setText(step);
            tv.setTextColor(step.startsWith("  ") ? resolveThemeColor(androidx.appcompat.R.attr.colorPrimary) : resolveThemeColor(com.google.android.material.R.attr.colorOnSurface));
            tv.setTextSize(14f);
            tv.setPadding(0, 4, 0, 4);
            llSteps.addView(tv);
        }
    }

    private void bindTilesPanel(Exercise exercise) {
        LinearLayout llLeft  = currentPanel.findViewById(R.id.ll_tiles_left);
        LinearLayout llRight = currentPanel.findViewById(R.id.ll_tiles_right);
        TextView     tvSt    = currentPanel.findViewById(R.id.tv_tiles_status);
        TextView     tvEq    = currentPanel.findViewById(R.id.tv_tiles_equation);
        TextView     tvDrop  = currentPanel.findViewById(R.id.tv_drop_hint);
        LinearLayout dropZone = currentPanel.findViewById(R.id.operation_drop_zone);
        LinearLayout llOps   = currentPanel.findViewById(R.id.ll_tiles_ops_bottom);

        vm.statusMessage.observe(this, tvSt::setText);
        vm.statusPositive.observe(this, pos -> {
            if (pos == null) {
                tvSt.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            } else {
                tvSt.setTextColor(Boolean.TRUE.equals(pos) ? getColor(R.color.accent_green) : resolveThemeColor(R.attr.colorWarnChipText));
            }
        });
        vm.lhsExpr.observe(this, lhs -> {
            String rhs = vm.rhsExpr.getValue();
            tvEq.setText(formatEquation(lhs, rhs));
        });
        vm.rhsExpr.observe(this, rhs -> {
            String lhs = vm.lhsExpr.getValue();
            tvEq.setText(formatEquation(lhs, rhs));
        });

        vm.ops.observe(this, opList -> {
            llOps.removeAllViews();
            for (String op : opList) llOps.addView(makeOpView(op));
        });

        setupOperationDropZone(dropZone, tvDrop);
        vm.leftTilesLd.observe(this, tiles -> renderTiles(tiles, llLeft, "L"));
        vm.rightTilesLd.observe(this, tiles -> renderTiles(tiles, llRight, "R"));
    }

    private void renderTiles(List<String> tiles, LinearLayout container, String side) {
        container.removeAllViews();
        List<String> compactTiles = compactTilesForDisplay(tiles);
        if (compactTiles.isEmpty()) {
            compactTiles = new ArrayList<>();
            compactTiles.add("0");
        }
        for (String label : compactTiles) {
            container.addView(makeTileView(label));
        }
    }

    private View makeTileView(String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(20f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        boolean isX = label.endsWith("x") || label.contains("/");
        if (isX) {
            tv.setBackgroundResource(R.drawable.bg_tile_x);
        } else if (label.startsWith("-")) {
            tv.setBackgroundResource(R.drawable.bg_tile_negative);
        } else {
            tv.setBackgroundResource(R.drawable.bg_tile_positive);
        }
        tv.setClickable(true);
        tv.setLongClickable(true);
        // MATCH_PARENT de ancho para que llene el panel y quede visible en pila
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(72));
        lp.setMargins(dpToPx(6), dpToPx(5), dpToPx(6), dpToPx(5));
        tv.setLayoutParams(lp);

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText("tile", label);
            v.startDragAndDrop(cd, new View.DragShadowBuilder(v), v, 0);
            return true;
        });
        return tv;
    }

    private String formatEquation(String lhs, String rhs) {
        String left = lhs == null || lhs.trim().isEmpty() ? "0" : lhs.trim();
        String right = rhs == null || rhs.trim().isEmpty() ? "0" : rhs.trim();
        return left + " = " + right;
    }

    private List<String> compactTilesForDisplay(List<String> source) {
        int xCount = 0;
        int halfXCount = 0;
        int units = 0;
        for (String tile : source) {
            if ("x".equals(tile)) xCount++;
            else if ("x/2".equals(tile)) halfXCount++;
            else if ("+1".equals(tile) || "1".equals(tile)) units++;
            else if ("-1".equals(tile)) units--;
        }

        List<String> compact = new ArrayList<>();
        int halfUnits = xCount * 2 + halfXCount;
        if (halfUnits > 0) {
            if (halfUnits % 2 == 0) {
                int coef = halfUnits / 2;
                compact.add(coef == 1 ? "x" : coef + "x");
            } else {
                compact.add(halfUnits == 1 ? "x/2" : halfUnits + "x/2");
            }
        }
        if (units > 0) compact.add("+" + units);
        if (units < 0) compact.add(String.valueOf(units));
        if (compact.isEmpty()) compact.add("0");
        return compact;
    }

    private TextView makeOpView(String op) {
        TextView tv = new TextView(this);
        tv.setText(op);
        tv.setTextSize(26f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(16), dpToPx(18), dpToPx(16), dpToPx(18));
        tv.setClickable(true);
        tv.setLongClickable(true);
        if ("-1".equals(op) || "+1".equals(op) && op.startsWith("-")) {
            tv.setBackgroundResource(R.drawable.bg_tile_negative);
        } else if ("÷2".equals(op) || "×2".equals(op)) {
            tv.setBackgroundResource(R.drawable.bg_tile_x);
        } else {
            tv.setBackgroundResource(R.drawable.bg_tile_positive);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dpToPx(72), 1f);
        lp.setMargins(dpToPx(5), dpToPx(4), dpToPx(5), dpToPx(4));
        tv.setLayoutParams(lp);

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText("op", op);
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
            v.startDragAndDrop(cd, shadow, v, 0);
            return true;
        });
        return tv;
    }

    private void setupOperationDropZone(LinearLayout dropZone, TextView dropHint) {
        dropZone.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    dropHint.setText("Suelta aquí ↓");
                    dropZone.setAlpha(0.85f);
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    dropZone.setScaleX(1.05f);
                    dropZone.setScaleY(1.05f);
                    dropHint.setText("¡Suelta para aplicar!");
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    dropZone.setScaleX(1f);
                    dropZone.setScaleY(1f);
                    dropHint.setText("Suelta aquí ↓");
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    dropZone.setScaleX(1f);
                    dropZone.setScaleY(1f);
                    dropZone.setAlpha(1f);
                    dropHint.setText("o toca un botón");
                    return true;
                case DragEvent.ACTION_DROP:
                    dropZone.setScaleX(1f);
                    dropZone.setScaleY(1f);
                    if (event.getClipData() != null && event.getClipData().getItemCount() > 0) {
                        String op = event.getClipData().getItemAt(0).getText().toString();
                        vm.applyTileOperation(op);
                    }
                    return true;
                default:
                    return true;
            }
        });
    }

    private int dpToPx(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }

    private int resolveThemeColor(int attrResId) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attrResId, tv, true);
        return tv.data;
    }

    private void showHint() {
        Exercise ex = vm.exercise.getValue();
        if (ex == null) return;
        vm.useHint();
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_hint, null);

        String hintContent = ex.hintText != null ? ex.hintText : "";
        // Para Tiles: añade dinámicamente la operación recomendada al final de la pista
        if (Exercise.TYPE_TILES.equals(ex.type)) {
            String nextOp = vm.expectedTileOp();
            if (!nextOp.isEmpty()) {
                hintContent += "\n\n💡 Próxima operación a aplicar: " + nextOp;
            }
        }

        ((TextView) v.findViewById(R.id.tv_hint_content)).setText(hintContent);
        v.findViewById(R.id.btn_close_hint).setOnClickListener(b -> sheet.dismiss());
        sheet.setContentView(v);
        sheet.show();
    }

    private void handleResult(ExerciseViewModel.ExerciseResult result) {
        switch (result) {
            case EMPTY_INPUT: Toast.makeText(this, "Ingresa tu respuesta", Toast.LENGTH_SHORT).show(); break;
            case CORRECT: showResultDialog(true, false); break;
            case CORRECT_WITH_HINT: showResultDialog(true, true); break;
            case INCORRECT: showResultDialog(false, false); break;
        }
    }

    private void showResultDialog(boolean correct, boolean withHint) {
        Exercise ex = vm.exercise.getValue();
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this);
        if (correct) {
            int pts = ex != null ? (withHint ? ex.pointsHint : ex.pointsCorrect) : (withHint ? 50 : 100);
            String msg = "¡Bien hecho!\n+" + pts + " puntos";
            if (withHint) msg += "\n\nUsaste pista. ¡Intenta sin pista para +" + pts + " extra!";
            b.setTitle("🎉 ¡Correcto!").setMessage(msg).setPositiveButton(isLastStep() ? "Ver resultados" : "Siguiente →", (d, w) -> goToNext()).setCancelable(false);
            if (withHint) b.setNeutralButton("Sin pista", (d, w) -> { etAnswer.setText(""); vm.retryWithoutHint(); });
        } else {
            b.setTitle("🤔 Incorrecto").setMessage("Revisa la sección de Información para reforzar el tema.").setPositiveButton("📖 Info", (d, w) -> goToInfo()).setNegativeButton("Reintentar", (d, w) -> { etAnswer.setText(""); vm.retryCurrentExercise(); }).setNeutralButton("Salir", (d, w) -> finish()).setCancelable(false);
        }
        b.show();
    }

    private void showTimeUpDialog() {
        new MaterialAlertDialogBuilder(this).setTitle("⏰ ¡Tiempo agotado!").setPositiveButton("Reintentar", (d, w) -> { etAnswer.setText(""); vm.retryCurrentExercise(); }).setNegativeButton("Salir", (d, w) -> finish()).setCancelable(false).show();
    }

    private boolean isLastStep() { return stepOrder >= AppState.getInstance().getModuleExerciseCount(moduleId); }

    private void goToNext() {
        if (isLastStep()) {
            Intent i = new Intent(this, FinEjerciciosActivity.class);
            i.putExtra("module_id", moduleId);
            startActivity(i);
        } else {
            Intent i = new Intent(this, ExerciseActivity.class);
            i.putExtra("module_id",      moduleId);
            i.putExtra("step_order",     stepOrder + 1);
            i.putExtra("session_continue", true);
            startActivity(i);
        }
        finish();
    }

    private void goToInfo() {
        Intent i = new Intent(this, InfoEjemplosActivity.class);
        i.putExtra("module_id", moduleId);
        i.putExtra("tab", 0);
        startActivity(i);
        finish();
    }

    private void updateToolbarTitle(Exercise exercise) {
        TextView tvTitle = findViewById(R.id.tv_toolbar_title);
        if (tvTitle != null) tvTitle.setText("Módulo " + moduleId + " · Ej. " + stepOrder);
    }

    private void setupHamburger() {
        findViewById(R.id.btn_hamburger).setOnClickListener(v -> drawerMenu.setVisibility(drawerMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        setupDrawerItem(R.id.drawer_inicio, MainActivity.class);
        setupDrawerItem(R.id.drawer_temas,  TemasActivity.class);
        setupDrawerItem(R.id.drawer_salas,  SalasActivity.class);
        setupDrawerItem(R.id.drawer_stats,  EstadisticasActivity.class);
        setupDrawerItem(R.id.drawer_config, ConfiguracionActivity.class);
    }

    private void setupDrawerItem(int viewId, Class<?> target) {
        View item = findViewById(viewId);
        if (item != null) item.setOnClickListener(v -> { vm.cancelTimer(); drawerMenu.setVisibility(View.GONE); startActivity(new Intent(this, target)); });
    }

    @Override protected void onDestroy() { super.onDestroy(); vm.cancelTimer(); }
}
