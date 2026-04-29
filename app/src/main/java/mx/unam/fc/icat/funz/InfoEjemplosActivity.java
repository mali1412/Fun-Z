package mx.unam.fc.icat.funz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;

/**
 * InfoEjemplosActivity — Pantalla C: Información / Ejemplos
 *
 * Dos pestañas en TabLayout:
 *   0 = Información  (tarjetas de teoría + video)
 *   1 = Ejemplos     (procedimiento paso a paso)
 *
 * NO tiene BottomNavigationView.
 * En su lugar tiene un botón hamburguesa (≡) que despliega
 * un drawer con los cinco destinos de navegación global.
 *
 * Al cambiar de pestaña se actualiza AppState:
 *   - Pestaña 0 → state.setInfoRead(true)
 *   - Pestaña 1 → state.setExamplesRead(true)
 * Esto incrementa el progreso del Módulo 1 en un 20% por hito.
 */
public class InfoEjemplosActivity extends AppCompatActivity {

    private AppState     state;
    private boolean      appliedDarkTheme;
    private LinearLayout drawerMenu;
    private int          currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        appliedDarkTheme = state.isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_info_ejemplos);

        currentTab = getIntent().getIntExtra("tab", 0);

        bindViews();
        setupTabs();
        setupHamburger();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state.isDarkTheme() != appliedDarkTheme) { recreate(); return; }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Binding
    // ════════════════════════════════════════════════════════════════════════

    private void bindViews() {
        drawerMenu = findViewById(R.id.drawer_menu);
        drawerMenu.setVisibility(View.GONE);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Tabs
    // ════════════════════════════════════════════════════════════════════════

    private void setupTabs() {
        TabLayout tabs    = findViewById(R.id.tabs);
        Button    btnNext = findViewById(R.id.btn_next);

        tabs.addTab(tabs.newTab().setText("Información"));
        tabs.addTab(tabs.newTab().setText("Ejemplos"));
        tabs.selectTab(tabs.getTabAt(currentTab));

        updateNextButton(btnNext);
        showTabContent(currentTab);
        applyProgressUpdate(currentTab);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                showTabContent(currentTab);
                updateNextButton(btnNext);
                applyProgressUpdate(currentTab);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnNext.setOnClickListener(v -> {
            if (currentTab == 0) {
                tabs.selectTab(tabs.getTabAt(1));
            } else {
                // Avanzar a ejercicios desde el paso guardado
                state.setExamplesRead(true);
                state.resetSession();
                goToExercise();
            }
        });
    }

    private void applyProgressUpdate(int tab) {
        if (tab == 0 && !state.isInfoRead())     state.setInfoRead(true);
        if (tab == 1 && !state.isExamplesRead()) state.setExamplesRead(true);
    }

    private void updateNextButton(Button btn) {
        btn.setText(currentTab == 0 ? "Ejemplos →" : "Ejercicios →");
        TextView tvIndicator = findViewById(R.id.tv_page_indicator);
        if (tvIndicator != null) {
            tvIndicator.setText((currentTab + 1) + " / 2");
        }
    }

    private void showTabContent(int tab) {
        View infoContent     = findViewById(R.id.content_info);
        View ejemplosContent = findViewById(R.id.content_ejemplos);
        infoContent.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        ejemplosContent.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
    }

    private void goToExercise() {
        Intent intent;
        switch (state.getCurrentExStep()) {
            case 2:  intent = new Intent(this, EjercicioClasicoActivity.class); break;
            case 3:  intent = new Intent(this, EjercicioTilesActivity.class);   break;
            default: intent = new Intent(this, EjercicioBalanzaActivity.class);
        }
        startActivity(intent);
        finish();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Hamburguesa
    // ════════════════════════════════════════════════════════════════════════

    private void setupHamburger() {
        View btnHam = findViewById(R.id.btn_hamburger);
        btnHam.setOnClickListener(v -> {
            drawerMenu.setVisibility(
                    drawerMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        setupDrawerItem(R.id.drawer_inicio,  MainActivity.class);
        setupDrawerItem(R.id.drawer_temas,   TemasActivity.class);
        setupDrawerItem(R.id.drawer_salas,   SalasActivity.class);
        setupDrawerItem(R.id.drawer_stats,   EstadisticasActivity.class);
        setupDrawerItem(R.id.drawer_config,  ConfiguracionActivity.class);
    }

    private void setupDrawerItem(int viewId, Class<?> target) {
        View item = findViewById(viewId);
        if (item != null) {
            item.setOnClickListener(v -> {
                drawerMenu.setVisibility(View.GONE);
                startActivity(new Intent(this, target));
            });
        }
    }
}
