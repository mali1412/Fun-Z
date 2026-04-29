package mx.unam.fc.icat.funz.ui.config;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.utils.NavigationUtils;
import mx.unam.fc.icat.funz.viewmodel.ConfigViewModel;

public class ConfiguracionActivity extends AppCompatActivity {

    private ConfigViewModel viewModel;
    private TextInputEditText etUsername;
    private RadioGroup rgTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar ViewModel primero para saber qué tema aplicar
        viewModel = new ViewModelProvider(this).get(ConfigViewModel.class);

        if (viewModel.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_configuracion);

        etUsername = findViewById(R.id.et_username);
        rgTheme    = findViewById(R.id.rg_theme);

        // UI carga datos iniciales desde el ViewModel
        etUsername.setText(viewModel.getUsername());
        rgTheme.check(viewModel.isDarkTheme() ? R.id.rb_dark : R.id.rb_light);

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            String name = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            boolean isDark = (rgTheme.getCheckedRadioButtonId() == R.id.rb_dark);
            viewModel.saveConfiguration(name, isDark);
        });

        setupObservers();
        NavigationUtils.setupBottomNavigation(this, findViewById(R.id.bottom_nav), R.id.nav_config);
    }

    private void setupObservers() {
        // Observamos si se guardó para dar feedback
        viewModel.getConfigSaved().observe(this, saved -> {
            if (saved) Toast.makeText(this, "✓ Cambios guardados", Toast.LENGTH_SHORT).show();
        });

        // Observamos si el tema cambió para recrear la Activity
        viewModel.getThemeChanged().observe(this, changed -> {
            if (changed) recreate();
        });
    }
}