package mx.unam.fc.icat.funz;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

/**
 * EjercicioClasicoActivity — Pantalla D2: Ejercicio 2/3 (Método Clásico / Baldor)
 *
 * Ecuación: 3x + 5 = 20
 *
 * LÓGICA MATEMÁTICA (Método Clásico):
 *   Paso 1: Transponer +5  →  3x = 20 − 5 = 15
 *   Paso 2: Dividir entre 3 →  x  = 15 ÷ 3 = 5
 *   Respuesta correcta: x = 5
 *
 * La UI presenta el desarrollo algebraico como LinearLayouts con Chips
 * de Material Design. Cada término (+3x, +5, 20, etc.) está en un Chip
 * para facilitar la lectura y posible interacción futura (arrastrar términos).
 *
 * El primer LinearLayout (step_active) usa bg_step_active para resaltarse
 * con borde izquierdo morado indicando el paso en curso.
 *
 * GAMIFICACIÓN: igual que Balanza (+100 sin pista / +50 con pista / reintentar).
 */
public class EjercicioClasicoActivity extends AppCompatActivity {

    private static final int TIME_MS     = 120_000;
    private static final int CORRECT_ANS = 5;

    private AppState       state;
    private boolean        appliedDarkTheme;
    private CountDownTimer timer;
    private boolean        hintUsed = false;

    // ── Views ────────────────────────────────────────────────────────────────
    private Chip         chipTimer;
    private EditText     etAnswer;
    private LinearLayout drawerMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_ejercicio_clasico);

        state.setHintUsedCla(false);
        hintUsed = false;

        bindViews();
        startTimer();
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
        chipTimer  = findViewById(R.id.tv_timer);
        etAnswer   = findViewById(R.id.et_answer);
        drawerMenu = findViewById(R.id.drawer_menu);
        drawerMenu.setVisibility(View.GONE);

        findViewById(R.id.btn_back).setOnClickListener(v -> { cancelTimer(); finish(); });
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        findViewById(R.id.btn_verify).setOnClickListener(v -> verify());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Temporizador
    // ════════════════════════════════════════════════════════════════════════

    private void startTimer() {
        cancelTimer();
        timer = new CountDownTimer(TIME_MS, 1000) {
            @Override
            public void onTick(long ms) {
                long s = ms / 1000;
                chipTimer.setText(String.format("%d:%02d", s / 60, s % 60));
                if (s <= 20) {
                    chipTimer.setChipBackgroundColorResource(R.color.error_bg);
                    chipTimer.setTextColor(getColor(R.color.error));
                }
            }
            @Override
            public void onFinish() {
                chipTimer.setText("0:00");
                showTimeUpDialog();
            }
        }.start();
    }

    private void cancelTimer() {
        if (timer != null) timer.cancel();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Pista
    // ════════════════════════════════════════════════════════════════════════

    private void showHint() {
        hintUsed = true;
        state.setHintUsedCla(true);

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
    //  Verificación
    // ════════════════════════════════════════════════════════════════════════

    private void verify() {
        String input = etAnswer.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Ingresa tu respuesta", Toast.LENGTH_SHORT).show();
            return;
        }
        cancelTimer();
        boolean correct;
        try { correct = Integer.parseInt(input) == CORRECT_ANS; }
        catch (NumberFormatException e) { correct = false; }

        state.markExerciseDone(2, correct, hintUsed);
        showResultDialog(correct);
    }

    private void showResultDialog(boolean correct) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        if (correct) {
            int pts = hintUsed ? 50 : 100;
            String msg = "¡Bien hecho!\n+" + pts + " puntos";
            if (hintUsed) msg += "\n\nUsaste pista. ¡Reinténtalo sin pista para +50 extra!";
            b.setTitle("🎉 ¡Correcto!")
                    .setMessage(msg)
                    .setPositiveButton("Ej. 3/3 →", (d, w) -> goToNext())
                    .setCancelable(false);
            if (hintUsed) b.setNeutralButton("Sin pista (+50)", (d, w) -> retryWithoutHint());
        } else {
            b.setTitle("🤔 Incorrecto")
                    .setMessage("Revisa la sección de Información.")
                    .setPositiveButton("📖 Información", (d, w) -> goToInfo())
                    .setNegativeButton("Reintentar", (d, w) -> startTimer())
                    .setNeutralButton("Salir", (d, w) -> finish())
                    .setCancelable(false);
        }
        b.show();
    }

    private void showTimeUpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⏰ ¡Tiempo agotado!")
                .setPositiveButton("Reintentar", (d, w) -> startTimer())
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false).show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación
    // ════════════════════════════════════════════════════════════════════════

    private void goToNext() {
        startActivity(new Intent(this, EjercicioTilesActivity.class));
        finish();
    }

    private void goToInfo() {
        Intent i = new Intent(this, InfoEjemplosActivity.class);
        i.putExtra("tab", 0);
        startActivity(i);
        finish();
    }

    private void retryWithoutHint() {
        hintUsed = false;
        state.setHintUsedCla(false);
        etAnswer.setText("");
        startTimer();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Hamburguesa
    // ════════════════════════════════════════════════════════════════════════

    private void setupHamburger() {
        findViewById(R.id.btn_hamburger).setOnClickListener(v ->
                drawerMenu.setVisibility(
                        drawerMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        setupDrawerItem(R.id.drawer_inicio,  MainActivity.class);
        setupDrawerItem(R.id.drawer_temas,   TemasActivity.class);
        setupDrawerItem(R.id.drawer_salas,   SalasActivity.class);
        setupDrawerItem(R.id.drawer_stats,   EstadisticasActivity.class);
        setupDrawerItem(R.id.drawer_config,  ConfiguracionActivity.class);
    }

    private void setupDrawerItem(int viewId, Class<?> target) {
        View item = findViewById(viewId);
        if (item != null) item.setOnClickListener(v -> {
            cancelTimer();
            drawerMenu.setVisibility(View.GONE);
            startActivity(new Intent(this, target));
        });
    }

    @Override
    protected void onDestroy() { super.onDestroy(); cancelTimer(); }
}
