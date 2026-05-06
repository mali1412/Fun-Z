package mx.unam.fc.icat.funz.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * ExerciseDao — acceso a datos para la tabla 'exercises'.
 *
 * Todas las queries síncronas deben ejecutarse fuera del hilo principal.
 * Las que devuelven LiveData son automáticamente asíncronas.
 */
@Dao
public interface ExerciseDao {

    // ── Inserción (usado por el seeder) ───────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Exercise> exercises);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Exercise exercise);

    // ── Lecturas ──────────────────────────────────────────────────────────────

    /**
     * Carga un ejercicio específico. Usado por el ViewModel genérico.
     * Se ejecuta en un hilo de fondo vía ExerciseRepository.
     */
    @Query("SELECT * FROM exercises WHERE module_id = :moduleId AND step_order = :step LIMIT 1")
    Exercise getExerciseSync(int moduleId, int step);

    /**
     * Todos los ejercicios de un módulo, en orden.
     * LiveData → se actualiza automáticamente si cambia la DB.
     */
    @Query("SELECT * FROM exercises WHERE module_id = :moduleId ORDER BY step_order ASC")
    LiveData<List<Exercise>> getExercisesByModule(int moduleId);

    /** Cantidad total de ejercicios en un módulo (síncrono, hilo de fondo). */
    @Query("SELECT COUNT(*) FROM exercises WHERE module_id = :moduleId")
    int countByModule(int moduleId);

    /** true si la tabla está vacía (usado por el seeder). */
    @Query("SELECT COUNT(*) FROM exercises")
    int count();
}
