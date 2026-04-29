package mx.unam.fc.icat.funz.ui.ejercicios;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.ui.temas.InfoEjemplosActivity;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.data.AppState;

/**
 * EjercicioTilesActivity — Pantalla D3: Ejercicio 3/3 (Algebra Tiles)
 *
 * Ecuación: x + 2 = 12
 *
 * LÓGICA MATEMÁTICA (Algebra Tiles):
 *   Estado inicial:
 *     Lado izquierdo: [x] [+1] [+1]
 *     Lado derecho:   [+1] × 12
 *
 *   Principio de par cero:
 *     Al mover un tile +1 del lado izquierdo al derecho,
 *     se busca un +1 en el derecho para cancelarlo (pares cero).
 *     Si no hay +1 en el derecho, se añade un −1.
 *
 *   Resultado:
 *     Al eliminar los dos +1 del lado izquierdo:
 *       Izquierdo: [x]
 *       Derecho:   [+1] × 10  →  x = 10 ✓
 *
 * INTERACCIÓN:
 *   - Clic en tile: mueve el tile al lado contrario (par cero).
 *   - Long press + drag: inicia DragAndDrop nativo de Android.
 *     Al soltar en el contenedor opuesto se ejecuta la misma lógica.
 *   - Al quedar solo x en el izquierdo, se autocompleta la respuesta.
 *
 * DISEÑO DE TILES:
 *   - x  → rectángulo morado (color_primary)
 *   - +1 → cuadrado azul  (#3B82F6)
 *   - −1 → cuadrado rojo  (#EF4444)
 */
public class EjercicioTilesActivity extends AppCompatActivity {

    private static final int CORRECT_ANS = 10;
    private static final int TIME_MS     = 120_000;

    private AppState state;
    private boolean        appliedDarkTheme;
    private CountDownTimer timer;
    private boolean        hintUsed = false;

    // Estado de los tiles (listas mutables)
    private final List<String> leftTiles  = new ArrayList<>();
    private final List<String> rightTiles = new ArrayList<>();

    // ── Views ────────────────────────────────────────────────────────────────
    private Chip         chipTimer;
    private LinearLayout llLeft, llRight;
    private TextView     tvStatus;
    private EditText     etAnswer;
    private LinearLayout drawerMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        if (state.isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_ejercicio_tiles);

        state.setHintUsedTil(false);
        hintUsed = false;

        bindViews();
        initTiles();
        renderTiles();
        startTimer();
        setupDropTargets();
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
        llLeft     = findViewById(R.id.ll_tiles_left);
        llRight    = findViewById(R.id.ll_tiles_right);
        tvStatus   = findViewById(R.id.tv_tiles_status);
        etAnswer   = findViewById(R.id.et_answer);
        drawerMenu = findViewById(R.id.drawer_menu);
        drawerMenu.setVisibility(View.GONE);

        findViewById(R.id.btn_back).setOnClickListener(v -> { cancelTimer(); finish(); });
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        findViewById(R.id.btn_verify).setOnClickListener(v -> verify());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Inicialización de tiles
    // ════════════════════════════════════════════════════════════════════════

    private void initTiles() {
        leftTiles.clear();
        rightTiles.clear();
        leftTiles.add("x");
        leftTiles.add("+1");
        leftTiles.add("+1");
        for (int i = 0; i < 12; i++) rightTiles.add("+1");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Renderizado de tiles
    // ════════════════════════════════════════════════════════════════════════

    private void renderTiles() {
        llLeft.removeAllViews();
        llRight.removeAllViews();

        for (int i = 0; i < leftTiles.size(); i++) {
            llLeft.addView(makeTileView(leftTiles.get(i), i, "L"));
        }
        for (int i = 0; i < rightTiles.size(); i++) {
            llRight.addView(makeTileView(rightTiles.get(i), i, "R"));
        }
        checkAutoComplete();
    }

    /**
     * Crea un TextView arrastrable y clickeable para cada tile.
     *
     * Colores semánticos:
     *   x   → morado (variable)
     *   +1  → azul   (constante positiva)
     *   -1  → rojo   (constante negativa)
     */
    private View makeTileView(String label, int idx, String side) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(10f);
        tv.setGravity(Gravity.CENTER);

        int bgColor;
        int widthDp, heightDp;
        if (label.equals("x")) {
            bgColor  = getColor(R.color.color_primary);
            widthDp  = 44;
            heightDp = 26;
        } else if (label.equals("+1")) {
            bgColor  = 0xFF3B82F6; // azul
            widthDp  = 22;
            heightDp = 22;
        } else {
            bgColor  = 0xFFEF4444; // rojo
            widthDp  = 22;
            heightDp = 22;
        }
        tv.setBackgroundColor(bgColor);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dpToPx(widthDp), dpToPx(heightDp));
        lp.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        tv.setLayoutParams(lp);
        tv.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

        // Long press → Drag
        tv.setOnLongClickListener(v -> {
            String clipText = side + "_" + idx + "_" + label;
            ClipData cd = ClipData.newPlainText("tile", clipText);
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
            v.startDragAndDrop(cd, shadow, v, 0);
            return true;
        });

        // Clic simple → mover (par cero)
        if (!label.equals("x")) {
            tv.setOnClickListener(v -> moveTile(side, idx, label));
        } else {
            tv.setOnClickListener(v ->
                    setStatus("No puedes mover x directamente. Mueve primero los tiles de constante.", false));
        }
        return tv;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Lógica de movimiento de tiles (par cero)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Mueve un tile al lado contrario aplicando el principio de par cero.
     *
     * @param side  "L" (izquierdo) o "R" (derecho)
     * @param idx   Índice del tile en su lista
     * @param label Valor del tile ("x", "+1", "-1")
     */
    private void moveTile(String side, int idx, String label) {
        if (side.equals("L")) {
            if (idx >= leftTiles.size()) return;
            leftTiles.remove(idx);

            // Buscar un +1 en el derecho para cancelar (par cero)
            int posR = rightTiles.indexOf("+1");
            if (posR >= 0) {
                rightTiles.remove(posR);
                setStatus("¡Par cero! El +1 izquierdo cancela un +1 derecho.", true);
            } else {
                rightTiles.add("-1");
                setStatus("Se añadió un −1 al lado derecho.", true);
            }

        } else { // side == "R"
            if (idx >= rightTiles.size()) return;
            rightTiles.remove(idx);

            // Buscar una constante en el izquierdo para cancelar
            int posL = -1;
            for (int i = 0; i < leftTiles.size(); i++) {
                if (!leftTiles.get(i).equals("x")) { posL = i; break; }
            }
            if (posL >= 0) {
                leftTiles.remove(posL);
                setStatus("¡Par cero! Tile del derecho cancela uno del izquierdo.", true);
            } else {
                leftTiles.add("-1");
                setStatus("Se añadió un −1 al lado izquierdo.", false);
            }
        }
        renderTiles();
    }

    /**
     * Detecta si solo queda x en el lado izquierdo y autocompleta la respuesta.
     * Calcula: positivosR − negativosR = valor de x.
     */
    private void checkAutoComplete() {
        long constL = leftTiles.stream().filter(t -> !t.equals("x")).count();
        boolean hasX = leftTiles.contains("x");

        if (hasX && constL == 0) {
            long pos = rightTiles.stream().filter("+1"::equals).count();
            long neg = rightTiles.stream().filter("-1"::equals).count();
            int val = (int)(pos - neg);
            etAnswer.setText(String.valueOf(val));
            setStatus("x = " + val + " · Presiona Verificar.", true);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Drop targets (Drag & Drop)
    // ════════════════════════════════════════════════════════════════════════

    private void setupDropTargets() {
        setupDropTarget(llLeft,  "L");
        setupDropTarget(llRight, "R");
    }

    private void setupDropTarget(LinearLayout container, String targetSide) {
        container.setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                ClipData.Item item = event.getClipData().getItemAt(0);
                String[] parts = item.getText().toString().split("_");
                if (parts.length == 3) {
                    String fromSide = parts[0];
                    int    fromIdx  = Integer.parseInt(parts[1]);
                    String label    = parts[2];
                    if (!fromSide.equals(targetSide) && !label.equals("x")) {
                        moveTile(fromSide, fromIdx, label);
                    }
                }
            }
            return true;
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers de UI
    // ════════════════════════════════════════════════════════════════════════

    private void setStatus(String msg, boolean positive) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(positive
                ? getColor(R.color.accent_green)
                : getColor(R.color.warn_chip_text));
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
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
            public void onFinish() { chipTimer.setText("0:00"); showTimeUpDialog(); }
        }.start();
    }

    private void cancelTimer() { if (timer != null) timer.cancel(); }

    // ════════════════════════════════════════════════════════════════════════
    //  Pista
    // ════════════════════════════════════════════════════════════════════════

    private void showHint() {
        hintUsed = true;
        state.setHintUsedTil(true);
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_hint, null);
        ((TextView) v.findViewById(R.id.tv_hint_content)).setText(
                "Paso 1: Haz clic (o arrastra) los tiles +1 del lado izquierdo.\n" +
                        "  Cada clic cancela un par cero: elimina un +1 del izquierdo\n" +
                        "  y un +1 del derecho al mismo tiempo.\n\n" +
                        "Paso 2: Cuando solo quede x en el lado izquierdo,\n" +
                        "  el conteo del lado derecho es el valor de x.\n\n" +
                        "  Aquí: x = 10 ✓");
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

        state.markExerciseDone(3, correct, hintUsed);
        showResultDialog(correct);
    }

    private void showResultDialog(boolean correct) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        if (correct) {
            int pts = hintUsed ? 50 : 100;
            String msg = "¡Bien hecho!\n+" + pts + " puntos\n\n" +
                    "Módulo 1 completado ✅";
            if (hintUsed) msg += "\n\nUsaste pista. ¡Reinténtalo sin pista para +50 extra!";
            b.setTitle("🎉 ¡Correcto!")
                    .setMessage(msg)
                    .setPositiveButton("Ver resultados", (d, w) -> goToFinish())
                    .setCancelable(false);
            if (hintUsed) b.setNeutralButton("Sin pista (+50)", (d, w) -> retryWithoutHint());
        } else {
            b.setTitle("🤔 Incorrecto")
                    .setMessage("Revisa la sección de Información.")
                    .setPositiveButton("📖 Información", (d, w) -> goToInfo())
                    .setNegativeButton("Reintentar", (d, w) -> { initTiles(); renderTiles(); startTimer(); })
                    .setNeutralButton("Salir", (d, w) -> finish())
                    .setCancelable(false);
        }
        b.show();
    }

    private void showTimeUpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⏰ ¡Tiempo agotado!")
                .setPositiveButton("Reintentar", (d, w) -> { initTiles(); renderTiles(); startTimer(); })
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false).show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegación
    // ════════════════════════════════════════════════════════════════════════

    private void goToFinish() {
        startActivity(new Intent(this, FinEjerciciosActivity.class));
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
        state.setHintUsedTil(false);
        initTiles(); renderTiles(); startTimer();
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
