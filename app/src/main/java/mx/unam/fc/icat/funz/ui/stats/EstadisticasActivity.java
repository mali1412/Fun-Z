package mx.unam.fc.icat.funz.ui.stats;

import mx.unam.fc.icat.funz.utils.NavigationUtils;

public class EstadisticasActivity extends AppCompatActivity {

    private StatsViewModel viewModel;
    private ActivityEstadisticasBinding binding; // Recomendado usar ViewBinding
    private boolean appliedDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        // Configurar tema inicial
        appliedDarkTheme = AppState.getInstance().isDarkTheme();
        if (appliedDarkTheme)
            setTheme(R.style.Theme_FunZ_Dark);

        setContentView(R.layout.activity_estadisticas);

        // 1. Configurar Observadores
        setupObservers();

        // 2. Configurar Navegación (Lógica de UI pura)
        // setupNavigation();
        NavigationUtils.setupBottomNavigation(this, findViewById(R.id.bottom_nav), R.id.nav_stats);
    }

    private void setupObservers() {
        viewModel.getStatsState().observe(this, state -> {
            // Actúa como observador pasivo: solo asigna valores
            ((TextView) findViewById(R.id.tv_stat_pts)).setText(String.valueOf(state.points));
            ((TextView) findViewById(R.id.tv_stat_prog)).setText(state.progress + "%");
            ((TextView) findViewById(R.id.tv_stat_streak)).setText(String.valueOf(state.streak));
            ((TextView) findViewById(R.id.tv_stat_resolved)).setText(String.valueOf(state.resolved));

            ((ProgressBar) findViewById(R.id.pb_mod1_stat)).setProgress(state.progress);
            ((TextView) findViewById(R.id.tv_mod1_pct)).setText(state.progress + "%");
            ((TextView) findViewById(R.id.tv_mod2_status)).setText(state.mod2Status);

            // Verificación de cambio de tema para recrear si es necesario
            if (state.isDarkTheme != appliedDarkTheme) {
                recreate();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Le pedimos al ViewModel que refresque los datos
        viewModel.loadStats();
    }

    /*
     * Configurar navegación
     * private void setupNavigation() {
     * BottomNavigationView nav = findViewById(R.id.bottom_nav);
     * nav.setSelectedItemId(R.id.nav_stats);
     * nav.setOnItemSelectedListener(item -> {
     * int id = item.getItemId();
     * if (id == R.id.nav_stats)
     * return true;
     * 
     * Intent intent = null;
     * if (id == R.id.nav_inicio)
     * intent = new Intent(this, MainActivity.class);
     * else if (id == R.id.nav_temas)
     * intent = new Intent(this, TemasActivity.class);
     * else if (id == R.id.nav_salas)
     * intent = new Intent(this, SalasActivity.class);
     * else if (id == R.id.nav_config)
     * intent = new Intent(this, ConfiguracionActivity.class);
     * 
     * if (intent != null) {
     * startActivity(intent);
     * if (id == R.id.nav_inicio)
     * finish();
     * return true;
     * }
     * return false;
     * });
     * }
     */
}