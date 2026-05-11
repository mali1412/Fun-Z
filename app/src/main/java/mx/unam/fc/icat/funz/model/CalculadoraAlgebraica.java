package mx.unam.fc.icat.funz.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de procesamiento para manipulación y validación de ecuaciones.
 */
public class CalculadoraAlgebraica {

    public static void aplicarOperacion(Ecuacion ec, String opStr) {
        String clean = opStr.replace("−", "-").replace("–", "-")
                .replace("×", "*").replace("÷", "/").trim();
        if (clean.length() < 2) return;

        char symbol = clean.charAt(0);
        String rest = clean.substring(1);

        try {
            if (symbol == '+' || symbol == '-') {
                Termino t = buildTerminoFromOp(symbol, rest);
                int igualIdx = ec.indiceIgual();
                if (igualIdx >= 0) {
                    ec.getTerminos().add(igualIdx, t.copiar());
                    ec.getTerminos().add(t.copiar());
                }
            } else if (symbol == '*' || symbol == '/') {
                int value = Integer.parseInt(rest);
                if (value == 0) return;
                distribuirOperacion(ec, symbol, value);
            }
        } catch (NumberFormatException ignored) {}

        simplificar(ec);
    }

    private static Termino buildTerminoFromOp(char symbol, String rest) {
        int divisor = 1;
        if (rest.contains("/")) {
            String[] dParts = rest.split("/");
            if (dParts.length > 1) divisor = Integer.parseInt(dParts[1]);
            rest = dParts[0];
        }

        if (rest.contains("x")) {
            String coefStr = rest.replace("x", "");
            int coef = (coefStr.isEmpty() || coefStr.equals("+")) ? 1 :
                    (coefStr.equals("-") ? -1 : Integer.parseInt(coefStr));
            if (symbol == '-') coef = -coef;
            return TerminoFactory.crearVariable(coef, divisor);
        } else {
            int val = Integer.parseInt(rest);
            if (symbol == '-') val = -val;
            return TerminoFactory.crearConstante(val, divisor);
        }
    }

    private static void distribuirOperacion(Ecuacion ec, char symbol, int value) {
        for (Termino t : ec.getTerminos()) {
            if (t.esIgual()) continue;
            int nVal = t.esVariable() ? t.getCoeficiente() : t.getValor();
            int nDiv = t.getDivisor();

            if (symbol == '*') nVal *= value;
            else nDiv *= value;

            int common = gcd(nVal, nDiv);
            if (t.esVariable()) {
                t.setCoeficiente(nVal / common);
            } else {
                t.setValor(nVal / common);
            }
            t.setDivisor(nDiv / common);
        }
    }

    public static void simplificar(Ecuacion ec) {
        int igualIdx = ec.indiceIgual();
        if (igualIdx < 0) return;

        Simplificacion resL = simplificarLado(ec.getLadoIzquierdo());
        Simplificacion resR = simplificarLado(ec.getLadoDerecho());

        ec.getTerminos().clear();
        reconstruirLado(ec.getTerminos(), resL);
        ec.getTerminos().add(TerminoFactory.crearIgual());
        reconstruirLado(ec.getTerminos(), resR);
    }

    private static void reconstruirLado(List<Termino> list, Simplificacion s) {
        if (s.coef != 0) list.add(TerminoFactory.crearVariable(s.coef, s.divCoef));
        if (s.val != 0 || s.coef == 0) list.add(TerminoFactory.crearConstante(s.val, s.divVal));
    }

    public static double evaluarLado(Ecuacion ec, double valorX, boolean izquierdo) {
        List<Termino> lado = izquierdo ? ec.getLadoIzquierdo() : ec.getLadoDerecho();
        double total = 0;
        for (Termino t : lado) {
            if (t.esVariable()) total += (double) t.getCoeficiente() * valorX / t.getDivisor();
            else if (t.esConstante()) total += (double) t.getValor() / t.getDivisor();
        }
        return total;
    }

    public static boolean xEstaAislada(Ecuacion ec) {
        List<Termino> lhs = ec.getLadoIzquierdo();
        int xCount = 0;
        boolean xOk = false;
        int constVal = 0;
        for (Termino t : lhs) {
            if (t.esVariable()) {
                xCount++;
                if (t.getCoeficiente() == 1 && t.getDivisor() == 1) xOk = true;
            } else if (t.esConstante()) {
                constVal = t.getValor();
            }
        }
        return xCount == 1 && xOk && constVal == 0;
    }

    private static class Simplificacion {
        int coef, divCoef = 1, val, divVal = 1;
    }

    private static Simplificacion simplificarLado(List<Termino> lado) {
        Simplificacion s = new Simplificacion();
        for (Termino t : lado) {
            if (t.esVariable()) s.divCoef = lcm(s.divCoef, t.getDivisor());
            else if (t.esConstante()) s.divVal = lcm(s.divVal, t.getDivisor());
        }
        for (Termino t : lado) {
            if (t.esVariable()) s.coef += t.getCoeficiente() * (s.divCoef / t.getDivisor());
            else if (t.esConstante()) s.val += t.getValor() * (s.divVal / t.getDivisor());
        }
        int cC = gcd(s.coef, s.divCoef);
        s.coef /= cC; s.divCoef /= cC;
        int cV = gcd(s.val, s.divVal);
        s.val /= cV; s.divVal /= cV;
        return s;
    }

    /**
     * Calcula el Máximo Común Divisor (MCD) de dos enteros.
     * Utiliza el Algoritmo de Euclides para mayor eficiencia.
     * * @param a Primer número.
     * @param b Segundo número.
     * @return El entero más grande que divide a 'a' y 'b'. Retorna 1 si no hay divisor común.
     */
    public static int gcd(int a, int b) {
        a = Math.abs(a); b = Math.abs(b);
        while (b > 0) { int temp = b; b = a % b; a = temp; }
        return a == 0 ? 1 : a;
    }

    /**
     * Calcula el Mínimo Común Múltiplo (mcm) de dos enteros.
     * Se basa en la propiedad: mcm(a,b) = |a * b| / MCD(a,b).
     * * @param a Primer número.
     * @param b Segundo número.
     * @return El múltiplo común más pequeño entre 'a' y 'b'.
     */
    public static int lcm(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a * b) / gcd(a, b);
    }
}