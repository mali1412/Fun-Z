package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.UUID;

/**
 * Clase atómica y modelo de datos que representa una entidad algebraica discreta dentro de una ecuación.
 * <p>
 * Modela variables lineales (incógnitas con coeficientes), constantes numéricas independientes,
 * operadores relacionales o signos de agrupación. Controla de forma encapsulada sus propiedades aritméticas
 * (valores, coeficientes y denominadores fraccionarios) y computa dinámicamente su representación textual
 * sanitizada (símbolo) cada vez que ocurre una mutación de estado.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class Termino {

    /**
     * Categorías válidas para discriminar la naturaleza y comportamiento del término algebraico.
     */
    public enum TipoTermino {
        VARIABLE, CONSTANTE, OPERADOR, IGUAL,
        POTENCIA, PARENTESIS_ABRE, PARENTESIS_CIERRA
    }

    @NonNull private final String id;
    @NonNull private TipoTermino tipo;
    @NonNull private String simbolo;
    private int coeficiente;
    private int valor;
    private int divisor;
    private boolean positivo;

    /**
     * Constructor con alcance de paquete (package-private). Delega la instanciación formal e inyección
     * de identificadores únicos atómicos a la fábrica especializada {@link TerminoFactory}.
     *
     * @param id          Identificador alfanumérico único (UUID). No debe ser nulo.
     * @param tipo        Clasificación taxonómica del término. No debe ser nulo.
     * @param simbolo     Cadena de texto representativa del término. No debe ser nulo.
     * @param coeficiente Multiplicador entero asignado a las variables unarias.
     * @param valor       Magnitud escalar entera asignada a las constantes independientes.
     * @param divisor     Denominador entero de la expresión (debe ser mayor a cero).
     * @param positivo    Bandera lógica de control de signo aritmético.
     */
    Termino(@NonNull String id, @NonNull TipoTermino tipo, @NonNull String simbolo, int coeficiente, int valor, int divisor, boolean positivo) {
        this.id = id;
        this.tipo = tipo;
        this.simbolo = simbolo;
        this.coeficiente = coeficiente;
        this.valor = valor;
        this.divisor = divisor;
        this.positivo = positivo;
    }

    // Getters

    @NonNull public String getId() { return id; }
    @NonNull public TipoTermino getTipo() { return tipo; }
    @NonNull public String getSimbolo() { return simbolo; }
    public int getCoeficiente() { return coeficiente; }
    public int getValor() { return valor; }
    public int getDivisor() { return divisor; }

    /** @return {@code true} si el término es algebraicamente mayor o igual a cero; {@code false} si es negativo. */
    public boolean isPositivo() { return positivo; }

    // Setters

    public void setCoeficiente(int n) {
        this.coeficiente = n;
        this.positivo = n >= 0;
        actualizarSimbolo();
    }

    public void setValor(int n) {
        this.valor = n;
        this.positivo = n >= 0;
        actualizarSimbolo();
    }

    public void setDivisor(int d) {
        this.divisor = (d <= 0) ? 1 : d;
        actualizarSimbolo();
    }

    /**
     * Recomputa dinámicamente la cadena de texto matemática que representa al término, abstrayendo
     * coeficientes unitarios implícitos (transformando "1x" a "x") y anexando denominadores fraccionarios.
     */
    private void actualizarSimbolo() {
        if (tipo == TipoTermino.VARIABLE) {
            String base = (coeficiente == 1) ? "x" : (coeficiente == -1) ? "-x" : coeficiente + "x";
            this.simbolo = (divisor > 1) ? base + "/" + divisor : base;
        } else if (tipo == TipoTermino.CONSTANTE) {
            String base = valor >= 0 ? "+" + valor : String.valueOf(valor);
            this.simbolo = (divisor > 1) ? base + "/" + divisor : base;
        }
    }

    /** @return {@code true} si el término es una incógnita variable lineal; {@code false} en caso contrario. */
    public boolean esVariable()  { return tipo == TipoTermino.VARIABLE; }

    /** @return {@code true} si el término es un escalar numérico independiente; {@code false} en caso contrario. */
    public boolean esConstante() { return tipo == TipoTermino.CONSTANTE; }

    /** @return {@code true} si el término representa el signo de igualdad relacional; {@code false} en caso contrario. */
    public boolean esIgual()     { return tipo == TipoTermino.IGUAL; }

    /**
     * Clona el estado numérico y estructural del término, encapsulándolo en una nueva instancia física
     * desvinculada en memoria RAM y provista de un identificador de unicidad UUID inédito.
     * <p>
     * <b>Principio de Seguridad Eficiente:</b> Previene colisiones referenciales de llaves primarias en los layouts
     * interactivos o colecciones de persistencia compartida.
     * </p>
     *
     * @return Una nueva instancia totalmente independiente de {@link Termino}.
     */
    public Termino copiar() {
        return new Termino(UUID.randomUUID().toString(), this.tipo, this.simbolo, this.coeficiente, this.valor, this.divisor, this.positivo);
    }
}