package mx.unam.fc.icat.funz.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.db.DbSeeder;
import mx.unam.fc.icat.funz.db.Exercise;
import mx.unam.fc.icat.funz.db.ExerciseDao;
import mx.unam.fc.icat.funz.db.FunZDatabase;
import mx.unam.fc.icat.funz.db.Module;
import mx.unam.fc.icat.funz.db.ModuleDao;

/**
 * Repositorio centralizado que actúa como la Única Fuente de Verdad (Single Source of Truth)
 * para la gestión e inyección de datos relacionados con los módulos didácticos y desafíos algebraicos.
 * <p>
 * Encapsula de forma hermética los Objetos de Acceso a Datos ({@link ModuleDao} y {@link ExerciseDao}),
 * abstrayendo la arquitectura física de Room Database. Coordina y traslada obligatoriamente el procesamiento
 * de consultas de lectura/escritura síncronas hacia hilos de fondo mediante un pool de concurrencia dedicado,
 * impidiendo el bloqueo del hilo principal de la interfaz de usuario (UI Thread).
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 2026.1.0
 */
public class ExerciseRepository {

    private final ExerciseDao exerciseDao;
    private final ModuleDao moduleDao;
    private final ExecutorService io;

    /**
     * Constructor principal por inyección de dependencias. Inicializa la conexión con la base de datos,
     * asocia el pool de hilos global detonando asíncronamente el aprovisionamiento de las semillas didácticas
     * e integra de forma atómica el volumen de reactivos dentro del gestor analítico {@link AppState}.
     *
     * @param context    Contexto de la aplicación utilizado de manera segura para mitigar fugas de memoria.
     * @param ioExecutor Instancia unificada de {@link ExecutorService} encargada del procesamiento asíncrono.
     */
    public ExerciseRepository(Context context, ExecutorService ioExecutor) {
        FunZDatabase db = FunZDatabase.getInstance(context);
        exerciseDao = db.exerciseDao();
        moduleDao = db.moduleDao();
        io = ioExecutor;

        // Sembrar datos si la DB está vacía (idempotente)
        // En ExerciseRepository.java, dentro del constructor o después de DbSeeder.seed:
        io.execute(() -> {
            DbSeeder.seed(db);
            // Actualizar AppState con el conteo real de cada módulo
            for (int i = 1; i <= 6; i++) {
                int realCount = exerciseDao.countByModule(i);
                AppState.getInstance().setModuleExerciseCount(i, realCount);
            }
        });
    }

    // ── Módulos ───────────────────────────────────────────────────────────────

    /**
     * Provee el contenedor observable reactivo con el listado ordenado de módulos.
     * La capa visual observa este flujo directo para actualizar candados de bloqueo e interactividad.
     *
     * @return Contenedor {@link LiveData} que emite de manera asíncrona la lista de {@link Module}.
     */
    public LiveData<List<Module>> getAllModules() {
        return moduleDao.getAllModules();
    }

    /**
     * Modifica de manera asíncrona en segundo plano la bandera de acceso de un módulo para habilitar
     * su exploración académica por el estudiante.
     *
     * @param moduleId Identificador único del módulo temático que se desea aperturar.
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

    /**
     * Ejecuta una consulta asíncrona para auditar y contar la cantidad de desafíos matemáticos indexados
     * dentro de una unidad de estudio.
     *
     * @param moduleId Identificador único del módulo evaluado.
     * @param callback Interfaz funcional que recibe el volumen entero acumulado de reactivos.
     */
    public void countExercises(int moduleId, Callback<Integer> callback) {
        io.execute(() -> callback.onResult(exerciseDao.countByModule(moduleId)));
    }

    // ── Interfaz callback ─────────────────────────────────────────────────────

    /**
     * Interfaz genérica funcional diseñada para encapsular el despacho e intercepción asíncrona
     * de consultas de bases de datos relacionales sin forzar retornos bloqueantes.
     *
     * @param <T> Tipo paramétrico del objeto estructurado esperado.
     */
    public interface Callback<T> {
        void onResult(T result);
    }
}
