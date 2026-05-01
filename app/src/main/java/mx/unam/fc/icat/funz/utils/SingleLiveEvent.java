package mx.unam.fc.icat.funz.utils;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SingleLiveEvent — LiveData especializado para eventos de un solo disparo.
 *
 * A diferencia del LiveData normal, este no re-entrega el último valor
 * cuando el observador se re-suscribe (p.ej. tras una rotación de pantalla).
 * Úsalo para eventos como navegación, diálogos o Toasts.
 *
 * Basado en la arquitectura de referencia de Google Architecture Components.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(LifecycleOwner owner, Observer<? super T> observer) {
        // Solo notifica al observador si hay un evento pendiente
        super.observe(owner, t -> {
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@Nullable T value) {
        pending.set(true);
        super.setValue(value);
    }

    /** Dispara el evento con valor nulo (para eventos sin payload). */
    @MainThread
    public void call() {
        setValue(null);
    }
}
