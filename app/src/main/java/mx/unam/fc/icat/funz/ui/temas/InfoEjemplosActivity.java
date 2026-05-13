package mx.unam.fc.icat.funz.ui.temas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.concurrent.Executors;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.db.FunZDatabase;
import mx.unam.fc.icat.funz.db.Module;
import mx.unam.fc.icat.funz.ui.ejercicios.ExerciseActivity;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.R;

/**
 * InfoEjemplosActivity — Pantalla C: Información / Ejemplos
 * Carga dinámicamente el contenido desde la base de datos según el moduleId.
 */
public class InfoEjemplosActivity extends AppCompatActivity {

    private AppState     state;
    private boolean      appliedDarkTheme;
    private LinearLayout drawerMenu;
    private int          currentTab = 0;
    private int          moduleId = 1;
    private Module       currentModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        appliedDarkTheme = state.isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_info_ejemplos);

        moduleId = getIntent().getIntExtra("module_id", 1);
        currentTab = getIntent().getIntExtra("tab", 0);

        bindViews();
        loadModuleData();
        setupHamburger();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state.isDarkTheme() != appliedDarkTheme) { recreate(); return; }
    }

    private void bindViews() {
        drawerMenu = findViewById(R.id.drawer_menu);
        drawerMenu.setVisibility(View.GONE);
        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());
    }

    private void loadModuleData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            currentModule = FunZDatabase.getInstance(this).moduleDao().getModuleSync(moduleId);
            runOnUiThread(() -> {
                if (currentModule != null) {
                    populateUI();
                    setupTabs();
                }
            });
        });
    }

    private void populateUI() {
        TextView tvTitle = findViewById(R.id.tv_module_title);
        if (tvTitle != null) tvTitle.setText(currentModule.name);

        ((TextView) findViewById(R.id.tv_info_title_1)).setText(currentModule.infoTitle1);
        ((TextView) findViewById(R.id.tv_info_text_1)).setText(currentModule.infoText1);
        ((TextView) findViewById(R.id.tv_info_title_2)).setText(currentModule.infoTitle2);
        ((TextView) findViewById(R.id.tv_info_text_2)).setText(currentModule.infoText2);
        ((TextView) findViewById(R.id.tv_info_title_3)).setText(currentModule.infoTitle3);
        ((TextView) findViewById(R.id.tv_info_text_3)).setText(currentModule.infoText3);

        renderExampleSteps();
    }

    private void renderExampleSteps() {
        LinearLayout mainContainer = findViewById(R.id.ll_examples_container);
        if (mainContainer == null) return;

        mainContainer.removeAllViews();

        try {
            JSONArray allSteps = new JSONArray(currentModule.exampleSteps);
            // Separamos las 3 ecuaciones que pusimos en el seeder con \n
            String[] equations = currentModule.exampleEquation.split("\n");
            String[] labels = {"Ejemplo A", "Ejemplo B", "Ejemplo C"};

            int stepPointer = 0;

            for (int i = 0; i < equations.length; i++) {
                // Inflar el cuadrado (Card)
                View cardView = getLayoutInflater().inflate(R.layout.item_example_card, mainContainer, false);

                TextView tvLabel = cardView.findViewById(R.id.tv_example_label);
                TextView tvEq = cardView.findViewById(R.id.tv_card_equation);
                LinearLayout llSteps = cardView.findViewById(R.id.ll_card_steps);

                tvLabel.setText(labels[i]);
                tvEq.setText(equations[i]);

                // Lógica para llenar los pasos de cada cuadrado
                while (stepPointer < allSteps.length()) {
                    String stepText = allSteps.getString(stepPointer);
                    stepPointer++;

                    if (stepText.isEmpty()) break; // El "" en el seeder indica fin de un ejemplo

                    // Inflar cada línea de la resolución
                    View stepItem = getLayoutInflater().inflate(R.layout.item_example_step, llSteps, false);
                    TextView tvDesc = stepItem.findViewById(R.id.tv_step_desc);
                    tvDesc.setText(stepText);

                    if (stepText.contains("✓")) {
                        tvDesc.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvDesc.setTextColor(getColor(R.color.color_primary));
                    }

                    llSteps.addView(stepItem);
                }
                mainContainer.addView(cardView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                state.setExamplesRead(moduleId, true);
                state.resetSession();
                goToExercise();
            }
        });
    }

    private void applyProgressUpdate(int tab) {
        if (tab == 0 && !state.isInfoRead(moduleId))     state.setInfoRead(moduleId, true);
        if (tab == 1 && !state.isExamplesRead(moduleId)) state.setExamplesRead(moduleId, true);
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
        Intent intent = new Intent(this, ExerciseActivity.class);
        intent.putExtra("module_id", moduleId);
        intent.putExtra("step_order", state.getCurrentStep(moduleId));
        startActivity(intent);
        finish();
    }

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
