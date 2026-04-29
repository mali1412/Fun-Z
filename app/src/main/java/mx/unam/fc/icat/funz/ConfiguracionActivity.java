package mx.unam.fc.icat.funz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

/**
 * ConfiguracionActivity — Pantalla N: Configuración
 *
 * Permite al usuario:
 *   1. Editar su nombre de usuario (se actualiza en la pantalla Home).
 *   2. Cambiar el tema entre claro y oscuro.
 *      Al guardar, se llama recreate() para que el nuevo tema
 *      se aplique inmediatamente en esta Activity.
 *      Las demás Activities leen AppState.isDarkTheme() en su onCreate().
 *
 * El botón "Guardar cambios" persiste los valores en AppState y
 * muestra un Toast de confirmación.
 */
public class ConfiguracionActivity extends AppCompatActivity {

    private AppState          state;
    private boolean           appliedDarkTheme;
    private TextInputEditText etUsername;
    private RadioGroup        rgTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        appliedDarkTheme = state.isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_configuracion);

        etUsername = findViewById(R.id.et_username);
        rgTheme    = findViewById(R.id.rg_theme);

        // Precargar valores actuales
        etUsername.setText(state.getUsername());
        rgTheme.check(state.isDarkTheme() ? R.id.rb_dark : R.id.rb_light);

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveConfig());

        setupNavigation();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Guardar configuración
    // ════════════════════════════════════════════════════════════════════════

    private void saveConfig() {
        // Nombre de usuario
        String newName = etUsername.getText() != null
                ? etUsername.getText().toString().trim()
                : "";
        if (!newName.isEmpty()) {
            state.setUsername(newName);
        }

        // Tema
        boolean dark = (rgTheme.getCheckedRadioButtonId() == R.id.rb_dark);
        boolean themeChanged = (dark != state.isDarkTheme());
        state.setDarkTheme(dark);

        Toast.makeText(this, getString(R.string.config_saved), Toast.LENGTH_SHORT).show();
        // Si el tema cambió, recrear la Activity para aplicarlo de inmediato
        if (themeChanged) {
            appliedDarkTheme = dark;
            recreate();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación global
    // ════════════════════════════════════════════════════════════════════════

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_config);
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
                startActivity(new Intent(this, EstadisticasActivity.class));
                return true;
            } else if (id == R.id.nav_config) {
                return true;
            }
            return false;
        });
    }
}
