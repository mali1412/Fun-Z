package mx.unam.fc.icat.funz.repository;

import mx.unam.fc.icat.funz.data.AppState;

/**
 * AppStateRepository centraliza el acceso al estado persistido del usuario.
 *
 * Mantiene a los ViewModel desacoplados del singleton AppState,
 * facilitando pruebas y futuras migraciones de almacenamiento.
 */
public class AppStateRepository {

    private final AppState state;

    public AppStateRepository(AppState state) {
        this.state = state;
    }

    // Usuario / configuración
    public String getUsername() { return state.getUsername(); }
    public void setUsername(String username) { state.setUsername(username); }
    public boolean isDarkTheme() { return state.isDarkTheme(); }
    public void setDarkTheme(boolean darkTheme) { state.setDarkTheme(darkTheme); }
    public boolean isHapticFeedbackEnabled() { return state.isHapticFeedbackEnabled(); }
    public void setHapticFeedbackEnabled(boolean enabled) { state.setHapticFeedbackEnabled(enabled); }
    public boolean isAudioFeedbackEnabled() { return state.isAudioFeedbackEnabled(); }
    public void setAudioFeedbackEnabled(boolean enabled) { state.setAudioFeedbackEnabled(enabled); }

    // Progreso general
    public int getTotalPoints() { return state.getTotalPoints(); }
    public int getStreakDays() { return state.getStreakDays(); }
    public int getActiveModuleId() { return state.getActiveModuleId(); }
    public void setActiveModuleId(int moduleId) { state.setActiveModuleId(moduleId); }

    public int getCurrentStep(int moduleId) { return state.getCurrentStep(moduleId); }
    public int getModuleProgress(int moduleId) { return state.getModuleProgress(moduleId); }
    public int getModuleExerciseCount(int moduleId) { return state.getModuleExerciseCount(moduleId); }
    public boolean isStepDone(int moduleId, int step) { return state.isStepDone(moduleId, step); }
    public boolean isModuleComplete(int moduleId) { return state.isModuleComplete(moduleId); }

    // Sesión de ejercicios
    public void resetSession() { state.resetSession(); }
    public void markExerciseDone(int moduleId, int step, int totalSteps,
                                 boolean correct, boolean hintUsed, int points) {
        state.markExerciseDone(moduleId, step, totalSteps, correct, hintUsed, points);
    }
}
