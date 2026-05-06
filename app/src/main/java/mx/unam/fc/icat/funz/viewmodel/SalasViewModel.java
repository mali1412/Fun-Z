package mx.unam.fc.icat.funz.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import mx.unam.fc.icat.funz.data.AppState;

public class SalasViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isDarkTheme = new MutableLiveData<>();
    private final AppState state = AppState.getInstance();

    public LiveData<Boolean> getIsDarkTheme() {
        return isDarkTheme;
    }

    public void checkThemeStatus() {
        // Notificamos si el tema ha cambiado en el AppState
        isDarkTheme.setValue(state.isDarkTheme());
    }
}