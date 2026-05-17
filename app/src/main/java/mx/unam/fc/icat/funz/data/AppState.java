package mx.unam.fc.icat.funz.data;

import android.content.Context;
import android.content.SharedPreferences;

import mx.unam.fc.icat.funz.R;

/**
 * Singleton de alto rendimiento encargado de la gestión, persistencia y auditoría
 * del estado global del usuario y las métricas analíticas de la sesión de juego.
 * <p>
 * Centraliza el almacenamiento ligero utilizando {@link SharedPreferences} para asegurar
 * accesos síncronos de lectura y escritura en tiempo real de configuraciones de usuario,
 * rachas, puntuaciones acumuladas y estados de flujo, reduciendo el estrés transaccional
 * sobre la base de datos relacional.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class AppState {

    // ── Constantes de Persisntencia ──
    private static final String PREFS = "funz_prefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_TOTAL_POINTS = "total_points";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String KEY_AUDIO_FEEDBACK = "audio_feedback";
    private static final String KEY_ACTIVE_MODULE = "active_module";

    // ── Sufijos y prefijos para la generación de llaves dinamicas ──
    private static final String KEY_MODULE_PREFIX = "mod_";
    private static final String KEY_STEP_SUFFIX = "_step";
    private static final String KEY_INFO_SUFFIX = "_info";
    private static final String KEY_EXAMPLES_SUFFIX = "_ex";
    private static final String KEY_DONE_SUFFIX = "_done";
    private static final String KEY_COUNT_SUFFIX = "_count";
    private static final String KEY_STEP_MARKER_PREFIX = "_s";

    // ── Variables de control y configuración ──
    private static AppState instance;
    private SharedPreferences prefs;
    private String defaultUsername;

    // ── Sesión de ejercicios ──
    private int sessionOk = 0, sessionFail = 0, sessionPts = 0, sessionHints = 0, sessionReveals = 0;

    /**
     * Recupera o construye la instancia única y global de AppState (Patrón Singleton).
     * Garantiza un único punto de acceso al estado de persistencia en la aplicación.
     *
     * @return Instancia única de {@link AppState}.
     */
    public static AppState getInstance() {
        if (instance == null) instance = new AppState();
        return instance;
    }

    /**
     * Inicializa el contenedor de SharedPreferences y precarga valores de contingencia.
     * Debe invocarse de manera temprana en el arranque global de la app (ej. en la clase FunZApp).
     *
     * @param ctx Contexto de la aplicación utilizado para inicializar las SharedPreferences.
     */
    public void init(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        defaultUsername = ctx.getString(R.string.default_username);
    }

    /**
     * Constructor privado para restringir la instanciación externa y forzar el uso del Singleton.
     */
    private AppState() {}

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Gestión de Usuario y Preferencias Sensoriales
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Recupera el nombre de usuario registrado. Si el campo se encuentra vacío,
     * retorna el valor por defecto configurado en las string resources del sistema.
     *
     * @return Cadena de caracteres con el nombre del usuario actual o el valor default.
     */
    public String getUsername() {
        return str(KEY_USERNAME, defaultUsername != null ? defaultUsername : "");
    }

    /**
     * Modifica y persiste de forma asíncrona el nombre de usuario.
     *
     * @param v Nuevo apodo o nombre de usuario a guardar.
     */
    public void setUsername(String v){
        put(KEY_USERNAME, v);
    }

    /**
     * Recupera el acumulado histórico de puntos de gamificación obtenidos por el usuario.
     *
     * @return Cantidad de puntos totales acumulados en el perfil de usuario.
     */
    public int getTotalPoints() {
        return i(KEY_TOTAL_POINTS, 0);
    }

    /**
     * Incrementa de forma aditiva la puntuación global del estudiante en el registro permanente.
     *
     * @param pts Unidades de puntuación calculadas que se sumarán al registro actual.
     */
    public void addPoints(int pts) {
        put(KEY_TOTAL_POINTS, getTotalPoints() + pts);
    }

    /**
     * Obtiene los días consecutivos en los que el usuario ha resuelto desafíos algebraicos.
     *
     * @return Número de días que conforman la racha actual del estudiante (mínimo 1).
     */
    public int getStreakDays() {
        return i("streak_days", 1);
    }

    /**
     * Evalúa si la interfaz visual debe renderizarse en Modo Oscuro.
     *
     * @return true si el tema oscuro está activo; false si se prefiere el tema claro de la app.
     */
    public boolean isDarkTheme() {
        return b(KEY_DARK_THEME, false);
    }

    /**
     * Almacena permanentemente la preferencia de la identidad gráfica (Modo Oscuro/Claro).
     *
     * @param v true para forzar el tema oscuro; false para restablecer al tema base.
     */
    public void setDarkTheme(boolean v) {
        put(KEY_DARK_THEME, v);
    }

    /**
     * Determina si las respuestas hápticas por vibración táctil están autorizadas en la UI.
     *
     * @return true si se deben disparar efectos de vibración sensorial ante interacciones.
     */
    public boolean isHapticFeedbackEnabled() {
        return b(KEY_HAPTIC_FEEDBACK, true);
    }

    /**
     * Almacena la bandera de activación para el sistema de respuesta háptica.
     *
     * @param v true para habilitar vibraciones táctiles; false para silenciarlas.
     */
    public void setHapticFeedbackEnabled(boolean v) {
        put(KEY_HAPTIC_FEEDBACK, v);
    }

    /**
     * Determina si la retroalimentación por audio está autorizada para las validaciones.
     *
     * @return true si los efectos sonoros de acierto/error están activos.
     */
    public boolean isAudioFeedbackEnabled() {
        return b(KEY_AUDIO_FEEDBACK, true);
    }

    /**
     * Almacena la bandera de activación para los efectos sonoros del sistema.
     *
     * @param v true para habilitar la reproducción de audio; false para mutear.
     */
    public void setAudioFeedbackEnabled(boolean v) {
        put(KEY_AUDIO_FEEDBACK, v);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Seguimiento de Módulos
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Obtiene el índice secuencial del ejercicio (paso) actual en el que se quedó el alumno.
     *
     * @param moduleId Identificador único del módulo consultado.
     * @return Índice numérico del ejercicio pendiente por resolver.
     */
    public int getCurrentStep(int moduleId) {
        return i(moduleStepKey(moduleId), 1);
    }

    /**
     * Setea y persiste internamente el índice del ejercicio en curso para un módulo.
     */
    private void setCurrentStep(int moduleId, int step) {
        put(moduleStepKey(moduleId), step);
    }

    /**
     * Verifica si el estudiante ya concluyó la lectura teórica del bloque de definición.
     *
     * @param moduleId Identificador del módulo consultado.
     * @return true si el bloque informativo ya fue explorado y marcado como leído.
     */
    public boolean isInfoRead(int moduleId) {
        return b(moduleInfoKey(moduleId), false);
    }

    /**
     * Registra el estado de exploración de la sección teórica del módulo.
     */
    public void setInfoRead(int moduleId, boolean v) {
        put(moduleInfoKey(moduleId), v);
    }

    /**
     * Verifica si la sección de ejemplos interactivos ya fue completada por el estudiante.
     *
     * @param moduleId Identificador del módulo consultado.
     * @return true si las tarjetas didácticas de ejemplos ya fueron leídas.
     */
    public boolean isExamplesRead(int moduleId) {
        return b(moduleExamplesKey(moduleId), false);
    }

    /**
     * Registra el estado de exploración del panel de ejemplos dinámicos.
     */
    public void setExamplesRead(int moduleId, boolean v) {
        put(moduleExamplesKey(moduleId), v);
    }

    /**
     * Verifica si un ejercicio en particular ya fue resuelto con éxito anteriormente.
     *
     * @param moduleId Identificador del módulo contenedor.
     * @param step     Número de orden del ejercicio dentro del módulo.
     * @return true si el paso ya cuenta con aprobación matemática guardada.
     */
    public boolean isStepDone(int moduleId, int step) {
        return b(moduleStepDoneKey(moduleId, step), false);
    }

    /**
     * Marca un ejercicio específico como resuelto de manera definitiva y exitosa.
     */
    private void setStepDone(int moduleId, int step) {
        put(moduleStepDoneKey(moduleId, step), true);
    }

    /**
     * Determina si el módulo de estudio fue completado en su totalidad (todos los pasos aprobados).
     *
     * @param moduleId Identificador numérico del módulo.
     * @return true si el módulo ya fue culminado exitosamente.
     */
    public boolean isModuleComplete(int moduleId) {
        return b(moduleDoneKey(moduleId), false);
    }

    /**
     * Cambia de forma permanente el estado de un módulo a Completado.
     */
    private void setModuleComplete(int moduleId) {
        put(moduleDoneKey(moduleId), true);
    }

    /**
     * Obtiene la cantidad total de ejercicios que componen a un módulo específico.
     *
     * @param moduleId Identificador del módulo.
     * @return Cantidad de desafíos matemáticos configurados en el seeder (por defecto 3).
     */
    public int getModuleExerciseCount(int moduleId) {
        return i(moduleCountKey(moduleId), 3);
    }

    /**
     * Define la cantidad máxima de ejercicios disponibles para un módulo de la base de datos.
     */
    public void setModuleExerciseCount(int moduleId, int count) {
        put(moduleCountKey(moduleId), count);
    }

    /**
     * Devuelve la cantidad fija de módulos que estructuran la ruta de aprendizaje de FunZ.
     *
     * @return Total de módulos construidos en el sistema (constante de negocio de Baldor igual a 6).
     */
    public int getTotalModules() {
        return 6;
    }

    /**
     * Calcula en tiempo real el porcentaje analítico de progreso de un módulo.
     * Si el módulo está marcado como completo, retorna directamente 100%. De lo contrario,
     * evalúa proporcionalmente los ejercicios resueltos sin superar el 99% hasta el cierre.
     *
     * @param moduleId Identificador del módulo a evaluar.
     * @return Valor numérico entero en el rango de [0 a 100] que representa el avance.
     */
    public int getModuleProgress(int moduleId) {
        if (isModuleComplete(moduleId)) return 100;
        int total = getModuleExerciseCount(moduleId);
        if (total <= 0) return 0;

        int done = 0;
        for (int s = 1; s <= total; s++) {
            if (isStepDone(moduleId, s)) done++;
        }
        return Math.min((done * 100) / total, 99);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Monitoreo de la sesión activa.
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Obtiene la cantidad de ejercicios resueltos correctamente en la sesión activa.
     *
     * @return Número de aciertos acumulados en la sesión actual.
     */
    public int getSessionOk() {
        return sessionOk;
    }

    /**
     * Obtiene la cantidad de ejercicios en los que el usuario falló en la sesión activa.
     *
     * @return Número de errores acumulados en la sesión actual.
     */
    public int getSessionFail() {
        return sessionFail;
    }

    /**
     * Obtiene el puntaje total acumulado exclusivamente durante la sesión activa.
     *
     * @return Puntos ganados en la sesión en curso.
     */
    public int getSessionPts() {
        return sessionPts;
    }

    /**
     * Obtiene la cantidad de veces que se solicitó una pista conceptual en la sesión activa.
     *
     * @return Número de ayudas pedagógicas consultadas.
     */
    public int getSessionHints() {
        return sessionHints;
    }

    /**
     * Obtiene la cantidad de veces que el usuario se rindió y reveló la respuesta en la sesión activa.
     *
     * @return Número de soluciones directas mostradas en la sesión.
     */
    public int getSessionReveals() {
        return sessionReveals;
    }

    /**
     * Reinicia por completo los contadores analíticos de desempeño de la sesión en curso.
     * Restablece aciertos, fallas, puntos, pistas y soluciones reveladas a cero.
     */
    public void resetSession() {
        sessionOk = 0;
        sessionFail = 0;
        sessionPts = 0;
        sessionHints = 0;
        sessionReveals = 0;
    }

    /**
     * Transacciona, calcula y asienta el resultado analítico de la resolución de un ejercicio.
     * <p>
     * Actualiza los puntos históricos del perfil, segrega las métricas de la sesión activa
     * (distinguiendo entre respuestas correctas limpias, asistidas por pistas o reveladas por rendición)
     * y controla la máquina de estados de navegación.
     * Si el alumno concluye el último desafío de un tema, marca el módulo como completo y actualiza
     * el puntero global hacia el siguiente módulo correspondiente.
     * </p>
     *
     * @param moduleId           Identificador del módulo en el que se encuentra operando el usuario.
     * @param step               Orden del ejercicio que se acaba de evaluar.
     * @param totalSteps         Cantidad de ejercicios máximos que estructuran el módulo actual.
     * @param correctOrRevealed  Bandera que indica si la respuesta fue matemáticamente correcta o se forzó el revelado.
     * @param hintUsed           Indica si el estudiante utilizó una pista conceptual para resolver el paso.
     * @param points             Puntuación calculada dinámicamente según el desempeño en tiempo y dificultad.
     */
    public void markExerciseDone(int moduleId, int step, int totalSteps,
                                 boolean correctOrRevealed, boolean hintUsed, int points) {
        if (correctOrRevealed) {
            addPoints(points);
            sessionPts += points;

            // Si los puntos son 0, el usuario usó la opción de rendición (Reveal)
            if (points == 0) {
                sessionReveals++;
            } else {
                sessionOk++;
                if (hintUsed) sessionHints++;
            }

            setStepDone(moduleId, step);

            // Control de la ruta de aprendizaje secuencial
            if (step < totalSteps) {
                setCurrentStep(moduleId, step + 1);
            } else {
                // El estudiante resolvió exitosamente el último paso
                setModuleComplete(moduleId);
                setCurrentStep(moduleId, 1);

                int maxModules = getTotalModules();
                if (moduleId < maxModules) {
                    setActiveModuleId(moduleId + 1); // Avanzar el foco global al siguiente tema didáctico
                } else {
                    setActiveModuleId(moduleId); // Fin de la ruta: Mantener anclado en Módulo 6
                }
            }
        } else {
            sessionFail++;
        }
    }

    /**
     * Obtiene el identificador del módulo que se encuentra activo en el foco de estudio del usuario.
     *
     * @return ID del módulo actual en curso (por defecto retorna 1).
     */
    public int getActiveModuleId() {
        return i(KEY_ACTIVE_MODULE, 1);
    }

    /**
     * Cambia de forma permanente el identificador del módulo seleccionado como foco de estudio activo.
     *
     * @param id Nuevo identificador de módulo que tomará el foco global.
     */
    public void setActiveModuleId(int id) {
        put(KEY_ACTIVE_MODULE, id);
    }

    // =========================================================================
    // Metodos auxiliares de persistencia
    // =========================================================================

    // Recupera de forma síncrona un String de SharedPreferences encapsulando la validación de nulos.
    private String str(String k, String def) {
        return prefs != null ? prefs.getString(k, def) : def;
    }

    // Recupera de forma síncrona un entero (int) de SharedPreferences.
    private int i(String k, int def) {
        return prefs != null ? prefs.getInt(k, def) : def;
    }

    // Recupera de forma síncrona un valor booleano de SharedPreferences.
    private boolean b(String k, boolean def) {
        return prefs != null ? prefs.getBoolean(k, def) : def;
    }

    // Inyecta y persiste un String en el almacenamiento de SharedPreferences mediante commit asíncrono (.apply).
    private void put(String k, String v) {
        if (prefs != null) prefs.edit().putString(k, v).apply();
    }

    // Inyecta y persiste un valor entero (int) en SharedPreferences de forma segura.
    private void put(String k, int v) {
        if (prefs != null) prefs.edit().putInt(k, v).apply();
    }

    // Inyecta y persiste un valor booleano en SharedPreferences de forma segura.
    private void put(String k, boolean v) {
        if (prefs != null) prefs.edit().putBoolean(k, v).apply();
    }

    // Generadores dinámicos de llaves string compuestas para mapear el progreso modular individual
    private String moduleStepKey(int moduleId) { return KEY_MODULE_PREFIX + moduleId + KEY_STEP_SUFFIX; }
    private String moduleInfoKey(int moduleId) { return KEY_MODULE_PREFIX + moduleId + KEY_INFO_SUFFIX; }
    private String moduleExamplesKey(int moduleId) { return KEY_MODULE_PREFIX + moduleId + KEY_EXAMPLES_SUFFIX; }
    private String moduleStepDoneKey(int moduleId, int step) { return KEY_MODULE_PREFIX + moduleId + KEY_STEP_MARKER_PREFIX + step; }
    private String moduleDoneKey(int moduleId) { return KEY_MODULE_PREFIX + moduleId + KEY_DONE_SUFFIX; }
    private String moduleCountKey(int moduleId) { return KEY_MODULE_PREFIX + moduleId + KEY_COUNT_SUFFIX; }
}