package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.UUID;

/**
 * Termino — Entidad que representa un elemento atómico de una ecuación.
 */
public class Termino {

    public enum TipoTermino {
        VARIABLE, CONSTANTE, OPERADOR, IGUAL,
        POTENCIA, PARENTESIS_ABRE, PARENTESIS_CIERRA
    }

    @NonNull private final String id;
    @NonNull private TipoTermino tipo;
    @NonNull private String simbolo;
    private int coeficiente;
    private int valor;
    private boolean positivo;

    private Termino(@NonNull TipoTermino tipo, @NonNull String simbolo, int coeficiente, int valor, boolean positivo) {
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.simbolo = simbolo;
        this.coeficiente = coeficiente;
        this.valor = valor;
        this.positivo = positivo;
    }

    private Termino(@NonNull String id, @NonNull TipoTermino tipo, @NonNull String simbolo, int coeficiente, int valor, boolean positivo) {
        this.id = id;
        this.tipo = tipo;
        this.simbolo = simbolo;
        this.coeficiente = coeficiente;
        this.valor = valor;
        this.positivo = positivo;
    }

    public static Termino reconstruir(String id, TipoTermino tipo, String simbolo, int coeficiente, int valor, boolean positivo) {
        return new Termino(id, tipo, simbolo, coeficiente, valor, positivo);
    }

    public static Termino crearVariable(int coeficiente) {
        String simbolo = (coeficiente == 1) ? "x" : (coeficiente == -1) ? "-x" : coeficiente + "x";
        return new Termino(TipoTermino.VARIABLE, simbolo, coeficiente, 0, coeficiente >= 0);
    }

    public static Termino crearConstante(int valor) {
        String simbolo = valor >= 0 ? "+" + valor : String.valueOf(valor);
        return new Termino(TipoTermino.CONSTANTE, simbolo, 0, valor, valor >= 0);
    }

    public static Termino crearOperador(@NonNull String operador) {
        return new Termino(TipoTermino.OPERADOR, operador, 0, 0, true);
    }

    public static Termino crearIgual() {
        return new Termino(TipoTermino.IGUAL, "=", 0, 0, true);
    }

    public static Termino crearPotencia() {
        return new Termino(TipoTermino.POTENCIA, "^", 0, 0, true);
    }

    public static Termino crearParentesis(boolean abre) {
        TipoTermino tipo = abre ? TipoTermino.PARENTESIS_ABRE : TipoTermino.PARENTESIS_CIERRA;
        String simbolo = abre ? "(" : ")";
        return new Termino(tipo, simbolo, 0, 0, true);
    }

    /**
     * Indica si este término puede cancelarse con {@code otro}.
     * Dos términos se cancelan (par cero) cuando son del mismo tipo,
     * tienen el mismo valor absoluto y signos opuestos.
     */
    public boolean cancelaCon(@NonNull Termino otro) {
        if (this.tipo != otro.tipo) return false;
        if (this.positivo == otro.positivo) return false; // mismo signo no cancela
        if (esVariable()) return Math.abs(this.coeficiente) == Math.abs(otro.coeficiente);
        if (esConstante()) return Math.abs(this.valor) == Math.abs(otro.valor);
        return false;
    }

    public Termino copiar() {
        return new Termino(this.tipo, this.simbolo, this.coeficiente, this.valor, this.positivo);
    }

    public boolean esVariable()  { return tipo == TipoTermino.VARIABLE; }
    public boolean esConstante() { return tipo == TipoTermino.CONSTANTE; }
    public boolean esOperador()  { return tipo == TipoTermino.OPERADOR; }
    public boolean esIgual()     { return tipo == TipoTermino.IGUAL; }
    public boolean esPotencia()  { return tipo == TipoTermino.POTENCIA; }

    @NonNull public String getId() { return id; }
    @NonNull public TipoTermino getTipo() { return tipo; }
    @NonNull public String getSimbolo() { return simbolo; }
    public int getCoeficiente() { return coeficiente; }
    public int getValor() { return valor; }
    public boolean isPositivo() { return positivo; }

    public void setCoeficiente(int n) {
        if (tipo != TipoTermino.VARIABLE) throw new IllegalStateException("Solo VARIABLE");
        this.coeficiente = n;
        this.positivo = n >= 0;
        this.simbolo = (n == 1) ? "x" : (n == -1) ? "-x" : n + "x";
    }

    public void setValor(int n) {
        if (tipo != TipoTermino.CONSTANTE) throw new IllegalStateException("Solo CONSTANTE");
        this.valor = n;
        this.positivo = n >= 0;
        this.simbolo = n >= 0 ? "+" + n : String.valueOf(n);
    }
}
