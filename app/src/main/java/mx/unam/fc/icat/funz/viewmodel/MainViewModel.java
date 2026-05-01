package mx.unam.fc.icat.funz.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import mx.unam.fc.icat.funz.data.AppState;

/**
 * MainViewModel — ViewModel para la pantalla A (Inicio).
 *
 * Expone como LiveData todos los campos que la pantalla Home necesita
 * mostrar: saludo, racha, badge/método/ecuación de la card Continuar
 * y el progreso del Módulo 1.
 *
 * La Activity llama a refreshUiState() en onResume para que el ViewModel
 * consulte AppState y actualice los LiveData; la Activity solo hace setText.
 *
 * También provee la lógica de "¿a qué pantalla navegar?" al pulsar
 * Continuar, eliminando el switch-case de la Activity.
 */
public class MainViewModel extends ViewModel {

    private final AppState state = AppState.getInstance();

    // ── LiveData ──────────────────────────────────────────────────────────────

    private final MutableLiveData<String>  _welcomeText    = new MutableLiveData<>();
    public  final LiveData<String>          welcomeText     = _welcomeText;

    private final MutableLiveData<String>  _streakText     = new MutableLiveData<>();
    public  final LiveData<String>          streakText      = _streakText;

    private final MutableLiveData<String>  _resumeBadge    = new MutableLiveData<>();
    public  final LiveData<String>          resumeBadge     = _resumeBadge;

    private final MutableLiveData<String>  _resumeMethod   = new MutableLiveData<>();
    public  final LiveData<String>          resumeMethod    = _resumeMethod;

    private final MutableLiveData<String>  _resumeEquation = new MutableLiveData<>();
    public  final LiveData<String>          resumeEquation  = _resumeEquation;

    private final MutableLiveData<Integer> _mod1Progress   = new MutableLiveData<>(0);
    public  final LiveData<Integer>         mod1Progress    = _mod1Progress;

    // ════════════════════════════════════════════════════════════════════════
    //  Refresco del estado de UI
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Sincroniza todos los LiveData con el estado actual de AppState.
     * La Activity llama a este método en onResume.
     */
    public void refreshUiState() {
        _welcomeText.setValue("¡Hola, " + state.getUsername() + "!");
        _streakText.setValue("🔥 Racha: " + state.getStreakDays() + " días");
        _resumeBadge.setValue(state.getResumeBadge());
        _resumeMethod.setValue(state.getResumeMethod());
        _resumeEquation.setValue(state.getResumeEquation());
        _mod1Progress.setValue(state.getMod1Progress());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Lógica de navegación
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Indica qué pantalla de ejercicio debe abrirse al pulsar "Continuar".
     * Devuelve el destino como un valor del enum Destino para que la Activity
     * construya el Intent sin necesidad de interpretar lógica de negocio.
     */
    public Destino getResumeDestino() {
        if (state.isMod1Complete()) return Destino.TEMAS;
        state.resetSession();
        switch (state.getCurrentExStep()) {
            case 2:  return Destino.EJERCICIO_CLASICO;
            case 3:  return Destino.EJERCICIO_TILES;
            default: return Destino.EJERCICIO_BALANZA;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Enum de destinos de navegación
    // ════════════════════════════════════════════════════════════════════════

    public enum Destino {
        EJERCICIO_BALANZA,
        EJERCICIO_CLASICO,
        EJERCICIO_TILES,
        TEMAS
    }
}
