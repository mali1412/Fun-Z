package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ecuacion — Representación estructural de una ecuación de primer grado.
 */
public class Ecuacion {
    private final List<Termino> terminos = new ArrayList<>();

    public Ecuacion() {}

    public Ecuacion(@NonNull List<Termino> terminos) {
        this.terminos.addAll(terminos);
    }

    public List<Termino> getTerminos() {
        return terminos;
    }

    public List<Termino> getLadoIzquierdo() {
        int idx = indiceIgual();
        return idx < 0 ? terminos : new ArrayList<>(terminos.subList(0, idx));
    }

    public List<Termino> getLadoDerecho() {
        int idx = indiceIgual();
        return (idx < 0 || idx == terminos.size() - 1)
                ? Collections.emptyList()
                : new ArrayList<>(terminos.subList(idx + 1, terminos.size()));
    }

    public int indiceIgual() {
        for (int i = 0; i < terminos.size(); i++) {
            if (terminos.get(i).esIgual()) return i;
        }
        return -1;
    }
}