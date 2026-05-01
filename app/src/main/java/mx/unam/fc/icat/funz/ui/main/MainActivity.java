package mx.unam.fc.icat.funz.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import mx.unam.fc.icat.funz.viewmodel.MainViewModel;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.ui.ejercicios.EjercicioBalanzaActivity;
import mx.unam.fc.icat.funz.ui.ejercicios.EjercicioTilesActivity;
import mx.unam.fc.icat.funz.ui.ejercicios.EjercicioClasicoActivity;



/**
 * MainActivity — Pantalla A: Inicio.
 *
 * [MVVM] Observador pasivo de MainViewModel.
 * Muestra el estado de bienvenida, racha y progreso del módulo.
 * La decisión de qué pantalla abrir al pulsar "Continuar" la toma el ViewModel.
 */
public class MainActivity extends AppCompatActivity {

    private MainViewModel vm;
    private boolean       appliedDarkTheme;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView     tvWelcome, tvStreak, tvResumeMethod, tvResumeEq;
    private Chip         chipResumeBadge;
    private ProgressBar  pbResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState state = AppState.getInstance();
        appliedDarkTheme = state.isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_main);

        vm = new ViewModelProvider(this).get(MainViewModel.class);

        bindViews();
        observeViewModel();
        setupQuickAccess();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppState.getInstance().isDarkTheme() != appliedDarkTheme) { recreate(); return; }
        vm.refreshUiState(); // actualiza los LiveData desde AppState
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
    //  Observadores
    // ════════════════════════════════════════════════════════════════════════

    private void observeViewModel() {
        vm.welcomeText   .observe(this, tvWelcome::setText);
        vm.streakText    .observe(this, tvStreak::setText);
        vm.resumeBadge   .observe(this, chipResumeBadge::setText);
        vm.resumeMethod  .observe(this, tvResumeMethod::setText);
        vm.resumeEquation.observe(this, tvResumeEq::setText);
        vm.mod1Progress  .observe(this, pbResume::setProgress);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Acceso rápido (grid 2×2)
    // ════════════════════════════════════════════════════════════════════════

    private void setupQuickAccess() {
        findViewById(R.id.qa_temas) .setOnClickListener(v -> startActivity(new Intent(this, TemasActivity.class)));
        findViewById(R.id.qa_salas) .setOnClickListener(v -> startActivity(new Intent(this, SalasActivity.class)));
        findViewById(R.id.qa_stats) .setOnClickListener(v -> startActivity(new Intent(this, EstadisticasActivity.class)));
        findViewById(R.id.qa_config).setOnClickListener(v -> startActivity(new Intent(this, ConfiguracionActivity.class)));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Reanudación del ejercicio — el ViewModel decide el destino
    // ════════════════════════════════════════════════════════════════════════

    private void resumeExercise() {
        MainViewModel.Destino destino = vm.getResumeDestino();
        Intent intent;
        switch (destino) {
            case EJERCICIO_CLASICO: intent = new Intent(this, EjercicioClasicoActivity.class); break;
            case EJERCICIO_TILES:   intent = new Intent(this, EjercicioTilesActivity.class);   break;
            case TEMAS:             intent = new Intent(this, TemasActivity.class);             break;
            default:                intent = new Intent(this, EjercicioBalanzaActivity.class); break;
        }
        startActivity(intent);
    }
}
