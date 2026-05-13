package mx.unam.fc.icat.funz.ui.ejercicios;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mx.unam.fc.icat.funz.db.Exercise;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.ui.temas.InfoEjemplosActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.viewmodel.ExerciseViewModel;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.model.Ecuacion;
import mx.unam.fc.icat.funz.model.Termino;
import mx.unam.fc.icat.funz.R;

/**
 * ExerciseActivity — IU Experta con soporte para Balanza (Física/Alquimia),
 * Tiles (Arrastrables) y modo Clásico.
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
        if (!getIntent().getBooleanExtra("session_continue", false)) {
            state.resetSession();
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
        drawerMenu = findViewById(R.id.drawer_menu);
        drawerMenu.setVisibility(View.GONE);
        findViewById(R.id.btn_back).setOnClickListener(v -> {vm.cancelTimer(); finish();});
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        findViewById(R.id.btn_verify).setOnClickListener(v -> vm.verify(etAnswer.getText().toString()));
    }

    private void observeViewModel() {
        vm.loading.observe(this, loading  -> loadingView.setVisibility(loading  ? View.VISIBLE : View.GONE));
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
        vm.autoAnswer.observe(this, a -> { if (a != null && !a.isEmpty()) etAnswer.setText(a); });
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
        TextView tvEq = currentPanel.findViewById(R.id.tv_balanza_equation);
        ImageView ivBase = currentPanel.findViewById(R.id.iv_balanza_base);
        RelativeLayout rlArm = currentPanel.findViewById(R.id.rl_balanza_arm);
        View plateL = currentPanel.findViewById(R.id.container_lhs);
        View plateR = currentPanel.findViewById(R.id.container_rhs);
        GridLayout gridL = currentPanel.findViewById(R.id.grid_lhs);
        GridLayout gridR = currentPanel.findViewById(R.id.grid_rhs);
        TextView tvStatus = currentPanel.findViewById(R.id.tv_balance_status);
        FrameLayout confettiContainer = currentPanel.findViewById(R.id.confetti_container);

        vm.lhsExpr.observe(this, lhs -> tvEq.setText(formatEquation(lhs, vm.rhsExpr.getValue())));
        vm.rhsExpr.observe(this, rhs -> tvEq.setText(formatEquation(vm.lhsExpr.getValue(), rhs)));

        vm.balanced.observe(this, balanced -> {
            int color = balanced ? Color.parseColor("#10B981") : Color.parseColor("#F59E0B");
            ivBase.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            if (balanced) {
                tvStatus.setTextColor(Color.parseColor("#059669"));
                tvStatus.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
                        .withEndAction(() -> tvStatus.animate().scaleX(1f).scaleY(1f).start()).start();
                triggerConfetti(confettiContainer);
            } else {
                tvStatus.setTextColor(Color.GRAY);
            }
        });

        vm.tilt.observe(this, angle -> {
            rlArm.animate().rotation(angle).setDuration(1000)
                    .setInterpolator(new OvershootInterpolator(1.5f)).start();
            plateL.animate().rotation(-angle).setDuration(1000).start();
            plateR.animate().rotation(-angle).setDuration(1000).start();
        });

        vm.ecuacion.observe(this, ec -> {
            if (ec != null) {
                renderBalanzaWeights(ec.getLadoIzquierdo(), gridL, true);
                renderBalanzaWeights(ec.getLadoDerecho(), gridR, false);
            }
        });

        vm.statusMessage.observe(this, tvStatus::setText);

        setupBalanzaInteraction(plateL, plateR);
    }

    private void setupBalanzaInteraction(View plateL, View plateR) {
        LinearLayout llSource = currentPanel.findViewById(R.id.ll_balanza_source_tiles);
        ImageView ivTrash = currentPanel.findViewById(R.id.iv_trash_bin);

        vm.ops.observe(this, opList -> {
            if (llSource != null) {
                llSource.removeAllViews();
                List<String> labels = new ArrayList<>();
                labels.add("x");
                labels.add("-x");
                labels.add("+1");
                labels.add("-1");
                for (String op : opList) {
                    String clean = op.replace(" ", "").replace("−", "-").replace("–", "-");
                    if ((clean.startsWith("+") || clean.startsWith("-")) && !labels.contains(clean)) {
                        labels.add(clean);
                    }
                }
                for (String label : labels) {
                    llSource.addView(makeBalanzaSourceTile(label));
                }
            }
        });

        View.OnDragListener plateListener = (v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                ClipData data = event.getClipData();
                if (data != null && data.getItemCount() > 0) {
                    String raw = data.getItemAt(0).getText().toString();
                    String label = raw.split("\\|")[0];
                    // Ahora aplicamos a ambos lados para mantener la igualdad
                    String op = label;
                    if (label.equals("x")) op = "+x";
                    else if (!label.startsWith("+") && !label.startsWith("-")) op = "+" + label;
                    vm.applyOp(op);
                }
            }
            return true;
        };

        if (plateL != null) plateL.setOnDragListener(plateListener);
        if (plateR != null) plateR.setOnDragListener(plateListener);

        if (ivTrash != null) {
            ivTrash.setOnDragListener((v, event) -> {
                if (event.getAction() == DragEvent.ACTION_DROP) {
                    ClipData data = event.getClipData();
                    if (data != null && data.getItemCount() > 0) {
                        String raw = data.getItemAt(0).getText().toString();
                        if (raw.contains("|")) {
                            String[] parts = raw.split("\\|");
                            String label = parts[0];
                            // Al tirar a la basura, restamos lo mismo de ambos lados para mantener la igualdad
                            String inverseOp;
                            if (label.equals("x")) inverseOp = "-x";
                            else if (label.startsWith("+")) inverseOp = "-" + label.substring(1);
                            else if (label.startsWith("-")) inverseOp = "+" + label.substring(1);
                            else inverseOp = "-" + label;
                            vm.applyOp(inverseOp);
                        }
                    }
                }
                return true;
            });
        }
    }

    private View makeBalanzaSourceTile(String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(18f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setSingleLine(true);
        tv.setIncludeFontPadding(false);
        int bg = label.startsWith("-") ? R.drawable.bg_tile_negative : (label.contains("x") ? R.drawable.bg_tile_x : R.drawable.bg_tile_positive);
        tv.setBackgroundResource(bg);

        int height = getResources().getDimensionPixelSize(R.dimen.balanza_source_tile_height);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
        lp.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        tv.setLayoutParams(lp);

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText("balanza_tile", label + "|S");
            v.startDragAndDrop(cd, new View.DragShadowBuilder(v), v, 0);
            return true;
        });
        return tv;
    }

    private void renderBalanzaWeights(List<Termino> terminos, GridLayout grid, boolean isLeft) {
        grid.removeAllViews();
        grid.setColumnCount(5);

        int sizeX = getResources().getDimensionPixelSize(R.dimen.balanza_tile_size_x);
        int sizeUnit = getResources().getDimensionPixelSize(R.dimen.balanza_tile_size_unit);

        for (Termino t : terminos) {
            if (t.esVariable()) {
                int count = Math.abs(t.getCoeficiente());
                int bg = t.getCoeficiente() > 0 ? R.drawable.bg_tile_x : R.drawable.bg_tile_negative;
                String label = t.getCoeficiente() > 0 ? "x" : "-x";
                for (int i = 0; i < count; i++) {
                    addBalanzaWeightIcon(grid, bg, sizeX, label, isLeft, label);
                }
            } else if (t.esConstante()) {
                int val = t.getValor();
                if (val == 0) continue;

                int absVal = Math.abs(val);
                String unitLabel = val > 0 ? "+1" : "-1";
                int bg = val > 0 ? R.drawable.bg_tile_positive : R.drawable.bg_tile_negative;

                for (int i = 0; i < absVal; i++) {
                    addBalanzaWeightIcon(grid, bg, sizeUnit, unitLabel, isLeft, unitLabel);
                }
            }
        }
    }

    private View addBalanzaWeightIcon(GridLayout grid, int bgRes, int sizePx, String label, boolean isLeft, String displayText) {
        TextView tv = new TextView(this);
        tv.setBackgroundResource(bgRes);
        tv.setText(displayText);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 0, 0, 0);
        tv.setIncludeFontPadding(false);
        tv.setSingleLine(true);
        // Texto muy pequeño para bloques de unidad
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, displayText.length() > 2 ? 7f : 8f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = sizePx;
        lp.height = sizePx;
        lp.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
        tv.setLayoutParams(lp);
        grid.addView(tv);

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText("balanza_tile", label + "|" + (isLeft ? "L" : "R"));
            v.startDragAndDrop(cd, new View.DragShadowBuilder(v), v, 0);
            return true;
        });
        return tv;
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
            tv.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurface));
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
        vm.lhsExpr.observe(this, lhs -> tvEq.setText(formatEquation(lhs, vm.rhsExpr.getValue())));
        vm.rhsExpr.observe(this, rhs -> tvEq.setText(formatEquation(vm.lhsExpr.getValue(), rhs)));

        vm.ops.observe(this, opList -> {
            llOps.removeAllViews();
            for (String op : opList) llOps.addView(makeOpView(op));
        });

        setupOperationDropZone(dropZone, tvDrop);
        vm.leftTilesLd.observe(this, tiles -> renderTiles(tiles, llLeft));
        vm.rightTilesLd.observe(this, tiles -> renderTiles(tiles, llRight));
    }

    private void renderTiles(List<String> tiles, LinearLayout container) {
        container.removeAllViews();
        List<String> compactTiles = compactTilesForDisplay(tiles);
        for (String label : compactTiles) {
            container.addView(makeTileView(label));
        }
    }

    private View makeTileView(String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(18f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setIncludeFontPadding(false);
        
        if (label.startsWith("-")) {
            tv.setBackgroundResource(R.drawable.bg_tile_negative);
        } else {
            boolean isX = label.endsWith("x") || label.contains("/");
            if (isX) tv.setBackgroundResource(R.drawable.bg_tile_x);
            else tv.setBackgroundResource(R.drawable.bg_tile_positive);
        }
        
        tv.setClickable(true);
        tv.setLongClickable(true);
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

    private List<String> compactTilesForDisplay(List<String> source) {
        int xHalfUnits = 0, units = 0;
        for (String tile : source) {
            if ("x".equals(tile)) xHalfUnits += 2;
            else if ("-x".equals(tile)) xHalfUnits -= 2;
            else if ("x/2".equals(tile)) xHalfUnits += 1;
            else if ("-x/2".equals(tile)) xHalfUnits -= 1;
            else if ("+1".equals(tile) || "1".equals(tile)) units++;
            else if ("-1".equals(tile)) units--;
        }
        List<String> compact = new ArrayList<>();
        if (xHalfUnits != 0) {
            if (xHalfUnits % 2 == 0) {
                int coef = xHalfUnits / 2;
                if (coef == 1) compact.add("x");
                else if (coef == -1) compact.add("-x");
                else compact.add(coef + "x");
            } else {
                if (xHalfUnits == 1) compact.add("x/2");
                else if (xHalfUnits == -1) compact.add("-x/2");
                else compact.add(xHalfUnits + "x/2");
            }
        }
        if (units > 0) compact.add("+" + units);
        else if (units < 0) compact.add(String.valueOf(units));

        if (compact.isEmpty()) compact.add("0");
        return compact;
    }

    private String formatEquation(String lhs, String rhs) {
        String left = lhs == null || lhs.trim().isEmpty() ? "0" : lhs.trim();
        String right = rhs == null || rhs.trim().isEmpty() ? "0" : rhs.trim();
        return left + " = " + right;
    }

    private TextView makeOpView(String op) {
        TextView tv = new TextView(this);
        tv.setText(op);
        tv.setTextSize(22f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));
        tv.setSingleLine(true);
        tv.setIncludeFontPadding(false);
        tv.setClickable(true);
        tv.setLongClickable(true);
        if (op.startsWith("-") || op.startsWith("−")) tv.setBackgroundResource(R.drawable.bg_tile_negative);
        else if (op.contains("÷") || op.contains("×") || op.contains("x")) tv.setBackgroundResource(R.drawable.bg_tile_x);
        else tv.setBackgroundResource(R.drawable.bg_tile_positive);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dpToPx(72), 1f);
        lp.setMargins(dpToPx(5), dpToPx(4), dpToPx(5), dpToPx(4));
        tv.setLayoutParams(lp);

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText("op", op);
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
                    if (dropHint != null) dropHint.setText("Suelta aquí ↓");
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
                    if (dropHint != null) dropHint.setText("o toca un botón");
                    return true;
                default: return true;
            }
        });
    }

    private void triggerConfetti(FrameLayout container) {
        if (container == null) return;
        Random random = new Random();
        for (int i = 0; i < 25; i++) {
            View p = new View(this);
            int size = dpToPx(random.nextInt(8) + 4);
            p.setLayoutParams(new FrameLayout.LayoutParams(size, size));
            p.setBackgroundColor(Color.HSVToColor(new float[]{random.nextInt(360), 0.8f, 1f}));
            p.setX(container.getWidth() / 2f); p.setY(container.getHeight() / 2f);
            container.addView(p);
            p.animate().translationX(random.nextFloat() * container.getWidth())
                .translationY(random.nextFloat() * container.getHeight())
                .rotation(random.nextInt(360)).alpha(0f).setDuration(1500)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() { 
                    @Override public void onAnimationEnd(Animator a) { container.removeView(p); } 
                }).start();
        }
    }

    private void handleResult(ExerciseViewModel.ExerciseResult res) {
        if (res == ExerciseViewModel.ExerciseResult.CORRECT || res == ExerciseViewModel.ExerciseResult.CORRECT_WITH_HINT) {
            showResultDialog(true, res == ExerciseViewModel.ExerciseResult.CORRECT_WITH_HINT);
        } else if (res == ExerciseViewModel.ExerciseResult.INCORRECT) {
            showResultDialog(false, false);
        } else if (res == ExerciseViewModel.ExerciseResult.EMPTY_INPUT) {
            Toast.makeText(this, "Ingresa una respuesta", Toast.LENGTH_SHORT).show();
        }
    }

    private void showResultDialog(boolean correct, boolean withHint) {
        Exercise ex = vm.exercise.getValue();
        int pts = ex != null ? (withHint ? ex.pointsHint : ex.pointsCorrect) : (withHint ? 50 : 100);
        
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this);
        if (correct) {
            String msg = "¡Bien hecho!\n+" + pts + " puntos";
            if (withHint) msg += "\n\nUsaste pista. ¡Intenta sin pista la próxima para más puntos!";
            b.setTitle("🎉 ¡Correcto!")
             .setMessage(msg)
             .setPositiveButton(isLastStep() ? "Finalizar" : "Siguiente →", (d, w) -> goToNext())
             .setCancelable(false);
        } else {
            b.setTitle("🤔 Intenta de nuevo")
             .setMessage("Revisa tus pasos para encontrar el error.")
             .setPositiveButton("Reintentar", (d, w) -> {
                 etAnswer.setText("");
                 vm.retryCurrentExercise();
             })
             .setNegativeButton("Salir", (d, w) -> finish())
             .setCancelable(false);
        }
        b.show();
    }

    private void showTimeUpDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("⏰ ¡Tiempo agotado!")
            .setMessage("No te rindas, ¡vuelve a intentarlo!")
            .setPositiveButton("Reintentar", (d, w) -> {
                etAnswer.setText("");
                vm.retryCurrentExercise();
            })
            .setNegativeButton("Salir", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }

    private boolean isLastStep() {
        return stepOrder >= AppState.getInstance().getModuleExerciseCount(moduleId);
    }

    private void goToNext() {
        if (isLastStep()) {
            Intent i = new Intent(this, FinEjerciciosActivity.class);
            i.putExtra("module_id", moduleId);
            startActivity(i);
        } else {
            Intent i = new Intent(this, ExerciseActivity.class);
            i.putExtra("module_id", moduleId);
            i.putExtra("step_order", stepOrder + 1);
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

    private void showHint() {
        Exercise ex = vm.exercise.getValue();
        if (ex == null) return;
        vm.useHint();
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_hint, null);
        String hintContent = ex.hintText != null ? ex.hintText : "";
        if (Exercise.TYPE_TILES.equals(ex.type)) {
            String nextOp = vm.expectedTileOp();
            if (!nextOp.isEmpty()) hintContent += "\n\n💡 Sugerencia: Intenta aplicar " + nextOp;
        }
        ((TextView) v.findViewById(R.id.tv_hint_content)).setText(hintContent);
        v.findViewById(R.id.btn_close_hint).setOnClickListener(b -> sheet.dismiss());
        sheet.setContentView(v);
        sheet.show();
    }

    private int dpToPx(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }
    private int resolveThemeColor(int attr) { TypedValue tv = new TypedValue(); getTheme().resolveAttribute(attr, tv, true); return tv.data; }

    @Override protected void onDestroy() { super.onDestroy(); vm.cancelTimer(); }
}
