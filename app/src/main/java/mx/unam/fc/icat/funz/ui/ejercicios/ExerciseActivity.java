package mx.unam.fc.icat.funz.ui.ejercicios;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

import java.util.List;

import mx.unam.fc.icat.funz.db.Exercise;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.viewmodel.ExerciseViewModel;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.ui.temas.InfoEjemplosActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.R;

/**
 * ExerciseActivity — Activity genérica para cualquier tipo de ejercicio.
 *
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  PARA AGREGAR UN NUEVO TIPO DE EJERCICIO                        ║
 * ║  1. Crear view_exercise_TIPO.xml con los controles necesarios   ║
 * ║  2. Inflar el nuevo layout en showPanelForType()                ║
 * ║  3. Conectar sus vistas en bindTypePanel()                      ║
 * ║  4. Agregar filas en SQLite (ExerciseType = "TIPO")             ║
 * ║  NO se toca ningún otro archivo.                                ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Extras del Intent:
 *   "module_id"  (int) — ID del módulo
 *   "step_order" (int) — número de ejercicio dentro del módulo
 */
public class ExerciseActivity extends AppCompatActivity {

    // ── ViewModel ─────────────────────────────────────────────────────────────
    private ExerciseViewModel vm;

    // ── Vistas comunes (todas las pantallas) ──────────────────────────────────
    private Chip         chipTimer;
    private EditText     etAnswer;
    private LinearLayout drawerMenu;
    private FrameLayout  panelContainer;
    private View         loadingView;

    // ── Vistas del panel activo (se asignan al inflar) ─────────────────────────
    private View         currentPanel;

    // ── Extras del Intent ─────────────────────────────────────────────────────
    private int moduleId;
    private int stepOrder;

    // ════════════════════════════════════════════════════════════════════════
    //  onCreate
    // ════════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_exercise);

        // Leer extras del Intent
        moduleId  = getIntent().getIntExtra("module_id",  1);
        stepOrder = getIntent().getIntExtra("step_order", 1);

        // Si el usuario llega aquí sin pasar por goToNext() (es decir, no es
        // continuación directa de un ejercicio anterior en la misma sesión),
        // se resetean los contadores de sesión para que la pantalla de fin
        // solo muestre los ejercicios hechos de corrido en esta actividad.
        boolean sessionContinue = getIntent().getBooleanExtra("session_continue", false);
        if (!sessionContinue) {
            AppState.getInstance().resetSession();
        }

        vm = new ViewModelProvider(this).get(ExerciseViewModel.class);

        bindCommonViews();
        observeViewModel();
        setupHamburger();

        // Cargar ejercicio de la DB (idempotente tras rotación)
        vm.loadExercise(moduleId, stepOrder);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Binding de vistas comunes
    // ════════════════════════════════════════════════════════════════════════

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

    // ════════════════════════════════════════════════════════════════════════
    //  Observadores del ViewModel
    // ════════════════════════════════════════════════════════════════════════

    private void observeViewModel() {
        // Loading spinner
        vm.loading.observe(this, loading ->
                loadingView.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        // Ejercicio cargado → inflar el panel correcto
        vm.exercise.observe(this, exercise -> {
            if (exercise == null) return;
            updateToolbarTitle(exercise);
            showPanelForType(exercise);
        });

        // Estado común
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

    // ════════════════════════════════════════════════════════════════════════
    //  Panel dinámico — el corazón de la arquitectura genérica
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Infla el layout correcto según {@link Exercise#type} y conecta sus vistas.
     * Para agregar un nuevo tipo: añadir un case aquí y crear el XML.
     */
    private void showPanelForType(Exercise exercise) {
        panelContainer.removeAllViews();

        int layoutRes;
        switch (exercise.type) {
            case Exercise.TYPE_BALANZA: layoutRes = R.layout.view_exercise_balanza; break;
            case Exercise.TYPE_CLASICO: layoutRes = R.layout.view_exercise_clasico; break;
            case Exercise.TYPE_TILES:   layoutRes = R.layout.view_exercise_tiles;   break;
            default:
                Toast.makeText(this, "Tipo de ejercicio desconocido: " + exercise.type,
                        Toast.LENGTH_LONG).show();
                return;
        }

        currentPanel = getLayoutInflater().inflate(layoutRes, panelContainer, false);
        panelContainer.addView(currentPanel);
        bindTypePanel(exercise);
    }

    /**
     * Conecta las vistas específicas del tipo con los LiveData del ViewModel.
     */
    private void bindTypePanel(Exercise exercise) {
        switch (exercise.type) {
            case Exercise.TYPE_BALANZA: bindBalanzaPanel(exercise); break;
            case Exercise.TYPE_CLASICO: bindClasicoPanel(exercise); break;
            case Exercise.TYPE_TILES:   bindTilesPanel(exercise);   break;
        }
    }

    // ── Panel BALANZA ─────────────────────────────────────────────────────────

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
                tvStatus.setTextColor(pos
                        ? getColor(R.color.accent_green)
                        : resolveThemeColor(R.attr.colorWarnChipText));
            }
        });
        vm.balanced.observe(this, balanced ->
                ivBalanza.setColorFilter(Boolean.TRUE.equals(balanced)
                        ? getColor(R.color.accent_green)
                        : resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)));

        // Botones de operación generados dinámicamente desde la DB
        vm.ops.observe(this, opList -> {
            llOps.removeAllViews();
            for (String op : opList) {
                Button btn = new Button(this, null, com.google.android.material.R.style.Widget_Material3_Button_OutlinedButton);
                btn.setText(op);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(4, 0, 4, 0);
                btn.setLayoutParams(lp);
                btn.setOnClickListener(v -> vm.applyOp(op));
                llOps.addView(btn);
            }
        });
    }

    // ── Panel CLÁSICO ─────────────────────────────────────────────────────────

    private void bindClasicoPanel(Exercise exercise) {
        TextView tvEquation = currentPanel.findViewById(R.id.tv_equation_display);
        LinearLayout llSteps = currentPanel.findViewById(R.id.ll_solution_steps);

        tvEquation.setText(exercise.equation);

        // Renderizar pasos desde JSON
        List<String> steps = ExerciseViewModel.parseJson(exercise.solutionSteps);
        llSteps.removeAllViews();
        for (int i = 0; i < steps.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(steps.get(i));
            tv.setTextColor(steps.get(i).startsWith("  ")
                    ? resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)
                    : resolveThemeColor(com.google.android.material.R.attr.colorOnSurface));
            tv.setTextSize(14f);
            tv.setPadding(0, 4, 0, 4);
            llSteps.addView(tv);
        }
    }

    // ── Panel TILES ───────────────────────────────────────────────────────────

    private void bindTilesPanel(Exercise exercise) {
        LinearLayout llLeft  = currentPanel.findViewById(R.id.ll_tiles_left);
        LinearLayout llRight = currentPanel.findViewById(R.id.ll_tiles_right);
        TextView     tvSt    = currentPanel.findViewById(R.id.tv_tiles_status);

        vm.statusMessage.observe(this, tvSt::setText);
        vm.statusPositive.observe(this, pos ->
                tvSt.setTextColor(Boolean.TRUE.equals(pos)
                        ? getColor(R.color.accent_green)
                        : resolveThemeColor(R.attr.colorWarnChipText)));

        vm.leftTilesLd.observe(this, tiles -> {
            renderTiles(tiles, llLeft, "L");
            setupDropTarget(llLeft, "L");
        });
        vm.rightTilesLd.observe(this, tiles -> {
            renderTiles(tiles, llRight, "R");
            setupDropTarget(llRight, "R");
        });
    }

    private void renderTiles(List<String> tiles, LinearLayout container, String side) {
        container.removeAllViews();
        for (int i = 0; i < tiles.size(); i++) {
            container.addView(makeTileView(tiles.get(i), i, side));
        }
    }

    private View makeTileView(String label, int idx, String side) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(10f);
        tv.setGravity(Gravity.CENTER);

        boolean isX = label.equals("x") || label.contains("/");
        int bgColor = isX ? getColor(R.color.color_primary)
                : (label.startsWith("-") ? 0xFFEF4444 : 0xFF3B82F6);
        tv.setBackgroundColor(bgColor);

        int w = isX ? dpToPx(44) : dpToPx(22);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, dpToPx(22));
        lp.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        tv.setLayoutParams(lp);
        tv.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText("tile", side + "_" + idx + "_" + label);
            v.startDragAndDrop(cd, new View.DragShadowBuilder(v), v, 0);
            return true;
        });
        if (!isX) {
            tv.setOnClickListener(v -> vm.moveTile(side, idx, label));
        }
        return tv;
    }

    private void setupDropTarget(LinearLayout container, String targetSide) {
        container.setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                String[] p = event.getClipData().getItemAt(0).getText().toString().split("_");
                if (p.length == 3 && !p[0].equals(targetSide) && !p[2].equals("x"))
                    vm.moveTile(p[0], Integer.parseInt(p[1]), p[2]);
            }
            return true;
        });
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    private int resolveThemeColor(int attrResId) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attrResId, tv, true);
        return tv.data;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Pista (BottomSheet)
    // ════════════════════════════════════════════════════════════════════════

    private void showHint() {
        Exercise ex = vm.exercise.getValue();
        if (ex == null) return;
        vm.useHint();

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_hint, null);
        ((TextView) v.findViewById(R.id.tv_hint_content)).setText(ex.hintText);
        v.findViewById(R.id.btn_close_hint).setOnClickListener(b -> sheet.dismiss());
        sheet.setContentView(v);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Resultado del ejercicio
    // ════════════════════════════════════════════════════════════════════════

    private void handleResult(ExerciseViewModel.ExerciseResult result) {
        switch (result) {
            case EMPTY_INPUT:
                Toast.makeText(this, "Ingresa tu respuesta", Toast.LENGTH_SHORT).show();
                break;
            case CORRECT:
                showResultDialog(true, false);
                break;
            case CORRECT_WITH_HINT:
                showResultDialog(true, true);
                break;
            case INCORRECT:
                showResultDialog(false, false);
                break;
        }
    }

    private void showResultDialog(boolean correct, boolean withHint) {
        Exercise ex = vm.exercise.getValue();
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this);
        if (correct) {
            int pts = ex != null ? (withHint ? ex.pointsHint : ex.pointsCorrect) : (withHint ? 50 : 100);
            String msg = "¡Bien hecho!\n+" + pts + " puntos";
            if (withHint) msg += "\n\nUsaste pista. ¡Intenta sin pista para +" + pts + " extra!";
            b.setTitle("🎉 ¡Correcto!")
                    .setMessage(msg)
                    .setPositiveButton(isLastStep() ? "Ver resultados" : "Siguiente →",
                            (d, w) -> goToNext())
                    .setCancelable(false);
            if (withHint) b.setNeutralButton("Sin pista", (d, w) -> {
                etAnswer.setText("");
                vm.retryWithoutHint();
            });
        } else {
            b.setTitle("🤔 Incorrecto")
                    .setMessage("Revisa la sección de Información para reforzar el tema.")
                    .setPositiveButton("📖 Info", (d, w) -> goToInfo())
                    .setNegativeButton("Reintentar", (d, w) -> { etAnswer.setText(""); vm.retryCurrentExercise(); })
                    .setNeutralButton("Salir", (d, w) -> finish())
                    .setCancelable(false);
        }
        b.show();
    }

    private void showTimeUpDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("⏰ ¡Tiempo agotado!")
                .setPositiveButton("Reintentar", (d, w) -> { etAnswer.setText(""); vm.retryCurrentExercise(); })
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false).show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación
    // ════════════════════════════════════════════════════════════════════════

    private boolean isLastStep() {
        AppState state = AppState.getInstance();
        return stepOrder >= state.getModuleExerciseCount(moduleId);
    }

    private void goToNext() {
        AppState state = AppState.getInstance();
        if (isLastStep()) {
            startActivity(new Intent(this, FinEjerciciosActivity.class));
        } else {
            Intent i = new Intent(this, ExerciseActivity.class);
            i.putExtra("module_id",      moduleId);
            i.putExtra("step_order",     stepOrder + 1);
            // Marcar como continuación directa para que NO se resetee la sesión
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
        if (tvTitle != null) {
            tvTitle.setText("Módulo " + moduleId + " · Ej. " + stepOrder);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Menú hamburguesa
    // ════════════════════════════════════════════════════════════════════════

    private void setupHamburger() {
        findViewById(R.id.btn_hamburger).setOnClickListener(v ->
                drawerMenu.setVisibility(
                        drawerMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        setupDrawerItem(R.id.drawer_inicio, MainActivity.class);
        setupDrawerItem(R.id.drawer_temas,  TemasActivity.class);
        setupDrawerItem(R.id.drawer_salas,  SalasActivity.class);
        setupDrawerItem(R.id.drawer_stats,  EstadisticasActivity.class);
        setupDrawerItem(R.id.drawer_config, ConfiguracionActivity.class);
    }

    private void setupDrawerItem(int viewId, Class<?> target) {
        View item = findViewById(viewId);
        if (item != null) item.setOnClickListener(v -> {
            vm.cancelTimer();
            drawerMenu.setVisibility(View.GONE);
            startActivity(new Intent(this, target));
        });
    }

    @Override protected void onDestroy() { super.onDestroy(); vm.cancelTimer(); }
}
