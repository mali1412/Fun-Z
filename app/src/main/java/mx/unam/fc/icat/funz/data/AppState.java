package mx.unam.fc.icat.funz.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * AppState — Singleton que gestiona el estado global del usuario.
 */
public class AppState {

    private static final String PREFS = "funz_prefs";
    private static AppState instance;

    private SharedPreferences prefs;

    public static AppState getInstance() {
        if (instance == null) instance = new AppState();
        return instance;
    }

    public void init(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private AppState() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Usuario
    // ════════════════════════════════════════════════════════════════════════

    public String  getUsername()           { return str("username", "Usuario"); }
    public void    setUsername(String v)   { put("username", v); }

    public int     getTotalPoints()        { return i("total_points", 0); }
    public void    addPoints(int pts)      { put("total_points", getTotalPoints() + pts); }

    public int     getStreakDays()         { return i("streak_days", 1); }

    public boolean isDarkTheme()           { return b("dark_theme", false); }
    public void    setDarkTheme(boolean v) { put("dark_theme", v); }

    public boolean isHapticFeedbackEnabled()           { return b("haptic_feedback", true); }
    public void    setHapticFeedbackEnabled(boolean v) { put("haptic_feedback", v); }

    // ════════════════════════════════════════════════════════════════════════
    //  Progreso genérico por módulo
    // ════════════════════════════════════════════════════════════════════════

    public int  getCurrentStep(int moduleId)             { return i("mod_" + moduleId + "_step", 1); }
    private void setCurrentStep(int moduleId, int step)  { put("mod_" + moduleId + "_step", step); }

    public boolean isInfoRead(int moduleId)              { return b("mod_" + moduleId + "_info", false); }
    public void    setInfoRead(int moduleId, boolean v)  { put("mod_" + moduleId + "_info", v); }

    public boolean isExamplesRead(int moduleId)              { return b("mod_" + moduleId + "_ex", false); }
    public void    setExamplesRead(int moduleId, boolean v)  { put("mod_" + moduleId + "_ex", v); }

    public boolean isStepDone(int moduleId, int step)        { return b("mod_" + moduleId + "_s" + step, false); }
    private void   setStepDone(int moduleId, int step)       { put("mod_" + moduleId + "_s" + step, true); }

    public boolean isModuleComplete(int moduleId)            { return b("mod_" + moduleId + "_done", false); }
    private void   setModuleComplete(int moduleId)           { put("mod_" + moduleId + "_done", true); }

    public int  getModuleExerciseCount(int moduleId)             { return i("mod_" + moduleId + "_count", 3); }
    public void setModuleExerciseCount(int moduleId, int count)  { put("mod_" + moduleId + "_count", count); }

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
        return !b("mod_" + moduleId + "_locked", true);
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
                put("mod_" + (moduleId + 1) + "_locked", false);
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
    public void setHintUsedBal(boolean v) { put("hint_bal", v); }
    public void setHintUsedCla(boolean v) { put("hint_cla", v); }
    public void setHintUsedTil(boolean v) { put("hint_til", v); }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════

    public int getActiveModuleId() { return i("active_module", 1); }
    public void setActiveModuleId(int id) { put("active_module", id); }

    public boolean isMod2Unlocked()  { return isModuleUnlocked(2); }
    
    public String getResumeBadge() {
        int mod = getActiveModuleId();
        if (isModuleComplete(mod)) return "Módulo " + mod + " ✅ Completado";
        return "Módulo " + mod + " · Ej. " + getCurrentStep(mod) + "/" + getModuleExerciseCount(mod);
    }

    private String  str(String k, String def)  { return prefs != null ? prefs.getString(k, def) : def; }
    private int     i(String k, int def)       { return prefs != null ? prefs.getInt(k, def) : def; }
    private boolean b(String k, boolean def)   { return prefs != null ? prefs.getBoolean(k, def) : def; }

    private void put(String k, String v)  { if (prefs != null) prefs.edit().putString(k, v).apply(); }
    private void put(String k, int v)     { if (prefs != null) prefs.edit().putInt(k, v).apply(); }
    private void put(String k, boolean v) { if (prefs != null) prefs.edit().putBoolean(k, v).apply(); }
}
