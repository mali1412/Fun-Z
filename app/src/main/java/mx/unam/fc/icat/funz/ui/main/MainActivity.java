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

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.ui.ejercicios.ExerciseActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.temas.InfoEjemplosActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.utils.AppIntentKeys;
import mx.unam.fc.icat.funz.viewmodel.MainViewModel;
import mx.unam.fc.icat.funz.R;

/**
 * MainActivity — Pantalla de Inicio.
 *
 * El botón "Continuar" (Play) ahora lleva directamente al último ejercicio
 * pendiente del módulo que el usuario estaba trabajando.
 */
public class MainActivity extends AppCompatActivity {

    private MainViewModel vm;
    private boolean       appliedDarkTheme;
    private TextView      tvWelcome, tvStreak, tvResumeTitle;
    private Chip          chipResumeBadge;
    private ProgressBar   pbResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedDarkTheme = AppState.getInstance().isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_main);

        vm = new ViewModelProvider(this).get(MainViewModel.class);

        tvWelcome       = findViewById(R.id.tv_welcome);
        tvStreak        = findViewById(R.id.tv_streak);
        tvResumeTitle   = findViewById(R.id.tv_resume_title);
        chipResumeBadge = findViewById(R.id.tv_resume_badge);
        pbResume        = findViewById(R.id.pb_resume);

        observeViewModel();
        setupActions();
    }

    private void observeViewModel() {
        vm.welcomeText.observe(this, tvWelcome::setText);
        vm.streakText .observe(this, tvStreak::setText);
        vm.resumeBadge.observe(this, chipResumeBadge::setText);
        vm.resumeProgress.observe(this, pbResume::setProgress);
        vm.resumeTitle.observe(this, tvResumeTitle::setText);
    }

    private void setupActions() {
        // Botón Continuar (Módulo activo) -> Lleva al último ejercicio guardado
        Button btnResume = findViewById(R.id.btn_resume);
        btnResume.setOnClickListener(v -> resumeExercise());

        MaterialCardView resumeCard = findViewById(R.id.card_resume);
        resumeCard.setOnClickListener(v -> resumeExercise());

        // Accesos rápidos estáticos
        findViewById(R.id.qa_temas) .setOnClickListener(v -> startActivity(new Intent(this, TemasActivity.class)));
        findViewById(R.id.qa_salas) .setOnClickListener(v -> startActivity(new Intent(this, SalasActivity.class)));
        findViewById(R.id.qa_stats) .setOnClickListener(v -> startActivity(new Intent(this, EstadisticasActivity.class)));
        findViewById(R.id.qa_config).setOnClickListener(v -> startActivity(new Intent(this, ConfiguracionActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppState.getInstance().isDarkTheme() != appliedDarkTheme) { recreate(); return; }
        vm.refreshUiState();
    }

    /**
     * Reanuda el aprendizaje en el punto exacto donde se quedó el usuario.
     */
    /**
     * Reanuda el aprendizaje en el punto exacto donde se quedó el usuario.
     */
    private void resumeExercise() {
        // Obtenemos el módulo activo y el paso actual desde el ViewModel/AppState
        int[] target = vm.getResumeTarget();
        int moduleId = target[0];
        int step     = target[1];

        // 1. Si el módulo actual ya está completo, intentamos pasar al siguiente
        if (vm.isActiveModuleComplete()) {
            // El siguiente módulo ya fue activado en AppState.markExerciseDone
            // por lo que volvemos a obtener el target.
            target = vm.getResumeTarget();
            moduleId = target[0];
            step     = target[1];
        }

        // 2. Si es el inicio de un módulo (paso 1) y NO ha leído la teoría,
        // lo mandamos a la pantalla de Información/Ejemplos primero.
        AppState state = AppState.getInstance();
        if (step == 1 && (!state.isInfoRead(moduleId) || !state.isExamplesRead(moduleId))) {
            Intent i = new Intent(this, InfoEjemplosActivity.class);
            i.putExtra(AppIntentKeys.MODULE_ID, moduleId);
            i.putExtra(AppIntentKeys.TAB, AppIntentKeys.TAB_INFO); // Empezar en la pestaña de Información
            startActivity(i);
            return;
        }

        // 3. De lo contrario, lo mandamos al ejercicio correspondiente
        Intent i = new Intent(this, ExerciseActivity.class);
        i.putExtra(AppIntentKeys.MODULE_ID,  moduleId);
        i.putExtra(AppIntentKeys.STEP_ORDER, step);
        startActivity(i);
    }
}
