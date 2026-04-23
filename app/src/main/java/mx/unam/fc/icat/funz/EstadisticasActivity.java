package mx.unam.fc.icat.funz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * EstadisticasActivity — Pantalla M: Estadísticas
 *
 * Muestra al usuario su progreso global:
 *   - Puntos totales acumulados.
 *   - Porcentaje de progreso del Módulo 1.
 *   - Racha de días consecutivos.
 *   - Número de ejercicios resueltos.
 *   - Barras de progreso por módulo.
 *   - Galería de medallas (desbloqueadas y pendientes).
 *
 * onResume() refresca todos los datos desde AppState para que
 * los valores estén siempre actualizados al regresar de ejercicios.
 */
public class EstadisticasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_estadisticas);
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Refresco de estadísticas
    // ════════════════════════════════════════════════════════════════════════

    private void refreshStats() {
        AppState s = AppState.getInstance();

        // Tarjetas de estadísticas principales
        ((TextView) findViewById(R.id.tv_stat_pts))
                .setText(String.valueOf(s.getTotalPoints()));
        ((TextView) findViewById(R.id.tv_stat_prog))
                .setText(s.getMod1Progress() + "%");
        ((TextView) findViewById(R.id.tv_stat_streak))
                .setText(String.valueOf(s.getStreakDays()));

        // Ejercicios resueltos (suma de los tres pasos completados)
        int resolved = 0;
        if (s.isEx1Done()) resolved++;
        if (s.isEx2Done()) resolved++;
        if (s.isEx3Done()) resolved++;
        ((TextView) findViewById(R.id.tv_stat_resolved))
                .setText(String.valueOf(resolved));

        // Barra de progreso Módulo 1
        ((ProgressBar) findViewById(R.id.pb_mod1_stat))
                .setProgress(s.getMod1Progress());
        ((TextView) findViewById(R.id.tv_mod1_pct))
                .setText(s.getMod1Progress() + "%");

        // Estado Módulo 2
        ((TextView) findViewById(R.id.tv_mod2_status))
                .setText(s.isMod2Unlocked() ? "0%" : "Bloqueado");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación global
    // ════════════════════════════════════════════════════════════════════════

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_stats);
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
                startActivity(new Intent(this, SalasActivity.class));
                return true;
            } else if (id == R.id.nav_stats) {
                return true;
            } else if (id == R.id.nav_config) {
                startActivity(new Intent(this, ConfiguracionActivity.class));
                return true;
            }
            return false;
        });
    }
}
