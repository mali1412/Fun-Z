package mx.unam.fc.icat.funz.ui.ejercicios;

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
 * FinEjerciciosActivity — Pantalla de resumen al completar un módulo.
 * Muestra retroalimentación detallada basada en el desempeño del usuario.
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

        setupUI();
    }

    private void setupUI() {
        int activeModuleId = state.getActiveModuleId() - 1;
        TextView tvMsgFinished = findViewById(R.id.msg_module_finished);
        tvMsgFinished.setText(getString(R.string.msg_module_completed, activeModuleId));

        // Obtener estadísticas de la sesión
        int perfect = state.getSessionOk() - state.getSessionHints();
        int hints = state.getSessionHints();
        int revealed = state.getSessionReveals();
        int total = perfect + hints + revealed;

        // Poblar vistas de resultados
        ((TextView) findViewById(R.id.tv_fin_perfect)).setText(String.valueOf(perfect));
        ((TextView) findViewById(R.id.tv_fin_hints)).setText(String.valueOf(hints));
        ((TextView) findViewById(R.id.tv_fin_revealed)).setText(String.valueOf(revealed));
        
        TextView tvAttempts = findViewById(R.id.tv_fin_attempts_msg);
        tvAttempts.setText("Intentos fallidos: " + state.getSessionFail());

        // Mensaje de retroalimentación
        TextView tvFeedback = findViewById(R.id.tv_feedback_msg);
        tvFeedback.setText(getFeedbackMessage(perfect, hints, revealed, total));

        Chip chipPts = findViewById(R.id.tv_fin_pts);
        chipPts.setText(getString(R.string.pts_earned_format, state.getSessionPts()));

        Button btnTemas = findViewById(R.id.btn_go_temas);
        btnTemas.setOnClickListener(v -> {
            startActivity(new Intent(this, TemasActivity.class));
            finish();
        });
    }

    private String getFeedbackMessage(int perfect, int hints, int revealed, int total) {
        if (total == 0) return getString(R.string.feedback_keep_trying);
        
        float score = (perfect * 1.0f + hints * 0.5f) / total;
        
        if (score >= 0.9f) {
            return getString(R.string.feedback_excellent);
        } else if (score >= 0.6f) {
            return getString(R.string.feedback_good);
        } else if (revealed > total / 2) {
            return getString(R.string.feedback_needs_review);
        } else {
            return getString(R.string.feedback_keep_trying);
        }
    }
}
