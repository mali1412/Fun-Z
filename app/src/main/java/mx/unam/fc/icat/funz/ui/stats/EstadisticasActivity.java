package mx.unam.fc.icat.funz.ui.stats;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.db.Module;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.viewmodel.EstadisticasViewModel;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.R;

import java.util.List;

/**
 * EstadisticasActivity — Pantalla M: Estadísticas.
 *
 * Muestra el progreso global y el detalle de los módulos más recientes de forma dinámica.
 */
public class EstadisticasActivity extends AppCompatActivity {

    private EstadisticasViewModel vm;
    private boolean               appliedDarkTheme;
    private LinearLayout          llModulesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedDarkTheme = AppState.getInstance().isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_estadisticas);
        llModulesContainer = findViewById(R.id.ll_modules_progress_container);
        vm = new ViewModelProvider(this).get(EstadisticasViewModel.class);

        observeViewModel();
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppState.getInstance().isDarkTheme() != appliedDarkTheme) { recreate(); return; }
        vm.refreshStats(); // Refresca los datos desde AppState (progreso) y Room
    }

    private void observeViewModel() {
        vm.uiState.observe(this, s -> {
            // Actualizar tarjetas de resumen
            ((TextView) findViewById(R.id.tv_stat_pts)).setText(String.valueOf(s.totalPoints));
            ((TextView) findViewById(R.id.tv_stat_prog)).setText(s.totalProgress + "%");
            ((TextView) findViewById(R.id.tv_stat_streak)).setText(String.valueOf(s.streakDays));
            ((TextView) findViewById(R.id.tv_stat_resolved)).setText(String.valueOf(s.exercisesResolved));

            // Actualizar lista de módulos recientes
            renderModulesProgress(s.recentModules);

            // Actualizar medallas
            updateMedalsUI(s);
        });
    }

    private void renderModulesProgress(List<Module> modules) {
        if (llModulesContainer == null || modules == null) return;

        // Evitar parpadeo: solo reconstruir si la cantidad o los IDs cambian
        llModulesContainer.removeAllViews();

        // Re-añadir el título de sección si se borró
        TextView title = new TextView(this);
        title.setText("PROGRESO POR MÓDULO");
        title.setTextSize(10);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getColor(R.color.text_secondary));
        title.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dpToPx(8));
        title.setLayoutParams(params);
        llModulesContainer.addView(title);

        for (Module m : modules) {
            View itemView = getLayoutInflater().inflate(R.layout.item_stat_module, llModulesContainer, false);

            TextView tvName = itemView.findViewById(R.id.tv_mod_name);
            TextView tvStatus = itemView.findViewById(R.id.tv_mod_status);
            ProgressBar pb = itemView.findViewById(R.id.pb_mod_stat);

            tvName.setText(m.name);
            int progress = vm.getModuleProgress(m.id);

            if (m.unlocked) {
                tvStatus.setText(progress + "%");
                pb.setProgress(progress);
                tvStatus.setTextColor(getColor(R.color.color_primary));
            } else {
                tvStatus.setText("Bloqueado");
                pb.setProgress(0);
                tvStatus.setTextColor(getColor(R.color.text_secondary));
            }

            llModulesContainer.addView(itemView);
        }
    }

    private void updateMedalsUI(EstadisticasViewModel.StatsUiState s) {
        View m1 = findViewById(R.id.tv_medal_1);
        View mStreak = findViewById(R.id.tv_medal_streak);
        View mMod = findViewById(R.id.tv_medal_mod);

        if (s.exercisesResolved > 0) m1.setBackgroundResource(R.drawable.bg_medal_earned);
        if (s.streakDays >= 3) mStreak.setBackgroundResource(R.drawable.bg_medal_earned);
        if (s.totalProgress >= 50) mMod.setBackgroundResource(R.drawable.bg_medal_earned);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

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
