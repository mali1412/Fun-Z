package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ecuacion — Modelo de dominio que representa una ecuación matemática de primer grado.
 *
 * <h3>Estructura interna</h3>
 * La ecuación se almacena como una {@link List} ordenada de {@link Termino}s.
 * El {@link Termino} de tipo {@link Termino.TipoTermino#IGUAL} divide la lista
 * en lado izquierdo (LHS) y derecho (RHS), como en una ecuación real:
 *
 * <pre>
 *   Ejemplo: "2x + 5 = 15"
 *   terminos = [ Var(2x), Op(+), Const(5), IGUAL, Const(15) ]
 *              |_________ LHS ___________|       |___ RHS ___|
 * </pre>
 *
 * <h3>Compatibilidad con Drag & Drop</h3>
 * Cada {@link Termino} lleva un {@code id} UUID único. El ViewModel codifica
 * ese id en el {@code ClipData} al iniciar el drag:
 * <pre>
 *   ClipData cd = ClipData.newPlainText("termino", termino.getId());
 * </pre>
 * Al recibir el drop, localiza el término con {@link #buscarPorId(String)}
 * y llama al método de manipulación correspondiente sin depender de índices
 * de posición (que pueden cambiar durante el reordenamiento visual).
 *
 * <h3>Inmutabilidad de la lista expuesta</h3>
 * Los métodos {@link #getTerminos()}, {@link #getLadoIzquierdo()} y
 * {@link #getLadoDerecho()} devuelven copias de solo lectura para evitar
 * modificaciones externas accidentales. Usa los métodos de mutación de esta
 * clase para alterar el estado de la ecuación.
 */
public class Ecuacion {

    // ════════════════════════════════════════════════════════════════════════
    //  Estado interno
    // ════════════════════════════════════════════════════════════════════════

    /** Lista maestra de términos en orden de izquierda a derecha. */
    private final List<Termino> terminos = new ArrayList<>();

    // ════════════════════════════════════════════════════════════════════════
    //  Constructores
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Crea una ecuación vacía.
     * Agrega los términos luego con {@link #agregarTermino(Termino)}.
     */
    public Ecuacion() {}

    /**
     * Crea una ecuación a partir de una lista de términos ya construida.
     *
     * @param terminos Lista ordenada que debe contener exactamente un IGUAL.
     * @throws IllegalArgumentException si la lista no tiene ningún término IGUAL.
     */
    public Ecuacion(@NonNull List<Termino> terminos) {
        this.terminos.addAll(terminos);
        validarEstructura();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Métodos de fábrica / parseo
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Parsea una cadena de texto simple con el formato "LHS = RHS" y construye
     * la {@link Ecuacion} correspondiente.
     *
     * <p>Convenciones del parser:
     * <ul>
     *   <li>Los tokens se separan por espacios.</li>
     *   <li>Un token numérico (p. ej. "5", "-3") → {@link Termino#crearConstante(int)}.</li>
     *   <li>Un token que termina en "x" → {@link Termino#crearVariable(int)}.</li>
     *   <li>Un token "+" o "-" solo → {@link Termino#crearOperador(String)}.</li>
     *   <li>Un token "=" → {@link Termino#crearIgual()}.</li>
     * </ul>
     *
     * <p>Ejemplos admitidos:
     * <pre>
     *   Ecuacion.parsear("x + 5 = 10")
     *   Ecuacion.parsear("2x - 3 = 7")
     *   Ecuacion.parsear("x = 4")
     * </pre>
     *
     * @param expresion Cadena de la ecuación.
     * @return Ecuación parseada.
     * @throws IllegalArgumentException si la cadena no contiene "=".
     */
    @NonNull
    public static Ecuacion parsear(@NonNull String expresion) {
        if (!expresion.contains("="))
            throw new IllegalArgumentException("La expresión debe contener '='");

        Ecuacion ec = new Ecuacion();
        String[] tokens = expresion.trim().split("\\s+");

        for (String token : tokens) {
            switch (token) {
                case "=":
                    ec.terminos.add(Termino.crearIgual());
                    break;
                case "+":
                case "-":
                    ec.terminos.add(Termino.crearOperador(token));
                    break;
                default:
                    if (token.endsWith("x")) {
                        // Variable: "x", "2x", "-x", "-3x"
                        String coefStr = token.replace("x", "");
                        int coef;
                        if (coefStr.isEmpty() || coefStr.equals("+"))  coef = 1;
                        else if (coefStr.equals("-"))                   coef = -1;
                        else                                            coef = Integer.parseInt(coefStr);
                        ec.terminos.add(Termino.crearVariable(coef));
                    } else {
                        // Constante numérica
                        ec.terminos.add(Termino.crearConstante(Integer.parseInt(token)));
                    }
                    break;
            }
        }
        ec.validarEstructura();
        return ec;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Consultas de estructura
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Devuelve una vista de solo lectura de la lista completa de términos,
     * en el orden en que aparecen en la ecuación.
     */
    @NonNull
    public List<Termino> getTerminos() {
        return Collections.unmodifiableList(terminos);
    }

    /**
     * Devuelve los términos del lado izquierdo (antes del {@code =}).
     *
     * @return Lista inmutable; vacía si no hay IGUAL aún.
     */
    @NonNull
    public List<Termino> getLadoIzquierdo() {
        int idx = indiceIgual();
        if (idx < 0) return Collections.unmodifiableList(terminos);
        return Collections.unmodifiableList(new ArrayList<>(terminos.subList(0, idx)));
    }

    /**
     * Devuelve los términos del lado derecho (después del {@code =}).
     *
     * @return Lista inmutable; vacía si no hay IGUAL aún.
     */
    @NonNull
    public List<Termino> getLadoDerecho() {
        int idx = indiceIgual();
        if (idx < 0 || idx == terminos.size() - 1)
            return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(terminos.subList(idx + 1, terminos.size())));
    }

    /**
     * Busca un término por su {@code id} UUID en toda la ecuación.
     *
     * @param id Identificador único del término.
     * @return El {@link Termino} encontrado, o {@code null} si no existe.
     */
    @Nullable
    public Termino buscarPorId(@NonNull String id) {
        for (Termino t : terminos) {
            if (t.getId().equals(id)) return t;
        }
        return null;
    }

    /**
     * Indica si el término con el {@code id} dado está en el lado izquierdo.
     *
     * @param id Id del término a consultar.
     * @return {@code true} si está en LHS; {@code false} si está en RHS o no existe.
     */
    public boolean estaEnLadoIzquierdo(@NonNull String id) {
        int igual = indiceIgual();
        if (igual < 0) return false;
        for (int i = 0; i < igual; i++) {
            if (terminos.get(i).getId().equals(id)) return true;
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Mutaciones (lógica algebraica)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Agrega un nuevo término al final del lado derecho de la ecuación.
     * Si la ecuación aún no tiene IGUAL, lo agrega al final general.
     *
     * @param termino Término a agregar.
     */
    public void agregarTermino(@NonNull Termino termino) {
        terminos.add(termino);
    }

    /**
     * Mueve un término al lado contrario de la ecuación (transposición).
     * Al cruzar el "=", el signo del término se invierte automáticamente
     * para mantener la equivalencia algebraica.
     *
     * <p>Si el término ya está aislado o es un IGUAL/OPERADOR, la operación
     * no tiene efecto.
     *
     * @param terminoId Id del término a mover.
     * @return {@code true} si el movimiento se realizó; {@code false} en caso contrario.
     */
    public boolean moverTermino(@NonNull String terminoId) {
        Termino t = buscarPorId(terminoId);
        if (t == null || t.esOperador() || t.esIgual()) return false;

        boolean eraIzquierdo = estaEnLadoIzquierdo(terminoId);
        terminos.remove(t);
        invertirSigno(t);          // transposición: cambia el signo al cruzar el "="

        int igual = indiceIgual();
        if (eraIzquierdo) {
            // Pasar al lado derecho: al final de la lista
            terminos.add(t);
        } else {
            // Pasar al lado izquierdo: justo antes del IGUAL
            terminos.add(igual, t);
        }
        limpiarOperadoresRedundantes();
        return true;
    }

    /**
     * Cancela un par cero: elimina el término indicado y su opuesto en el mismo lado.
     * Útil para la mecánica de Algebra Tiles donde "+1" y "-1" se anulan.
     *
     * @param terminoId Id del primer término del par.
     * @return {@code true} si se encontró y eliminó el par; {@code false} si no.
     */
    public boolean cancelarParCero(@NonNull String terminoId) {
        Termino t = buscarPorId(terminoId);
        if (t == null) return false;

        boolean esIzq = estaEnLadoIzquierdo(terminoId);
        List<Termino> ladoActual = esIzq ? getLadoIzquierdo() : getLadoDerecho();

        // Buscar el primer término que cancele con t en el mismo lado
        for (Termino otro : ladoActual) {
            if (!otro.getId().equals(t.getId()) && t.cancelaCon(otro)) {
                terminos.remove(t);
                terminos.remove(otro);
                limpiarOperadoresRedundantes();
                return true;
            }
        }
        return false;
    }

    /**
     * Reordena la posición de un término dentro de su lado mediante un índice
     * de destino relativo al lado (0 = primero del lado).
     * Pensado para el reordenamiento visual por drag dentro del mismo contenedor.
     *
     * @param terminoId   Id del término a reubicar.
     * @param idxDestino  Posición destino relativa al lado (LHS o RHS).
     * @return {@code true} si el reordenamiento se realizó.
     */
    public boolean reordenarEnMismoLado(@NonNull String terminoId, int idxDestino) {
        Termino t = buscarPorId(terminoId);
        if (t == null || t.esIgual()) return false;

        boolean esIzq = estaEnLadoIzquierdo(terminoId);
        int igualIdx  = indiceIgual();

        // Rango del lado en la lista maestra
        int base  = esIzq ? 0 : igualIdx + 1;
        int limit = esIzq ? igualIdx : terminos.size();
        int size  = limit - base;

        if (idxDestino < 0 || idxDestino >= size) return false;

        terminos.remove(t);
        terminos.add(base + idxDestino, t);
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Estado de resolución
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Indica si la variable x está aislada en el lado izquierdo.
     * Condición: LHS contiene exactamente un término de tipo VARIABLE con coeficiente 1
     * y ningún término CONSTANTE.
     *
     * @return {@code true} si la ecuación está en la forma "x = valor".
     */
    public boolean xEstaAislada() {
        List<Termino> lhs = getLadoIzquierdo();
        // Filtrar solo variables y constantes (excluir operadores)
        long variables  = lhs.stream().filter(Termino::esVariable).count();
        long constantes = lhs.stream().filter(Termino::esConstante).count();
        if (variables != 1 || constantes != 0) return false;
        Termino var = lhs.stream().filter(Termino::esVariable).findFirst().orElse(null);
        return var != null && var.getCoeficiente() == 1;
    }

    /**
     * Calcula el valor numérico del lado derecho sumando todas las constantes.
     * Solo tiene sentido cuando {@link #xEstaAislada()} es {@code true}.
     *
     * @return Suma de las constantes del RHS; 0 si no hay constantes.
     */
    public int valorRHS() {
        return getLadoDerecho().stream()
                .filter(Termino::esConstante)
                .mapToInt(Termino::getValor)
                .sum();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Representación textual
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Genera la representación textual de la ecuación tal como se mostraría
     * al usuario, concatenando los símbolos de cada término separados por espacio.
     *
     * @return Cadena legible, p. ej. "2x + 5 = 15".
     */
    @NonNull
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < terminos.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(terminos.get(i).getSimbolo());
        }
        return sb.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return "Ecuacion{" + toDisplayString() + ", términos=" + terminos.size() + "}";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers privados
    // ════════════════════════════════════════════════════════════════════════

    /** Devuelve el índice del término IGUAL en la lista; -1 si no existe. */
    private int indiceIgual() {
        for (int i = 0; i < terminos.size(); i++) {
            if (terminos.get(i).esIgual()) return i;
        }
        return -1;
    }

    /**
     * Invierte el signo de un término VARIABLE o CONSTANTE
     * (operación de transposición algebraica).
     */
    private static void invertirSigno(@NonNull Termino t) {
        if (t.esVariable())  t.setCoeficiente(-t.getCoeficiente());
        if (t.esConstante()) t.setValor(-t.getValor());
    }

    /**
     * Elimina operadores sueltos (p. ej. un "+" al inicio de un lado o
     * dos operadores consecutivos) que pueden quedar tras mover términos.
     */
    private void limpiarOperadoresRedundantes() {
        int igual = indiceIgual();
        if (igual < 0) return;

        // Primer token del LHS no debe ser un operador suelto "+"
        if (!terminos.isEmpty() && terminos.get(0).esOperador()
                && terminos.get(0).getSimbolo().equals("+")) {
            terminos.remove(0);
        }

        // Primer token del RHS (si existe) no debe ser un operador suelto "+"
        igual = indiceIgual(); // recalcular tras posible eliminación
        if (igual >= 0 && igual + 1 < terminos.size()
                && terminos.get(igual + 1).esOperador()
                && terminos.get(igual + 1).getSimbolo().equals("+")) {
            terminos.remove(igual + 1);
        }
    }

    /** Lanza excepción si la lista de términos no contiene exactamente un IGUAL. */
    private void validarEstructura() {
        long iguales = terminos.stream().filter(Termino::esIgual).count();
        if (iguales == 0)
            throw new IllegalArgumentException("Una ecuación debe tener exactamente un '='");
        if (iguales > 1)
            throw new IllegalArgumentException("Una ecuación no puede tener más de un '='");
    }
}

