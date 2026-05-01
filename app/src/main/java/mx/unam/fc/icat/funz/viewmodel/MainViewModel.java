package mx.unam.fc.icat.funz.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.Collections;
import java.util.List;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.db.Module;
import mx.unam.fc.icat.funz.repository.ExerciseRepository;

/**
 * MainViewModel — ViewModel para la pantalla de Inicio.
 */
public class MainViewModel extends AndroidViewModel {

    private final AppState           state = AppState.getInstance();
    private final ExerciseRepository repo;

    private final MutableLiveData<String>  _welcomeText    = new MutableLiveData<>();
    public  final LiveData<String>          welcomeText     = _welcomeText;

    private final MutableLiveData<String>  _streakText     = new MutableLiveData<>();
    public  final LiveData<String>          streakText      = _streakText;

    private final MutableLiveData<String>  _resumeBadge    = new MutableLiveData<>();
    public  final LiveData<String>          resumeBadge     = _resumeBadge;

    private final MutableLiveData<Integer> _resumeProgress = new MutableLiveData<>(0);
    public  final LiveData<Integer>         resumeProgress  = _resumeProgress;

    public final LiveData<List<Module>> allModules;
    public final LiveData<List<Module>> recentModules;
    
    /** Título del módulo que se está reanudando. */
    public final LiveData<String>       resumeTitle;

    public MainViewModel(@NonNull Application app) {
        super(app);
        repo = new ExerciseRepository(app);
        allModules = repo.getAllModules();

        // Obtener el nombre del módulo activo de la lista completa
        resumeTitle = Transformations.map(allModules, modules -> {
            int activeId = state.getActiveModuleId();
            if (modules != null) {
                for (Module m : modules) {
                    if (m.id == activeId) return m.name;
                }
            }
            return "Comenzar aprendizaje";
        });

        // Lógica para obtener los últimos 3 módulos activos/desbloqueados
        recentModules = Transformations.map(allModules, modules -> {
            if (modules == null || modules.isEmpty()) return Collections.emptyList();
            
            int activeId = state.getActiveModuleId();
            int endIdx = 0;
            for (int i = 0; i < modules.size(); i++) {
                if (modules.get(i).id == activeId) {
                    endIdx = i;
                    break;
                }
            }

            int start = Math.max(0, endIdx - 2);
            int end   = Math.min(modules.size(), start + 3);
            if (end - start < 3 && modules.size() >= 3) {
                start = Math.max(0, modules.size() - 3);
                end = modules.size();
            }
            return modules.subList(start, end);
        });
    }

    public void refreshUiState() {
        _welcomeText.setValue("¡Hola, " + state.getUsername() + "!");
        _streakText.setValue("🔥 Racha: " + state.getStreakDays() + " días");
        
        int activeModId = state.getActiveModuleId();
        _resumeBadge.setValue(state.getResumeBadge());
        _resumeProgress.setValue(state.getModuleProgress(activeModId));
    }

    public int getModuleProgress(int moduleId) {
        return state.getModuleProgress(moduleId);
    }

    public int[] getResumeTarget() {
        int mod  = state.getActiveModuleId();
        int step = state.getCurrentStep(mod);
        state.resetSession();
        return new int[]{ mod, step };
    }

    public int[] getModuleTarget(int moduleId) {
        int step = state.getCurrentStep(moduleId);
        return new int[]{ moduleId, step };
    }

    public boolean isActiveModuleComplete() {
        return state.isModuleComplete(state.getActiveModuleId());
    }
}
