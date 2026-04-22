package com.unam.funz;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_salas);
        setupNavigation();
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
