package mx.unam.fc.icat.funz.model;

import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.viewmodel.EstadisticasViewModel.StatsUiState;
import mx.unam.fc.icat.funz.data.AppState;

public enum Medalla {
    PRIMER_PASO("Primer Paso", "Resuelve tu primer ejercicio clásico", R.id.tv_medal_1) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return state.exercisesResolved > 0; }
    },
    RACHA_BALDOR("Racha de Hierro", "Consigue una racha de 3 días", R.id.tv_medal_streak) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return state.streakDays >= 3; }
    },
    MAESTRO_ALGEBRA("Gran Maestro", "Alcanza el 50% de progreso global", R.id.tv_medal_mod) {
        @Override public boolean estaDesbloqueada(StatsUiState state) { return state.totalProgress >= 50; }
    },

    // ── NUEVAS MEDALLAS POR MÓDULO ──
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

    private final String titulo;
    private final String descripcion;
    private final int viewId;

    Medalla(String titulo, String descripcion, int viewId) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.viewId = viewId;
    }

    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public int getViewId() { return viewId; }

    public abstract boolean estaDesbloqueada(StatsUiState state);
}