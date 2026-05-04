package mx.unam.fc.icat.funz.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import mx.unam.fc.icat.funz.data.FunZApp;
import mx.unam.fc.icat.funz.db.Module;
import mx.unam.fc.icat.funz.repository.AppStateRepository;
import mx.unam.fc.icat.funz.repository.ExerciseRepository;

/**
 * TemasViewModel — ViewModel para TemasActivity.
 *
 * Expone la lista de módulos directamente como LiveData<List<Module>>
 * desde Room. Cuando la DB se actualiza (p.ej. se desbloquea un módulo),
 * TemasActivity se refresca automáticamente sin llamar a onResume.
 */
public class TemasViewModel extends AndroidViewModel {

    private final ExerciseRepository repo;
    private final AppStateRepository stateRepo;

    /** LiveData reactivo — se actualiza solo cuando cambia la DB. */
    public final LiveData<List<Module>> modules;

    public TemasViewModel(@NonNull Application app) {
        super(app);
        FunZApp appScope = (FunZApp) app;
        repo    = appScope.getExerciseRepository();
        stateRepo = appScope.getAppStateRepository();
        modules = repo.getAllModules();
    }

    /**
     * Inicia el flujo de ejercicios de un módulo desde el paso actual
     * guardado en AppState. Devuelve (moduleId, stepOrder).
     */
    public int[] getStartTarget(int moduleId) {
        stateRepo.resetSession();
        int step = stateRepo.getCurrentStep(moduleId);
        return new int[]{ moduleId, step };
    }

    /**
     * Progreso del módulo como porcentaje 0-100.
     * Delega a AppState que ya tiene los flags persistidos.
     */
    public int getModuleProgress(int moduleId) {
        return stateRepo.getModuleProgress(moduleId);
    }
}
