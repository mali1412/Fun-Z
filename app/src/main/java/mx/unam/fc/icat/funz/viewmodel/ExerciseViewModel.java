package mx.unam.fc.icat.funz.viewmodel;

import android.app.Application;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import mx.unam.fc.icat.funz.data.FunZApp;
import mx.unam.fc.icat.funz.db.Exercise;
import mx.unam.fc.icat.funz.model.Ecuacion;
import mx.unam.fc.icat.funz.model.TraductorEcuacion;
import mx.unam.fc.icat.funz.repository.AppStateRepository;
import mx.unam.fc.icat.funz.repository.ExerciseRepository;
import mx.unam.fc.icat.funz.utils.SingleLiveEvent;

/**
 * ExerciseViewModel — ViewModel genérico para cualquier tipo de ejercicio.
 */
public class ExerciseViewModel extends AndroidViewModel {

    private static final long TIME_MS = 120_000L;

    private final ExerciseRepository repo;
    private final AppStateRepository stateRepo;
    private volatile CountDownTimer   timer;
    private       boolean             initialized = false;
    private       boolean             hintUsed    = false;
    private       int                 totalSteps  = 3; 

    // ── Exercise cargado ──────────────────────────────────────────────────────

    private final MutableLiveData<Exercise> _exercise = new MutableLiveData<>();
    public  final LiveData<Exercise>         exercise  = _exercise;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(true);
    public  final LiveData<Boolean>         loading  = _loading;

    // ── Estado común ──────────────────────────────────────────────────────────

    private final MutableLiveData<String>  _timerDisplay = new MutableLiveData<>("2:00");
    public  final LiveData<String>          timerDisplay  = _timerDisplay;

    private final MutableLiveData<Boolean> _timerUrgent = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         timerUrgent  = _timerUrgent;

    private final MutableLiveData<String>  _statusMessage  = new MutableLiveData<>("");
    public  final LiveData<String>          statusMessage   = _statusMessage;

    private final MutableLiveData<Boolean> _statusPositive = new MutableLiveData<>(null);
    public  final LiveData<Boolean>         statusPositive  = _statusPositive;

    // ── Estado BALANZA ────────────────────────────────────────────────────────

    private final MutableLiveData<String>  _lhsExpr  = new MutableLiveData<>("");
    public  final LiveData<String>          lhsExpr   = _lhsExpr;

    private final MutableLiveData<String>  _rhsExpr  = new MutableLiveData<>("");
    public  final LiveData<String>          rhsExpr   = _rhsExpr;

    private final MutableLiveData<Boolean> _balanced = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         balanced  = _balanced;

    private final MutableLiveData<List<String>> _ops = new MutableLiveData<>(new ArrayList<>());
    public  final LiveData<List<String>>         ops  = _ops;

    // ── Estado TILES ──────────────────────────────────────────────────────────

    private final MutableLiveData<List<String>> _leftTiles  = new MutableLiveData<>(new ArrayList<>());
    public  final LiveData<List<String>>         leftTilesLd = _leftTiles;

    private final MutableLiveData<List<String>> _rightTiles  = new MutableLiveData<>(new ArrayList<>());
    public  final LiveData<List<String>>         rightTilesLd = _rightTiles;

    // ── Auto-completado (todos los tipos) ─────────────────────────────────────

    private final MutableLiveData<String> _autoAnswer = new MutableLiveData<>(null);
    public  final LiveData<String>         autoAnswer  = _autoAnswer;

    // ── Eventos de un solo disparo ────────────────────────────────────────────

    private final SingleLiveEvent<ExerciseResult> _exerciseResult = new SingleLiveEvent<>();
    public  final LiveData<ExerciseResult>         exerciseResult  = _exerciseResult;

    private final SingleLiveEvent<Void> _timerFinished = new SingleLiveEvent<>();
    public  final LiveData<Void>         timerFinished  = _timerFinished;

    // ── Listas mutables internas (Tiles) ──────────────────────────────────────
    private final List<String> leftTiles  = new ArrayList<>();
    private final List<String> rightTiles = new ArrayList<>();

    public ExerciseViewModel(@NonNull Application app) {
        super(app);
        FunZApp appScope = (FunZApp) app;
        repo = appScope.getExerciseRepository();
        stateRepo = appScope.getAppStateRepository();
    }

    public void loadExercise(int moduleId, int stepOrder) {
        if (initialized) return;

        // Persistir que este es el módulo activo para la funcionalidad de "Continuar"
        stateRepo.setActiveModuleId(moduleId);

        _loading.setValue(true);
        repo.countExercises(moduleId, count -> totalSteps = count);
        repo.loadExercise(moduleId, stepOrder, exercise -> {
            if (exercise == null) {
                _loading.postValue(false);
                return;
            }
            _exercise.postValue(exercise);
            _loading.postValue(false);
            initForExercise(exercise);
        });
    }

    private void initForExercise(Exercise ex) {
        if (initialized) return;
        initialized = true;
        hintUsed = false;

        switch (ex.type) {
            case Exercise.TYPE_BALANZA: initBalanza(ex); break;
            case Exercise.TYPE_TILES:   initTiles(ex);   break;
            case Exercise.TYPE_CLASICO: break;
        }
        startTimer();
    }

    private void initBalanza(Exercise ex) {
        _lhsExpr.postValue(ex.lhsExpr);
        _rhsExpr.postValue(ex.rhsExpr);
        _balanced.postValue(false);
        _statusMessage.postValue("Elige la operación para aislar x");
        _statusPositive.postValue(null);
        _ops.postValue(parseJson(ex.ops));
    }

    private void initTiles(Exercise ex) {
        leftTiles.clear();  leftTiles.addAll(parseJson(ex.tilesLeft));
        rightTiles.clear(); rightTiles.addAll(parseJson(ex.tilesRight));
        _leftTiles.postValue(new ArrayList<>(leftTiles));
        _rightTiles.postValue(new ArrayList<>(rightTiles));
        _statusMessage.postValue("Mueve los tiles para aislar x");
    }

    // En ExerciseViewModel.java

    public void applyOp(String op) {
        Exercise ex = _exercise.getValue();
        if (ex == null) return;

        // PUNTO 1: Configuración de controladores (Clic en operación)
        // Comparamos contra la operación correcta definida en la DB (DbSeeder)
        if (op.equals(ex.correctOp)) {
            // PUNTO 2: Sincronización del estado visual con el ViewModel
            _lhsExpr.setValue(ex.lhsAfterOp);
            _rhsExpr.setValue(ex.rhsAfterOp);

            _balanced.setValue(true); // Equilibrio visual (Punto 1)
            _statusMessage.setValue("✓ Ecuación equilibrada. ¡Bien hecho!");
            _statusPositive.setValue(true);

            // Sincronización: Autocompletado del valor numérico
            _autoAnswer.setValue(ex.correctAnswer);
        } else {
            // Feedback en tiempo real (Punto 3)
            _balanced.setValue(false);
            _statusMessage.setValue("Esa operación no aísla x. Intenta " + ex.correctOp);
            _statusPositive.setValue(false);
        }
    }

    public void resetBalanza() {
        Exercise ex = _exercise.getValue();
        if (ex == null) return;
        _lhsExpr.setValue(ex.lhsExpr);
        _rhsExpr.setValue(ex.rhsExpr);
        _balanced.setValue(false);
        _statusMessage.setValue("Elige la operación para aislar x");
        _statusPositive.setValue(null);
        _autoAnswer.setValue("");
    }

    public void moveTile(String side, int idx, String label) {
        if (side.equals("L")) {
            if (idx >= leftTiles.size()) return;
            leftTiles.remove(idx);
            int pos = rightTiles.indexOf(label);
            if (pos >= 0) {
                rightTiles.remove(pos);
                _statusMessage.postValue("Par cero: " + label + " cancela con el del derecho.");
                _statusPositive.postValue(true);
            } else {
                rightTiles.add(label.startsWith("+") ? label.replace("+", "-") : "+" + label.replace("-", ""));
                _statusMessage.postValue("Se añadió el inverso al lado derecho.");
                _statusPositive.postValue(true);
            }
        } else {
            if (idx >= rightTiles.size()) return;
            rightTiles.remove(idx);
            int posL = -1;
            for (int i = 0; i < leftTiles.size(); i++) {
                if (!leftTiles.get(i).equals("x") && !leftTiles.get(i).contains("/")) {
                    posL = i; break;
                }
            }
            if (posL >= 0) {
                leftTiles.remove(posL);
                _statusMessage.postValue("Par cero: tile derecho cancela con izquierdo.");
                _statusPositive.postValue(true);
            } else {
                leftTiles.add("-1");
                _statusMessage.postValue("Se añadió −1 al lado izquierdo.");
                _statusPositive.postValue(false);
            }
        }
        publishTiles();
        checkTilesAutoComplete();
    }

    private void checkTilesAutoComplete() {
        Exercise ex = _exercise.getValue();
        if (ex == null) return;
        boolean hasX = leftTiles.contains("x") || leftTiles.stream().anyMatch(t -> t.contains("/"));
        long constL  = leftTiles.stream().filter(t -> !t.equals("x") && !t.contains("/")).count();
        if (hasX && constL == 0) {
            long pos = rightTiles.stream().filter("+1"::equals).count();
            long neg = rightTiles.stream().filter("-1"::equals).count();
            int  val = (int)(pos - neg);
            _autoAnswer.postValue(String.valueOf(val));
            _statusMessage.postValue("x = " + val + " · Presiona Verificar.");
            _statusPositive.postValue(true);
        }
    }

    private void publishTiles() {
        _leftTiles.postValue(new ArrayList<>(leftTiles));
        _rightTiles.postValue(new ArrayList<>(rightTiles));

        // PUNTO 2 & 5: Sincronización y Auditoría Matemática
        // Creamos una lista temporal para el traductor
        List<String> secuenciaParaTraductor = new ArrayList<>(leftTiles);
        secuenciaParaTraductor.add("=");
        secuenciaParaTraductor.addAll(rightTiles);

        // El algoritmo puente traduce la disposición visual a lógica
        Ecuacion ecActual = TraductorEcuacion.traducirSecuencia(secuenciaParaTraductor);

        // Si la x quedó sola, informamos al usuario en tiempo real
        if (ecActual.xEstaAislada()) {
            _statusMessage.postValue("¡Excelente! x está despejada.");
            _autoAnswer.postValue(String.valueOf(ecActual.valorRHS()));
        }
    }

    public void resetTiles() {
        Exercise ex = _exercise.getValue();
        if (ex == null) return;
        leftTiles.clear();  leftTiles.addAll(parseJson(ex.tilesLeft));
        rightTiles.clear(); rightTiles.addAll(parseJson(ex.tilesRight));
        publishTiles();
        _autoAnswer.setValue(null);
        _statusMessage.setValue("Mueve los tiles para aislar x");
        startTimer();
    }

    public void verify(String input) {
        Exercise ex = _exercise.getValue();
        if (ex == null) return;
        if (input == null || input.trim().isEmpty()) {
            _exerciseResult.setValue(ExerciseResult.EMPTY_INPUT);
            return;
        }
        cancelTimer();
        boolean correct = input.trim().equals(ex.correctAnswer.trim());
        if (!correct) {
            try {
                correct = Double.parseDouble(input.trim()) == Double.parseDouble(ex.correctAnswer.trim());
            } catch (NumberFormatException ignored) {}
        }
        stateRepo.markExerciseDone(ex.moduleId, ex.stepOrder, totalSteps, correct, hintUsed,
                               correct ? (hintUsed ? ex.pointsHint : ex.pointsCorrect) : 0);
        if (correct) {
            // Si es el último paso, desbloqueamos el siguiente módulo en la base de datos
            if (ex.stepOrder >= totalSteps) {
                repo.unlockModule(ex.moduleId + 1);
            }
            _exerciseResult.setValue(hintUsed ? ExerciseResult.CORRECT_WITH_HINT : ExerciseResult.CORRECT);
        } else {
            _exerciseResult.setValue(ExerciseResult.INCORRECT);
        }
    }

    public void useHint()          { hintUsed = true; }
    public boolean isHintUsed()    { return hintUsed; }

    public void retryWithoutHint() {
        hintUsed = false;
        retryCurrentExercise();
    }

    public void retryCurrentExercise() {
        Exercise ex = _exercise.getValue();
        if (ex == null) return;
        switch (ex.type) {
            case Exercise.TYPE_BALANZA: resetBalanza(); break;
            case Exercise.TYPE_TILES:   resetTiles();   break;
        }
        startTimer();
    }

    public void startTimer() {
        new Handler(Looper.getMainLooper()).post(() -> {
            cancelTimer();
            _timerUrgent.setValue(false);
            _timerDisplay.setValue("2:00");
            timer = new CountDownTimer(TIME_MS, 1000) {
                @Override public void onTick(long ms) {
                    long s = ms / 1000;
                    _timerDisplay.setValue(String.format("%d:%02d", s / 60, s % 60));
                    _timerUrgent.setValue(s <= 20);
                }
                @Override public void onFinish() {
                    _timerDisplay.setValue("0:00");
                    _timerFinished.setValue(null);
                }
            }.start();
        });
    }

    public void cancelTimer() {
        if (timer != null) { timer.cancel(); timer = null; }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelTimer();
    }

    public static List<String> parseJson(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) result.add(arr.getString(i));
        } catch (JSONException e) {
            for (String s : json.replaceAll("[\\[\\]\"]", "").split(","))
                if (!s.isEmpty()) result.add(s.trim());
        }
        return result;
    }

    public enum ExerciseResult {
        CORRECT, CORRECT_WITH_HINT, INCORRECT, EMPTY_INPUT
    }
}
