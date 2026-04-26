package mx.unam.fc.icat.funz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

/**
 * ConfiguracionActivity — Pantalla N: Configuración
 */
public class ConfiguracionActivity extends AppCompatActivity {

    private AppState          state;
    private TextInputEditText etUsername;
    private RadioGroup        rgTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        
        // Aplicar el tema actual antes de setContentView
        if (state.isDarkTheme()) {
            setTheme(R.style.Theme_FunZ_Dark);
        } else {
            setTheme(R.style.Theme_FunZ);
        }
        
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

    private void saveConfig() {
        String newName = etUsername.getText() != null
                ? etUsername.getText().toString().trim()
                : "";
        if (!newName.isEmpty()) {
            state.setUsername(newName);
        }

        boolean dark = (rgTheme.getCheckedRadioButtonId() == R.id.rb_dark);
        boolean themeChanged = (dark != state.isDarkTheme());
        
        if (themeChanged) {
            state.setDarkTheme(dark);
            // Aplicar el modo noche globalmente usando AppCompatDelegate
            AppCompatDelegate.setDefaultNightMode(
                    dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            Toast.makeText(this, "Tema actualizado", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "✓ Cambios guardados", Toast.LENGTH_SHORT).show();
        }
    }

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
