package mx.unam.fc.icat.funz.viewmodel;

import android.os.CountDownTimer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.utils.SingleLiveEvent;

/**
 * EjercicioTilesViewModel — ViewModel para la pantalla D3 (Algebra Tiles).
 *
 * Ecuación fija: x + 2 = 12  →  x = 10
 *
 * Responsabilidades:
 *   - Mantener las listas de tiles de cada lado de la balanza.
 *   - Ejecutar la lógica de par cero (moveTile) pura de Java.
 *   - Detectar el auto-completado cuando solo queda x en el lado izquierdo.
 *   - Controlar el temporizador.
 *   - Evaluar la respuesta final y delegar puntos a AppState.
 *
 * La Activity solo construye vistas a partir de las listas observadas
 * y maneja el Drag & Drop de la UI; no contiene lógica matemática.
 */
public class EjercicioTilesViewModel extends ViewModel {

    private static final long TIME_MS     = 120_000L;
    private static final int  CORRECT_ANS = 10;

    private final AppState     state  = AppState.getInstance();
    private       CountDownTimer timer;
    private       boolean        hintUsed    = false;
    private       boolean        initialized = false;

    // ── Estado de tiles ───────────────────────────────────────────────────────

    // Listas mutables internas (la Activity no las modifica directamente)
    private final List<String> leftTiles  = new ArrayList<>();
    private final List<String> rightTiles = new ArrayList<>();

    // ── LiveData ──────────────────────────────────────────────────────────────

    /**
     * Copia inmutable del lado izquierdo. Se emite una nueva List en cada cambio
     * para disparar la observación (LiveData compara por referencia).
     */
    private final MutableLiveData<List<String>> _leftTiles = new MutableLiveData<>();
    public  final LiveData<List<String>>         leftTilesLd  = _leftTiles;

    private final MutableLiveData<List<String>> _rightTiles = new MutableLiveData<>();
    public  final LiveData<List<String>>         rightTilesLd  = _rightTiles;

    /** Mensaje de retroalimentación al mover tiles. */
    private final MutableLiveData<String>  _statusMessage  = new MutableLiveData<>("");
    public  final LiveData<String>          statusMessage   = _statusMessage;

    /** true = verde (positivo), false = naranja (negativo). */
    private final MutableLiveData<Boolean> _statusPositive = new MutableLiveData<>(true);
    public  final LiveData<Boolean>         statusPositive  = _statusPositive;

    /** Valor calculado para el auto-completado; null = sin cambio. */
    private final MutableLiveData<String>  _autoAnswer = new MutableLiveData<>(null);
    public  final LiveData<String>          autoAnswer  = _autoAnswer;

    private final MutableLiveData<String>  _timerDisplay = new MutableLiveData<>("2:00");
    public  final LiveData<String>          timerDisplay  = _timerDisplay;

    private final MutableLiveData<Boolean> _timerUrgent = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         timerUrgent  = _timerUrgent;

    // ── Eventos ───────────────────────────────────────────────────────────────

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
        state.setHintUsedTil(false);
        hintUsed = false;
        initTiles();
        startTimer();
    }

    private void initTiles() {
        leftTiles.clear();
        rightTiles.clear();
        leftTiles.add("x");
        leftTiles.add("+1");
        leftTiles.add("+1");
        for (int i = 0; i < 12; i++) rightTiles.add("+1");
        publishTiles();
    }

    /** Reinicia el ejercicio al estado inicial (usado en "Reintentar"). */
    public void resetTiles() {
        initTiles();
        startTimer();
        _autoAnswer.setValue(null);
        _statusMessage.setValue("");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Lógica de movimiento de tiles (par cero)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Mueve un tile al lado contrario aplicando el principio de par cero.
     *
     * @param side  "L" (izquierdo) o "R" (derecho)
     * @param idx   Índice del tile en su lista
     * @param label Valor del tile ("x", "+1", "-1")
     */
    public void moveTile(String side, int idx, String label) {
        if (side.equals("L")) {
            if (idx >= leftTiles.size()) return;
            leftTiles.remove(idx);

            int posR = rightTiles.indexOf("+1");
            if (posR >= 0) {
                rightTiles.remove(posR);
                setStatus("¡Par cero! El +1 izquierdo cancela un +1 derecho.", true);
            } else {
                rightTiles.add("-1");
                setStatus("Se añadió un −1 al lado derecho.", true);
            }
        } else {
            if (idx >= rightTiles.size()) return;
            rightTiles.remove(idx);

            int posL = -1;
            for (int i = 0; i < leftTiles.size(); i++) {
                if (!leftTiles.get(i).equals("x")) { posL = i; break; }
            }
            if (posL >= 0) {
                leftTiles.remove(posL);
                setStatus("¡Par cero! Tile del derecho cancela uno del izquierdo.", true);
            } else {
                leftTiles.add("-1");
                setStatus("Se añadió un −1 al lado izquierdo.", false);
            }
        }
        publishTiles();
        checkAutoComplete();
    }

    /**
     * Detecta si solo queda x en el lado izquierdo y calcula el valor de x.
     * Emite el valor calculado para que la Activity autocomplete el EditText.
     */
    private void checkAutoComplete() {
        long constL = leftTiles.stream().filter(t -> !t.equals("x")).count();
        boolean hasX = leftTiles.contains("x");

        if (hasX && constL == 0) {
            long pos = rightTiles.stream().filter("+1"::equals).count();
            long neg = rightTiles.stream().filter("-1"::equals).count();
            int  val = (int)(pos - neg);
            _autoAnswer.setValue(String.valueOf(val));
            setStatus("x = " + val + " · Presiona Verificar.", true);
        }
    }

    /** Publica copias nuevas de ambas listas para disparar la observación. */
    private void publishTiles() {
        _leftTiles.setValue(new ArrayList<>(leftTiles));
        _rightTiles.setValue(new ArrayList<>(rightTiles));
    }

    private void setStatus(String msg, boolean positive) {
        _statusMessage.setValue(msg);
        _statusPositive.setValue(positive);
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

        state.markExerciseDone(3, correct, hintUsed);

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
        state.setHintUsedTil(true);
    }

    public boolean isHintUsed() { return hintUsed; }

    public void retryWithoutHint() {
        hintUsed = false;
        state.setHintUsedTil(false);
        resetTiles();
    }

    public void retryAfterFailure() {
        resetTiles();
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
