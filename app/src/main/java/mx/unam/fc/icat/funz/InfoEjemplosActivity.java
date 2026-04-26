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
 */
public class InfoEjemplosActivity extends AppCompatActivity {

    private AppState     state;
    private LinearLayout drawerMenu;
    private int          currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_info_ejemplos);

        currentTab = getIntent().getIntExtra("tab", 0);

        bindViews();
        setupTabs();
        setupHamburger();
    }

    private void bindViews() {
        drawerMenu = findViewById(R.id.drawer_menu);
        if (drawerMenu != null) {
            drawerMenu.setVisibility(View.GONE);
        }

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnVolver = findViewById(R.id.btn_volver);
        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }
    }

    private void setupTabs() {
        TabLayout tabs    = findViewById(R.id.tabs);
        Button    btnNext = findViewById(R.id.btn_next);

        if (tabs == null || btnNext == null) return;

        tabs.addTab(tabs.newTab().setText("Información"));
        tabs.addTab(tabs.newTab().setText("Ejemplos"));
        
        if (currentTab < tabs.getTabCount()) {
            tabs.selectTab(tabs.getTabAt(currentTab));
        }

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
        if (infoContent != null) infoContent.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        if (ejemplosContent != null) ejemplosContent.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
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

    private void setupHamburger() {
        View btnHam = findViewById(R.id.btn_hamburger);
        if (btnHam != null && drawerMenu != null) {
            btnHam.setOnClickListener(v -> {
                drawerMenu.setVisibility(
                        drawerMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });
        }

        setupDrawerItem(R.id.drawer_inicio,  MainActivity.class);
        setupDrawerItem(R.id.drawer_temas,   TemasActivity.class);
        setupDrawerItem(R.id.drawer_salas,   SalasActivity.class);
        setupDrawerItem(R.id.drawer_stats,   EstadisticasActivity.class);
        setupDrawerItem(R.id.drawer_config,  ConfiguracionActivity.class);
    }

    private void setupDrawerItem(int viewId, Class<?> target) {
        View item = findViewById(viewId);
        if (item != null && drawerMenu != null) {
            item.setOnClickListener(v -> {
                drawerMenu.setVisibility(View.GONE);
                startActivity(new Intent(this, target));
            });
        }
    }
}
