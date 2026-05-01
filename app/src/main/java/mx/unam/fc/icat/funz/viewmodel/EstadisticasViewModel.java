package mx.unam.fc.icat.funz.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import mx.unam.fc.icat.funz.data.AppState;

/**
 * EstadisticasViewModel — ViewModel para la pantalla M (Estadísticas).
 *
 * Agrega todos los valores necesarios para la pantalla de estadísticas
 * en un único objeto {@link StatsUiState} emitido por LiveData,
 * evitando múltiples llamadas directas a AppState desde la Activity.
 *
 * La Activity observa {@link #uiState} y actualiza cada vista en bloque.
 */
public class EstadisticasViewModel extends ViewModel {

    private final AppState state = AppState.getInstance();

    private final MutableLiveData<StatsUiState> _uiState = new MutableLiveData<>();
    public  final LiveData<StatsUiState>         uiState  = _uiState;

    // ════════════════════════════════════════════════════════════════════════
    //  Refresco
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Construye un nuevo StatsUiState con los valores actuales de AppState
     * y lo emite. La Activity llama a este método en onResume.
     */
    public void refreshStats() {
        int resolved = 0;
        if (state.isEx1Done()) resolved++;
        if (state.isEx2Done()) resolved++;
        if (state.isEx3Done()) resolved++;

        _uiState.setValue(new StatsUiState(
                state.getTotalPoints(),
                state.getMod1Progress(),
                state.getStreakDays(),
                resolved,
                state.isMod2Unlocked()
        ));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Estado de UI (objeto inmutable)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Snapshot inmutable del estado de la pantalla de estadísticas.
     * Todos los campos son definitivos para garantizar la inmutabilidad.
     */
    public static final class StatsUiState {
        public final int     totalPoints;
        public final int     mod1Progress;
        public final int     streakDays;
        public final int     exercisesResolved;
        public final boolean mod2Unlocked;

        public StatsUiState(int totalPoints, int mod1Progress, int streakDays,
                            int exercisesResolved, boolean mod2Unlocked) {
            this.totalPoints        = totalPoints;
            this.mod1Progress       = mod1Progress;
            this.streakDays         = streakDays;
            this.exercisesResolved  = exercisesResolved;
            this.mod2Unlocked       = mod2Unlocked;
        }
    }
}
