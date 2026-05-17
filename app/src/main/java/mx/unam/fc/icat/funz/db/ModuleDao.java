package mx.unam.fc.icat.funz.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Interfaz de Acceso a Datos (DAO) para la entidad {@link Module} que interactúa con la tabla {@code modules}.
 * <p>
 * Define las operaciones transaccionales y las consultas SQL compiladas necesarias para gestionar la
 * progresión teórica, el control de accesos por bloqueo y la lectura del mapa de aprendizaje de los alumnos.
 * </p>
 * <p>
 * Las funciones con retornos reactivos de tipo {@link LiveData} se ejecutan de manera asíncrona por el motor de Room.
 * Las funciones transaccionales con retornos primitivos u objetos directos (síncronas) deben ser invocadas
 * estrictamente fuera del hilo principal de ejecución de la interfaz de usuario (UI Thread) desde la capa del repositorio.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
@Dao
public interface ModuleDao {

    /**
     * Inserta un lote masivo de objetos de tipo {@link Module} en la base de datos de forma atómica.
     * @param modules  Listado estructurado de módulos de estudio generados por la semilla de datos.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Module> modules);

    /**
     * Recupera el listado completo de módulos de aprendizaje almacenados en el sistema,
     * ordenados de forma ascendente según su índice de secuencia didáctica.
     * <p>
     * Devuelve un contenedor observable reactivo que notifica dinámicamente a la interfaz
     * de usuario (ej. {@code TemasActivity}) ante cualquier mutación o cambio en las banderas de progreso.
     * </p>
     *
     * @return Contenedor reactivo {@link LiveData} con el listado ordenado de módulos.
     */
    @Query("SELECT * FROM modules ORDER BY order_index ASC")
    LiveData<List<Module>> getAllModules();

    /**
     * Ejecuta un conteo escalar absoluto para verificar el volumen de módulos registrados en la tabla.
     * Utilizado síncronamente por el inicializador de datos para determinar si el mapa académico requiere ser poblado.
     *
     * @return Cantidad total de registros enteros presentes en la tabla de módulos.
     */
    @Query("SELECT COUNT(*) FROM modules")
    int count();

    /**
     * Modifica directamente a nivel de base de datos el estado de acceso de un módulo específico,
     * otorgándole permisos de apertura al estudiante (Desbloqueo de tema).
     * <p>
     * <b>Nota de Optimización:</b> Esta consulta ejecuta un UPDATE directo en disco sin necesidad
     * de cargar la entidad completa a la memoria RAM del dispositivo, optimizando el rendimiento físico.
     * </p>
     *
     * @param moduleId Identificador único del módulo de estudio que se desea desbloquear.
     */
    @Query("UPDATE modules SET unlocked = 1 WHERE id = :moduleId")
    void unlock(int moduleId);

    /**
     * Recupera de forma síncrona y en un hilo de fondo la información estructurada de un módulo
     * basándose en su clave primaria.
     *
     * @param moduleId Identificador único del módulo consultado.
     * @return Instancia completa de {@link Module} poblada con sus bloques teóricos, o {@code null} si no existe.
     */
    @Query("SELECT * FROM modules WHERE id = :moduleId LIMIT 1")
    Module getModuleSync(int moduleId);

    /**
     * Actualiza un modulo en la base de datos de forma atómica.
     * @param module Instancia de módulo a actualizar.
     */
    @Update
    void update(Module module);
}
