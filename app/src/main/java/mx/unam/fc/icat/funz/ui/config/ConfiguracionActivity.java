package mx.unam.fc.icat.funz.ui.config;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.viewmodel.ConfiguracionViewModel;
import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.data.AppState;


/**
 * ConfiguracionActivity — Pantalla N: Configuración.
 *
 * [MVVM] Observador pasivo de ConfiguracionViewModel.
 * Pre-carga los campos desde el ViewModel y delega el guardado al ViewModel.
 * Reacciona al evento SaveResult para mostrar un Toast o llamar a recreate().
 */
public class ConfiguracionActivity extends AppCompatActivity {

    private ConfiguracionViewModel vm;
    private boolean                appliedDarkTheme;

    private TextInputEditText etUsername;
    private RadioGroup        rgTheme;
    private RadioGroup        rgHaptic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState state = AppState.getInstance();
        appliedDarkTheme = state.isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_configuracion);

        vm = new ViewModelProvider(this).get(ConfiguracionViewModel.class);

        etUsername = findViewById(R.id.et_username);
        rgTheme    = findViewById(R.id.rg_theme);
        rgHaptic   = findViewById(R.id.rg_haptic);

        observeViewModel();
        vm.loadCurrentConfig(); // dispara la carga inicial de los LiveData

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            String newName = etUsername.getText() != null
                    ? etUsername.getText().toString().trim()
                    : "";
            boolean isDark = (rgTheme.getCheckedRadioButtonId() == R.id.rb_dark);
            boolean hapticEnabled = (rgHaptic.getCheckedRadioButtonId() == R.id.rb_haptic_on);
            vm.saveConfig(newName, isDark, hapticEnabled);
        });

        setupNavigation();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Observadores
    // ════════════════════════════════════════════════════════════════════════

    private void observeViewModel() {
        // Pre-carga de valores actuales
        vm.currentUsername.observe(this, etUsername::setText);
        vm.currentDarkTheme.observe(this, dark ->
                rgTheme.check(Boolean.TRUE.equals(dark) ? R.id.rb_dark : R.id.rb_light));
        vm.currentHapticFeedback.observe(this, enabled ->
                rgHaptic.check(Boolean.TRUE.equals(enabled) ? R.id.rb_haptic_on : R.id.rb_haptic_off));

        // Evento de resultado del guardado (un solo disparo)
        vm.saveEvent.observe(this, result -> {
            Toast.makeText(this, "✓ Cambios guardados", Toast.LENGTH_SHORT).show();
            if (result == ConfiguracionViewModel.SaveResult.THEME_CHANGED) {
                appliedDarkTheme = AppState.getInstance().isDarkTheme();
                recreate(); // re-aplica el tema de inmediato
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación global
    // ════════════════════════════════════════════════════════════════════════

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_config);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_inicio) { startActivity(new Intent(this, MainActivity.class));       finish(); return true; }
            else if (id == R.id.nav_temas)  { startActivity(new Intent(this, TemasActivity.class));      return true; }
            else if (id == R.id.nav_salas)  { startActivity(new Intent(this, SalasActivity.class));      return true; }
            else if (id == R.id.nav_stats)  { startActivity(new Intent(this, EstadisticasActivity.class)); return true; }
            else if (id == R.id.nav_config) { return true; }
            return false;
        });
    }
}
