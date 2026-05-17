package mx.unam.fc.icat.funz.repository;

import mx.unam.fc.icat.funz.data.AppState;

/**
 * Repositorio intermediario encargado de centralizar y coordinar el acceso al estado analítico,
 * progreso académico y configuraciones globales de la sesión del usuario.
 * <p>
 * Su objetivo arquitectónico principal es desacoplar de forma estricta las capas de la interfaz
 * de usuario (ViewModels) de la implementación concreta del Singleton de persistencia ligera {@link AppState}.
 * Esto facilita el aislamiento para pruebas unitarias y blinda al sistema ante futuras migraciones de infraestructura
 * de almacenamiento (como Jetpack DataStore, Room o sincronización remota mediante APIs).
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class AppStateRepository {

    private final AppState state;

    /**
     * Constructor por inyección que asocia el origen de datos del estado global de la aplicación.
     *
     * @param state Instancia centralizada de {@link AppState}.
     */
    public AppStateRepository(AppState state) {
        this.state = state;
    }

    // Ajustes de configuración

    /** @return El alias o nombre de perfil registrado por el estudiante. */
    public String getUsername() { return state.getUsername(); }

    /**
     * Actualiza el alias de identidad del estudiante en el almacenamiento persistente.
     *
     * @param username Nuevo nombre de perfil.
     */
    public void setUsername(String username) { state.setUsername(username); }

    /** @return {@code true} si el tema oscuro de la UI se encuentra activo; {@code false} para tema claro. */
    public boolean isDarkTheme() { return state.isDarkTheme(); }

    /**
     * Almacena la preferencia de estilo visual de la UI en el dispositivo físico.
     *
     * @param darkTheme {@code true} para forzar el modo noche; {@code false} para modo día.
     */
    public void setDarkTheme(boolean darkTheme) { state.setDarkTheme(darkTheme); }

    /** @return {@code true} si la respuesta háptica (vibración sensorial de éxito/error) está habilitada. */
    public boolean isHapticFeedbackEnabled() { return state.isHapticFeedbackEnabled(); }

    /**
     * Sincroniza la preferencia de activación del motor de vibración del dispositivo.
     *
     * @param enabled {@code true} para activar la vibración ante eventos didácticos.
     */
    public void setHapticFeedbackEnabled(boolean enabled) { state.setHapticFeedbackEnabled(enabled); }

    /** @return {@code true} si los efectos de sonido y alertas auditivas están activos en los desafíos. */
    public boolean isAudioFeedbackEnabled() { return state.isAudioFeedbackEnabled(); }

    /**
     * Sincroniza la preferencia de reproducción de audio y efectos didácticos.
     *
     * @param enabled {@code true} para habilitar el sonido del sistema de gamificación.
     */
    public void setAudioFeedbackEnabled(boolean enabled) { state.setAudioFeedbackEnabled(enabled); }

    // Progreso general de gamificacion

    /** @return Volumen entero acumulado de puntos de experiencia (XP) obtenidos por el usuario. */
    public int getTotalPoints() { return state.getTotalPoints(); }

    /** @return Cantidad de días consecutivos acumulados en la racha de estudio del estudiante. */
    public int getStreakDays() { return state.getStreakDays(); }

    /** @return Identificador del módulo temático activo en el mapa de aprendizaje. */
    public int getActiveModuleId() { return state.getActiveModuleId(); }

    /**
     * Modifica el puntero de navegación del mapa para rastrear qué módulo está inspeccionando el usuario.
     *
     * @param moduleId Identificador único del módulo de estudio.
     */
    public void setActiveModuleId(int moduleId) { state.setActiveModuleId(moduleId); }

    /**
     * Consulta el índice de paso o ejercicio actual en el que se encuentra encallado el alumno dentro de un tema.
     *
     * @param moduleId Identificador del módulo consultado.
     * @return Índice entero secuencial del ejercicio objetivo.
     */
    public int getCurrentStep(int moduleId) { return state.getCurrentStep(moduleId); }

    /**
     * Recupera el porcentaje de avance lineal calculado para un bloque temático específico.
     *
     * @param moduleId Identificador del módulo consultado.
     * @return Escalar entero en el rango de [0 a 100] que representa el progreso.
     */
    public int getModuleProgress(int moduleId) { return state.getModuleProgress(moduleId); }

    /**
     * Devuelve el volumen total de desafíos matemáticos indexados que integran un módulo.
     *
     * @param moduleId Identificador del módulo bajo análisis.
     * @return Cantidad absoluta de ejercicios que componen la unidad.
     */
    public int getModuleExerciseCount(int moduleId) { return state.getModuleExerciseCount(moduleId); }

    /**
     * Predicado analítico que audita si un desafío específico ya fue resuelto con éxito con anterioridad.
     *
     * @param moduleId Identificador del módulo contenedor.
     * @param step     Número de orden secuencial del ejercicio.
     * @return {@code true} si el ejercicio ya está aprobado; {@code false} en caso contrario.
     */
    public boolean isStepDone(int moduleId, int step) { return state.isStepDone(moduleId, step); }
    /**
     * Diagnostica si un bloque de estudio ha cumplido con el total de sus desafíos y criterios de aprobación.
     *
     * @param moduleId Identificador del módulo bajo análisis.
     * @return {@code true} si el progreso de la unidad ha alcanzado el 100%; {@code false} en caso contrario.
     */
    public boolean isModuleComplete(int moduleId) { return state.isModuleComplete(moduleId); }

    // Sesión de ejercicios

    /**
     * Purga y restablece los cronómetros, variables volátiles e hilos temporales de la sesión en curso
     * para iniciar un nuevo flujo interactivo libre de residuos de memoria.
     */
    public void resetSession() { state.resetSession(); }

    /**
     * Modifica de manera permanente las banderas de progreso y el balance de gamificación del usuario tras concluir
     * un ejercicio algebraico.
     * <p>
     * Registra analíticamente si el reactivo fue aprobado al primer intento, si se requirió el consumo de pistas
     * (BottomSheet) e inyecta de forma atómica los puntos de recompensa calculados dinámicamente por el ViewModel.
     * </p>
     *
     * @param moduleId   Identificador único del módulo actual.
     * @param step       Índice posicional del ejercicio resuelto.
     * @param totalSteps Volumen de pasos totales del módulo para validar si se deba detonar un desbloqueo en cascada.
     * @param correct    {@code true} si la resolución matemática fue exitosa.
     * @param hintUsed   {@code true} si el estudiante desplegó la ayuda didáctica.
     * @param points     Puntuación dinámica neta calculada en el área de trabajo que será sumada a la experiencia global.
     */
    public void markExerciseDone(int moduleId, int step, int totalSteps,
                                 boolean correct, boolean hintUsed, int points) {
        state.markExerciseDone(moduleId, step, totalSteps, correct, hintUsed, points);
    }
}
