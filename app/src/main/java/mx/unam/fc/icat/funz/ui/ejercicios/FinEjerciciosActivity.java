package mx.unam.fc.icat.funz.ui.ejercicios;

import static mx.unam.fc.icat.funz.R.*;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;


import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.R;
/**
 * FinEjerciciosActivity — Pantalla de resumen al completar el Módulo 1.
 *
 * Muestra:
 *   - Ejercicios correctos / incorrectos de la sesión.
 *   - Puntos totales acumulados en la sesión.
 *   - Botón "Ir a Temas" que lleva de vuelta a TemasActivity.
 *
 * AppState.mod1Complete == true cuando se llega aquí.
 * El Módulo 2 ya quedó desbloqueado en markExerciseDone(3, true, ...).
 */
public class FinEjerciciosActivity extends AppCompatActivity {

    private AppState state;
    private boolean  appliedDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = AppState.getInstance();
        appliedDarkTheme = state.isDarkTheme();
        if (appliedDarkTheme) setTheme(R.style.Theme_FunZ_Dark);
        setContentView(R.layout.activity_fin_ejercicios);

//1. Obtenemos la referencia al TextView
        TextView tvMsgFinished = findViewById(R.id.msg_module_finished);

// 2. Obtenemos el ID del módulo desde el estado
        int activeModuleId = state.getActiveModuleId() - 1;

// 3. Creamos el texto usando el recurso de string y el ID del módulo
        String message = getString(R.string.msg_module_completed, activeModuleId);

// 4. Lo aplicamos al TextView
        tvMsgFinished.setText(message);

        ((TextView) findViewById(R.id.tv_fin_ok))
                .setText(String.valueOf(state.getSessionOk()));
        ((TextView) findViewById(R.id.tv_fin_fail))
                .setText(String.valueOf(state.getSessionFail()));



        Chip chipPts = findViewById(R.id.tv_fin_pts);
        chipPts.setText(getString(R.string.pts_earned_format, state.getSessionPts()));

        Button btnTemas = findViewById(R.id.btn_go_temas);
        btnTemas.setOnClickListener(v -> {
            startActivity(new Intent(this, TemasActivity.class));
            finish();
        });
    }
}
