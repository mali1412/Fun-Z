package mx.unam.fc.icat.funz.ui.ejercicios;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

import mx.unam.fc.icat.funz.viewmodel.EjercicioBalanzaViewModel;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.ui.temas.InfoEjemplosActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;



/**
 * EjercicioBalanzaActivity — Pantalla D1: Ejercicio 1/3 (Método de la Balanza).
 *
 * [MVVM] Observador pasivo de EjercicioBalanzaViewModel.
 * No contiene lógica matemática ni de gamificación.
 * Solo liga: eventos UI → llamadas al ViewModel, y LiveData → actualizaciones de Vista.
 */
public class EjercicioBalanzaActivity extends AppCompatActivity {

    private EjercicioBalanzaViewModel vm;

    // ── Views ─────────────────────────────────────────────────────────────────
    private Chip         chipTimer;
    private TextView     tvLhs, tvRhs, tvStatus;
    private EditText     etAnswer;
    private ImageView    ivBalanza;
    private LinearLayout drawerMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppState.getInstance().isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_ejercicio_balanza);

        vm = new ViewModelProvider(this).get(EjercicioBalanzaViewModel.class);

        bindViews();
        observeViewModel();
        setupOpButtons();
        setupHamburger();
        vm.init(); // idempotente: no reinicia el timer tras una rotación
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Binding
    // ════════════════════════════════════════════════════════════════════════

    private void bindViews() {
        chipTimer  = findViewById(R.id.tv_timer);
        tvLhs      = findViewById(R.id.tv_lhs);
        tvRhs      = findViewById(R.id.tv_rhs);
        tvStatus   = findViewById(R.id.tv_balance_status);
        etAnswer   = findViewById(R.id.et_answer);
        ivBalanza  = findViewById(R.id.iv_balanza);
        drawerMenu = findViewById(R.id.drawer_menu);
        drawerMenu.setVisibility(View.GONE);

        findViewById(R.id.btn_back).setOnClickListener(v -> { vm.cancelTimer(); finish(); });
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        findViewById(R.id.btn_verify).setOnClickListener(v ->
                vm.verify(etAnswer.getText().toString()));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Observadores — fuente única de verdad para la UI
    // ════════════════════════════════════════════════════════════════════════

    private void observeViewModel() {
        vm.lhsExpr.observe(this, tvLhs::setText);
        vm.rhsExpr.observe(this, tvRhs::setText);

        vm.statusMessage.observe(this, tvStatus::setText);
        vm.statusPositive.observe(this, positive -> {
            if (positive == null) {
                tvStatus.setTextColor(getColor(android.R.color.darker_gray));
            } else {
                tvStatus.setTextColor(getColor(
                        positive ? R.color.accent_green : R.color.warn_chip_text));
            }
        });

        vm.balanced.observe(this, balanced -> {
            int tint = Boolean.TRUE.equals(balanced)
                    ? getColor(R.color.accent_green)
                    : getColor(R.color.color_primary);
            ivBalanza.setColorFilter(tint);
        });

        vm.timerDisplay.observe(this, chipTimer::setText);
        vm.timerUrgent.observe(this, urgent -> {
            if (Boolean.TRUE.equals(urgent)) {
                chipTimer.setChipBackgroundColorResource(R.color.error_bg);
                chipTimer.setTextColor(getColor(R.color.error));
            }
        });

        vm.autoAnswer.observe(this, answer -> {
            if (answer != null) etAnswer.setText(answer);
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
    //  Botones de operación
    // ════════════════════════════════════════════════════════════════════════

    private void setupOpButtons() {
        ((Button) findViewById(R.id.btn_op_minus5)).setOnClickListener(v -> vm.applyOp("-5"));
        ((Button) findViewById(R.id.btn_op_plus5)) .setOnClickListener(v -> vm.applyOp("+5"));
        ((Button) findViewById(R.id.btn_op_times2)).setOnClickListener(v -> vm.applyOp("x2"));
        ((Button) findViewById(R.id.btn_op_div2))  .setOnClickListener(v -> vm.applyOp("/2"));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Pista (BottomSheet — solo UI)
    // ════════════════════════════════════════════════════════════════════════

    private void showHint() {
        vm.useHint();
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_hint, null);
        ((TextView) view.findViewById(R.id.tv_hint_content)).setText(
                "Paso 1: Resta 5 a ambos lados de la balanza.\n" +
                        "  x + 5 − 5 = 10 − 5\n\n" +
                        "Paso 2: Simplifica.\n" +
                        "  x = 5 ✓");
        view.findViewById(R.id.btn_close_hint).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(view);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Diálogos de resultado — solo presentación
    // ════════════════════════════════════════════════════════════════════════

    private void showResultDialog(boolean correct, boolean withHint) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        if (correct) {
            int pts = withHint ? 50 : 100;
            String msg = "¡Bien hecho!\n+" + pts + " puntos";
            if (withHint) msg += "\n\nUsaste una pista. ¡Intenta sin pista para ganar +50 puntos extra!";
            b.setTitle("🎉 ¡Correcto!")
                    .setMessage(msg)
                    .setPositiveButton("Ej. 2/3 →", (d, w) -> goToNext())
                    .setCancelable(false);
            if (withHint) b.setNeutralButton("Sin pista (+50)", (d, w) -> vm.retryWithoutHint());
        } else {
            b.setTitle("🤔 Incorrecto")
                    .setMessage("Revisa la sección de Información para reforzar el tema.")
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
                .setMessage("No te preocupes, puedes reintentar.")
                .setPositiveButton("Reintentar", (d, w) -> vm.retryAfterFailure())
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false).show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación
    // ════════════════════════════════════════════════════════════════════════

    private void goToNext() { startActivity(new Intent(this, EjercicioClasicoActivity.class)); finish(); }

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
