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
import mx.unam.fc.icat.funz.model.CalculadoraAlgebraica;
import mx.unam.fc.icat.funz.model.ParserEcuacion;

/**
 * ExerciseViewModel — ViewModel genérico para cualquier tipo de ejercicio.
 * Incluye motor algebraico dinámico y Efecto Gravedad para Balanza,
 * y lógica de manipulación de Tiles.
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

    private final MutableLiveData<String> _history = new MutableLiveData<>("");
    public final LiveData<String> history = _history;

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
        Ecuacion ec = ParserEcuacion.parsear(ex.lhsExpr + " = " + ex.rhsExpr);
        _ecuacion.postValue(ec);
        _lhsExpr.postValue(ParserEcuacion.terminosAString(ec.getLadoIzquierdo()));
        _rhsExpr.postValue(ParserEcuacion.terminosAString(ec.getLadoDerecho()));
        _ops.postValue(parseJson(ex.ops));
        _statusMessage.postValue("Aísla x para equilibrar la balanza");
        updateBalanzaVisuals(ec);
    }

    private void initTiles(Exercise ex) {
        leftTiles.clear();  leftTiles.addAll(parseJson(ex.tilesLeft));
        rightTiles.clear(); rightTiles.addAll(parseJson(ex.tilesRight));
        publishTiles();
        _statusMessage.postValue("Arrastra una operación al centro para aplicarla en ambos lados.");
        _statusPositive.postValue(null);
    }

    private void initClasico(Exercise ex) {
        Ecuacion ec = ParserEcuacion.parsear(ex.equation);
        _ecuacion.postValue(ec);
        _statusMessage.postValue("Resuelve la ecuación paso a paso");
    }

    private void updateBalanzaVisuals(Ecuacion ec) {
        String lhsStr = ParserEcuacion.terminosAString(ec.getLadoIzquierdo());
        String rhsStr = ParserEcuacion.terminosAString(ec.getLadoDerecho());
        _lhsExpr.postValue(lhsStr);
        _rhsExpr.postValue(rhsStr);

        if (CalculadoraAlgebraica.xEstaAislada(ec)) {
            _tilt.postValue(0f);
            _balanced.postValue(true);
            int valorRhs = (int) Math.round(CalculadoraAlgebraica.evaluarLado(ec, 0, false));
            _autoAnswer.postValue(String.valueOf(valorRhs));
            _statusMessage.postValue("✓ ¡Excelente! x está aislada.");
            _statusPositive.postValue(true);
        } else {
            // Usamos un peso de x que no coincida necesariamente con la solución
            // para que el usuario vea la necesidad de aislar físicamente los términos.
            double weightX = 25.0;
            double l = CalculadoraAlgebraica.evaluarLado(ec, weightX, true);
            double r = CalculadoraAlgebraica.evaluarLado(ec, weightX, false);
            double diff = l - r; 
            float factor = 3.0f;
            float t = (float) (diff * factor);
            if (t > 30f) t = 30f;
            if (t < -30f) t = -30f;
            _tilt.postValue(-t);
            _balanced.postValue(false);
            if (Math.abs(t) >= 30f) _statusMessage.postValue("⚠️ La balanza ha tocado suelo");
            else _statusMessage.postValue("Busca el equilibrio de la ecuación");
            _statusPositive.postValue(false);
        }
    }

    public void applyOp(String op) {
        Exercise ex = _exercise.getValue();
        Ecuacion current = _ecuacion.getValue();
        if (ex == null || current == null) return;

        String normalizedOp = op.replace("−", "-").replace("–", "-").replace("÷", "/").trim();
        if (normalizedOp.startsWith("/")) {
            try {
                int divisor = Integer.parseInt(normalizedOp.substring(1).trim());
                if (divisor == 0) {
                    _statusMessage.setValue("No se puede dividir entre 0");
                    _statusPositive.setValue(false);
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }

        CalculadoraAlgebraica.aplicarOperacion(current, op);
        _ecuacion.setValue(current);
        updateBalanzaVisuals(current);

        if (op.equals(ex.correctOp)) {
            _statusMessage.setValue("✓ Movimiento estratégico. ¡Sigue así!");
            _statusPositive.setValue(true);
        } else {
            _statusMessage.setValue("La balanza cambió, pero busca aislar x.");
            _statusPositive.setValue(false);
        }
    }

    public void applyOpToSide(String op, boolean isLeft) {
        Ecuacion current = _ecuacion.getValue();
        if (current == null) return;

        CalculadoraAlgebraica.aplicarOperacionALado(current, op, isLeft);
        _ecuacion.setValue(current);
        updateBalanzaVisuals(current);

        _statusMessage.setValue("Has modificado solo un lado. ¡Cuidado con el equilibrio!");
        _statusPositive.setValue(false);
    }

    public void applyTileOperation(String op) {
        String normalized = normalizeOp(op);
        if (normalized.isEmpty()) return;

        String expected = expectedTileOp();
        if (!expected.isEmpty() && !expected.equals(normalized)) {
            _statusMessage.setValue("Esa operación no es recomendada en este paso.");
            _statusPositive.setValue(false);
            return;
        }

        boolean applied = false;
        switch (normalized) {
            case "-1": applied = removeUnitBothSides("+1"); break;
            case "+1": applied = removeUnitBothSides("-1"); break;
            case "-x": applied = removeXBothSides("x"); break;
            case "+x": applied = removeXBothSides("-x"); break;
            case "÷2": applied = divideBothSidesByTwo(); break;
            case "×2": applied = multiplyBothSidesByTwo(); break;
        }

        if (!applied) {
            _statusMessage.setValue("No se puede aplicar " + normalized + " ahora.");
            _statusPositive.setValue(false);
            return;
        }

        publishTiles();
        _statusMessage.setValue("Operación aplicada: " + normalized);
        _statusPositive.setValue(true);
    }

    private boolean removeUnitBothSides(String unitLabel) {
        if (leftTiles.contains(unitLabel) && rightTiles.contains(unitLabel)) {
            leftTiles.remove(unitLabel);
            rightTiles.remove(unitLabel);
            return true;
        }
        return false;
    }

    private boolean removeXBothSides(String xLabel) {
        if (leftTiles.contains(xLabel) && rightTiles.contains(xLabel)) {
            leftTiles.remove(xLabel);
            rightTiles.remove(xLabel);
            return true;
        }
        return false;
    }

    private boolean divideBothSidesByTwo() {
        SideState left = summarizeSide(leftTiles);
        SideState right = summarizeSide(rightTiles);
        if (left.halfX > 0 || right.halfX > 0) return false;
        if (left.xCount % 2 != 0 || Math.abs(left.units) % 2 != 0 || Math.abs(right.units) % 2 != 0) return false;
        
        leftTiles.clear(); rightTiles.clear();
        for (int i = 0; i < left.xCount / 2; i++) leftTiles.add("x");
        addUnits(leftTiles, left.units / 2);
        for (int i = 0; i < right.xCount / 2; i++) rightTiles.add("x");
        addUnits(rightTiles, right.units / 2);
        return true;
    }

    private boolean multiplyBothSidesByTwo() {
        SideState left = summarizeSide(leftTiles);
        SideState right = summarizeSide(rightTiles);
        leftTiles.clear(); rightTiles.clear();
        for (int i = 0; i < left.xCount * 2 + left.halfX; i++) leftTiles.add("x");
        addUnits(leftTiles, left.units * 2);
        for (int i = 0; i < right.xCount * 2 + right.halfX; i++) rightTiles.add("x");
        addUnits(rightTiles, right.units * 2);
        return true;
    }

    private void addUnits(List<String> target, int units) {
        if (units > 0) for (int i = 0; i < units; i++) target.add("+1");
        else for (int i = 0; i < -units; i++) target.add("-1");
    }

    private void publishTiles() {
        _leftTiles.postValue(new ArrayList<>(leftTiles));
        _rightTiles.postValue(new ArrayList<>(rightTiles));

        List<String> tokens = new ArrayList<>(leftTiles);
        tokens.add("=");
        tokens.addAll(rightTiles);

        Ecuacion ecActual = TraductorEcuacion.traducirSecuencia(tokens);
        CalculadoraAlgebraica.simplificar(ecActual);

        _ecuacion.postValue(ecActual);
        _lhsExpr.postValue(ParserEcuacion.terminosAString(ecActual.getLadoIzquierdo()));
        _rhsExpr.postValue(ParserEcuacion.terminosAString(ecActual.getLadoDerecho()));
        _ops.postValue(getTileOps());

        if (CalculadoraAlgebraica.xEstaAislada(ecActual)) {
            int valorRhs = (int) Math.round(CalculadoraAlgebraica.evaluarLado(ecActual, 0, false));
            _autoAnswer.postValue(String.valueOf(valorRhs));
            _statusMessage.postValue("¡Excelente! x = " + valorRhs);
            _statusPositive.postValue(true);
        }
    }

    private List<String> getTileOps() {
        List<String> result = new ArrayList<>();
        result.add("-1"); result.add("+1");
        result.add("-x"); result.add("+x");
        result.add("÷2"); result.add("×2");
        return result;
    }

    public String expectedTileOp() {
        SideState left = summarizeSide(leftTiles);
        SideState right = summarizeSide(rightTiles);
        if (left.units > 0) return "-1";
        if (left.units < 0) return "+1";
        if (left.xCount > 0 && right.xCount > 0) return "-x";
        if (left.negXCount > 0 && right.negXCount > 0) return "+x";
        if (left.halfX > 0) return "×2";
        if (left.xCount > 1) return "÷2";
        return "";
    }

    private SideState summarizeSide(List<String> side) {
        SideState s = new SideState();
        for (String tile : side) {
            if ("x".equals(tile)) s.xCount++;
            else if ("-x".equals(tile)) s.negXCount++;
            else if ("x/2".equals(tile)) s.halfX++;
            else if ("+1".equals(tile) || "1".equals(tile)) s.units++;
            else if ("-1".equals(tile)) s.units--;
        }
        return s;
    }

    public void verify(String input) {
        Exercise ex = _exercise.getValue();
        if (ex == null || input == null || input.trim().isEmpty()) {
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
        stateRepo.markExerciseDone(ex.moduleId, ex.stepOrder, totalSteps, correct, hintUsed, correct ? (hintUsed ? 50 : 100) : 0);
        if (correct) {
            if (ex.stepOrder >= totalSteps) repo.unlockModule(ex.moduleId + 1);
            _exerciseResult.setValue(hintUsed ? ExerciseResult.CORRECT_WITH_HINT : ExerciseResult.CORRECT);
        } else {
            _exerciseResult.setValue(ExerciseResult.INCORRECT);
        }
    }

    public void resetBalanza() {
        Exercise ex = _exercise.getValue();
        if (ex != null) initBalanza(ex);
    }

    public void resetTiles() {
        Exercise ex = _exercise.getValue();
        if (ex != null) initTiles(ex);
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

    public void useHint()          { hintUsed = true; }
    public void retryWithoutHint() { hintUsed = false; retryCurrentExercise(); }

    public void startTimer() {
        new Handler(Looper.getMainLooper()).post(() -> {
            cancelTimer();
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

    @Override protected void onCleared() { super.onCleared(); cancelTimer(); }

    private String normalizeOp(String op) {
        if (op == null) return "";
        return op.replace("−", "-").replace("–", "-").replace("/", "÷").replace("x", "×").replace("*", "×").trim();
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

    private static class SideState { int xCount; int negXCount; int halfX; int units; }

    public enum ExerciseResult { CORRECT, CORRECT_WITH_HINT, INCORRECT, EMPTY_INPUT }
}
