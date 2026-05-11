package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.UUID;

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
    private int divisor;
    private boolean positivo;

    // Único constructor: Siempre requiere ID
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

    private void actualizarSimbolo() {
        if (tipo == TipoTermino.VARIABLE) {
            String base = (coeficiente == 1) ? "x" : (coeficiente == -1) ? "-x" : coeficiente + "x";
            this.simbolo = (divisor > 1) ? base + "/" + divisor : base;
        } else if (tipo == TipoTermino.CONSTANTE) {
            String base = valor >= 0 ? "+" + valor : String.valueOf(valor);
            this.simbolo = (divisor > 1) ? base + "/" + divisor : base;
        }
    }

    public boolean esVariable()  { return tipo == TipoTermino.VARIABLE; }
    public boolean esConstante() { return tipo == TipoTermino.CONSTANTE; }
    public boolean esIgual()     { return tipo == TipoTermino.IGUAL; }

    // Al copiar, generamos un ID NUEVO porque es un objeto distinto
    public Termino copiar() {
        return new Termino(UUID.randomUUID().toString(), this.tipo, this.simbolo, this.coeficiente, this.valor, this.divisor, this.positivo);
    }
}