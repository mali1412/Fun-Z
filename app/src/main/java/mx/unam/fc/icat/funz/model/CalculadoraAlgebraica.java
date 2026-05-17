package mx.unam.fc.icat.funz.model;

import java.util.List;

import mx.unam.fc.icat.funz.utils.AlgebraTokens;

/**
 * Motor central de procesamiento y evaluación algebraica del sistema FunZ.
 * <p>
 * Implementa la lógica matemática abstracta para la manipulación dinámica de ecuaciones lineales.
 * Se encarga de procesar la transposición de términos (sumas y restas), la distribución de operaciones
 * multiplicativas y divisivas sobre los miembros de la ecuación, la reducción fraccionaria por mínimo
 * común múltiplo (MCM) y el análisis de aislamiento analítico de la incógnita 'x'.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class CalculadoraAlgebraica {

    /**
     * Aplica de forma espejo una operación matemática (ej. "+5", "-2x", "*3") en ambos miembros de la ecuación.
     * Mantiene el principio de equivalencia del axioma fundamental de igualdad.
     *
     * @param ec    Instancia de la {@link Ecuacion} que será modificada.
     * @param opStr Cadena de texto con la operación aritmética a aplicar.
     */
    public static void aplicarOperacion(Ecuacion ec, String opStr) {
        aplicarOperacionLados(ec, opStr, true, true);
    }

    /**
     * Aplica una operación matemática de forma asimétrica afectando únicamente a uno de los miembros de la ecuación.
     * Utilizado para simular desequilibrios o interacciones libres en las metáforas manipulativas (como la Balanza).
     *
     * @param ec        Instancia de la {@link Ecuacion} que será modificada.
     * @param opStr     Cadena de texto con la operación aritmética.
     * @param izquierdo {@code true} para afectar únicamente al miembro izquierdo; {@code false} para el derecho.
     */
    public static void aplicarOperacionALado(Ecuacion ec, String opStr, boolean izquierdo) {
        aplicarOperacionLados(ec, opStr, izquierdo, !izquierdo);
    }

    /**
     * Núcleo de control de operaciones. Normaliza las cadenas de texto del sistema de tiles o botones,
     * discrimina el operador y altera la estructura interna de términos de los miembros seleccionados.
     */
    private static void aplicarOperacionLados(Ecuacion ec, String opStr, boolean ladoIzquierdo, boolean ladoDerecho) {
        // Normalización de caracteres tipográficos especiales de álgebra
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

    /**
     * Desglosa y parsea el operando de una cadena para construir una instancia pura de {@link Termino}.
     */
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

    /**
     * Aplica la propiedad distributiva multiplicando o dividiendo cada coeficiente y término independiente
     * del miembro seleccionado, calculando inmediatamente el Máximo Común Divisor (MCD) para simplificar fracciones.
     */
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

    /**
     * Coordina la reducción e inflado de términos semejantes de ambos platos de la ecuación,
     * fusionando variables con variables y constantes con constantes.
     *
     * @param ec Instancia de la {@link Ecuacion} que se desea reducir.
     */
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

    /**
     * Reconstruye las listas de términos reducidos listos para ser reinyectados en la ecuación.
     */
    private static void reconstruirLado(List<Termino> list, Simplificacion s) {
        if (s.coef != 0) list.add(TerminoFactory.crearVariable(s.coef, s.divCoef));
        if (s.val != 0 || s.coef == 0) list.add(TerminoFactory.crearConstante(s.val, s.divVal));
    }

    /**
     * Evalúa numéricamente el valor ponderado de un miembro de la ecuación sustituyendo la incógnita 'x'.
     * Utilizado para calcular la inclinación e inercia física del hilo visual de la Balanza.
     *
     * @param ec        Ecuación bajo análisis.
     * @param valorX    Valor escalar asignado temporalmente a la variable 'x'.
     * @param izquierdo {@code true} para evaluar el miembro izquierdo; {@code false} para el derecho.
     * @return Resultado numérico con punto flotante de la evaluación del miembro.
     */
    public static double evaluarLado(Ecuacion ec, double valorX, boolean izquierdo) {
        List<Termino> lado = izquierdo ? ec.getLadoIzquierdo() : ec.getLadoDerecho();
        double total = 0;
        for (Termino t : lado) {
            if (t.esVariable()) total += (double) t.getCoeficiente() * valorX / t.getDivisor();
            else if (t.esConstante()) total += (double) t.getValor() / t.getDivisor();
        }
        return total;
    }

    /**
     * Diagnostica de forma matemática si la ecuación lineal se encuentra completamente resuelta.
     * <p>
     * Criterio de Aislamiento Eficiente: El miembro izquierdo debe contener única y estrictamente una
     * variable 'x' con coeficiente lineal unitario (1) y denominador unitario (1), libre de constantes.
     * Simultáneamente, el miembro derecho debe estar completamente libre de la incógnita 'x'.
     * </p>
     *
     * @param ec Ecuación lineal a diagnosticar.
     * @return {@code true} si la variable está perfectamente despejada e aislada; {@code false} en caso contrario.
     */
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

    /** Estructura de datos interna tipo Snapshot para la consolidación de coeficientes reducidos. */
    private static class Simplificacion {
        int coef, divCoef = 1, val, divVal = 1;
    }

    /**
     * Reduce una lista de términos dispersos calculando el Mínimo Común Múltiplo (LCM) de sus denominadores.
     */
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
     * Calcula el Máximo Común Divisor (MCD) entre dos números enteros utilizando el Algoritmo de Euclides.
     * Garantiza valores absolutos y previene divisiones indeterminadas por cero.
     *
     * @param a Primer escalar entero.
     * @param b Segundo escalar entero.
     * @return El Máximo Común Divisor entero positivo.
     */
    public static int gcd(int a, int b) {
        a = Math.abs(a); b = Math.abs(b);
        while (b > 0) { int temp = b; b = a % b; a = temp; }
        return a == 0 ? 1 : a;
    }

    /**
     * Calcula el Mínimo Común Múltiplo (MCM) entre dos números enteros utilizando la relación del MCD.
     *
     * @param a Primer escalar entero.
     * @param b Segundo escalar entero.
     * @return El Mínimo Común Múltiplo entero positivo.
     */
    public static int lcm(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a * b) / gcd(a, b);
    }
}
