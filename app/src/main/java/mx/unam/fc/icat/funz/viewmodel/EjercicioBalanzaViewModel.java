package mx.unam.fc.icat.funz.viewmodel;

import android.os.CountDownTimer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.db.Exercise;
import mx.unam.fc.icat.funz.utils.SingleLiveEvent;

/**
 * EjercicioBalanzaViewModel — ViewModel para la pantalla D1 (Método Balanza).
 *
 * Responsabilidades:
 *   - Gestionar el estado de la balanza (expresiones LHS/RHS, equilibrio).
 *   - Controlar el temporizador de cuenta regresiva.
 *   - Evaluar la respuesta del usuario y delegar puntos a AppState.
 *   - Exponer el resultado del ejercicio mediante un SingleLiveEvent
 *     para que la Activity muestre el AlertDialog correspondiente.
 *
 * La Activity solo observa LiveData y llama a métodos públicos del ViewModel;
 * no contiene lógica matemática ni de gamificación.
 */
public class EjercicioBalanzaViewModel extends ViewModel {

    // ── Constantes ────────────────────────────────────────────────────────────
    private static final long TIME_MS     = 120_000L;
    private static final int  CORRECT_ANS = 5;

    // ── Estado interno ────────────────────────────────────────────────────────
    private final AppState state = AppState.getInstance();
    private CountDownTimer timer;
    private boolean        hintUsed   = false;
    private boolean        initialized = false;

    // ── LiveData expuesta a la View ──────────────────────────────────────────

    /** Expresión del plato izquierdo de la balanza (p.ej. "x+5" o "x"). */
    private final MutableLiveData<String>  _lhsExpr       = new MutableLiveData<>("x+5");
    public  final LiveData<String>          lhsExpr        = _lhsExpr;

    /** Expresión del plato derecho de la balanza (p.ej. "10" o "5"). */
    private final MutableLiveData<String>  _rhsExpr       = new MutableLiveData<>("10");
    public  final LiveData<String>          rhsExpr        = _rhsExpr;

    /** Mensaje de estado que se muestra debajo de la imagen de la balanza. */
    private final MutableLiveData<String>  _statusMessage = new MutableLiveData<>("Aplica −5 a ambos lados para aislar x");
    public  final LiveData<String>          statusMessage  = _statusMessage;

    /**
     * Indica si el estado es positivo (verde), negativo (naranja) o neutro (null).
     * La Activity usa este valor para colorear tvStatus.
     */
    private final MutableLiveData<Boolean> _statusPositive = new MutableLiveData<>(null);
    public  final LiveData<Boolean>         statusPositive  = _statusPositive;

    /** true cuando la operación −5 se ha aplicado y la balanza está equilibrada. */
    private final MutableLiveData<Boolean> _balanced = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         balanced  = _balanced;

    /** Texto del chip del temporizador ("2:00", "1:45", …). */
    private final MutableLiveData<String>  _timerDisplay = new MutableLiveData<>("2:00");
    public  final LiveData<String>          timerDisplay  = _timerDisplay;

    /** true cuando quedan ≤ 20 s para colorear el chip en rojo. */
    private final MutableLiveData<Boolean> _timerUrgent = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         timerUrgent  = _timerUrgent;

    /**
     * Valor que se auto-completa en el EditText cuando la operación correcta se aplica.
     * La Activity observa y setea el texto del campo; emite "" para limpiar.
     */
    private final MutableLiveData<String>  _autoAnswer = new MutableLiveData<>("");
    public  final LiveData<String>          autoAnswer  = _autoAnswer;

    // ── Eventos de un solo disparo ───────────────────────────────────────────

    /** Resultado del ejercicio: CORRECTO, CORRECTO_CON_PISTA, INCORRECTO, ENTRADA_VACIA. */
    private final SingleLiveEvent<ExerciseResult> _exerciseResult = new SingleLiveEvent<>();
    public  final LiveData<ExerciseResult>         exerciseResult  = _exerciseResult;

    /** Se dispara cuando el temporizador llega a cero. */
    private final SingleLiveEvent<Void>   _timerFinished = new SingleLiveEvent<>();
    public  final LiveData<Void>           timerFinished  = _timerFinished;

    // ════════════════════════════════════════════════════════════════════════
    //  Inicialización
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Inicializa el ejercicio. Idempotente: solo se ejecuta una vez por instancia
     * del ViewModel para evitar reiniciar el timer tras una rotación de pantalla.
     */
    public void init() {
        if (initialized) return;
        initialized = true;
        state.setHintUsedBal(false);
        hintUsed = false;
        resetBalanza();
        startTimer();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Lógica de la Balanza
    // ════════════════════════════════════════════════════════════════════════

    /** Reinicia las expresiones de la balanza al estado inicial del ejercicio. */
    public void resetBalanza() {
        _lhsExpr.setValue("x+5");
        _rhsExpr.setValue("10");
        _balanced.setValue(false);
        _statusMessage.setValue("Aplica −5 a ambos lados para aislar x");
        _statusPositive.setValue(null);
        _autoAnswer.setValue("");
    }

    public void applyOp(String op) {
        // PUNTO 1: Interacción Dinámica (Equilibrio)
        // En este ejercicio específico, la operación correcta es "-5"
        if (op.equals("-5")) {
            // PUNTO 2: Sincronización del estado Visual
            _lhsExpr.setValue("x");
            _rhsExpr.setValue("5");

            _balanced.setValue(true); // Activa el feedback visual (Balanza equilibrada)
            _statusMessage.setValue("✓ ¡Equilibrada! x + 5 - 5 = 10 - 5. Ahora ingresa el valor de x.");
            _statusPositive.setValue(true);

            // Auto-completado (Sincronización de datos)
            _autoAnswer.setValue(String.valueOf(CORRECT_ANS));
        } else {
            // Feedback de error en tiempo real
            _balanced.setValue(false);
            _statusMessage.setValue("Esa operación no despeja la x. ¡Intenta restar 5!");
            _statusPositive.setValue(false);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Verificación de respuesta
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Evalúa la respuesta ingresada por el usuario.
     * Delega el registro de puntos a AppState y emite el resultado
     * como evento para que la Activity muestre el diálogo apropiado.
     *
     * @param input Texto del EditText (puede ser null o vacío)
     */
    public void verify(String input) {
        if (input == null || input.trim().isEmpty()) {
            _exerciseResult.setValue(ExerciseResult.EMPTY_INPUT);
            return;
        }
        cancelTimer();

        boolean correct;
        try   { correct = Integer.parseInt(input.trim()) == CORRECT_ANS; }
        catch (NumberFormatException e) { correct = false; }

        state.markExerciseDone(1, correct, hintUsed);

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

    /** Registra que el usuario solicitó la pista. */
    public void useHint() {
        hintUsed = true;
        state.setHintUsedBal(true);
    }

    public boolean isHintUsed() { return hintUsed; }

    /**
     * Reinicia el ejercicio descartando la penalización de pista.
     * Llamado cuando el usuario elige "Sin pista (+50)" en el diálogo.
     */
    public void retryWithoutHint() {
        hintUsed = false;
        state.setHintUsedBal(false);
        resetBalanza();
        startTimer();
    }

    /** Reinicia el ejercicio manteniendo el estado de pista. */
    public void retryAfterFailure() {
        resetBalanza();
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
