package mx.unam.fc.icat.funz.data;

import android.content.Context;
import android.content.SharedPreferences;

import mx.unam.fc.icat.funz.R;

/**
 * AppState — Singleton que gestiona el estado global del usuario.
 */
public class AppState {

    private static final String PREFS = "funz_prefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_TOTAL_POINTS = "total_points";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String KEY_AUDIO_FEEDBACK = "audio_feedback";
    private static final String KEY_ACTIVE_MODULE = "active_module";
    private static final String KEY_HINT_BAL = "hint_bal";
    private static final String KEY_HINT_CLA = "hint_cla";
    private static final String KEY_HINT_TIL = "hint_til";

    private static final String KEY_MODULE_PREFIX = "mod_";
    private static final String KEY_STEP_SUFFIX = "_step";
    private static final String KEY_INFO_SUFFIX = "_info";
    private static final String KEY_EXAMPLES_SUFFIX = "_ex";
    private static final String KEY_DONE_SUFFIX = "_done";
    private static final String KEY_COUNT_SUFFIX = "_count";
    private static final String KEY_LOCKED_SUFFIX = "_locked";
    private static final String KEY_STEP_MARKER_PREFIX = "_s";

    private static AppState instance;

    private SharedPreferences prefs;
    private String defaultUsername;

    public static AppState getInstance() {
        if (instance == null) instance = new AppState();
        return instance;
    }

    public void init(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        defaultUsername = ctx.getString(R.string.default_username);
    }

    private AppState() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Usuario
    // ════════════════════════════════════════════════════════════════════════

    public String  getUsername()           { return str(KEY_USERNAME, defaultUsername != null ? defaultUsername : ""); }
    public void    setUsername(String v)   { put(KEY_USERNAME, v); }

    public int     getTotalPoints()        { return i(KEY_TOTAL_POINTS, 0); }
    public void    addPoints(int pts)      { put(KEY_TOTAL_POINTS, getTotalPoints() + pts); }

    public int     getStreakDays()         { return i("streak_days", 1); }

    public boolean isDarkTheme()           { return b(KEY_DARK_THEME, false); }
    public void    setDarkTheme(boolean v) { put(KEY_DARK_THEME, v); }

    public boolean isHapticFeedbackEnabled()           { return b(KEY_HAPTIC_FEEDBACK, true); }
    public void    setHapticFeedbackEnabled(boolean v) { put(KEY_HAPTIC_FEEDBACK, v); }

    public boolean isAudioFeedbackEnabled()           { return b(KEY_AUDIO_FEEDBACK, true); }
    public void    setAudioFeedbackEnabled(boolean v) { put(KEY_AUDIO_FEEDBACK, v); }

    // ════════════════════════════════════════════════════════════════════════
    //  Progreso genérico por módulo
    // ════════════════════════════════════════════════════════════════════════

    public int  getCurrentStep(int moduleId)             { return i(moduleStepKey(moduleId), 1); }
    private void setCurrentStep(int moduleId, int step)  { put(moduleStepKey(moduleId), step); }

    public boolean isInfoRead(int moduleId)              { return b(moduleInfoKey(moduleId), false); }
    public void    setInfoRead(int moduleId, boolean v)  { put(moduleInfoKey(moduleId), v); }

    public boolean isExamplesRead(int moduleId)              { return b(moduleExamplesKey(moduleId), false); }
    public void    setExamplesRead(int moduleId, boolean v)  { put(moduleExamplesKey(moduleId), v); }

    public boolean isStepDone(int moduleId, int step)        { return b(moduleStepDoneKey(moduleId, step), false); }
    private void   setStepDone(int moduleId, int step)       { put(moduleStepDoneKey(moduleId, step), true); }

    public boolean isModuleComplete(int moduleId)            { return b(moduleDoneKey(moduleId), false); }
    private void   setModuleComplete(int moduleId)           { put(moduleDoneKey(moduleId), true); }

    public int  getModuleExerciseCount(int moduleId)             { return i(moduleCountKey(moduleId), 3); }
    public void setModuleExerciseCount(int moduleId, int count)  { put(moduleCountKey(moduleId), count); }

    public int getModuleProgress(int moduleId) {
        if (isModuleComplete(moduleId)) return 100;
        int total = getModuleExerciseCount(moduleId);
        if (total <= 0) return 0;

        int done = 0;
        for (int s = 1; s <= total; s++) {
            if (isStepDone(moduleId, s)) done++;
        }

        // Progreso proporcional a ejercicios completados (1/3 = 33, 2/3 = 66, etc.)
        int pct = (done * 100) / total;
        return Math.min(pct, 99);
    }

    public boolean isModuleUnlocked(int moduleId) {
        if (moduleId <= 1) return true;
        return !b(moduleLockedKey(moduleId), true);
    }

    // Shorthands para Estadísticas y Main
    public int     getMod1Progress() { return getModuleProgress(1); }
    public boolean isEx1Done()       { return isModuleComplete(1); }
    public boolean isEx2Done()       { return isModuleComplete(2); }
    public boolean isEx3Done()       { return isModuleComplete(3); }

    // ════════════════════════════════════════════════════════════════════════
    //  Sesión de ejercicios
    // ════════════════════════════════════════════════════════════════════════

    private int sessionOk = 0, sessionFail = 0, sessionPts = 0;

    public int getSessionOk()   { return sessionOk; }
    public int getSessionFail() { return sessionFail; }
    public int getSessionPts()  { return sessionPts; }

    public void resetSession() { sessionOk = 0; sessionFail = 0; sessionPts = 0; }

    public void markExerciseDone(int moduleId, int step, int totalSteps,
                                 boolean correct, boolean hintUsed, int points) {
        if (correct) {
            addPoints(points);
            sessionOk++;
            sessionPts += points;
            setStepDone(moduleId, step);
            if (step < totalSteps) {
                setCurrentStep(moduleId, step + 1);
            } else {
                setModuleComplete(moduleId);
                setCurrentStep(moduleId, 1);
                // Desbloqueo lógico para el siguiente módulo
                put(moduleLockedKey(moduleId + 1), false);
                // Al completar el módulo, el siguiente se vuelve el activo automáticamente
                setActiveModuleId(moduleId + 1);
            }
        } else {
            sessionFail++;
        }
    }

    public void markExerciseDone(int moduleId, boolean correct, boolean hintUsed) {
        int step = getCurrentStep(moduleId);
        int total = getModuleExerciseCount(moduleId);
        int points = correct ? (hintUsed ? 5 : 10) : 0;
        markExerciseDone(moduleId, step, total, correct, hintUsed, points);
    }

    // Pistas (Hints)
    public void setHintUsedBal(boolean v) { put(KEY_HINT_BAL, v); }
    public void setHintUsedCla(boolean v) { put(KEY_HINT_CLA, v); }
    public void setHintUsedTil(boolean v) { put(KEY_HINT_TIL, v); }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════

    public int getActiveModuleId() { return i(KEY_ACTIVE_MODULE, 1); }
    public void setActiveModuleId(int id) { put(KEY_ACTIVE_MODULE, id); }

    public boolean isMod2Unlocked()  { return isModuleUnlocked(2); }

    private String  str(String k, String def)  { return prefs != null ? prefs.getString(k, def) : def; }
    private int     i(String k, int def)       { return prefs != null ? prefs.getInt(k, def) : def; }
    private boolean b(String k, boolean def)   { return prefs != null ? prefs.getBoolean(k, def) : def; }

    private void put(String k, String v)  { if (prefs != null) prefs.edit().putString(k, v).apply(); }
    private void put(String k, int v)     { if (prefs != null) prefs.edit().putInt(k, v).apply(); }
    private void put(String k, boolean v) { if (prefs != null) prefs.edit().putBoolean(k, v).apply(); }

    private String moduleStepKey(int moduleId) {
        return KEY_MODULE_PREFIX + moduleId + KEY_STEP_SUFFIX;
    }

    private String moduleInfoKey(int moduleId) {
        return KEY_MODULE_PREFIX + moduleId + KEY_INFO_SUFFIX;
    }

    private String moduleExamplesKey(int moduleId) {
        return KEY_MODULE_PREFIX + moduleId + KEY_EXAMPLES_SUFFIX;
    }

    private String moduleStepDoneKey(int moduleId, int step) {
        return KEY_MODULE_PREFIX + moduleId + KEY_STEP_MARKER_PREFIX + step;
    }

    private String moduleDoneKey(int moduleId) {
        return KEY_MODULE_PREFIX + moduleId + KEY_DONE_SUFFIX;
    }

    private String moduleCountKey(int moduleId) {
        return KEY_MODULE_PREFIX + moduleId + KEY_COUNT_SUFFIX;
    }

    private String moduleLockedKey(int moduleId) {
        return KEY_MODULE_PREFIX + moduleId + KEY_LOCKED_SUFFIX;
    }
}
