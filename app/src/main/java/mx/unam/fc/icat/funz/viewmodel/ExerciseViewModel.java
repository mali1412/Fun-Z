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
 * Ahora incluye un motor algebraico dinámico para la Balanza.
 */
public class ExerciseViewModel extends AndroidViewModel {

    private static final long TIME_MS = 120_000L;

    private final ExerciseRepository repo;
    private final AppStateRepository stateRepo;
    private volatile CountDownTimer   timer;
    private       boolean             initialized = false;
    private       boolean             hintUsed    = false;
    private       int                 totalSteps  = 3; 

    private final MutableLiveData<Exercise> _exercise = new MutableLiveData<>();
    public  final LiveData<Exercise>         exercise  = _exercise;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(true);
    public  final LiveData<Boolean>         loading  = _loading;

    // ── Estado de la Ecuación y Balanza ───────────────────────────────────────

    private final MutableLiveData<Ecuacion> _ecuacion = new MutableLiveData<>();
    public  final LiveData<Ecuacion>         ecuacion  = _ecuacion;

    private final MutableLiveData<Float> _tilt = new MutableLiveData<>(0f);
    public  final LiveData<Float>         tilt  = _tilt;

    private final MutableLiveData<String>  _lhsExpr  = new MutableLiveData<>("");
    public  final LiveData<String>          lhsExpr   = _lhsExpr;

    private final MutableLiveData<String>  _rhsExpr  = new MutableLiveData<>("");
    public  final LiveData<String>          rhsExpr   = _rhsExpr;

    private final MutableLiveData<Boolean> _balanced = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         balanced  = _balanced;

    // ── Otros estados ─────────────────────────────────────────────────────────

    private final MutableLiveData<String>  _timerDisplay = new MutableLiveData<>("2:00");
    public  final LiveData<String>          timerDisplay  = _timerDisplay;

    private final MutableLiveData<Boolean> _timerUrgent = new MutableLiveData<>(false);
    public  final LiveData<Boolean>         timerUrgent  = _timerUrgent;

    private final MutableLiveData<String>  _statusMessage  = new MutableLiveData<>("");
    public  final LiveData<String>          statusMessage   = _statusMessage;

    private final MutableLiveData<Boolean> _statusPositive = new MutableLiveData<>(null);
    public  final LiveData<Boolean>         statusPositive  = _statusPositive;

    private final MutableLiveData<List<String>> _ops = new MutableLiveData<>(new ArrayList<>());
    public  final LiveData<List<String>>         ops  = _ops;

    private final MutableLiveData<List<String>> _leftTiles  = new MutableLiveData<>(new ArrayList<>());
    public  final LiveData<List<String>>         leftTilesLd = _leftTiles;

    private final MutableLiveData<List<String>> _rightTiles  = new MutableLiveData<>(new ArrayList<>());
    public  final LiveData<List<String>>         rightTilesLd = _rightTiles;

    private final MutableLiveData<String> _autoAnswer = new MutableLiveData<>(null);
    public  final LiveData<String>         autoAnswer  = _autoAnswer;

    private final SingleLiveEvent<ExerciseResult> _exerciseResult = new SingleLiveEvent<>();
    public  final LiveData<ExerciseResult>         exerciseResult  = _exerciseResult;

    private final SingleLiveEvent<Void> _timerFinished = new SingleLiveEvent<>();
    public  final LiveData<Void>         timerFinished  = _timerFinished;

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
            case Exercise.TYPE_CLASICO: initClasico(ex); break;
        }
        startTimer();
    }

    private void initBalanza(Exercise ex) {
        Ecuacion ec = Ecuacion.parsear(ex.lhsExpr + " = " + ex.rhsExpr);
        _ecuacion.postValue(ec);
        _lhsExpr.postValue(ec.getLhsString());
        _rhsExpr.postValue(ec.getRhsString());
        _ops.postValue(parseJson(ex.ops));
        _statusMessage.postValue("Aísla x aplicando operaciones a ambos lados");
        updateBalanzaVisuals(ec);
    }

    private void initTiles(Exercise ex) {
        leftTiles.clear();  leftTiles.addAll(parseJson(ex.tilesLeft));
        rightTiles.clear(); rightTiles.addAll(parseJson(ex.tilesRight));
        publishTiles();
        _statusMessage.postValue("Mueve los tiles para aislar x");
    }

    private void initClasico(Exercise ex) {
        Ecuacion ec = Ecuacion.parsear(ex.equation);
        _ecuacion.postValue(ec);
        _statusMessage.postValue("Resuelve la ecuación paso a paso");
    }

    private void updateBalanzaVisuals(Ecuacion ec) {
        if (ec.xEstaAislada()) {
            _tilt.postValue(0f);
            _balanced.postValue(true);
            _autoAnswer.postValue(String.valueOf(ec.valorRHS()));
            _statusMessage.postValue("✓ ¡Excelente! x está aislada.");
            _statusPositive.postValue(true);
        } else {
            // Calculamos peso visual basado en constantes
            double l = ec.evaluarLado(0, true);
            double r = ec.evaluarLado(0, false);
            float t = (float)(r - l) * 1.5f; 
            if (t > 15f) t = 15f;
            if (t < -15f) t = -15f;
            _tilt.postValue(t);
            _balanced.postValue(false);
        }
    }

    /**
     * Aplica una operación dinámica a la ecuación.
     * Ahora actualiza la ecuación matemáticamente sin importar si es la op "correcta" de la DB.
     */
    public void applyOp(String op) {
        Exercise ex = _exercise.getValue();
        Ecuacion current = _ecuacion.getValue();
        if (ex == null || current == null) return;

        // 1. Aplicamos la transformación matemática real
        current.aplicarOperacion(op);
        
        // 2. Sincronizamos la UI con el nuevo estado del objeto
        _ecuacion.setValue(current);
        _lhsExpr.setValue(current.getLhsString());
        _rhsExpr.setValue(current.getRhsString());
        updateBalanzaVisuals(current);

        // 3. Feedback pedagógico basado en la sugerencia de la DB
        if (op.equals(ex.correctOp)) {
            _statusMessage.setValue("✓ Movimiento estratégico. ¡Sigue así!");
            _statusPositive.setValue(true);
        } else {
            _statusMessage.setValue("La balanza cambió, pero busca una operación que aísle x.");
            _statusPositive.setValue(false);
        }
    }

    public void resetBalanza() {
        Exercise ex = _exercise.getValue();
        if (ex == null) return;
        initBalanza(ex);
        _autoAnswer.setValue("");
    }

    public void moveTile(String side, int idx, String label) {
        if (side.equals("L")) {
            if (idx < leftTiles.size()) {
                leftTiles.remove(idx);
                rightTiles.add(invertirSigno(label));
            }
        } else {
            if (idx < rightTiles.size()) {
                rightTiles.remove(idx);
                leftTiles.add(invertirSigno(label));
            }
        }
        ejecutarReglaParCero(leftTiles);
        ejecutarReglaParCero(rightTiles);
        publishTiles();
    }

    private String invertirSigno(String label) {
        if (label.equals("x")) return "x";
        if (label.startsWith("+")) return label.replace("+", "-");
        if (label.startsWith("-")) return label.replace("-", "+");
        return "-" + label;
    }

    private void ejecutarReglaParCero(List<String> lista) {
        if (lista.contains("+1") && lista.contains("-1")) {
            lista.remove("+1");
            lista.remove("-1");
            _statusMessage.postValue("¡Par Cero! Se anularon.");
            _statusPositive.postValue(true);
        }
    }

    private void publishTiles() {
        _leftTiles.postValue(new ArrayList<>(leftTiles));
        _rightTiles.postValue(new ArrayList<>(rightTiles));
        List<String> tokens = new ArrayList<>(leftTiles);
        tokens.add("=");
        tokens.addAll(rightTiles);
        Ecuacion ecActual = TraductorEcuacion.traducirSecuencia(tokens);
        _ecuacion.postValue(ecActual);
        if (ecActual.xEstaAislada()) {
            int valorRhs = ecActual.valorRHS();
            _autoAnswer.postValue(String.valueOf(valorRhs));
            _statusMessage.postValue("¡Excelente! x = " + valorRhs);
            _statusPositive.postValue(true);
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
            if (ex.stepOrder >= totalSteps) repo.unlockModule(ex.moduleId + 1);
            _exerciseResult.setValue(hintUsed ? ExerciseResult.CORRECT_WITH_HINT : ExerciseResult.CORRECT);
        } else {
            _exerciseResult.setValue(ExerciseResult.INCORRECT);
        }
    }

    public void useHint()          { hintUsed = true; }
    public boolean isHintUsed()    { return hintUsed; }
    public void retryWithoutHint() { hintUsed = false; retryCurrentExercise(); }

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

    public enum ExerciseResult { CORRECT, CORRECT_WITH_HINT, INCORRECT, EMPTY_INPUT }
}
