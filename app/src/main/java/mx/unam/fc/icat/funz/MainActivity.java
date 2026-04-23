package mx.unam.fc.icat.funz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

/**
 * MainActivity — Pantalla A: Inicio
 *
 * Muestra:
 *   - Saludo dinámico con username leído de AppState.
 *   - Medallas y racha de días.
 *   - Card "Continuar" que refleja el paso actual del módulo.
 *   - Cuadrícula 2×2 de Acceso Rápido (Temas, Salas, Stats, Config).
 *
 * Esta pantalla NO tiene BottomNavigationView.
 * La navegación global solo existe en Temas, Salas, Stats y Config.
 */

public class MainActivity extends AppCompatActivity {


    private AppState state;

    // ── Views ────────────────────────────────────────────────────────────────
    private TextView     tvWelcome;
    private TextView     tvStreak;
    private Chip         chipResumeBadge;
    private TextView     tvResumeMethod;
    private TextView     tvResumeEq;
    private ProgressBar  pbResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        if (state.isDarkTheme()) {
            setTheme(R.style.Theme_FunZ_Dark);
        }
        setContentView(R.layout.activity_main);
        bindViews();
        setupQuickAccess();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Binding
    // ════════════════════════════════════════════════════════════════════════

    private void bindViews() {
        tvWelcome       = findViewById(R.id.tv_welcome);
        tvStreak        = findViewById(R.id.tv_streak);
        chipResumeBadge = findViewById(R.id.tv_resume_badge);
        tvResumeMethod  = findViewById(R.id.tv_resume_method);
        tvResumeEq      = findViewById(R.id.tv_resume_eq);
        pbResume        = findViewById(R.id.pb_resume);

        Button btnResume = findViewById(R.id.btn_resume);
        btnResume.setOnClickListener(v -> resumeExercise());

        MaterialCardView resumeCard = findViewById(R.id.card_resume);
        resumeCard.setOnClickListener(v -> resumeExercise());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Actualización de UI
    // ════════════════════════════════════════════════════════════════════════

    private void refreshUI() {
        tvWelcome.setText("¡Hola, " + state.getUsername() + "!");
        tvStreak.setText("🔥 Racha: " + state.getStreakDays() + " días");

        chipResumeBadge.setText(state.getResumeBadge());
        tvResumeMethod.setText(state.getResumeMethod());
        tvResumeEq.setText(state.getResumeEquation());
        pbResume.setProgress(state.getMod1Progress());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación de acceso rápido (grid 2×2)
    // ════════════════════════════════════════════════════════════════════════

    private void setupQuickAccess() {
        findViewById(R.id.qa_temas).setOnClickListener(v ->
                startActivity(new Intent(this, TemasActivity.class)));

        findViewById(R.id.qa_salas).setOnClickListener(v ->
                startActivity(new Intent(this, SalasActivity.class)));

        findViewById(R.id.qa_stats).setOnClickListener(v ->
                startActivity(new Intent(this, EstadisticasActivity.class)));

        findViewById(R.id.qa_config).setOnClickListener(v ->
                startActivity(new Intent(this, ConfiguracionActivity.class)));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Reanudación de ejercicio
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Navega al ejercicio pendiente según AppState.currentExStep.
     * Si el módulo ya está completo, va a Temas.
     */
    private void resumeExercise() {
        if (state.isMod1Complete()) {
            startActivity(new Intent(this, TemasActivity.class));
            return;
        }
        state.resetSession();
        Intent intent;
        switch (state.getCurrentExStep()) {
            case 2:
                intent = new Intent(this, EjercicioClasicoActivity.class);
                break;
            case 3:
                intent = new Intent(this, EjercicioTilesActivity.class);
                break;
            default:
                intent = new Intent(this, EjercicioBalanzaActivity.class);
        }
        startActivity(intent);
    }
}
