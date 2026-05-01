package mx.unam.fc.icat.funz.viewmodel;

import android.os.CountDownTimer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.utils.SingleLiveEvent;

/**
 * EjercicioClasicoViewModel — ViewModel para la pantalla D2 (Método Clásico/Baldor).
 *
 * Ecuación fija: 3x + 5 = 20  →  x = 5
 *
 * En este ejercicio el desarrollo algebraico es estático (fijado en el XML),
 * por lo que el ViewModel solo gestiona:
 *   - Temporizador de cuenta regresiva.
 *   - Validación y evaluación de la respuesta del usuario.
 *   - Registro de puntos en AppState.
 *   - Emisión del resultado como evento de un solo disparo.
 */
public class EjercicioClasicoViewModel extends ViewModel {

    private static final long TIME_MS     = 120_000L;
    private static final int  CORRECT_ANS = 5;

    private final AppState     state  = AppState.getInstance();
    private       CountDownTimer timer;
    private       boolean        hintUsed    = false;
    private       boolean        initialized = false;

    // ── LiveData ──────────────────────────────────────────────────────────────

    private final MutableLiveData<String>  _timerDisplay = new MutableLiveData<>("2:00");
    public  final LiveData<String>          timerDisplay  = _timerDisplay;

    private final MutableLiveData<Boolean> _timerUrgent = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         timerUrgent  = _timerUrgent;

    // ── Eventos de un solo disparo ────────────────────────────────────────────

    private final SingleLiveEvent<ExerciseResult> _exerciseResult = new SingleLiveEvent<>();
    public  final LiveData<ExerciseResult>         exerciseResult  = _exerciseResult;

    private final SingleLiveEvent<Void> _timerFinished = new SingleLiveEvent<>();
    public  final LiveData<Void>         timerFinished  = _timerFinished;

    // ════════════════════════════════════════════════════════════════════════
    //  Inicialización
    // ════════════════════════════════════════════════════════════════════════

    public void init() {
        if (initialized) return;
        initialized = true;
        state.setHintUsedCla(false);
        hintUsed = false;
        startTimer();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Verificación de respuesta
    // ════════════════════════════════════════════════════════════════════════

    public void verify(String input) {
        if (input == null || input.trim().isEmpty()) {
            _exerciseResult.setValue(ExerciseResult.EMPTY_INPUT);
            return;
        }
        cancelTimer();

        boolean correct;
        try   { correct = Integer.parseInt(input.trim()) == CORRECT_ANS; }
        catch (NumberFormatException e) { correct = false; }

        state.markExerciseDone(2, correct, hintUsed);

        if (correct) {
            _exerciseResult.setValue(hintUsed
                    ? ExerciseResult.CORRECT_WITH_HINT
                    : ExerciseResult.CORRECT);
        } else {
            _exerciseResult.setValue(ExerciseResult.INCORRECT);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Pista y reintento
    // ════════════════════════════════════════════════════════════════════════

    public void useHint() {
        hintUsed = true;
        state.setHintUsedCla(true);
    }

    public boolean isHintUsed() { return hintUsed; }

    public void retryWithoutHint() {
        hintUsed = false;
        state.setHintUsedCla(false);
        startTimer();
    }

    public void retryAfterFailure() {
        startTimer();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Temporizador
    // ════════════════════════════════════════════════════════════════════════

    public void startTimer() {
        cancelTimer();
        _timerUrgent.setValue(false);
        timer = new CountDownTimer(TIME_MS, 1000) {
            @Override
            public void onTick(long ms) {
                long s = ms / 1000;
                _timerDisplay.postValue(String.format("%d:%02d", s / 60, s % 60));
                _timerUrgent.postValue(s <= 20);
            }
            @Override
            public void onFinish() {
                _timerDisplay.postValue("0:00");
                _timerFinished.postValue(null);
            }
        }.start();
    }

    public void cancelTimer() {
        if (timer != null) { timer.cancel(); timer = null; }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelTimer();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Enumeración de resultados
    // ════════════════════════════════════════════════════════════════════════

    public enum ExerciseResult {
        CORRECT,
        CORRECT_WITH_HINT,
        INCORRECT,
        EMPTY_INPUT
    }
}
