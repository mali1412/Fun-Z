package mx.unam.fc.icat.funz.ui.ejercicios;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;

import java.util.List;


import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;
import mx.unam.fc.icat.funz.viewmodel.EjercicioTilesViewModel;
import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.ui.temas.InfoEjemplosActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.R;



/**
 * EjercicioTilesActivity — Pantalla D3: Ejercicio 3/3 (Algebra Tiles).
 *
 * [MVVM] Observador pasivo de EjercicioTilesViewModel.
 * Responsabilidades de la Activity:
 *   - Construir y renderizar los tiles a partir de las listas observadas.
 *   - Gestionar el DragAndDrop de la UI y delegar los movimientos al ViewModel.
 *   - Mostrar mensajes de estado y diálogos según eventos del ViewModel.
 *
 * NO contiene lógica de par cero ni cálculo del valor de x.
 */
public class EjercicioTilesActivity extends AppCompatActivity {

    private EjercicioTilesViewModel vm;

    // ── Views ─────────────────────────────────────────────────────────────────
    private Chip         chipTimer;
    private LinearLayout llLeft, llRight;
    private TextView     tvStatus;
    private EditText     etAnswer;
    private LinearLayout drawerMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppState.getInstance().isDarkTheme()) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_ejercicio_tiles);

        vm = new ViewModelProvider(this).get(EjercicioTilesViewModel.class);

        bindViews();
        observeViewModel();
        setupDropTargets();
        setupHamburger();
        vm.init();
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

        findViewById(R.id.btn_back).setOnClickListener(v -> { vm.cancelTimer(); finish(); });
        findViewById(R.id.btn_hint).setOnClickListener(v -> showHint());
        findViewById(R.id.btn_verify).setOnClickListener(v ->
                vm.verify(etAnswer.getText().toString()));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Observadores — la UI se construye desde los LiveData del ViewModel
    // ════════════════════════════════════════════════════════════════════════

    private void observeViewModel() {
        // Tiles: al cambiar las listas, se reconstruyen todas las vistas
        vm.leftTilesLd.observe(this,  tiles -> renderTiles(tiles, llLeft,  "L"));
        vm.rightTilesLd.observe(this, tiles -> renderTiles(tiles, llRight, "R"));

        // Retroalimentación de movimientos
        vm.statusMessage.observe(this, tvStatus::setText);
        vm.statusPositive.observe(this, positive -> tvStatus.setTextColor(
                Boolean.TRUE.equals(positive)
                        ? getColor(R.color.accent_green)
                        : getColor(R.color.warn_chip_text)));

        // Auto-completado del campo respuesta
        vm.autoAnswer.observe(this, answer -> {
            if (answer != null) etAnswer.setText(answer);
        });

        // Temporizador
        vm.timerDisplay.observe(this, chipTimer::setText);
        vm.timerUrgent.observe(this, urgent -> {
            if (Boolean.TRUE.equals(urgent)) {
                chipTimer.setChipBackgroundColorResource(R.color.error_bg);
                chipTimer.setTextColor(getColor(R.color.error));
            }
        });

        vm.timerFinished.observe(this, unused -> showTimeUpDialog());

        // Resultado del ejercicio
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
    //  Renderizado de tiles (solo responsabilidad de UI)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Reconstruye el contenedor de tiles a partir de la lista actualizada.
     * Cada tile es un TextView arrastrable y clickeable.
     *
     * @param tiles     Lista de etiquetas ("x", "+1", "-1")
     * @param container Contenedor LinearLayout a poblar
     * @param side      "L" o "R" para identificar el origen en drag/click
     */
    private void renderTiles(List<String> tiles, LinearLayout container, String side) {
        container.removeAllViews();
        for (int i = 0; i < tiles.size(); i++) {
            container.addView(makeTileView(tiles.get(i), i, side));
        }
    }

    /**
     * Crea un TextView arrastrable para representar un tile.
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
            bgColor = getColor(R.color.color_primary); widthDp = 44; heightDp = 26;
        } else if (label.equals("+1")) {
            bgColor = 0xFF3B82F6; widthDp = 22; heightDp = 22;
        } else {
            bgColor = 0xFFEF4444; widthDp = 22; heightDp = 22;
        }
        tv.setBackgroundColor(bgColor);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(widthDp), dpToPx(heightDp));
        lp.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        tv.setLayoutParams(lp);
        tv.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

        // Long press → inicia drag
        tv.setOnLongClickListener(v -> {
            String clipText = side + "_" + idx + "_" + label;
            ClipData cd = ClipData.newPlainText("tile", clipText);
            v.startDragAndDrop(cd, new View.DragShadowBuilder(v), v, 0);
            return true;
        });

        // Clic simple → delegar movimiento al ViewModel
        if (!label.equals("x")) {
            tv.setOnClickListener(v -> vm.moveTile(side, idx, label));
        } else {
            tv.setOnClickListener(v -> tvStatus.setText(
                    "No puedes mover x directamente. Mueve primero los tiles de constante."));
        }
        return tv;
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
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
                        vm.moveTile(fromSide, fromIdx, label); // delegar lógica al ViewModel
                    }
                }
            }
            return true;
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
    //  Diálogos
    // ════════════════════════════════════════════════════════════════════════

    private void showResultDialog(boolean correct, boolean withHint) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        if (correct) {
            int pts = withHint ? 50 : 100;
            String msg = "¡Bien hecho!\n+" + pts + " puntos\n\nMódulo 1 completado ✅";
            if (withHint) msg += "\n\nUsaste pista. ¡Reinténtalo sin pista para +50 extra!";
            b.setTitle("🎉 ¡Correcto!")
                    .setMessage(msg)
                    .setPositiveButton("Ver resultados", (d, w) -> goToFinish())
                    .setCancelable(false);
            if (withHint) b.setNeutralButton("Sin pista (+50)", (d, w) -> vm.retryWithoutHint());
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

    private void goToFinish() { startActivity(new Intent(this, FinEjerciciosActivity.class)); finish(); }

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
