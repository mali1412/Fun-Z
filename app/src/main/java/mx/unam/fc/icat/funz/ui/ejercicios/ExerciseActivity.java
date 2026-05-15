package mx.unam.fc.icat.funz.ui.ejercicios;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipData;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.utils.AlgebraTokens;
import mx.unam.fc.icat.funz.utils.AppIntentKeys;
import mx.unam.fc.icat.funz.viewmodel.ExerciseViewModel;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.model.Termino;
import mx.unam.fc.icat.funz.R;

/**
 * ExerciseActivity — IU Experta con soporte para Balanza (Física/Alquimia),
 * Tiles (Arrastrables) y modo Clásico.
 */
public class ExerciseActivity extends AppCompatActivity {

    private static final String DRAG_LABEL_BALANZA_TILE = "balanza_tile";
    private static final String DRAG_LABEL_TILE = "tile";
    private static final String DRAG_LABEL_OP = "op";
    private static final String DRAG_PAYLOAD_SEPARATOR = "|";
    private static final String DRAG_PAYLOAD_SEPARATOR_REGEX = "\\|";
    private static final String DRAG_SIDE_SOURCE = "S";
    private static final String DRAG_SIDE_LEFT = "L";
    private static final String DRAG_SIDE_RIGHT = "R";

    private ExerciseViewModel vm;
    private Chip         chipTimer;
    private EditText     etAnswer;
    private LinearLayout drawerMenu;
    private FrameLayout  panelContainer;
    private FrameLayout  celebrationLayer;
    private View         loadingView;
    private View         currentPanel;
    private ToneGenerator toneGenerator;
    private int moduleId;
    private int stepOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_exercise);

        moduleId  = getIntent().getIntExtra(AppIntentKeys.MODULE_ID,  1);
        stepOrder = getIntent().getIntExtra(AppIntentKeys.STEP_ORDER, 1);
        if (!getIntent().getBooleanExtra(AppIntentKeys.SESSION_CONTINUE, false)) {
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
        celebrationLayer = findViewById(R.id.celebration_layer);
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
            updateToolbarTitle();
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
            case Exercise.TYPE_BALANZA: bindBalanzaPanel(); break;
            case Exercise.TYPE_CLASICO: bindClasicoPanel(exercise); break;
            case Exercise.TYPE_TILES:   bindTilesPanel();   break;
        }
    }

    private void bindBalanzaPanel() {
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
            int color = ContextCompat.getColor(this, balanced ? R.color.success : R.color.warning);
            ivBase.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            if (balanced) {
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                tvStatus.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
                        .withEndAction(() -> tvStatus.animate().scaleX(1f).scaleY(1f).start()).start();
                triggerConfetti(confettiContainer);
            } else {
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
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
                labels.add(AlgebraTokens.X);
                labels.add(AlgebraTokens.NEG_X);
                labels.add(AlgebraTokens.POS_ONE);
                labels.add(AlgebraTokens.NEG_ONE);
                for (String op : opList) {
                    String clean = op.replace(" ", "")
                            .replace(AlgebraTokens.MINUS_SIGN, AlgebraTokens.MINUS)
                            .replace(AlgebraTokens.EN_DASH, AlgebraTokens.MINUS);
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
                    String label = parseDragPayload(raw)[0];
                    // Ahora aplicamos a ambos lados para mantener la igualdad
                    String op = label;
                    if (label.equals(AlgebraTokens.X)) op = AlgebraTokens.POS_X;
                    else if (!label.startsWith("+") && !label.startsWith("-")) op = "+" + label;
                    vm.applyOp(op);
                    playMoveSound();
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
                        if (raw.contains(DRAG_PAYLOAD_SEPARATOR)) {
                            String[] parts = parseDragPayload(raw);
                            String label = parts[0];
                            // Al tirar a la basura, restamos lo mismo de ambos lados para mantener la igualdad
                            String inverseOp;
                            if (label.equals(AlgebraTokens.X)) inverseOp = AlgebraTokens.NEG_X;
                            else if (label.startsWith("+")) inverseOp = "-" + label.substring(1);
                            else if (label.startsWith("-")) inverseOp = "+" + label.substring(1);
                            else inverseOp = "-" + label;
                            vm.applyOp(inverseOp);
                            playMoveSound();
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
        tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_body));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setSingleLine(true);
        tv.setIncludeFontPadding(false);
        int bg = label.startsWith(AlgebraTokens.MINUS)
                ? R.drawable.bg_tile_negative
                : (label.contains(AlgebraTokens.X_SYMBOL) ? R.drawable.bg_tile_x : R.drawable.bg_tile_positive);
        tv.setBackgroundResource(bg);

        int height = getResources().getDimensionPixelSize(R.dimen.balanza_tile_size_x);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
        lp.setMargins(dimenPx(R.dimen.margin_tiny), dimenPx(R.dimen.margin_tiny), dimenPx(R.dimen.margin_tiny), dimenPx(R.dimen.margin_tiny));
        tv.setLayoutParams(lp);
        tv.setHapticFeedbackEnabled(isHapticFeedbackEnabled());

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText(DRAG_LABEL_BALANZA_TILE, label + DRAG_PAYLOAD_SEPARATOR + DRAG_SIDE_SOURCE);
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
                String label = t.getCoeficiente() > 0 ? AlgebraTokens.X : AlgebraTokens.NEG_X;
                for (int i = 0; i < count; i++) {
                    addBalanzaWeightIcon(grid, bg, sizeX, label, isLeft, label);
                }
            } else if (t.esConstante()) {
                int val = t.getValor();
                if (val == 0) continue;

                int absVal = Math.abs(val);
                String unitLabel = val > 0 ? AlgebraTokens.POS_ONE : AlgebraTokens.NEG_ONE;
                int bg = val > 0 ? R.drawable.bg_tile_positive : R.drawable.bg_tile_negative;

                for (int i = 0; i < absVal; i++) {
                    addBalanzaWeightIcon(grid, bg, sizeUnit, unitLabel, isLeft, unitLabel);
                }
            }
        }
    }

    private void addBalanzaWeightIcon(GridLayout grid, int bgRes, int sizePx, String label, boolean isLeft, String displayText) {
        TextView tv = new TextView(this);
        tv.setBackgroundResource(bgRes);
        tv.setText(displayText);
        tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 0, 0, 0);
        tv.setIncludeFontPadding(false);
        tv.setSingleLine(true);
        // Texto muy pequeño para bloques de unidad
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(displayText.length() > 2
                        ? R.dimen.text_size_micro
                        : R.dimen.text_size_tiny));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = sizePx;
        lp.height = sizePx;
        lp.setMargins(dimenPx(R.dimen.margin_xsmall), dimenPx(R.dimen.margin_xsmall), dimenPx(R.dimen.margin_xsmall), dimenPx(R.dimen.margin_xsmall));
        tv.setLayoutParams(lp);
        grid.addView(tv);
        tv.setHapticFeedbackEnabled(isHapticFeedbackEnabled());

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText(DRAG_LABEL_BALANZA_TILE,
                    label + DRAG_PAYLOAD_SEPARATOR + (isLeft ? DRAG_SIDE_LEFT : DRAG_SIDE_RIGHT));
            v.startDragAndDrop(cd, new View.DragShadowBuilder(v), v, 0);
            return true;
        });
    }

    private void bindClasicoPanel(Exercise exercise) {
        TextView tvEquation = currentPanel.findViewById(R.id.tv_equation_display);
        LinearLayout llSteps = currentPanel.findViewById(R.id.ll_solution_steps);
        if (llSteps == null) {
            Log.e(getString(R.string.log_tag_error), getString(R.string.log_missing_steps_container));
            return;
        }
        tvEquation.setText(exercise.equation);
        List<String> steps = ExerciseViewModel.parseJson(exercise.solutionSteps);
        llSteps.removeAllViews();
        mostrarSiguientePaso(llSteps, steps, 0);
    }

    private void mostrarSiguientePaso(LinearLayout container, List<String> steps, int index) {
        if (index >= steps.size()) return;

        String stepText = steps.get(index);
        if (index == steps.size() - 1) {
            // ACTIVAMOS LA BARRA GLOBAL
            etAnswer.requestFocus(); // Ponemos el foco en el EditText de abajo
            // Opcionalmente podemos resaltar la barra global con un color para avisar al usuario
            findViewById(R.id.btn_verify).setAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
            return;
        }

        // Usaremos un delimitador como "|" para saber dónde va el cuadro de texto
        if (!stepText.contains("|")) {
            TextView tvInstruction = new TextView(this);
            tvInstruction.setText(stepText);
            tvInstruction.setPadding(dimenPx(R.dimen.margin_small), dimenPx(R.dimen.margin_small), dimenPx(R.dimen.margin_small), dimenPx(R.dimen.margin_tiny));
            tvInstruction.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            container.addView(tvInstruction);
            // Pasamos automáticamente al siguiente, que debería ser el que tiene el hueco
            mostrarSiguientePaso(container, steps, index + 1);
            return;
        }

        String[] parts = stepText.split("\\|");

        View stepView = getLayoutInflater().inflate(R.layout.item_step_clasico, container, false);
        TextView tvPre = stepView.findViewById(R.id.tv_step_prefix);
        EditText etInput = stepView.findViewById(R.id.et_step_input);
        TextView tvPost = stepView.findViewById(R.id.tv_step_suffix);
        Button btnCheck = stepView.findViewById(R.id.btn_step_verify);

        tvPre.setText(parts[0]);
        if (parts.length > 2) tvPost.setText(parts[2]);

        String correctAnswer = parts[1]; // La respuesta esperada está entre los | |

        btnCheck.setOnClickListener(v -> {
            if (etInput.getText().toString().trim().equals(correctAnswer)) {
                playStepSuccessHaptic();
                playStepSuccessSound();
                // ÉXITO
                etInput.setEnabled(false);
                etInput.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success_bg)));
                btnCheck.setVisibility(View.GONE);
                tvPre.setTextColor(getColor(R.color.accent_green));
                // Mostrar el siguiente paso
                mostrarSiguientePaso(container, steps, index + 1);
            } else {
                playStepErrorHaptic();
                playStepErrorSound();
                // ERROR
                etInput.setError(getString(R.string.input_error_incorrect));
                etInput.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake));
            }
        });

        container.addView(stepView);
    }

    private void bindTilesPanel() {
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
                tvSt.setTextColor(pos ? getColor(R.color.accent_green) : resolveThemeColor(R.attr.colorWarnChipText));
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
        tv.setTextColor(ContextCompat.getColor(this, R.color.white));
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
        tv.setHapticFeedbackEnabled(isHapticFeedbackEnabled());

        tv.setOnLongClickListener(v -> {
            ClipData cd = ClipData.newPlainText(DRAG_LABEL_TILE, label);
            v.startDragAndDrop(cd, new View.DragShadowBuilder(v), v, 0);
            return true;
        });
        return tv;
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

    private TextView makeOpView(String op) {
        TextView tv = new TextView(this);
        tv.setText(op);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_op));
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setTextColor(ContextCompat.getColor(this, R.color.white));
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
        tv.setHapticFeedbackEnabled(isHapticFeedbackEnabled());

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
                        playMoveSound();
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

    private void triggerConfetti(FrameLayout container) {
        if (container == null) return;
        container.post(() -> {
            int width = container.getWidth();
            int height = container.getHeight();
            if (width == 0 || height == 0) return;
            Random random = new Random();
            for (int i = 0; i < 25; i++) {
                View p = new View(this);
                int size = dpToPx(random.nextInt(8) + 4);
                p.setLayoutParams(new FrameLayout.LayoutParams(size, size));
                p.setBackgroundColor(Color.HSVToColor(new float[]{random.nextInt(360), 0.8f, 1f}));
                p.setX(width / 2f);
                p.setY(height / 2f);
                container.addView(p);
                p.animate().translationX(random.nextFloat() * width)
                    .translationY(random.nextFloat() * height)
                    .rotation(random.nextInt(360)).alpha(0f).setDuration(1500)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override public void onAnimationEnd(Animator a) { container.removeView(p); }
                    }).start();
            }
        });
    }

    private void handleResult(ExerciseViewModel.ExerciseResult res) {
        if (res == ExerciseViewModel.ExerciseResult.CORRECT || res == ExerciseViewModel.ExerciseResult.CORRECT_WITH_HINT) {
            playSuccessFeedback();
            showResultDialog(true, res == ExerciseViewModel.ExerciseResult.CORRECT_WITH_HINT);
        } else if (res == ExerciseViewModel.ExerciseResult.INCORRECT) {
            playErrorFeedback();
            showResultDialog(false, false);
        } else if (res == ExerciseViewModel.ExerciseResult.EMPTY_INPUT) {
            Toast.makeText(this, getString(R.string.toast_enter_answer), Toast.LENGTH_SHORT).show();
        }
    }

    private void playSuccessFeedback() {
        playFinalSuccessHaptic();
        playFinalSuccessSound();
        playSuccessCelebration();
    }

    private void playErrorFeedback() {
        playFinalErrorHaptic();
        playFinalErrorSound();
    }

    private void playStepSuccessHaptic() {
        if (!isHapticFeedbackEnabled()) return;
        View anchor = panelContainer != null ? panelContainer : findViewById(android.R.id.content);
        if (anchor != null) anchor.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        vibratePattern(new long[]{0, 18}, new int[]{0, 110});
    }

    private void playStepErrorHaptic() {
        if (!isHapticFeedbackEnabled()) return;
        View anchor = etAnswer != null ? etAnswer : findViewById(android.R.id.content);
        if (anchor != null) anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        vibratePattern(new long[]{0, 34, 18, 26}, new int[]{0, 120, 0, 90});
    }

    private void playFinalSuccessHaptic() {
        if (!isHapticFeedbackEnabled()) return;
        View anchor = panelContainer != null ? panelContainer : findViewById(android.R.id.content);
        if (anchor != null) anchor.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        vibratePattern(new long[]{0, 28, 40, 36}, new int[]{0, 170, 0, 220});
    }

    private void playFinalErrorHaptic() {
        if (!isHapticFeedbackEnabled()) return;
        View anchor = etAnswer != null ? etAnswer : findViewById(android.R.id.content);
        if (anchor != null) anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        vibratePattern(new long[]{0, 80, 30, 60}, new int[]{0, 180, 0, 120});
    }

    private void playStepSuccessSound() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 30);
    }

    private void playMoveSound() {
        // Sonido corto tipo tecla para confirmar movimiento sin ser invasivo.
        playTone(ToneGenerator.TONE_PROP_BEEP, 22);
    }

    private void playStepErrorSound() {
        playTone(ToneGenerator.TONE_PROP_NACK, 45);
    }

    private void playFinalSuccessSound() {
        playTone(ToneGenerator.TONE_PROP_ACK, 85);
    }

    private void playFinalErrorSound() {
        playTone(ToneGenerator.TONE_PROP_NACK, 70);
    }

    private ToneGenerator getToneGenerator() {
        if (toneGenerator != null) return toneGenerator;
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 35);
        } catch (RuntimeException ignored) {
            toneGenerator = null;
        }
        return toneGenerator;
    }

    private void playTone(int toneType, int durationMs) {
        if (!isAudioFeedbackEnabled()) return;
        ToneGenerator tg = getToneGenerator();
        if (tg != null) tg.startTone(toneType, durationMs);
    }

    private boolean isHapticFeedbackEnabled() {
        return AppState.getInstance().isHapticFeedbackEnabled();
    }

    private boolean isAudioFeedbackEnabled() {
        return AppState.getInstance().isAudioFeedbackEnabled();
    }

    private void vibratePattern(long[] timings, int[] amplitudes) {
        Vibrator vibrator = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            if (manager != null) vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;

        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
    }

    private void playSuccessCelebration() {
        if (panelContainer != null) {
            panelContainer.animate()
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .setDuration(140)
                    .withEndAction(() -> panelContainer.animate().scaleX(1f).scaleY(1f).setDuration(180).start())
                    .start();
        }
        if (celebrationLayer != null) {
            triggerConfetti(celebrationLayer);
        }
    }

    private void showResultDialog(boolean correct, boolean withHint) {
        Exercise ex = vm.exercise.getValue();
        int pts = ex != null ? (withHint ? ex.pointsHint : ex.pointsCorrect) : (withHint ? 50 : 100);
        
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this);
        if (correct) {
            String msg = getString(R.string.dialog_correct_points_format, pts);
            if (withHint) msg += getString(R.string.dialog_correct_hint_extra);
            b.setTitle(R.string.result_correct)
             .setMessage(msg)
             .setPositiveButton(isLastStep() ? getString(R.string.btn_finish) : getString(R.string.btn_next_arrow), (d, w) -> goToNext())
             .setCancelable(false);
        } else {
            b.setTitle(R.string.dialog_incorrect_title)
             .setMessage(R.string.dialog_incorrect_message)
             .setPositiveButton(R.string.btn_retry, (d, w) -> {
                 etAnswer.setText("");
                 vm.retryCurrentExercise();
             })
             .setNegativeButton(R.string.btn_exit_text_plain, (d, w) -> finish())
             .setCancelable(false);
        }
        b.show();
    }

    private boolean isLastStep() {
        return stepOrder >= AppState.getInstance().getModuleExerciseCount(moduleId);
    }

    private void goToNext() {
        if (isLastStep()) {
            Intent i = new Intent(this, FinEjerciciosActivity.class);
            i.putExtra(AppIntentKeys.MODULE_ID, moduleId);
            startActivity(i);
        } else {
            Intent i = new Intent(this, ExerciseActivity.class);
            i.putExtra(AppIntentKeys.MODULE_ID, moduleId);
            i.putExtra(AppIntentKeys.STEP_ORDER, stepOrder + 1);
            i.putExtra(AppIntentKeys.SESSION_CONTINUE, true);
            startActivity(i);
        }
        overridePendingTransition(R.anim.screen_enter_right, R.anim.screen_exit_left);
        finish();
    }

    private void updateToolbarTitle() {
        TextView tvTitle = findViewById(R.id.tv_toolbar_title);
        if (tvTitle != null) tvTitle.setText(getString(R.string.toolbar_module_exercise_format, moduleId, stepOrder));
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
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_hint, new FrameLayout(this), false);
        String hintContent = ex.hintText;
        if (Exercise.TYPE_TILES.equals(ex.type)) {
            String nextOp = vm.expectedTileOp();
            if (!nextOp.isEmpty()) hintContent += getString(R.string.hint_tile_suggestion_format, nextOp);
        }
        ((TextView) v.findViewById(R.id.tv_hint_content)).setText(hintContent);
        v.findViewById(R.id.btn_close_hint).setOnClickListener(b -> sheet.dismiss());
        sheet.setContentView(v);
        sheet.show();
    }

    private int dpToPx(int dp) { return (int)(dp * getResources().getDisplayMetrics().density); }
    private int dimenPx(int dimenRes) { return getResources().getDimensionPixelSize(dimenRes); }
    private int resolveThemeColor(int attr) { TypedValue tv = new TypedValue(); getTheme().resolveAttribute(attr, tv, true); return tv.data; }

    private String[] parseDragPayload(String raw) {
        return raw.split(DRAG_PAYLOAD_SEPARATOR_REGEX, 2);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        vm.cancelTimer();
    }
}
