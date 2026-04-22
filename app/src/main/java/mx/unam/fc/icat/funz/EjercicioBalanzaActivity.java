package com.unam.funz;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

/**
 * EjercicioBalanzaActivity — Pantalla D1: Ejercicio 1/3 (Método de la Balanza)
 *
 * Ecuación: x + 5 = 10
 *
 * LÓGICA MATEMÁTICA (Método de la Balanza):
 *   Estado inicial : LHS = "x+5",  RHS = "10"
 *   Operación correcta: −5 en ambos lados
 *     → LHS = "x",   RHS = "5"   → x = 5
 *   Cualquier otra operación muestra retroalimentación de error.
 *
 * GAMIFICACIÓN:
 *   - Sin pista → +100 pts si correcto.
 *   - Con pista → +50 pts si correcto + opción de reintentar sin pista (+50 extra).
 *
 * COMPONENTES UI:
 *   - Topbar: ← Temas | "Ejercicio 1/3" | Timer | 💡 | ≡
 *   - ImageView ic_balanza (SVG vectorial).
 *   - TextViews tvLhs / tvRhs para mostrar la expresión de cada plato.
 *   - Botones de operación: −5, +5, ×2, ÷2.
 *   - TextInputEditText para respuesta final.
 *   - BottomSheet de pista.
 *   - AlertDialog de resultado.
 */
public class EjercicioBalanzaActivity extends AppCompatActivity {

    // ── Constantes ───────────────────────────────────────────────────────────
    private static final int TIME_MS     = 120_000; // 2 minutos
    private static final int CORRECT_ANS = 5;

    // ── Estado ───────────────────────────────────────────────────────────────
    private AppState       state;
    private CountDownTimer timer;
    private boolean        hintUsed    = false;
    private boolean        timerActive = false;

    // Expresiones actuales de cada plato
    private String lhsExpr = "x+5";
    private String rhsExpr = "10";

    // ── Views ────────────────────────────────────────────────────────────────
    private Chip         chipTimer;
    private TextView     tvLhs, tvRhs, tvStatus;
    private EditText     etAnswer;
    private ImageView    ivBalanza;
    private LinearLayout drawerMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_ejercicio_balanza);

        state.setHintUsedBal(false);
        hintUsed = false;

        bindViews();
        resetBalanza();
        startTimer();
        setupOpButtons();
        setupHamburger();
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

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            cancelTimer();
            finish();
        });
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        findViewById(R.id.btn_verify).setOnClickListener(v -> verify());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Lógica de la Balanza
    // ════════════════════════════════════════════════════════════════════════

    private void resetBalanza() {
        lhsExpr = "x+5";
        rhsExpr = "10";
        updateBalanzaUI(false);
        tvStatus.setText("Aplica −5 a ambos lados para aislar x");
        tvStatus.setTextColor(getColor(android.R.color.darker_gray));
        if (etAnswer != null) etAnswer.setText("");
    }

    /**
     * Actualiza los labels LHS/RHS y el tint del SVG de la balanza.
     * Verde = equilibrada, morado = desequilibrada.
     */
    private void updateBalanzaUI(boolean balanced) {
        tvLhs.setText(lhsExpr);
        tvRhs.setText(rhsExpr);
        int tint = balanced
                ? getColor(R.color.accent_green)
                : getColor(R.color.color_primary);
        ivBalanza.setColorFilter(tint);
    }

    private void setupOpButtons() {
        Button btnM5 = findViewById(R.id.btn_op_minus5);
        Button btnP5 = findViewById(R.id.btn_op_plus5);
        Button btnX2 = findViewById(R.id.btn_op_times2);
        Button btnD2 = findViewById(R.id.btn_op_div2);

        btnM5.setOnClickListener(v -> applyOp("-5"));
        btnP5.setOnClickListener(v -> applyOp("+5"));
        btnX2.setOnClickListener(v -> applyOp("x2"));
        btnD2.setOnClickListener(v -> applyOp("/2"));
    }

    /**
     * Aplica la operación seleccionada a ambos platos.
     * Solo −5 aísla correctamente la variable x en esta ecuación.
     */
    private void applyOp(String op) {
        switch (op) {
            case "-5":
                lhsExpr = "x";
                rhsExpr = "5";
                tvStatus.setText("✓ x+5−5 = 10−5  →  x = 5. Ingresa la respuesta.");
                tvStatus.setTextColor(getColor(R.color.accent_green));
                updateBalanzaUI(true);
                etAnswer.setText("5");
                break;
            case "+5":
                tvStatus.setText("Eso incrementa ambos lados pero no aísla x. Intenta −5.");
                tvStatus.setTextColor(getColor(R.color.warn_chip_text));
                updateBalanzaUI(false);
                break;
            case "x2":
            case "/2":
                tvStatus.setText("Esa operación no aísla x en esta ecuación. Intenta −5.");
                tvStatus.setTextColor(getColor(R.color.warn_chip_text));
                updateBalanzaUI(false);
                break;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Temporizador
    // ════════════════════════════════════════════════════════════════════════

    private void startTimer() {
        cancelTimer();
        timerActive = true;
        timer = new CountDownTimer(TIME_MS, 1000) {
            @Override
            public void onTick(long ms) {
                long secs = ms / 1000;
                long m = secs / 60, s = secs % 60;
                chipTimer.setText(String.format("%d:%02d", m, s));
                if (secs <= 20) {
                    chipTimer.setChipBackgroundColorResource(R.color.error_bg);
                    chipTimer.setTextColor(getColor(R.color.error));
                }
            }
            @Override
            public void onFinish() {
                timerActive = false;
                chipTimer.setText("0:00");
                showTimeUpDialog();
            }
        }.start();
    }

    private void cancelTimer() {
        if (timer != null) { timer.cancel(); timerActive = false; }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Pista (BottomSheet)
    // ════════════════════════════════════════════════════════════════════════

    private void showHint() {
        hintUsed = true;
        state.setHintUsedBal(true);

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
    //  Verificación y resultados
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

        state.markExerciseDone(1, correct, hintUsed);
        showResultDialog(correct);
    }

    private void showResultDialog(boolean correct) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (correct) {
            int pts = hintUsed ? 50 : 100;
            String msg = "¡Bien hecho!\n+" + pts + " puntos";
            if (hintUsed) {
                msg += "\n\nUsaste una pista. ¡Intenta sin pista para ganar +50 puntos extra!";
            }
            builder.setTitle("🎉 ¡Correcto!")
                   .setMessage(msg)
                   .setPositiveButton("Ej. 2/3 →", (d, w) -> goToNext())
                   .setCancelable(false);
            if (hintUsed) {
                builder.setNeutralButton("Sin pista (+50)", (d, w) -> retryWithoutHint());
            }
        } else {
            builder.setTitle("🤔 Incorrecto")
                   .setMessage("Revisa la sección de Información para reforzar el tema.")
                   .setPositiveButton("📖 Información", (d, w) -> goToInfo())
                   .setNegativeButton("Reintentar", (d, w) -> { resetBalanza(); startTimer(); })
                   .setNeutralButton("Salir", (d, w) -> finish())
                   .setCancelable(false);
        }
        builder.show();
    }

    private void showTimeUpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⏰ ¡Tiempo agotado!")
                .setMessage("No te preocupes, puedes reintentar.")
                .setPositiveButton("Reintentar", (d, w) -> { resetBalanza(); startTimer(); })
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación
    // ════════════════════════════════════════════════════════════════════════

    private void goToNext() {
        startActivity(new Intent(this, EjercicioClasicoActivity.class));
        finish();
    }

    private void goToInfo() {
        Intent i = new Intent(this, InfoEjemplosActivity.class);
        i.putExtra("tab", 0);
        startActivity(i);
        finish();
    }

    private void retryWithoutHint() {
        // Descontar los puntos que se otorgaron con pista antes de reintentar
        hintUsed = false;
        state.setHintUsedBal(false);
        resetBalanza();
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
        if (item != null) {
            item.setOnClickListener(v -> {
                cancelTimer();
                drawerMenu.setVisibility(View.GONE);
                startActivity(new Intent(this, target));
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}
