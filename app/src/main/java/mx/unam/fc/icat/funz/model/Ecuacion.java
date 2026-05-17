package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modelo estructural y contenedor secuencial que representa una ecuación lineal de primer grado.
 * <p>
 * Encapsula una colección ordenada de instancias de tipo {@link Termino}, actuando como el reflejo
 * abstracto de los platos de una balanza algebraica. Provee la API lógica para localizar el signo de igualdad
 * discriminador y segmentar dinámicamente los términos en sus respectivos miembros (miembro izquierdo y miembro derecho).
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class Ecuacion {

    /** Colección indexada de términos algebraicos y operadores que estructuran la ecuación completa. */
    private final List<Termino> terminos = new ArrayList<>();

    /**
     * Constructor predeterminado que inicializa una instancia de ecuación completamente vacía.
     */
    public Ecuacion() {}

    /**
     * Constructor por asignación masiva que puebla la estructura copiando los elementos de una colección externa.
     * Garantiza la inmutabilidad de la referencia interna original mediante copia profunda superficial.
     *
     * @param terminos Listado inicial de {@link Termino} que conformarán la ecuación. No debe ser nulo.
     */
    public Ecuacion(@NonNull List<Termino> terminos) {
        this.terminos.addAll(terminos);
    }

    /**
     * Recupera la lista completa y mutable de términos que integran la ecuación.
     * <p>
     *
     * @return Referencia directa a la lista interna estructurada de {@link Termino}.
     */
    public List<Termino> getTerminos() {
        return terminos;
    }

    /**
     * Filtra y extrae exclusivamente los términos localizados en el miembro izquierdo de la ecuación
     * (el elemento previo al signo de igualdad '=').
     * <p>
     * En caso de no detectarse un signo igual, devuelve la colección completa como medida de contingencia.
     * Retorna una nueva lista desacoplada para evitar mutaciones colaterales sobre la estructura permanente.
     * </p>
     *
     * @return Una sublista con los términos algebraicos del primer miembro.
     */
    public List<Termino> getLadoIzquierdo() {
        int idx = indiceIgual();
        return idx < 0 ? terminos : new ArrayList<>(terminos.subList(0, idx));
    }

    /**
     * Filtra y extrae exclusivamente los términos independientes o variables localizados en el miembro derecho
     * de la ecuación (el elemento posterior al signo de igualdad '=').
     * <p>
     * En caso de no detectarse el signo de igualdad o si este se sitúa en la frontera final de la expresión,
     * retorna una lista vacía inmutable segura mediante {@link Collections#emptyList()}.
     * </p>
     *
     * @return Una sublista con los términos algebraicos del segundo miembro.
     */
    public List<Termino> getLadoDerecho() {
        int idx = indiceIgual();
        return (idx < 0 || idx == terminos.size() - 1)
                ? Collections.emptyList()
                : new ArrayList<>(terminos.subList(idx + 1, terminos.size()));
    }

    /**
     * Ejecuta un barrido secuencial indexado sobre la colección para localizar la posición física exacta
     * del token que representa el signo de igualdad matemático.
     *
     * @return Índice numérico entero de la posición del signo igual en el rango de [0 a size()-1];
     * retorna {@code -1} si la expresión carece de un operador relacional de igualdad.
     */
    public int indiceIgual() {
        for (int i = 0; i < terminos.size(); i++) {
            if (terminos.get(i).esIgual()) return i;
        }
        return -1;
    }
}