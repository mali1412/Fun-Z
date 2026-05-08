package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.UUID;

/**
 * Termino — Entidad que representa un elemento atómico de una ecuación.
 * Ahora incluye soporte para denominadores (divisiones).
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
    private int divisor = 1; // <--- Nuevo campo: por defecto es 1 (sin división)
    private boolean positivo;

    // Constructor privado actualizado
    private Termino(@NonNull TipoTermino tipo, @NonNull String simbolo, int coeficiente, int valor, int divisor, boolean positivo) {
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.simbolo = simbolo;
        this.coeficiente = coeficiente;
        this.valor = valor;
        this.divisor = divisor;
        this.positivo = positivo;
    }

    // Constructor para reconstrucción (Room/Base de datos)
    private Termino(@NonNull String id, @NonNull TipoTermino tipo, @NonNull String simbolo, int coeficiente, int valor, int divisor, boolean positivo) {
        this.id = id;
        this.tipo = tipo;
        this.simbolo = simbolo;
        this.coeficiente = coeficiente;
        this.valor = valor;
        this.divisor = divisor;
        this.positivo = positivo;
    }

    public static Termino reconstruir(String id, TipoTermino tipo, String simbolo, int coeficiente, int valor, int divisor, boolean positivo) {
        return new Termino(id, tipo, simbolo, coeficiente, valor, divisor, positivo);
    }

    // Sobrecarga para mantener compatibilidad con código antiguo
    public static Termino crearVariable(int coeficiente) {
        return crearVariable(coeficiente, 1);
    }

    // NUEVO: Permite crear x/2, 3x/4, etc.
    public static Termino crearVariable(int coeficiente, int divisor) {
        String base = (coeficiente == 1) ? "x" : (coeficiente == -1) ? "-x" : coeficiente + "x";
        String simbolo = (divisor > 1) ? base + "/" + divisor : base;
        return new Termino(TipoTermino.VARIABLE, simbolo, coeficiente, 0, divisor, coeficiente >= 0);
    }

    // Sobrecarga para constantes
    public static Termino crearConstante(int valor) {
        return crearConstante(valor, 1);
    }

    // NUEVO: Permite crear 1/2, 5/3, etc.
    public static Termino crearConstante(int valor, int divisor) {
        String base = valor >= 0 ? "+" + valor : String.valueOf(valor);
        String simbolo = (divisor > 1) ? base + "/" + divisor : base;
        return new Termino(TipoTermino.CONSTANTE, simbolo, 0, valor, divisor, valor >= 0);
    }

    public static Termino crearOperador(@NonNull String operador) {
        return new Termino(TipoTermino.OPERADOR, operador, 0, 0, 1, true);
    }

    public static Termino crearIgual() {
        return new Termino(TipoTermino.IGUAL, "=", 0, 0, 1, true);
    }

    public static Termino crearPotencia() {
        return new Termino(TipoTermino.POTENCIA, "^", 0, 0, 1, true);
    }

    public static Termino crearParentesis(boolean abre) {
        TipoTermino tipo = abre ? TipoTermino.PARENTESIS_ABRE : TipoTermino.PARENTESIS_CIERRA;
        String simbolo = abre ? "(" : ")";
        return new Termino(tipo, simbolo, 0, 0, 1, true);
    }

    // Getters y Setters actualizados
    public int getDivisor() { return divisor; }

    public void setDivisor(int d) {
        this.divisor = (d <= 0) ? 1 : d;
        actualizarSimbolo();
    }

    public void setCoeficiente(int n) {
        if (tipo != TipoTermino.VARIABLE) throw new IllegalStateException("Solo VARIABLE");
        this.coeficiente = n;
        this.positivo = n >= 0;
        actualizarSimbolo();
    }

    public void setValor(int n) {
        if (tipo != TipoTermino.CONSTANTE) throw new IllegalStateException("Solo CONSTANTE");
        this.valor = n;
        this.positivo = n >= 0;
        actualizarSimbolo();
    }

    // Método interno para refrescar el texto visual (simbolo)
    private void actualizarSimbolo() {
        if (tipo == TipoTermino.VARIABLE) {
            String base = (coeficiente == 1) ? "x" : (coeficiente == -1) ? "-x" : coeficiente + "x";
            this.simbolo = (divisor > 1) ? base + "/" + divisor : base;
        } else if (tipo == TipoTermino.CONSTANTE) {
            String base = valor >= 0 ? "+" + valor : String.valueOf(valor);
            this.simbolo = (divisor > 1) ? base + "/" + divisor : base;
        }
    }

    // El resto de tus métodos (cancelaCon, esVariable, etc.) se mantienen igual...
    public boolean esVariable()  { return tipo == TipoTermino.VARIABLE; }
    public boolean esConstante() { return tipo == TipoTermino.CONSTANTE; }
    public boolean esOperador()  { return tipo == TipoTermino.OPERADOR; }
    public boolean esIgual()     { return tipo == TipoTermino.IGUAL; }

    @NonNull public String getId() { return id; }
    @NonNull public TipoTermino getTipo() { return tipo; }
    @NonNull public String getSimbolo() { return simbolo; }
    public int getCoeficiente() { return coeficiente; }
    public int getValor() { return valor; }
    public boolean isPositivo() { return positivo; }

    public Termino copiar() {
        return new Termino(this.tipo, this.simbolo, this.coeficiente, this.valor, this.divisor, this.positivo);
    }
}