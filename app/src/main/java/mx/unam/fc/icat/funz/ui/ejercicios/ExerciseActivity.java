package mx.unam.fc.icat.funz.ui.ejercicios;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Random;

import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.db.Exercise;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.utils.AppIntentKeys;
import mx.unam.fc.icat.funz.viewmodel.ExerciseViewModel;

/**
 * ExerciseActivity — IU Refactorizada.
 * Gestiona el ciclo de vida del ejercicio, temporizador, navegación y pistas dinámicas.
 * Delega la lógica visual a BalanzaFragment, TilesFragment y ClasicoFragment.
 */
public class ExerciseActivity extends AppCompatActivity {

    private ExerciseViewModel vm;
    private Chip chipTimer;
    private EditText etAnswer;
    private LinearLayout drawerMenu;
    private FrameLayout panelContainer;
    private FrameLayout celebrationLayer;
    private View loadingView;
    private ToneGenerator toneGenerator;
    private int moduleId;
    private int stepOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_exercise);

        moduleId = getIntent().getIntExtra(AppIntentKeys.MODULE_ID, 1);
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
        chipTimer = findViewById(R.id.tv_timer);
        etAnswer = findViewById(R.id.et_answer);
        panelContainer = findViewById(R.id.panel_container);
        celebrationLayer = findViewById(R.id.celebration_layer);
        loadingView = findViewById(R.id.loading_view);
        drawerMenu = findViewById(R.id.drawer_menu);
        drawerMenu.setVisibility(View.GONE);
        findViewById(R.id.btn_back).setOnClickListener(v -> { vm.cancelTimer(); finish(); });
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        findViewById(R.id.btn_verify).setOnClickListener(v -> vm.verify(etAnswer.getText().toString()));
    }

    private void observeViewModel() {
        vm.loading.observe(this, loading -> loadingView.setVisibility(loading ? View.VISIBLE : View.GONE));
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
        vm.timerFinished.observe(this, v -> showTimeoutDialog());
    }

    private void showPanelForType(Exercise exercise) {
        Fragment fragment;
        switch (exercise.type) {
            case Exercise.TYPE_BALANZA: fragment = new BalanzaFragment(); break;
            case Exercise.TYPE_CLASICO: fragment = new ClasicoFragment(); break;
            case Exercise.TYPE_TILES:   fragment = new TilesFragment();   break;
            default: return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.panel_container, fragment)
                .commit();
    }

    private void handleResult(ExerciseViewModel.ExerciseResult res) {
        if (res == ExerciseViewModel.ExerciseResult.CORRECT || 
            res == ExerciseViewModel.ExerciseResult.CORRECT_WITH_HINT || 
            res == ExerciseViewModel.ExerciseResult.REVEALED) {
            
            if (res != ExerciseViewModel.ExerciseResult.REVEALED) playSuccessFeedback();
            showResultDialog(res);
        } else if (res == ExerciseViewModel.ExerciseResult.INCORRECT) {
            playErrorFeedback();
            showResultDialog(res);
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

    private void playFinalSuccessSound() {
        playTone(ToneGenerator.TONE_PROP_ACK, 85);
    }

    private void playFinalErrorSound() {
        playTone(ToneGenerator.TONE_PROP_NACK, 70);
    }

    private void playTone(int toneType, int durationMs) {
        if (!isAudioFeedbackEnabled()) return;
        ToneGenerator tg = getToneGenerator();
        if (tg != null) tg.startTone(toneType, durationMs);
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

    private boolean isHapticFeedbackEnabled() { return AppState.getInstance().isHapticFeedbackEnabled(); }
    private boolean isAudioFeedbackEnabled() { return AppState.getInstance().isAudioFeedbackEnabled(); }

    private void vibratePattern(long[] timings, int[] amplitudes) {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            vibrator = (manager != null) ? manager.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
    }

    private void playSuccessCelebration() {
        if (panelContainer != null) {
            panelContainer.animate().scaleX(1.03f).scaleY(1.03f).setDuration(140)
                    .withEndAction(() -> panelContainer.animate().scaleX(1f).scaleY(1f).setDuration(180).start()).start();
        }
        if (celebrationLayer != null) triggerConfetti(celebrationLayer);
    }

    private void triggerConfetti(FrameLayout container) {
        if (container == null) return;
        container.post(() -> {
            int width = container.getWidth();
            int height = container.getHeight();
            if (width == 0 || height == 0) return;
            Random random = new Random();
            float density = getResources().getDisplayMetrics().density;
            for (int i = 0; i < 25; i++) {
                View p = new View(this);
                int size = (int) ((random.nextInt(8) + 4) * density);
                p.setLayoutParams(new FrameLayout.LayoutParams(size, size));
                p.setBackgroundColor(Color.HSVToColor(new float[]{random.nextInt(360), 0.8f, 1f}));
                p.setX(width / 2f); p.setY(height / 2f);
                container.addView(p);
                p.animate().translationX(random.nextFloat() * width).translationY(random.nextFloat() * height)
                        .rotation(random.nextInt(360)).alpha(0f).setDuration(1500)
                        .setInterpolator(new AccelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override public void onAnimationEnd(Animator a) { container.removeView(p); }
                        }).start();
            }
        });
    }

    private void showResultDialog(ExerciseViewModel.ExerciseResult result) {
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this);
        if (result == ExerciseViewModel.ExerciseResult.INCORRECT) {
            b.setTitle(R.string.dialog_incorrect_title).setMessage(R.string.dialog_incorrect_message)
                    .setPositiveButton(R.string.btn_retry, (d, w) -> { etAnswer.setText(""); vm.retryCurrentExercise(); })
                    .setNegativeButton(R.string.btn_exit_text_plain, (d, w) -> finish())
                    .setCancelable(false).show();
            return;
        }

        int points = 0;
        if (result == ExerciseViewModel.ExerciseResult.CORRECT) points = 100;
        else if (result == ExerciseViewModel.ExerciseResult.CORRECT_WITH_HINT) points = 50;

        String title = (result == ExerciseViewModel.ExerciseResult.REVEALED) ? "Respuesta Revelada" : getString(R.string.result_correct);
        String msg = (result == ExerciseViewModel.ExerciseResult.REVEALED) ?
                "Has revelado la respuesta. No se han sumado puntos en este ejercicio." :
                getString(R.string.dialog_correct_points_format, points);

        if (result == ExerciseViewModel.ExerciseResult.CORRECT_WITH_HINT) {
            msg += getString(R.string.dialog_correct_hint_extra);
        }

        b.setTitle(title).setMessage(msg)
                .setPositiveButton(isLastStep() ? getString(R.string.btn_finish) : getString(R.string.btn_next_arrow), (d, w) -> goToNext())
                .setCancelable(false).show();
    }

    private void showTimeoutDialog() {
        playFinalErrorHaptic();
        playFinalErrorSound();
        new MaterialAlertDialogBuilder(this).setTitle(R.string.result_timeout).setMessage(R.string.dialog_timeup_message)
                .setPositiveButton(R.string.btn_retry, (d, w) -> { etAnswer.setText(""); vm.retryCurrentExercise(); })
                .setNegativeButton(R.string.btn_exit_text_plain, (d, w) -> finish())
                .setCancelable(false).show();
    }

    private boolean isLastStep() { return stepOrder >= AppState.getInstance().getModuleExerciseCount(moduleId); }

    private void goToNext() {
        Intent i = isLastStep() ? new Intent(this, FinEjerciciosActivity.class) : new Intent(this, ExerciseActivity.class);
        i.putExtra(AppIntentKeys.MODULE_ID, moduleId);
        if (!isLastStep()) {
            i.putExtra(AppIntentKeys.STEP_ORDER, stepOrder + 1);
            i.putExtra(AppIntentKeys.SESSION_CONTINUE, true);
        }
        startActivity(i);
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
        vm.generateSmartHint();
        String msg = vm.hintMessage.getValue();
        if (msg == null) return;
        vm.useHint();
        new MaterialAlertDialogBuilder(this)
                .setTitle("💡 Guía de ayuda")
                .setMessage(msg)
                .setPositiveButton("¡Entendido!", null)
                .setNeutralButton("Ver respuesta (0 pts)", (dialog, which) -> {
                    vm.revealAnswer();
                    playFinalErrorHaptic();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) { toneGenerator.release(); toneGenerator = null; }
        vm.cancelTimer();
    }
}
