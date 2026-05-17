package mx.unam.fc.icat.funz.data;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mx.unam.fc.icat.funz.repository.AppStateRepository;
import mx.unam.fc.icat.funz.repository.ExerciseRepository;

/**
 * <h2>FunZApp</h2>
 * Punto de entrada inicial y clase base de configuración global para la aplicación FunZ.
 * <p>
 * Hereda de {@link Application} y actúa como el contenedor de ciclo de vida más alto del proceso.
 * Se encarga de inicializar de forma temprana el Singleton de persistencia ligera, así como de
 * proveer una infraestructura unificada y centralizada de repositorios y un pool de hilos único
 * para operaciones de entrada/salida (I/O), garantizando el desacoplamiento y la inyección
 * de dependencias hacia los ViewModels de la interfaz de usuario.
 * </p>
 * <p>
 * Esta clase está registrada de forma obligatoria en el archivo {@code AndroidManifest.xml}
 * mediante el atributo {@code android:name=".data.FunZApp"}.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class FunZApp extends Application {

    /** Pool de hilos dedicado exclusivamente a transacciones asíncronas en segundo plano (I/O, Room, Red). */
    private ExecutorService ioExecutor;

    /** Repositorio centralizado para el acceso, conteo y desbloqueo de módulos y ejercicios. */
    private ExerciseRepository exerciseRepository;

    /** Repositorio intermediario encargado de gestionar las preferencias y el estado analítico de gamificación. */
    private AppStateRepository appStateRepository;

    /**
     * Invocado por el sistema operativo Android cuando la aplicación se crea
     * y arranca antes de cualquier otra Activity, Service o Fragment.
     * <p>
     * Se encarga de detonar el aprovisionamiento de dependencias core del sistema, enlazando
     * el Singleton de SharedPreferences y construyendo las instancias permanentes de los repositorios.
     * </p>
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // Inicialización síncrona y temprana del Singleton de estado y preferencias del usuario
        AppState.getInstance().init(this);

        // Inicialización de la infraestructura de concurrencia y repositorios.
        // Se utiliza un SingleThreadExecutor para asegurar que las consultas secuenciales a la base
        // de datos mantengan consistencia de hilos y eviten colisiones de escritura asíncrona.
        ioExecutor = Executors.newSingleThreadExecutor();
        exerciseRepository = new ExerciseRepository(this, ioExecutor);
        appStateRepository = new AppStateRepository(AppState.getInstance());
    }

    /**
     * Provee el repositorio encargado del procesamiento y la lógica de acceso a los ejercicios didácticos.
     * Utilizado por los ViewModels para interactuar de forma segura con la capa de datos de Room.
     *
     * @return Instancia única de {@link ExerciseRepository}.
     */
    public ExerciseRepository getExerciseRepository() {
        return exerciseRepository;
    }

    /**
     * Provee el repositorio encargado de la persistencia ligera de configuraciones, puntos y estado de sesión.
     * Permite desacoplar el acceso directo a SharedPreferences a lo largo del ciclo de vida de la UI.
     *
     * @return Instancia única de {@link AppStateRepository}.
     */
    public AppStateRepository getAppStateRepository() {
        return appStateRepository;
    }
}