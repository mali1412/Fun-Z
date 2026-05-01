package mx.unam.fc.icat.funz.ui.ejercicios;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

import mx.unam.fc.icat.funz.ui.ejercicios.EjercicioTilesActivity;
import mx.unam.fc.icat.funz.viewmodel.EjercicioClasicoViewModel;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.ui.temas.InfoEjemplosActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.R;

/**
 * EjercicioClasicoActivity — Pantalla D2: Ejercicio 2/3 (Método Clásico/Baldor).
 *
 * [MVVM] Observador pasivo de EjercicioClasicoViewModel.
 * El desarrollo algebraico es estático en el XML; este Activity solo gestiona
 * el temporizador (via ViewModel) y la entrada del usuario.
 */
public class EjercicioClasicoActivity extends AppCompatActivity {

    private EjercicioClasicoViewModel vm;

    private Chip         chipTimer;
    private EditText     etAnswer;
    private LinearLayout drawerMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppState.getInstance().isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_ejercicio_clasico);

        vm = new ViewModelProvider(this).get(EjercicioClasicoViewModel.class);

        bindViews();
        observeViewModel();
        setupHamburger();
        vm.init();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Binding
    // ════════════════════════════════════════════════════════════════════════

    private void bindViews() {
        chipTimer  = findViewById(R.id.tv_timer);
        etAnswer   = findViewById(R.id.et_answer);
        drawerMenu = findViewById(R.id.drawer_menu);
        drawerMenu.setVisibility(View.GONE);

        findViewById(R.id.btn_back).setOnClickListener(v -> { vm.cancelTimer(); finish(); });
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        findViewById(R.id.btn_verify).setOnClickListener(v ->
                vm.verify(etAnswer.getText().toString()));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Observadores
    // ════════════════════════════════════════════════════════════════════════

    private void observeViewModel() {
        vm.timerDisplay.observe(this, chipTimer::setText);
        vm.timerUrgent.observe(this, urgent -> {
            if (Boolean.TRUE.equals(urgent)) {
                chipTimer.setChipBackgroundColorResource(R.color.error_bg);
                chipTimer.setTextColor(getColor(R.color.error));
            }
        });

        vm.timerFinished.observe(this, unused -> showTimeUpDialog());

        vm.exerciseResult.observe(this, result -> {
            switch (result) {
                case EMPTY_INPUT:
                    Toast.makeText(this, "Ingresa tu respuesta", Toast.LENGTH_SHORT).show();
                    break;
                case CORRECT:
                    showResultDialog(true, false); break;
                case CORRECT_WITH_HINT:
                    showResultDialog(true, true);  break;
                case INCORRECT:
                    showResultDialog(false, false); break;
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Pista (BottomSheet)
    // ════════════════════════════════════════════════════════════════════════

    private void showHint() {
        vm.useHint();
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_hint, null);
        ((TextView) v.findViewById(R.id.tv_hint_content)).setText(
                "Paso 1: Transponer +5 al otro lado con signo contrario.\n" +
                        "  3x = 20 − 5 = 15\n\n" +
                        "Paso 2: Dividir ambos lados entre el coeficiente 3.\n" +
                        "  x = 15 ÷ 3 = 5 ✓");
        v.findViewById(R.id.btn_close_hint).setOnClickListener(b -> sheet.dismiss());
        sheet.setContentView(v);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Diálogos
    // ════════════════════════════════════════════════════════════════════════

    private void showResultDialog(boolean correct, boolean withHint) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        if (correct) {
            int pts = withHint ? 50 : 100;
            String msg = "¡Bien hecho!\n+" + pts + " puntos";
            if (withHint) msg += "\n\nUsaste pista. ¡Reinténtalo sin pista para +50 extra!";
            b.setTitle("🎉 ¡Correcto!")
                    .setMessage(msg)
                    .setPositiveButton("Ej. 3/3 →", (d, w) -> goToNext())
                    .setCancelable(false);
            if (withHint) b.setNeutralButton("Sin pista (+50)", (d, w) -> {
                etAnswer.setText("");
                vm.retryWithoutHint();
            });
        } else {
            b.setTitle("🤔 Incorrecto")
                    .setMessage("Revisa la sección de Información.")
                    .setPositiveButton("📖 Información", (d, w) -> goToInfo())
                    .setNegativeButton("Reintentar", (d, w) -> vm.retryAfterFailure())
                    .setNeutralButton("Salir", (d, w) -> finish())
                    .setCancelable(false);
        }
        b.show();
    }

    private void showTimeUpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⏰ ¡Tiempo agotado!")
                .setPositiveButton("Reintentar", (d, w) -> vm.retryAfterFailure())
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false).show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación
    // ════════════════════════════════════════════════════════════════════════

    private void goToNext() { startActivity(new Intent(this, EjercicioTilesActivity.class)); finish(); }

    private void goToInfo() {
        Intent i = new Intent(this, InfoEjemplosActivity.class);
        i.putExtra("tab", 0);
        startActivity(i); finish();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Menú hamburguesa
    // ════════════════════════════════════════════════════════════════════════

    private void setupHamburger() {
        findViewById(R.id.btn_hamburger).setOnClickListener(v ->
                drawerMenu.setVisibility(
                        drawerMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        setupDrawerItem(R.id.drawer_inicio, MainActivity.class);
        setupDrawerItem(R.id.drawer_temas,  TemasActivity.class);
        setupDrawerItem(R.id.drawer_salas,  SalasActivity.class);
        setupDrawerItem(R.id.drawer_stats,  EstadisticasActivity.class);
        setupDrawerItem(R.id.drawer_config, ConfiguracionActivity.class);
    }

    private void setupDrawerItem(int viewId, Class<?> target) {
        View item = findViewById(viewId);
        if (item != null) item.setOnClickListener(v -> {
            vm.cancelTimer();
            drawerMenu.setVisibility(View.GONE);
            startActivity(new Intent(this, target));
        });
    }

    @Override
    protected void onDestroy() { super.onDestroy(); vm.cancelTimer(); }
}
