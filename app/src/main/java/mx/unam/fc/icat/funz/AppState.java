package mx.unam.fc.icat.funz;


/**
 * AppState — Singleton para gestionar el estado global de FunZ.
 *
 * Mantiene en memoria: username, puntos, racha, progreso de módulos,
 * paso actual del flujo de ejercicios y flags de pista usada.
 *
 * En producción se persistiría con SharedPreferences dentro de FunZApp.
 */
public class AppState {

    // ── Singleton ────────────────────────────────────────────────────────────
    private static AppState instance;

    public static AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    private AppState() {}

    // ── Datos del usuario ────────────────────────────────────────────────────
    private String  username    = "Marco";
    private int     totalPoints = 1040;
    private int     streakDays  = 7;
    private boolean darkTheme   = false;

    // ── Progreso Módulo 1 ────────────────────────────────────────────────────
    // currentExStep: 1 = Balanza, 2 = Clásico, 3 = Tiles
    private int     currentExStep = 1;
    private boolean infoRead      = false;
    private boolean examplesRead  = false;
    private boolean ex1Done       = false;
    private boolean ex2Done       = false;
    private boolean ex3Done       = false;
    private boolean mod1Complete  = false;
    private boolean mod2Unlocked  = false;

    // ── Sesión actual de ejercicios ──────────────────────────────────────────
    private int     sessionOk   = 0;
    private int     sessionFail = 0;
    private int     sessionPts  = 0;
    private boolean hintUsedBal = false;
    private boolean hintUsedCla = false;
    private boolean hintUsedTil = false;

    // ════════════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ════════════════════════════════════════════════════════════════════════

    public String  getUsername()              { return username; }
    public void    setUsername(String u)      { username = u; }

    public int     getTotalPoints()           { return totalPoints; }
    public void    addPoints(int pts)         { totalPoints += pts; }

    public int     getStreakDays()            { return streakDays; }

    public boolean isDarkTheme()              { return darkTheme; }
    public void    setDarkTheme(boolean d)    { darkTheme = d; }

    public int     getCurrentExStep()         { return currentExStep; }

    public boolean isInfoRead()               { return infoRead; }
    public void    setInfoRead(boolean v)     { infoRead = v; }

    public boolean isExamplesRead()           { return examplesRead; }
    public void    setExamplesRead(boolean v) { examplesRead = v; }

    public boolean isEx1Done()                { return ex1Done; }
    public boolean isEx2Done()                { return ex2Done; }
    public boolean isEx3Done()                { return ex3Done; }

    public boolean isMod1Complete()           { return mod1Complete; }
    public boolean isMod2Unlocked()           { return mod2Unlocked; }

    public boolean isHintUsedBal()            { return hintUsedBal; }
    public void    setHintUsedBal(boolean v)  { hintUsedBal = v; }

    public boolean isHintUsedCla()            { return hintUsedCla; }
    public void    setHintUsedCla(boolean v)  { hintUsedCla = v; }

    public boolean isHintUsedTil()            { return hintUsedTil; }
    public void    setHintUsedTil(boolean v)  { hintUsedTil = v; }

    public int getSessionOk()                 { return sessionOk; }
    public int getSessionFail()               { return sessionFail; }
    public int getSessionPts()                { return sessionPts; }

    // ════════════════════════════════════════════════════════════════════════
    //  Lógica de negocio
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Registra el resultado de un ejercicio.
     * @param step     1 = Balanza, 2 = Clásico, 3 = Tiles
     * @param correct  true si respondió correctamente
     * @param hintUsed true si usó la pista en este intento
     */
    public void markExerciseDone(int step, boolean correct, boolean hintUsed) {
        if (correct) {
            int pts = hintUsed ? 50 : 100;
            addPoints(pts);
            sessionOk++;
            sessionPts += pts;
            switch (step) {
                case 1: ex1Done = true; currentExStep = 2; break;
                case 2: ex2Done = true; currentExStep = 3; break;
                case 3:
                    ex3Done      = true;
                    mod1Complete = true;
                    mod2Unlocked = true;
                    currentExStep = 1;
                    break;
            }
        } else {
            sessionFail++;
        }
    }

    /**
     * Porcentaje de progreso del Módulo 1 (0 – 100).
     * Cada hito (info, ejemplos, ej1, ej2, ej3) aporta 20 %.
     */
    public int getMod1Progress() {
        if (mod1Complete) return 100;
        int steps = 0;
        if (infoRead)     steps++;
        if (examplesRead) steps++;
        if (ex1Done)      steps++;
        if (ex2Done)      steps++;
        if (ex3Done)      steps++;
        return steps * 20;
    }

    /** Reinicia contadores de la sesión actual sin borrar el paso guardado. */
    public void resetSession() {
        sessionOk   = 0;
        sessionFail = 0;
        sessionPts  = 0;
    }

    // ── Helpers para la card "Continuar" en Home ─────────────────────────────

    public String getResumeBadge() {
        if (mod1Complete) return "Módulo 1 ✅ Completado";
        return "Módulo 1 · Ej. " + currentExStep + "/3";
    }

    public String getResumeMethod() {
        switch (currentExStep) {
            case 2:  return "Clásico (Baldor)";
            case 3:  return "Algebra Tiles";
            default: return "Balanza";
        }
    }

    public String getResumeEquation() {
        switch (currentExStep) {
            case 2:  return "3x + 5 = 20";
            case 3:  return "x + 2 = 12";
            default: return "x + 5 = 10";
        }
    }
}
