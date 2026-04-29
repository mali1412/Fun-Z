package mx.unam.fc.icat.funz.ui.temas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import mx.unam.fc.icat.funz.ui.ejercicios.EjercicioBalanzaActivity;
import mx.unam.fc.icat.funz.ui.ejercicios.EjercicioClasicoActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.ejercicios.EjercicioTilesActivity;
import mx.unam.fc.icat.funz.ui.main.MainActivity;

/**
 * TemasActivity — Pantalla B: Sección de Temas
 *
 * Muestra la lista de módulos con su progreso.
 * La BottomNavigationView está en la parte superior (solo existe en
 * Temas, Salas, Estadísticas y Configuración).
 *
 * Patrón List Inlay (Tidwell): al tocar el Módulo 1, se despliegan
 * en la misma pantalla los botones Info / Ejemplos / Ejercicios
 * sin navegar a otra Activity.
 *
 * El progreso se actualiza en cada onResume() leyendo AppState.
 */
public class TemasActivity extends AppCompatActivity {

    private AppState state;
    private boolean  appliedDarkTheme;

    // ── Módulo 1 ──────────────────────────────────────────────────────────────
    private MaterialCardView cardMod1;
    private ProgressBar      pbMod1;
    private Chip             chipMod1Badge;
    private TextView         tvMod1Sub;
    private LinearLayout     llMod1Actions;

    // ── Módulo 2 ──────────────────────────────────────────────────────────────
    private MaterialCardView cardMod2;
    private TextView         tvMod2Lock;

    // ── Notas de estado ───────────────────────────────────────────────────────
    private TextView tvMod1Note;
    private TextView tvMod1DoneNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        appliedDarkTheme = state.isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_temas);
        bindViews();
        setupMod1Actions();
        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state.isDarkTheme() != appliedDarkTheme) { recreate(); return; }
        refreshModules();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Binding
    // ════════════════════════════════════════════════════════════════════════

    private void bindViews() {
        cardMod1       = findViewById(R.id.card_mod1);
        pbMod1         = findViewById(R.id.pb_mod1);
        chipMod1Badge  = findViewById(R.id.tv_mod1_badge);
        tvMod1Sub      = findViewById(R.id.tv_mod1_sub);
        llMod1Actions  = findViewById(R.id.ll_mod1_actions);
        cardMod2       = findViewById(R.id.card_mod2);
        tvMod2Lock     = findViewById(R.id.tv_mod2_lock);
        tvMod1Note     = findViewById(R.id.tv_mod1_note);
        tvMod1DoneNote = findViewById(R.id.tv_mod1_done_note);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Refresco de módulos
    // ════════════════════════════════════════════════════════════════════════

    private void refreshModules() {
        int pct = state.getMod1Progress();
        pbMod1.setProgress(pct);
        chipMod1Badge.setText(pct + "%");

        if (state.isMod1Complete()) {
            cardMod1.setStrokeColor(getColor(R.color.accent_green));
            tvMod1Sub.setText("x+a=b · ✅ Completado");
            tvMod1Note.setVisibility(View.GONE);
            tvMod1DoneNote.setVisibility(View.VISIBLE);
        } else {
            tvMod1Note.setVisibility(View.VISIBLE);
            tvMod1DoneNote.setVisibility(View.GONE);
        }

        if (state.isMod2Unlocked()) {
            cardMod2.setAlpha(1f);
            tvMod2Lock.setText("▶");
            cardMod2.setClickable(true);
            cardMod2.setOnClickListener(v ->
                    Toast.makeText(this, "Módulo 2: ¡Próximamente!", Toast.LENGTH_SHORT).show());
        } else {
            cardMod2.setAlpha(0.45f);
            tvMod2Lock.setText("🔒");
            cardMod2.setOnClickListener(v ->
                    Toast.makeText(this, "Completa el Módulo 1 primero", Toast.LENGTH_SHORT).show());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Acciones del Módulo 1 (List Inlay)
    // ════════════════════════════════════════════════════════════════════════

    private void setupMod1Actions() {
        // Tap en la tarjeta: mostrar/ocultar sub-botones (List Inlay)
        cardMod1.setOnClickListener(v -> {
            int vis = llMod1Actions.getVisibility();
            llMod1Actions.setVisibility(vis == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        Button btnInfo = findViewById(R.id.btn_info);
        Button btnEjemplos = findViewById(R.id.btn_ejemplos);
        Button btnEjercicios = findViewById(R.id.btn_ejercicios);

        btnInfo.setOnClickListener(v -> {
            Intent i = new Intent(this, InfoEjemplosActivity.class);
            i.putExtra("tab", 0);
            startActivity(i);
        });

        btnEjemplos.setOnClickListener(v -> {
            Intent i = new Intent(this, InfoEjemplosActivity.class);
            i.putExtra("tab", 1);
            startActivity(i);
        });

        btnEjercicios.setOnClickListener(v -> startExFlow());
    }

    private void startExFlow() {
        state.resetSession();
        Intent intent;
        switch (state.getCurrentExStep()) {
            case 2:  intent = new Intent(this, EjercicioClasicoActivity.class); break;
            case 3:  intent = new Intent(this, EjercicioTilesActivity.class);   break;
            default: intent = new Intent(this, EjercicioBalanzaActivity.class);
        }
        startActivity(intent);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación global
    // ════════════════════════════════════════════════════════════════════════

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_temas);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_temas) {
                return true;
            } else if (id == R.id.nav_salas) {
                startActivity(new Intent(this, SalasActivity.class));
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
