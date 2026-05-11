package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.UUID;

public class TerminoFactory {

    public static Termino crearVariable(int coeficiente, int divisor) {
        String base = (coeficiente == 1) ? "x" : (coeficiente == -1) ? "-x" : coeficiente + "x";
        String simbolo = (divisor > 1) ? base + "/" + divisor : base;
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.VARIABLE, simbolo, coeficiente, 0, divisor, coeficiente >= 0);
    }

    public static Termino crearConstante(int valor, int divisor) {
        String base = valor >= 0 ? "+" + valor : String.valueOf(valor);
        String simbolo = (divisor > 1) ? base + "/" + divisor : base;
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.CONSTANTE, simbolo, 0, valor, divisor, valor >= 0);
    }

    public static Termino crearOperador(@NonNull String op) {
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.OPERADOR, op, 0, 0, 1, true);
    }

    public static Termino crearIgual() {
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.IGUAL, "=", 0, 0, 1, true);
    }

    public static Termino crearParentesis(boolean abre) {
        Termino.TipoTermino tipo = abre ? Termino.TipoTermino.PARENTESIS_ABRE : Termino.TipoTermino.PARENTESIS_CIERRA;
        String simbolo = abre ? "(" : ")";
        return new Termino(UUID.randomUUID().toString(), tipo, simbolo, 0, 0, 1, true);
    }

    public static Termino crearPotencia() {
        return new Termino(UUID.randomUUID().toString(), Termino.TipoTermino.POTENCIA, "^", 0, 0, 1, true);
    }

    public static Termino reconstruir(String id, Termino.TipoTermino tipo, String simbolo, int coeficiente, int valor, int divisor, boolean positivo) {
        return new Termino(id, tipo, simbolo, coeficiente, valor, divisor, positivo);
    }
}