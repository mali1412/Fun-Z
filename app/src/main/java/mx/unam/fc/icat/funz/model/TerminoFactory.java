package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.UUID;

import mx.unam.fc.icat.funz.db.Converters;

/**
 * Fábrica estática especializada encargada de la creación, inicialización y reconstrucción
 * controlada de instancias de tipo {@link Termino}.
 * <p>
 * Abstrae y encapsula la complejidad asociada al cálculo inicial de símbolos textuales algebraicos,
 * el aprovisionamiento de identificadores únicos universales (UUID) y el control de signos binarios.
 * Actúa como el único punto de instanciación formal para mitigar el acoplamiento directo dentro del motor.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class TerminoFactory {

    /**
     * Construye una variable unaria lineal provista de un identificador único, formateando su
     * símbolo e inicializando sus coeficientes y denominadores.
     *
     * @param coeficiente Multiplicador entero de la incógnita (ej. 2 para "2x", -1 para "-x").
     * @param divisor     Denominador fraccionario entero (debe ser mayor a cero).
     * @return Una instancia de {@link Termino} parametrizada como {@link Termino.TipoTermino#VARIABLE}.
     */
    public static Termino crearVariable(int coeficiente, int divisor) {
        String base = (coeficiente == 1) ? "x" : (coeficiente == -1) ? "-x" : coeficiente + "x";
        String simbolo = (divisor > 1) ? base + "/" + divisor : base;
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.VARIABLE, simbolo, coeficiente, 0, divisor, coeficiente >= 0);
    }

    /**
     * Construye una cantidad constante independiente asignando su valor escalar y procesando
     * su representación fraccionaria.
     *
     * @param valor   Magnitud escalar entera independiente (positiva o negativa).
     * @param divisor Denominador fraccionario entero (debe ser mayor a cero).
     * @return Una instancia de {@link Termino} parametrizada como {@link Termino.TipoTermino#CONSTANTE}.
     */
    public static Termino crearConstante(int valor, int divisor) {
        String base = valor >= 0 ? "+" + valor : String.valueOf(valor);
        String simbolo = (divisor > 1) ? base + "/" + divisor : base;
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.CONSTANTE, simbolo, 0, valor, divisor, valor >= 0);
    }

    /**
     * Construye un término de tipo operador aritmético binario para la sintaxis de la expresión.
     *
     * @param op Cadena de texto representativa del operador (ej. "+", "-", "*", "/"). No debe ser nula.
     * @return Una instancia de {@link Termino} parametrizada como {@link Termino.TipoTermino#OPERADOR}.
     */
    public static Termino crearOperador(@NonNull String op) {
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.OPERADOR, op, 0, 0, 1, true);
    }

    /**
     * Construye el operador relacional de igualdad de la ecuación.
     *
     * @return Una instancia única de {@link Termino} parametrizada como {@link Termino.TipoTermino#IGUAL} con símbolo "=".
     */
    public static Termino crearIgual() {
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.IGUAL, "=", 0, 0, 1, true);
    }

    /**
     * Construye un término delimitador de agrupación (paréntesis), discriminando su orientación de apertura o cierre.
     *
     * @param abre {@code true} si representa un paréntesis de apertura '('; {@code false} para uno de cierre ')'.
     * @return Una instancia de {@link Termino} parametrizada según corresponda.
     */
    public static Termino crearParentesis(boolean abre) {
        Termino.TipoTermino tipo = abre ? Termino.TipoTermino.PARENTESIS_ABRE : Termino.TipoTermino.PARENTESIS_CIERRA;
        String simbolo = abre ? "(" : ")";
        return new Termino(UUID.randomUUID().toString(), tipo, simbolo, 0, 0, 1, true);
    }

    /**
     * Construye un término que representa el operador de potencia o potenciación exponencial.
     *
     * @return Una instancia de {@link Termino} parametrizada como {@link Termino.TipoTermino#POTENCIA} con símbolo "^".
     */
    public static Termino crearPotencia() {
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.POTENCIA, "^", 0, 0, 1, true);
    }

    /**
     * Restaura y clona un término algebraico de manera exacta inyectando un identificador histórico persistido.
     * <p>
     * <b>Nota Arquitectónica Crítica:</b> Este metodo es utilizado exclusivamente por los conversores
     * de tipos de Room Database ({@link Converters}) para inflar e instanciar los términos recuperados
     * de las consultas JSON de SQLite, evitando la alteración de los UUIDs que controlan la unicidad en las vistas.
     * </p>
     *
     * @param id          Identificador universal persistido original.
     * @param tipo        Clasificación taxonómica guardada.
     * @param simbolo     Símbolo matemático recuperado.
     * @param coeficiente Coeficiente unario recuperado.
     * @param valor       Valor de la constante recuperado.
     * @param divisor     Denominador fraccionario recuperado.
     * @param positivo    Signo booleano recuperado.
     * @return Instancia exacta reconstituida de {@link Termino}.
     */
    public static Termino reconstruir(String id, Termino.TipoTermino tipo, String simbolo, int coeficiente, int valor, int divisor, boolean positivo) {
        return new Termino(id, tipo, simbolo, coeficiente, valor, divisor, positivo);
    }
}