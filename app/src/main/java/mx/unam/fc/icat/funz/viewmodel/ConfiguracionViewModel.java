package mx.unam.fc.icat.funz.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import mx.unam.fc.icat.funz.data.AppState;
import mx.unam.fc.icat.funz.utils.SingleLiveEvent;

/**
 * ConfiguracionViewModel — ViewModel para la pantalla N (Configuración).
 *
 * Responsabilidades:
 *   - Exponer los valores actuales de username y darkTheme para pre-cargar
 *     los campos de la pantalla.
 *   - Procesar y validar el guardado de la configuración.
 *   - Emitir un evento de un solo disparo que indica si la Activity
 *     debe recrearse (porque el tema cambió) o solo mostrar un Toast.
 */
public class ConfiguracionViewModel extends ViewModel {

    private final AppState state = AppState.getInstance();

    // ── LiveData de valores actuales (para pre-cargar la UI) ──────────────────

    private final MutableLiveData<String>  _currentUsername  = new MutableLiveData<>();
    public  final LiveData<String>          currentUsername   = _currentUsername;

    private final MutableLiveData<Boolean> _currentDarkTheme = new MutableLiveData<>();
    public  final LiveData<Boolean>         currentDarkTheme  = _currentDarkTheme;

    // ── Eventos de un solo disparo ────────────────────────────────────────────

    /**
     * Emite el resultado del guardado.
     * SaveResult.THEME_CHANGED → la Activity debe llamar a recreate().
     * SaveResult.SAVED         → solo mostrar Toast de confirmación.
     */
    private final SingleLiveEvent<SaveResult> _saveEvent = new SingleLiveEvent<>();
    public  final LiveData<SaveResult>         saveEvent  = _saveEvent;

    // ════════════════════════════════════════════════════════════════════════
    //  Carga inicial
    // ════════════════════════════════════════════════════════════════════════

    /** Carga los valores actuales de AppState para pre-llenar la pantalla. */
    public void loadCurrentConfig() {
        _currentUsername.setValue(state.getUsername());
        _currentDarkTheme.setValue(state.isDarkTheme());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Guardado de configuración
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Persiste los cambios de configuración en AppState y emite el evento
     * apropiado para que la Activity reaccione.
     *
     * @param newUsername Nombre introducido por el usuario (puede estar vacío)
     * @param newDark     true si el usuario seleccionó tema oscuro
     */
    public void saveConfig(String newUsername, boolean newDark) {
        // Actualizar nombre solo si no está vacío
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            state.setUsername(newUsername.trim());
        }

        boolean themeChanged = newDark != state.isDarkTheme();
        state.setDarkTheme(newDark);

        _saveEvent.setValue(themeChanged ? SaveResult.THEME_CHANGED : SaveResult.SAVED);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Enum de resultado del guardado
    // ════════════════════════════════════════════════════════════════════════

    public enum SaveResult {
        /** Se guardó correctamente y no hay cambio de tema; solo mostrar Toast. */
        SAVED,
        /** El tema cambió; la Activity debe llamar a recreate(). */
        THEME_CHANGED
    }
}
