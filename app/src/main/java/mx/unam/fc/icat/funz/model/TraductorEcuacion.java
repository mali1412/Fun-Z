package mx.unam.fc.icat.funz.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import mx.unam.fc.icat.funz.utils.AlgebraTokens;

/**
 * Componente algorítmico encargado de traducir secuencias de tokens lineales provenientes de las
 * interacciones visuales de la UI hacia la estructura semántica y de precedencia del modelo de datos.
 * <p>
 * Implementa una variante adaptada del algoritmo de Shunting-yard (Patio de Maniobras) de Dijkstra. Utiliza
 * una estructura de pila ({@link Stack}) para evaluar en tiempo de ejecución las prioridades operacionales
 * bajo la jerarquía PEMDAS, gestionando de forma segura signos de agrupación (paréntesis), operadores unarios/binarios
 * y tokens compuestos que requieran segmentación léxica de contingencia.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class TraductorEcuacion {

    /**
     * Traduce una colección secuencial lineal de símbolos vectoriales en un objeto estructurado {@link Ecuacion}.
     * <p>
     * Procesa cada token discriminando entre operadores relacionales, signos de agrupación y operandos elementales.
     * Cuenta con una sub-rutina de contingencia de análisis sintáctico que intercepta y desglosa tokens pegados
     * (ej. "/2" o "÷5") generados por imprecisiones del tokenizer del panel visual.
     * </p>
     *
     * @param tokens Lista indexada de strings que representan la secuencia actual de la interfaz de usuario.
     * @return Una instancia íntegra de {@link Ecuacion} con sus términos organizados y validados.
     */
    public static Ecuacion traducirSecuencia(List<String> tokens) {
        List<Termino> resultado = new ArrayList<>();
        Stack<String> operadores = new Stack<>();

        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) continue;

            if (t.equals(AlgebraTokens.EQUALS)) {
                vaciarPila(operadores, resultado);
                resultado.add(TerminoFactory.crearIgual());
            } else if (t.equals(AlgebraTokens.OPEN_PAREN)) {
                operadores.push(t);
            } else if (t.equals(AlgebraTokens.CLOSE_PAREN)) {
                while (!operadores.isEmpty() && !operadores.peek().equals(AlgebraTokens.OPEN_PAREN)) {
                    resultado.add(crearTerminoEspecial(operadores.pop()));
                }
                if (!operadores.isEmpty()) operadores.pop();
            } else if (esOperador(t)) {
                // Si el token es un operador puro (+, -, /, etc.)
                while (!operadores.isEmpty() &&
                        obtenerPrecedencia(operadores.peek()) >= obtenerPrecedencia(t)) {
                    resultado.add(crearTerminoEspecial(operadores.pop()));
                }
                operadores.push(t);
            } else {
                // Si el token es un operando (ej: "x", "5", o incluso "/2" por error del tokenizer)
                // Primero verificamos si el token "sucio" contiene un operador al inicio
                if ((t.startsWith(AlgebraTokens.DIV_ASCII) || t.startsWith(AlgebraTokens.DIV_SYMBOL)) && t.length() > 1) {
                    // Si el token es "/2", lo separamos manualmente para no perder la operación
                    String op = t.substring(0, 1);
                    String num = t.substring(1);

                    while (!operadores.isEmpty() && obtenerPrecedencia(operadores.peek()) >= obtenerPrecedencia(op)) {
                        resultado.add(crearTerminoEspecial(operadores.pop()));
                    }
                    operadores.push(op);
                    resultado.add(parsearOperando(num));
                } else {
                    resultado.add(parsearOperando(t));
                }
            }
        }
        vaciarPila(operadores, resultado);
        return new Ecuacion(resultado);
    }

    /**
     * Puga y vacía de forma secuencial los operadores remanentes en la pila hacia la lista consolidada de resultados,
     * omitiendo delimitadores decorativos abiertos residuales.
     */
    private static void vaciarPila(Stack<String> pila, List<Termino> res) {
        while (!pila.isEmpty()) {
            String op = pila.pop();
            if (!op.equals(AlgebraTokens.OPEN_PAREN)) res.add(crearTerminoEspecial(op));
        }
    }

    /**
     * Predicado de control léxico que evalúa si un token califica como operador algebraico core.
     */
    private static boolean esOperador(String t) {
        return t.equals(AlgebraTokens.PLUS)
                || t.equals(AlgebraTokens.MINUS)
                || t.equals(AlgebraTokens.MUL_SYMBOL)
                || t.equals(AlgebraTokens.DIV_SYMBOL)
                || t.equals(AlgebraTokens.DIV_ASCII)
                || t.equals(AlgebraTokens.POW);
    }

    /**
     * Delega a la fábrica la construcción del componente unario especializado basándose en el símbolo del operador.
     */
    private static Termino crearTerminoEspecial(String op) {
        if (op.equals(AlgebraTokens.POW)) return TerminoFactory.crearPotencia();
        return TerminoFactory.crearOperador(op);
    }

    /**
     * Define los niveles de precedencia matemática (PEMDAS).
     * Incluimos la barra "/" con la misma prioridad que la división "÷".
     */
    private static int obtenerPrecedencia(String op) {
        switch (op) {
            case AlgebraTokens.OPEN_PAREN:
            case AlgebraTokens.CLOSE_PAREN:
                return 0;
            case AlgebraTokens.PLUS:
            case AlgebraTokens.MINUS:
                return 1;
            case AlgebraTokens.MUL_SYMBOL:
            case AlgebraTokens.DIV_SYMBOL:
            case AlgebraTokens.DIV_ASCII:
                return 2;
            case AlgebraTokens.POW:
                return 3;
            default: return -1;
        }
    }

    /**
     * Convierte un texto en un Termino (Variable o Constante).
     * Preserva fracciones tipo x/2 o 3/2 para mantener equivalencia matemática.
     */
    private static Termino parsearOperando(String t) {
        // Quitamos paréntesis decorativos
        String clean = t.replace(AlgebraTokens.OPEN_PAREN, "")
                .replace(AlgebraTokens.CLOSE_PAREN, "")
                .trim();

        if (clean.contains(AlgebraTokens.X_SYMBOL)) {
            // Lógica para x, 2x, -x, x/2
            int divisor = 1;
            int coef = 1;

            if (clean.contains(AlgebraTokens.DIV_ASCII) || clean.contains(AlgebraTokens.DIV_SYMBOL)) {
                String[] parts = clean.split(AlgebraTokens.DIV_SPLIT_REGEX);
                divisor = Integer.parseInt(parts[1].trim());
                clean = parts[0].trim();
            }

            String coefStr = clean.replace(AlgebraTokens.X_SYMBOL, "").trim();
            if (coefStr.equals(AlgebraTokens.MINUS)) coef = -1;
            else if (!coefStr.isEmpty() && !coefStr.equals(AlgebraTokens.PLUS)) coef = Integer.parseInt(coefStr);

            return TerminoFactory.crearVariable(coef, divisor);
        } else {
            // Lógica para constantes y fracciones numéricas
            int divisor = 1;
            int val;

            if (clean.contains(AlgebraTokens.DIV_ASCII) || clean.contains(AlgebraTokens.DIV_SYMBOL)) {
                String[] parts = clean.split(AlgebraTokens.DIV_SPLIT_REGEX);
                divisor = Integer.parseInt(parts[1].trim());
                val = Integer.parseInt(parts[0].trim());
            } else {
                val = Integer.parseInt(clean);
            }

            return TerminoFactory.crearConstante(val, divisor);
        }
    }
}
