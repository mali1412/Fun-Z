package mx.unam.fc.icat.funz.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Interfaz de Acceso a Datos (DAO) para la entidad {@link Exercise} que interactúa con la tabla {@code exercises}.
 * <p>
 * Define los contratos transaccionales y las consultas SQL compiladas en tiempo de ejecución para
 * la inserción, filtrado, paginación secuencial y auditoría analítica de los desafíos matemáticos.
 * </p>
 * <p>
 * Los métodos que retornan tipos directos u primitivos deben ser invocados estrictamente fuera del hilo
 * principal de la interfaz de usuario (UI Thread) mediante el pool de conexiones asíncronas. Los métodos
 * que encapsulan objetos {@link LiveData} gestionan su concurrencia de forma nativa y asíncrona a través de Room.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 2026.1.0
 */
@Dao
public interface ExerciseDao {

    // ── Inserción (usado por el seeder) ───────────────────────────────────────

    /**
     * Inserta un lote masivo de objetos de tipo {@link Exercise} en la base de datos de forma atómica.
     * Utiliza la estrategia de conflicto {@link OnConflictStrategy#IGNORE} para evitar colisiones o
     * duplicaciones si los registros ya fueron precargados en arranques previos de la app.
     *
     * @param exercises Listado estructurado de ejercicios didácticos generados por la semilla de datos.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Exercise> exercises);

    // ── Lecturas ──────────────────────────────────────────────────────────────

    /**
     * Carga un ejercicio específico. Usado por el ViewModel genérico.
     * Se ejecuta en un hilo de fondo vía ExerciseRepository.
     */
    @Query("SELECT * FROM exercises WHERE module_id = :moduleId AND step_order = :step LIMIT 1")
    Exercise getExerciseSync(int moduleId, int step);

    /** Cantidad total de ejercicios en un módulo (síncrono, hilo de fondo). */
    @Query("SELECT COUNT(*) FROM exercises WHERE module_id = :moduleId")
    int countByModule(int moduleId);

    /** true si la tabla está vacía (usado por el seeder). */
    @Query("SELECT COUNT(*) FROM exercises")
    int count();
}
