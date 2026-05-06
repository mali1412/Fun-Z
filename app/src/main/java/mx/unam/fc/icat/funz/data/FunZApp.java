package mx.unam.fc.icat.funz.data;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mx.unam.fc.icat.funz.repository.AppStateRepository;
import mx.unam.fc.icat.funz.repository.ExerciseRepository;

/**
 * FunZApp — Clase Application de FunZ.
 *<p>
 * Punto de entrada global de la app. En producción se usaría para:
 *   - Cargar AppState desde SharedPreferences al arrancar.
 *   - Guardar AppState en SharedPreferences al ir a background.
 * <p>
 * Registrada en AndroidManifest.xml con android:name=".FunZApp".
 */
public class FunZApp extends Application {

    private ExecutorService ioExecutor;
    private ExerciseRepository exerciseRepository;
    private AppStateRepository appStateRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO (producción): cargar estado persistido
        // SharedPreferences prefs = getSharedPreferences("funz_prefs", MODE_PRIVATE);
        // AppState s = AppState.getInstance();
        // s.setUsername(prefs.getString("username", "Usuario"));
        // s.addPoints(prefs.getInt("points", 0));
        // Conectar AppState a SharedPreferences
        AppState.getInstance().init(this);

        // Fuente única de hilos/repositorios para toda la app.
        ioExecutor = Executors.newSingleThreadExecutor();
        exerciseRepository = new ExerciseRepository(this, ioExecutor);
        appStateRepository = new AppStateRepository(AppState.getInstance());
    }

    public ExerciseRepository getExerciseRepository() {
        return exerciseRepository;
    }

    public AppStateRepository getAppStateRepository() {
        return appStateRepository;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        // TODO (producción): persistir AppState
    }
}
