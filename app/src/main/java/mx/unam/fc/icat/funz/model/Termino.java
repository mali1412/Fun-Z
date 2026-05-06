package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.UUID;

/**
 * Termino — Entidad que representa un elemento atómico de una ecuación.
 *
 * Cada término tiene:
 *   - Un identificador único {@link #id} para rastrear el elemento durante
 *     operaciones de Drag & Drop sin depender de su posición en la lista.
 *   - Un {@link TipoTermino} que clasifica el elemento semánticamente.
 *   - Un símbolo de visualización ({@link #simbolo}) listo para mostrar en la UI.
 *   - Datos numéricos ({@link #coeficiente}, {@link #valor}) para la lógica algebraica.
 *
 * <h3>Tipos de término</h3>
 * <ul>
 *   <li>{@link TipoTermino#VARIABLE}  — p. ej. "x", "2x", "-x"</li>
 *   <li>{@link TipoTermino#CONSTANTE} — p. ej. "5", "-3", "+7"</li>
 *   <li>{@link TipoTermino#OPERADOR}  — p. ej. "+", "-", "×", "÷"</li>
 *   <li>{@link TipoTermino#IGUAL}     — el signo "="</li>
 * </ul>
 *
 * <h3>Uso con Drag & Drop</h3>
 * Al iniciar un drag, codifica el {@link #id} del término en el ClipData.
 * Al soltarlo en un destino, el ViewModel localiza el término por su id
 * con {@code ecuacion.buscarPorId(id)} e invoca la operación correspondiente.
 */
public class Termino {

    // ════════════════════════════════════════════════════════════════════════
    //  Enum de tipo de término
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Clasificación semántica de un término dentro de la ecuación.
     */
    public enum TipoTermino {
        VARIABLE, CONSTANTE, OPERADOR, IGUAL
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Campos de instancia
    // ════════════════════════════════════════════════════════════════════════

    /** Identificador único inmutable; sirve como etiqueta en Drag & Drop. */
    @NonNull private final String id;

    /** Clasificación semántica. */
    @NonNull private TipoTermino tipo;

    /**
     * Símbolo de visualización tal como debe aparecer en pantalla.
     * Ejemplos: "x", "2x", "-x", "+5", "-3", "+", "=".
     */
    @NonNull private String simbolo;

    /**
     * Coeficiente numérico para términos de tipo {@link TipoTermino#VARIABLE}.
     * Ejemplos: 1 para "x", 2 para "2x", -1 para "-x".
     * Para otros tipos el valor carece de significado.
     */
    private int coeficiente;

    /**
     * Valor numérico para términos de tipo {@link TipoTermino#CONSTANTE}.
     * Puede ser negativo.
     */
    private int valor;

    /**
     * Indica si el término es positivo ({@code true}) o negativo ({@code false}).
     * Aplica a VARIABLE y CONSTANTE.
     */
    private boolean positivo;

    // ════════════════════════════════════════════════════════════════════════
    //  Constructor privado — usa los métodos de fábrica
    // ════════════════════════════════════════════════════════════════════════

    private Termino(@NonNull TipoTermino tipo, @NonNull String simbolo, int coeficiente, int valor, boolean positivo) {
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.simbolo = simbolo;
        this.coeficiente = coeficiente;
        this.valor = valor;
        this.positivo = positivo;
    }

    // Constructor para reconstrucción desde DB
    private Termino(@NonNull String id, @NonNull TipoTermino tipo, @NonNull String simbolo, int coeficiente, int valor, boolean positivo) {
        this.id = id;
        this.tipo = tipo;
        this.simbolo = simbolo;
        this.coeficiente = coeficiente;
        this.valor = valor;
        this.positivo = positivo;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Métodos de fábrica (Factory Methods)
    // ════════════════════════════════════════════════════════════════════════

    public static Termino reconstruir(String id, TipoTermino tipo, String simbolo, int coeficiente, int valor, boolean positivo) {
        return new Termino(id, tipo, simbolo, coeficiente, valor, positivo);
    }

    /**
     * Crea una variable con coeficiente entero.
     *
     * @param coeficiente Número que multiplica a x; puede ser negativo.
     * @return Término de tipo VARIABLE.
     * <pre>
     *   crearVariable(1)  → "x"
     *   crearVariable(2)  → "2x"
     *   crearVariable(-1) → "-x"
     *   crearVariable(-3) → "-3x"
     * </pre>
     */
    public static Termino crearVariable(int coeficiente) {
        String simbolo;
        if (coeficiente == 1) simbolo = "x";
        else if (coeficiente == -1) simbolo = "-x";
        else simbolo = coeficiente + "x";
        return new Termino(TipoTermino.VARIABLE, simbolo, coeficiente, 0, coeficiente >= 0);
    }

    /**
     * Crea una constante entera.
     *
     * @param valor Valor numérico (positivo o negativo).
     * @return Término de tipo CONSTANTE.
     * <pre>
     *   crearConstante(5)  → "+5"
     *   crearConstante(-3) → "-3"
     * </pre>
     */
    public static Termino crearConstante(int valor) {
        String simbolo = valor >= 0 ? "+" + valor : String.valueOf(valor);
        return new Termino(TipoTermino.CONSTANTE, simbolo, 0, valor, valor >= 0);
    }

    /**
     * Crea un operador aritmético.
     *
     * @param operador Símbolo del operador: "+", "-", "×", "÷".
     * @return Término de tipo OPERADOR.
     */
    public static Termino crearOperador(@NonNull String operador) {
        return new Termino(TipoTermino.OPERADOR, operador, 0, 0, true);
    }

    /**
     * Crea el signo de igualdad "=".
     *
     * @return Término de tipo IGUAL.
     */
    public static Termino crearIgual() {
        return new Termino(TipoTermino.IGUAL, "=", 0, 0, true);
    }

    /**
     * Crea una copia profunda del término con un nuevo {@link #id} único.
     * Útil cuando se necesita duplicar fichas en la UI sin aliasing.
     *
     * @return Nuevo término con los mismos datos pero distinto id.
     */
    public Termino copiar() {
        return new Termino(this.tipo, this.simbolo, this.coeficiente, this.valor, this.positivo);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Consultas semánticas de conveniencia
    // ════════════════════════════════════════════════════════════════════════

    /** {@code true} si el término es una variable (incógnita). */
    public boolean esVariable()  { return tipo == TipoTermino.VARIABLE; }

    /** {@code true} si el término es una constante numérica. */
    public boolean esConstante() { return tipo == TipoTermino.CONSTANTE; }

    /** {@code true} si el término es un operador aritmético. */
    public boolean esOperador()  { return tipo == TipoTermino.OPERADOR; }

    /** {@code true} si el término es el signo "=". */
    public boolean esIgual()     { return tipo == TipoTermino.IGUAL; }

    /**
     * Indica si este término puede cancelarse con {@code otro}.
     * Dos términos se cancelan (par cero) cuando son del mismo tipo,
     * tienen el mismo coeficiente/valor en valor absoluto y signos opuestos.
     *
     * @param otro Término candidato a cancelar.
     * @return {@code true} si forman un par cero.
     */
    public boolean cancelaCon(@NonNull Termino otro) {
        if (this.tipo != otro.tipo) return false;
        if (this.positivo == otro.positivo) return false;
        if (esVariable()) return Math.abs(this.coeficiente) == Math.abs(otro.coeficiente);
        if (esConstante()) return Math.abs(this.valor) == Math.abs(otro.valor);
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Getters
    // ════════════════════════════════════════════════════════════════════════

    @NonNull public String getId() { return id; }
    @NonNull public TipoTermino getTipo() { return tipo; }
    @NonNull public String getSimbolo() { return simbolo; }
    public int getCoeficiente() { return coeficiente; }
    public int getValor() { return valor; }
    public boolean isPositivo() { return positivo; }

    // ════════════════════════════════════════════════════════════════════════
    //  Setters (solo para manipulaciones algebraicas controladas)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Cambia el coeficiente de una variable y actualiza el símbolo.
     * Solo aplicable a términos de tipo {@link TipoTermino#VARIABLE}.
     *
     * @param nuevoCoef Nuevo coeficiente.
     * @throws IllegalStateException si el término no es VARIABLE.
     */
    public void setCoeficiente(int nuevoCoef) {
        if (tipo != TipoTermino.VARIABLE) throw new IllegalStateException("Solo VARIABLE");
        this.coeficiente = nuevoCoef;
        this.positivo = nuevoCoef >= 0;
        if (nuevoCoef == 1) this.simbolo = "x";
        else if (nuevoCoef == -1) this.simbolo = "-x";
        else this.simbolo = nuevoCoef + "x";
    }

    /**
     * Cambia el valor de una constante y actualiza el símbolo.
     * Solo aplicable a términos de tipo {@link TipoTermino#CONSTANTE}.
     *
     * @param nuevoValor Nuevo valor numérico.
     * @throws IllegalStateException si el término no es CONSTANTE.
     */
    public void setValor(int nuevoValor) {
        if (tipo != TipoTermino.CONSTANTE) throw new IllegalStateException("Solo CONSTANTE");
        this.valor = nuevoValor;
        this.positivo = nuevoValor >= 0;
        this.simbolo = nuevoValor >= 0 ? "+" + nuevoValor : String.valueOf(nuevoValor);
    }
}
