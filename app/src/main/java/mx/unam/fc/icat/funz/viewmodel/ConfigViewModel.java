package mx.unam.fc.icat.funz.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import mx.unam.fc.icat.funz.data.AppState;

public class ConfigViewModel extends ViewModel {
    private final AppState state = AppState.getInstance();

    // LiveData para notificar a la vista cambios específicos
    private final MutableLiveData<Boolean> themeChanged = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> configSaved = new MutableLiveData<>(false);

    public String getUsername() { return state.getUsername(); }
    public boolean isDarkTheme() { return state.isDarkTheme(); }

    public LiveData<Boolean> getThemeChanged() { return themeChanged; }
    public LiveData<Boolean> getConfigSaved() { return configSaved; }

    public void saveConfiguration(String newName, boolean isDark) {
        boolean previousTheme = state.isDarkTheme();

        if (!newName.isEmpty()) state.setUsername(newName);
        state.setDarkTheme(isDark);

        configSaved.setValue(true);

        // Solo notificamos cambio de tema si realmente es distinto al anterior
        if (previousTheme != isDark) {
            themeChanged.setValue(true);
        }
    }
}