package mx.unam.fc.icat.funz.ui.sala;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.ui.main.MainActivity;


/**
 * SalasActivity — Pantalla E: Salas de Juego
 *
 * En esta versión del proyecto, la sección de Salas (colaborativo y
 * competitivo) no será implementada. Se muestra un mensaje de
 * "Próximamente" con la BottomNavigationView activa en "Salas".
 *
 * La implementación completa de salas está planificada para la
 * siguiente versión e incluirá:
 *   - Creación de sala con NIP único.
 *   - Modo Competitivo (Pantalla K).
 *   - Modo Colaborativo con turnos de 10 s (Pantallas I y J).
 *   - Pantalla de resultados (Pantalla L).
 */
public class SalasActivity extends AppCompatActivity {

    private AppState state;
    private boolean  appliedDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        appliedDarkTheme = state.isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_salas);
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state.isDarkTheme() != appliedDarkTheme) { recreate(); return; }
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_salas);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_temas) {
                startActivity(new Intent(this, TemasActivity.class));
                return true;
            } else if (id == R.id.nav_salas) {
                return true;
            } else if (id == R.id.nav_stats) {
                startActivity(new Intent(this, EstadisticasActivity.class));
                return true;
            } else if (id == R.id.nav_config) {
                startActivity(new Intent(this, ConfiguracionActivity.class));
                return true;
            }
            return false;
        });
    }
}
