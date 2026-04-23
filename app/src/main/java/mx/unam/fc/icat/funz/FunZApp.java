package mx.unam.fc.icat.funz;

import android.app.Application;

/**
 * FunZApp — Clase Application de FunZ.
 *
 * Punto de entrada global de la app. En producción se usaría para:
 *   - Cargar AppState desde SharedPreferences al arrancar.
 *   - Guardar AppState en SharedPreferences al ir a background.
 *
 * Registrada en AndroidManifest.xml con android:name=".FunZApp".
 */
public class FunZApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO (producción): cargar estado persistido
        // SharedPreferences prefs = getSharedPreferences("funz_prefs", MODE_PRIVATE);
        // AppState s = AppState.getInstance();
        // s.setUsername(prefs.getString("username", "Usuario"));
        // s.addPoints(prefs.getInt("points", 0));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // TODO (producción): persistir AppState
    }
}
