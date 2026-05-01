package mx.unam.fc.icat.funz.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import java.util.ArrayList;
import java.util.List;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.db.Module;
import mx.unam.fc.icat.funz.repository.ExerciseRepository;

/**
 * EstadisticasViewModel — ViewModel para la pantalla M (Estadísticas).
 *
 * Proporciona un estado unificado y reactivo que refleja el progreso
 * de todos los módulos del sistema, filtrando los 3 más recientes para la UI.
 */
public class EstadisticasViewModel extends AndroidViewModel {

    private final AppState state = AppState.getInstance();
    private final ExerciseRepository repo;

    private final MediatorLiveData<StatsUiState> _uiState = new MediatorLiveData<>();
    public  final LiveData<StatsUiState>         uiState  = _uiState;

    /** Lista completa de módulos desde la DB. */
    public final LiveData<List<Module>> allModules;

    public EstadisticasViewModel(@NonNull Application app) {
        super(app);
        repo = new ExerciseRepository(app);
        allModules = repo.getAllModules();

        // El uiState reacciona automáticamente cuando Room carga los módulos de la DB
        _uiState.addSource(allModules, this::calculateStats);
    }

    /**
     * Fuerza el recalculo de las estadísticas. Se llama en onResume para capturar
     * cambios en SharedPreferences (progreso de ejercicios) que no disparan LiveData de Room.
     */
    public void refreshStats() {
        calculateStats(allModules.getValue());
    }

    private void calculateStats(List<Module> modules) {
        if (modules == null || modules.isEmpty()) return;

        int totalPoints = state.getTotalPoints();
        int streakDays = state.getStreakDays();
        int resolved = 0;
        int sumProgress = 0;

        // 1. Calcular ejercicios resueltos y progreso total sobre todos los módulos
        for (Module m : modules) {
            sumProgress += state.getModuleProgress(m.id);
            int count = state.getModuleExerciseCount(m.id);
            for (int s = 1; s <= count; s++) {
                if (state.isStepDone(m.id, s)) resolved++;
            }
        }
        int totalProgress = sumProgress / modules.size();

        // 2. Determinar cuáles son los módulos "recientes" a mostrar (ventana de 3)
        // Buscamos el último módulo desbloqueado como ancla
        int activeId = 1;
        for (Module m : modules) {
            if (m.unlocked) activeId = m.id;
        }

        int endIdx = 0;
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).id == activeId) {
                endIdx = i;
                break;
            }
        }

        // Ventana de 3: del (activo-2) al activo
        int start = Math.max(0, endIdx - 2);
        int end   = Math.min(modules.size(), start + 3);
        
        // Ajuste para asegurar 3 si existen
        if (end - start < 3 && modules.size() >= 3) {
            start = Math.max(0, modules.size() - 3);
            end = modules.size();
        }

        List<Module> recent = new ArrayList<>(modules.subList(start, end));

        // 3. Publicar el nuevo estado
        _uiState.setValue(new StatsUiState(
                totalPoints,
                totalProgress,
                streakDays,
                resolved,
                recent
        ));
    }

    public int getModuleProgress(int moduleId) {
        return state.getModuleProgress(moduleId);
    }

    /**
     * Snapshot de datos para la UI.
     */
    public static final class StatsUiState {
        public final int totalPoints;
        public final int totalProgress;
        public final int streakDays;
        public final int exercisesResolved;
        public final List<Module> recentModules;

        public StatsUiState(int totalPoints, int totalProgress, int streakDays, 
                            int exercisesResolved, List<Module> recentModules) {
            this.totalPoints = totalPoints;
            this.totalProgress = totalProgress;
            this.streakDays = streakDays;
            this.exercisesResolved = exercisesResolved;
            this.recentModules = recentModules;
        }
    }
}
