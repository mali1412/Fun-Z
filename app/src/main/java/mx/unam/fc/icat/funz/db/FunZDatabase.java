package mx.unam.fc.icat.funz.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Componente central y contenedor relacional de la arquitectura Room que expone la base de datos SQLite del sistema.
 * <p>
 * Implementa el patrón estructural Singleton con bloqueo de doble verificación (Double-Checked Locking) para asegurar
 * un único canal de acceso asíncrono y seguro entre hilos (Thread-Safe) hacia el almacenamiento local del dispositivo.
 * </p>
 * <p>
 * Centraliza la declaración de las entidades relacionales ({@link Module} y {@link Exercise}), la inyección de
 * convertidores personalizados de tipos estructurados ({@link Converters}) y coordina el Callback de alimentación
 * pedagógica automática inicial de las semillas didácticas ({@link DbSeeder}).
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
@Database(
    entities  = { Module.class, Exercise.class },
    version   = 5,
    exportSchema = false
)
@TypeConverters({ Converters.class })
public abstract class FunZDatabase extends RoomDatabase {

    /**
     * Expone el Objeto de Acceso a Datos (DAO) para la gestión y consultas de los módulos de estudio.
     *
     * @return Instancia autogenerada del contrato {@link ModuleDao}.
     */
    public abstract ModuleDao   moduleDao();

    /**
     * Expone el Objeto de Acceso a Datos (DAO) para la gestión y consultas polimórficas de los ejercicios algebraicos.
     *
     * @return Instancia autogenerada del contrato {@link ExerciseDao}.
     */
    public abstract ExerciseDao exerciseDao();

    /** Instancia única de la base de datos con visibilidad atómica e inmediata entre hilos gracias al modificador volatile. */
    private static volatile FunZDatabase INSTANCE;

    /**
     * Provee o construye de forma controlada la conexión única y global de la base de datos local de FunZ.
     * <p>
     * Utiliza un mecanismo de sincronización por hilos exclusivo (synchronized class block) para evitar colisiones
     * operacionales o inicializaciones duplicadas en memoria RAM durante lecturas/escrituras masivas concurrentes.
     * </p>
     *
     * @param context Contexto de la aplicación utilizado de forma segura mediante {@code getApplicationContext()}
     * para mitigar riesgos de fugas de memoria (Memory Leaks) en el ciclo de vida.
     * @return Instancia única e integrada de {@link FunZDatabase}.
     */
    public static FunZDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FunZDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            FunZDatabase.class,
                            "funz_db"
                    )
                    .fallbackToDestructiveMigration() // Para desarrollo, recrea la DB al cambiar versión
                    .addCallback(new DbSeeder())
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
