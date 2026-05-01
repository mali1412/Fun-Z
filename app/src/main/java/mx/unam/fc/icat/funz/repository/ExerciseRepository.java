package mx.unam.fc.icat.funz.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mx.unam.fc.icat.funz.db.DbSeeder;
import mx.unam.fc.icat.funz.db.Exercise;
import mx.unam.fc.icat.funz.db.ExerciseDao;
import mx.unam.fc.icat.funz.db.FunZDatabase;
import mx.unam.fc.icat.funz.db.Module;
import mx.unam.fc.icat.funz.db.ModuleDao;

/**
 * ExerciseRepository — única fuente de verdad para módulos y ejercicios.
 * <p>
 * Abstrae el acceso a Room y garantiza que las queries síncronas
 * no ocurran en el hilo principal.
 * <p>
 * El ViewModel crea una instancia de este repositorio; nunca accede
 * directamente a los DAOS.
 */
public class ExerciseRepository {

    private final ExerciseDao      exerciseDao;
    private final ModuleDao        moduleDao;
    private final ExecutorService  io = Executors.newSingleThreadExecutor();

    public ExerciseRepository(Context context) {
        FunZDatabase db = FunZDatabase.getInstance(context);
        exerciseDao = db.exerciseDao();
        moduleDao   = db.moduleDao();

        // Sembrar datos si la DB está vacía (idempotente)
        io.execute(() -> DbSeeder.seed(moduleDao, exerciseDao));
    }

    // ── Módulos ───────────────────────────────────────────────────────────────

    /** LiveData con todos los módulos en orden. La UI observa esto directamente. */
    public LiveData<List<Module>> getAllModules() {
        return moduleDao.getAllModules();
    }

    /**
     * Desbloquea el módulo con el ID dado (p.ej. al completar el anterior).
     * Se ejecuta en el hilo de IO.
     */
    public void unlockModule(int moduleId) {
        io.execute(() -> moduleDao.unlock(moduleId));
    }

    // ── Ejercicios ────────────────────────────────────────────────────────────

    /**
     * Carga un ejercicio específico de forma asíncrona y entrega el resultado
     * al callback en el hilo de IO. El ViewModel lo publica con postValue().
     *
     * @param moduleId  ID del módulo
     * @param stepOrder Número de paso (1-based)
     * @param callback  Recibe el Exercise (o null si no existe)
     */
    public void loadExercise(int moduleId, int stepOrder, Callback<Exercise> callback) {
        io.execute(() -> {
            Exercise ex = exerciseDao.getExerciseSync(moduleId, stepOrder);
            callback.onResult(ex);
        });
    }

    /** Total de ejercicios en un módulo (asíncrono, resultado vía callback). */
    public void countExercises(int moduleId, Callback<Integer> callback) {
        io.execute(() -> callback.onResult(exerciseDao.countByModule(moduleId)));
    }

    // ── Interfaz callback ─────────────────────────────────────────────────────

    public interface Callback<T> {
        void onResult(T result);
    }
}
