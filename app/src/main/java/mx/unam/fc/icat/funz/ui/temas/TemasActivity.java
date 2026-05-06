package mx.unam.fc.icat.funz.ui.temas;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.List;

import mx.unam.fc.icat.funz.db.Module;
import mx.unam.fc.icat.funz.ui.ejercicios.ExerciseActivity;
import mx.unam.fc.icat.funz.viewmodel.TemasViewModel;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.R;

/**
 * TemasActivity — Pantalla B: Lista de Módulos.
 *
 * [MVVM + Data-Driven] Observa TemasViewModel.modules (LiveData<List<Module>>).
 * Cada vez que Room cambia la tabla 'modules' (p.ej. al desbloquear uno),
 * esta Activity se actualiza automáticamente.
 *
 * Para agregar un Módulo 4: insertar la fila en SQLite.
 * Esta Activity lo mostrará sin ningún cambio de código.
 */
public class TemasActivity extends AppCompatActivity {

    private TemasViewModel vm;
    private LinearLayout   llModulesContainer;
    private boolean        appliedDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedDarkTheme = AppState.getInstance().isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_temas);

        llModulesContainer = findViewById(R.id.ll_modules_container);

        vm = new ViewModelProvider(this).get(TemasViewModel.class);
        vm.modules.observe(this, this::renderModules);

        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppState.getInstance().isDarkTheme() != appliedDarkTheme) recreate();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Renderizado dinámico de módulos
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Construye dinámicamente una card por cada módulo en la lista.
     * Funciona para cualquier cantidad de módulos sin cambiar código.
     */
    // Busca el método renderModules y actualízalo así:

    private void renderModules(List<Module> modules) {
        // 1. Si la lista es nula o está vacía (primer inicio),
        // detenemos el proceso para evitar errores de puntero nulo o bucles vacíos.
        if (modules == null || modules.isEmpty()) {
            llModulesContainer.removeAllViews();
            // Opcional: Podrías inflar una vista de "Cargando..." aquí
            return;
        }

        // 2. Limpiar el contenedor antes de renderizar
        llModulesContainer.removeAllViews();

        // 3. Renderizar solo si tenemos datos seguros
        for (Module mod : modules) {
            View moduleView = buildModuleCard(mod);
            if (moduleView != null) {
                llModulesContainer.addView(moduleView);
            }
        }
    }

    /**
     * Infla la card de un módulo y la configura según su estado.
     * Si el módulo está bloqueado, muestra un overlay semitransparente.
     */
    private View buildModuleCard(Module mod) {
        View card = getLayoutInflater().inflate(R.layout.item_module_card, llModulesContainer, false);

        TextView tvName      = card.findViewById(R.id.tv_mod_name);
        TextView tvSub       = card.findViewById(R.id.tv_mod_sub);
        Chip     chipBadge   = card.findViewById(R.id.chip_mod_badge);
        ProgressBar pbMod    = card.findViewById(R.id.pb_mod);
        LinearLayout llActions = card.findViewById(R.id.ll_mod_actions);
        View     lockOverlay = card.findViewById(R.id.view_lock_overlay);
        TextView tvLockMsg   = card.findViewById(R.id.tv_lock_msg);

        tvName.setText(mod.name);
        tvSub.setText(mod.subtitle);

        int pct = vm.getModuleProgress(mod.id);
        pbMod.setProgress(pct);
        chipBadge.setText(pct + "%");

        if (!mod.unlocked) {
            // Módulo bloqueado
            lockOverlay.setVisibility(View.VISIBLE);
            tvLockMsg.setText("Completa el módulo anterior para desbloquear");
            llActions.setVisibility(View.GONE);
            card.setOnClickListener(v ->
                    Toast.makeText(this, "🔒 " + mod.name + " bloqueado", Toast.LENGTH_SHORT).show());
        } else {
            // Módulo disponible
            lockOverlay.setVisibility(View.GONE);

            // List Inlay: tap en card → mostrar/ocultar botones
            MaterialCardView materialCard = card.findViewById(R.id.material_card);
            if (materialCard != null) {
                materialCard.setOnClickListener(v -> llActions.setVisibility(
                        llActions.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
            }

            Button btnInfo      = card.findViewById(R.id.btn_info);
            Button btnEjemplos  = card.findViewById(R.id.btn_ejemplos);
            Button btnEjercicios = card.findViewById(R.id.btn_ejercicios);

            btnInfo.setOnClickListener(v -> {
                Intent i = new Intent(this, InfoEjemplosActivity.class);
                i.putExtra("module_id", mod.id);
                i.putExtra("tab", 0);
                startActivity(i);
            });

            btnEjemplos.setOnClickListener(v -> {
                Intent i = new Intent(this, InfoEjemplosActivity.class);
                i.putExtra("module_id", mod.id);
                i.putExtra("tab", 1);
                startActivity(i);
            });

            btnEjercicios.setOnClickListener(v -> startExercise(mod.id));

            // Borde verde si completado
            AppState state = AppState.getInstance();
            if (state.isModuleComplete(mod.id)) {
                chipBadge.setText("✅");
                tvSub.setText(mod.subtitle + " · Completado");
            }
        }

        return card;
    }

    private void startExercise(int moduleId) {
        int[] target = vm.getStartTarget(moduleId);
        Intent i = new Intent(this, ExerciseActivity.class);
        i.putExtra("module_id",  target[0]);
        i.putExtra("step_order", target[1]);
        startActivity(i);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación
    // ════════════════════════════════════════════════════════════════════════

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_temas);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_inicio) { startActivity(new Intent(this, MainActivity.class)); finish(); return true; }
            else if (id == R.id.nav_temas)  { return true; }
            else if (id == R.id.nav_salas)  { startActivity(new Intent(this, SalasActivity.class)); return true; }
            else if (id == R.id.nav_stats)  { startActivity(new Intent(this, EstadisticasActivity.class)); return true; }
            else if (id == R.id.nav_config) { startActivity(new Intent(this, ConfiguracionActivity.class)); return true; }
            return false;
        });
    }
}
