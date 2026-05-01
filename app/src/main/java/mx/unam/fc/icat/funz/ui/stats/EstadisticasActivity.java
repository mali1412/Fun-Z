package mx.unam.fc.icat.funz.ui.stats;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import mx.unam.fc.icat.funz.viewmodel.EstadisticasViewModel;
import mx.unam.fc.icat.funz.R;
import  mx.unam.fc.icat.funz.data.AppState;
import  mx.unam.fc.icat.funz.ui.main.MainActivity;
import  mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import  mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;



/**
 * EstadisticasActivity — Pantalla M: Estadísticas.
 *
 * [MVVM] Observador pasivo de EstadisticasViewModel.
 * Recibe un único snapshot StatsUiState y distribuye cada campo
 * a su vista correspondiente. No accede directamente a AppState.
 */
public class EstadisticasActivity extends AppCompatActivity {

    private EstadisticasViewModel vm;
    private boolean               appliedDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedDarkTheme = AppState.getInstance().isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_estadisticas);

        vm = new ViewModelProvider(this).get(EstadisticasViewModel.class);

        observeViewModel();
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppState.getInstance().isDarkTheme() != appliedDarkTheme) { recreate(); return; }
        vm.refreshStats(); // solicita al ViewModel que actualice los LiveData
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Observadores — un solo objeto StatsUiState maneja toda la pantalla
    // ════════════════════════════════════════════════════════════════════════

    private void observeViewModel() {
        vm.uiState.observe(this, s -> {
            ((TextView)  findViewById(R.id.tv_stat_pts))
                    .setText(String.valueOf(s.totalPoints));
            ((TextView)  findViewById(R.id.tv_stat_prog))
                    .setText(s.mod1Progress + "%");
            ((TextView)  findViewById(R.id.tv_stat_streak))
                    .setText(String.valueOf(s.streakDays));
            ((TextView)  findViewById(R.id.tv_stat_resolved))
                    .setText(String.valueOf(s.exercisesResolved));
            ((ProgressBar) findViewById(R.id.pb_mod1_stat))
                    .setProgress(s.mod1Progress);
            ((TextView)  findViewById(R.id.tv_mod1_pct))
                    .setText(s.mod1Progress + "%");
            ((TextView)  findViewById(R.id.tv_mod2_status))
                    .setText(s.mod2Unlocked ? "0%" : "Bloqueado");
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación global
    // ════════════════════════════════════════════════════════════════════════

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_stats);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_inicio) { startActivity(new Intent(this, MainActivity.class));       finish(); return true; }
            else if (id == R.id.nav_temas)  { startActivity(new Intent(this, TemasActivity.class));      return true; }
            else if (id == R.id.nav_salas)  { startActivity(new Intent(this, SalasActivity.class));      return true; }
            else if (id == R.id.nav_stats)  { return true; }
            else if (id == R.id.nav_config) { startActivity(new Intent(this, ConfiguracionActivity.class)); return true; }
            return false;
        });
    }
}
