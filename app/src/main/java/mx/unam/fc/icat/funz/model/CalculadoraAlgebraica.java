package mx.unam.fc.icat.funz.model;

import java.util.List;

import mx.unam.fc.icat.funz.utils.AlgebraTokens;

/**
 * Motor de procesamiento para manipulación y validación de ecuaciones.
 */
public class CalculadoraAlgebraica {

    public static void aplicarOperacion(Ecuacion ec, String opStr) {
        aplicarOperacionLados(ec, opStr, true, true);
    }

    public static void aplicarOperacionALado(Ecuacion ec, String opStr, boolean izquierdo) {
        aplicarOperacionLados(ec, opStr, izquierdo, !izquierdo);
    }

    private static void aplicarOperacionLados(Ecuacion ec, String opStr, boolean ladoIzquierdo, boolean ladoDerecho) {
        String clean = opStr.replace(AlgebraTokens.MINUS_SIGN, AlgebraTokens.MINUS)
                .replace(AlgebraTokens.EN_DASH, AlgebraTokens.MINUS)
                .replace(AlgebraTokens.MUL_SYMBOL, AlgebraTokens.MUL_ASCII)
                .replace(AlgebraTokens.DIV_SYMBOL, AlgebraTokens.DIV_ASCII)
                .trim();
        if (clean.length() < 2) return;

        char symbol = clean.charAt(0);
        String rest = clean.substring(1);

        try {
            if (symbol == '+' || symbol == '-') {
                Termino t = buildTerminoFromOp(symbol, rest);
                int igualIdx = ec.indiceIgual();
                if (igualIdx >= 0) {
                    if (ladoIzquierdo) {
                        ec.getTerminos().add(igualIdx, t.copiar());
                    }
                    if (ladoDerecho) {
                        ec.getTerminos().add(t.copiar());
                    }
                }
            } else if (symbol == '*' || symbol == '/') {
                int value = Integer.parseInt(rest);
                if (value == 0) return;
                if (ladoIzquierdo) distribuirOperacionLado(ec, symbol, value, true);
                if (ladoDerecho) distribuirOperacionLado(ec, symbol, value, false);
            }
        } catch (NumberFormatException ignored) {}

        simplificar(ec);
    }

    private static Termino buildTerminoFromOp(char symbol, String rest) {
        int divisor = 1;
        if (rest.contains(AlgebraTokens.DIV_ASCII)) {
            String[] dParts = rest.split(AlgebraTokens.DIV_ASCII);
            if (dParts.length > 1) divisor = Integer.parseInt(dParts[1]);
            rest = dParts[0];
        }

        if (rest.contains(AlgebraTokens.X_SYMBOL)) {
            String coefStr = rest.replace(AlgebraTokens.X_SYMBOL, "");
            int coef = (coefStr.isEmpty() || coefStr.equals(AlgebraTokens.PLUS)) ? 1 :
                    (coefStr.equals(AlgebraTokens.MINUS) ? -1 : Integer.parseInt(coefStr));
            if (symbol == '-') coef = -coef;
            return TerminoFactory.crearVariable(coef, divisor);
        } else {
            int val = Integer.parseInt(rest);
            if (symbol == '-') val = -val;
            return TerminoFactory.crearConstante(val, divisor);
        }
    }

    private static void distribuirOperacionLado(Ecuacion ec, char symbol, int value, boolean izquierdo) {
        int igualIdx = ec.indiceIgual();
        int start = izquierdo ? 0 : (igualIdx >= 0 ? igualIdx + 1 : 0);
        int end = izquierdo ? (igualIdx >= 0 ? igualIdx : ec.getTerminos().size()) : ec.getTerminos().size();

        for (int i = start; i < end; i++) {
            Termino t = ec.getTerminos().get(i);
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
        List<Termino> rhs = ec.getLadoDerecho();

        // 1. Verificar LHS: Debe tener exactamente una 'x' (coef 1, div 1) y ninguna constante != 0
        int xCountL = 0;
        boolean xOkL = false;
        for (Termino t : lhs) {
            if (t.esVariable()) {
                xCountL++;
                if (t.getCoeficiente() == 1 && t.getDivisor() == 1) xOkL = true;
            } else if (t.esConstante() && t.getValor() != 0) {
                return false; // Hay constantes en el lado de la x
            }
        }
        if (xCountL != 1 || !xOkL) return false;

        // 2. Verificar RHS: No debe haber NINGUNA variable x
        for (Termino t : rhs) {
            if (t.esVariable()) return false;
        }

        return true;
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

    public static int gcd(int a, int b) {
        a = Math.abs(a); b = Math.abs(b);
        while (b > 0) { int temp = b; b = a % b; a = temp; }
        return a == 0 ? 1 : a;
    }

    public static int lcm(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a * b) / gcd(a, b);
    }
}
