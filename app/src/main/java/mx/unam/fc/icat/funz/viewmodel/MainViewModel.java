package mx.unam.fc.icat.funz.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.Collections;
import java.util.List;

import mx.unam.fc.icat.funz.data.FunZApp;
import mx.unam.fc.icat.funz.db.Module;
import mx.unam.fc.icat.funz.repository.AppStateRepository;
import mx.unam.fc.icat.funz.repository.ExerciseRepository;

/**
 * MainViewModel — ViewModel para la pantalla de Inicio.
 */
public class MainViewModel extends AndroidViewModel {

    private final AppStateRepository stateRepo;
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
        FunZApp appScope = (FunZApp) app;
        repo = appScope.getExerciseRepository();
        stateRepo = appScope.getAppStateRepository();
        allModules = repo.getAllModules();

        // Obtener el nombre del módulo activo de la lista completa
        resumeTitle = Transformations.map(allModules, modules -> {
            int activeId = stateRepo.getActiveModuleId();
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
            
            int activeId = stateRepo.getActiveModuleId();
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
        _welcomeText.setValue("¡Hola, " + stateRepo.getUsername() + "!");
        _streakText.setValue("🔥 Racha: " + stateRepo.getStreakDays() + " días");

        int activeModId = stateRepo.getActiveModuleId();
        _resumeBadge.setValue(stateRepo.getResumeBadge());
        _resumeProgress.setValue(stateRepo.getModuleProgress(activeModId));
    }

    public int getModuleProgress(int moduleId) {
        return stateRepo.getModuleProgress(moduleId);
    }

    public int[] getResumeTarget() {
        int mod  = stateRepo.getActiveModuleId();
        int step = stateRepo.getCurrentStep(mod);
        stateRepo.resetSession();
        return new int[]{ mod, step };
    }

    public int[] getModuleTarget(int moduleId) {
        int step = stateRepo.getCurrentStep(moduleId);
        return new int[]{ moduleId, step };
    }

    public boolean isActiveModuleComplete() {
        return stateRepo.isModuleComplete(stateRepo.getActiveModuleId());
    }
}
