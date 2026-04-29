package mx.unam.fc.icat.funz.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import mx.unam.fc.icat.funz.data.AppState;

public class StatsViewModel extends ViewModel {

    // Usaremos un LiveData para un objeto que contenga todos los datos procesados
    private final MutableLiveData<StatsState> statsState = new MutableLiveData<>();
    private final AppState appState = AppState.getInstance();

    public LiveData<StatsState> getStatsState() {
        return statsState;
    }

    public void loadStats() {
        // EXTRAEMOS LA LÓGICA MATEMÁTICA DE LA UI
        int resolvedCount = 0;
        if (appState.isEx1Done())
            resolvedCount++;
        if (appState.isEx2Done())
            resolvedCount++;
        if (appState.isEx3Done())
            resolvedCount++;

        String mod2Status = appState.isMod2Unlocked() ? "0%" : "Bloqueado";

        // Creamos un nuevo estado con los datos procesados
        StatsState newState = new StatsState(
                appState.getTotalPoints(),
                appState.getMod1Progress(),
                appState.getStreakDays(),
                resolvedCount,
                mod2Status,
                appState.isDarkTheme());

        statsState.setValue(newState);
    }
}

// Clase de apoyo para representar el estado de la vista
class StatsState {
    public final int points;
    public final int progress;
    public final int streak;
    public final int resolved;
    public final String mod2Status;
    public final boolean isDarkTheme;

    public StatsState(int points, int progress, int streak, int resolved, String mod2Status, boolean isDarkTheme) {
        this.points = points;
        this.progress = progress;
        this.streak = streak;
        this.resolved = resolved;
        this.mod2Status = mod2Status;
        this.isDarkTheme = isDarkTheme;
    }
}