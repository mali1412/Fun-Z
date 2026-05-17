package mx.unam.fc.icat.funz.model;

import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.viewmodel.EstadisticasViewModel.StatsUiState;
import mx.unam.fc.icat.funz.data.AppState;

/**
 * Enumerado polimórfico encargado de la declaración, almacenamiento de metadatos y
 * evaluación analítica del sistema de logros y medallas del universo FunZ.
 * <p>
 * Cada constante del enumerado encapsula de manera aislada sus reglas de negocio específicas
 * mediante la sobreescritura del metodo abstracto {@link #estaDesbloqueada(StatsUiState)},
 * mapeando de forma unificada identificadores de layouts gráficos con variables de progreso histórico
 * en tiempo de ejecución.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public enum Medalla {

    /** Medalla otorgada al resolver con éxito el primer ejercicio procedimental de tipo Clásico. */
    PRIMER_PASO("Primer Paso", "Resuelve tu primer ejercicio clásico", R.id.tv_medal_1) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return state.exercisesResolved > 0; }
    },

    /** Medalla global intermedia otorgada al cruzar el umbral del 50% de progreso total del mapa de temas. */
    MAESTRO_ALGEBRA("Gran Maestro", "Alcanza el 50% de progreso global", R.id.tv_medal_mod) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return state.totalProgress >= 50; }
    },

    // ── Medallas onbtenidas por completar módulos ──
    COMPLETO_MOD1("Iniciación", "Completa el Módulo 1", R.id.tv_medal_mod1) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return AppState.getInstance().isModuleComplete(1); }
    },
    COMPLETO_MOD2("Fuerza Coeficiente", "Completa el Módulo 2", R.id.tv_medal_mod2) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return AppState.getInstance().isModuleComplete(2); }
    },
    COMPLETO_MOD3("Fracción Lineal", "Completa el Módulo 3", R.id.tv_medal_mod3) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return AppState.getInstance().isModuleComplete(3); }
    },
    COMPLETO_MOD4("Destructor Candados", "Completa el Módulo 4", R.id.tv_medal_mod4) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return AppState.getInstance().isModuleComplete(4); }
    },
    COMPLETO_MOD5("Gran Agrupador", "Completa el Módulo 5", R.id.tv_medal_mod5) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return AppState.getInstance().isModuleComplete(5); }
    },
    COMPLETO_MOD6("Baldor Omnisciente", "Completa el Módulo 6", R.id.tv_medal_mod6) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return AppState.getInstance().isModuleComplete(6); }
    },

    // ── MEDALLA MAESTRA (100% LOGROS) ──
    PERFECCION_ABSOLUTA("Perfección Absoluta", "Consigue todas las medallas del juego", R.id.tv_medal_perfeccion) {
        @Override
        public boolean estaDesbloqueada(StatsUiState state) {
            // Evaluamos si todas las medallas anteriores están desbloqueadas
            for (Medalla m : values()) {
                if (m != PERFECCION_ABSOLUTA && !m.estaDesbloqueada(state)) {
                    return false;
                }
            }
            return true;
        }
    };

    // atributos
    private final String titulo;
    private final String descripcion;
    private final int viewId;

    /**
     * Constructor base de inyección de metadatos para la inicialización formal del Enum.
     */
    Medalla(String titulo, String descripcion, int viewId) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.viewId = viewId;
    }

    // getters
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public int getViewId() { return viewId; }

    /**
     * Contrato analítico abstracto sobreescrito por cada logro para diagnosticar el estado
     * permanente de su desbloqueo según los datos vigentes de la sesión y el repositorio.
     *
     * @param state Foto instantánea reactiva del progreso global {@link StatsUiState}.
     * @return {@code true} si se cumplen los requisitos del logro; {@code false} en caso contrario.
     */
    public abstract boolean estaDesbloqueada(StatsUiState state);
}