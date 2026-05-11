package mx.unam.fc.icat.funz.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * TraductorEcuacion — Algoritmo puente que transforma la disposición visual
 * de los elementos en pantalla hacia la jerarquía lógica del modelo.
 */
public class TraductorEcuacion {

    /**
     * Define los niveles de precedencia matemática (PEMDAS).
     * Incluimos la barra "/" con la misma prioridad que la división "÷".
     */
    private static int obtenerPrecedencia(String op) {
        switch (op) {
            case "(": case ")": return 0;
            case "+": case "-": return 1;
            case "×": case "÷": case "/": return 2;
            case "^":           return 3;
            default: return -1;
        }
    }

    /**
     * Traduce una lista de símbolos a un objeto Ecuacion.
     */
    public static Ecuacion traducirSecuencia(List<String> tokens) {
        List<Termino> resultado = new ArrayList<>();
        Stack<String> operadores = new Stack<>();

        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) continue;

            if (t.equals("=")) {
                vaciarPila(operadores, resultado);
                resultado.add(Termino.crearIgual());
            } else if (t.equals("(")) {
                operadores.push(t);
            } else if (t.equals(")")) {
                while (!operadores.isEmpty() && !operadores.peek().equals("(")) {
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
                if ((t.startsWith("/") || t.startsWith("÷")) && t.length() > 1) {
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

    private static void vaciarPila(Stack<String> pila, List<Termino> res) {
        while (!pila.isEmpty()) {
            String op = pila.pop();
            if (!op.equals("(")) res.add(crearTerminoEspecial(op));
        }
    }

    private static boolean esOperador(String t) {
        return t.equals("+") || t.equals("-") || t.equals("×") || t.equals("÷") || t.equals("/") || t.equals("^");
    }

    private static Termino crearTerminoEspecial(String op) {
        if (op.equals("^")) return Termino.crearPotencia();
        return Termino.crearOperador(op);
    }

    /**
     * Convierte un texto en un Termino (Variable o Constante).
     * Preserva fracciones tipo x/2 o 3/2 para mantener equivalencia matemática.
     */
    private static Termino parsearOperando(String t) {
        String clean = t.replace("(", "")
                .replace(")", "")
                .trim();

        if (clean.contains("x")) {
            int divisor = 1;
            String base = clean;
            if (base.contains("/") || base.contains("÷")) {
                String[] parts = base.split("[÷/]", 2);
                base = parts[0].trim();
                if (parts.length > 1) {
                    try {
                        int parsed = Integer.parseInt(parts[1].trim());
                        if (parsed > 0) divisor = parsed;
                    } catch (NumberFormatException ignored) {}
                }
            }

            String coefStr = base.replace("x", "").trim();
            int coef;
            if (coefStr.isEmpty() || coefStr.equals("+")) {
                coef = 1;
            } else if (coefStr.equals("-")) {
                coef = -1;
            } else {
                try {
                    coef = Integer.parseInt(coefStr);
                } catch (NumberFormatException e) {
                    coef = 1;
                }
            }
            return Termino.crearVariable(coef, divisor);
        }

        try {
            if (clean.isEmpty()) return Termino.crearConstante(0);
            int divisor = 1;
            String numerador = clean;
            if (clean.contains("/") || clean.contains("÷")) {
                String[] parts = clean.split("[÷/]", 2);
                numerador = parts[0].trim();
                if (parts.length > 1) {
                    try {
                        int parsed = Integer.parseInt(parts[1].trim());
                        if (parsed > 0) divisor = parsed;
                    } catch (NumberFormatException ignored) {}
                }
            }
            return Termino.crearConstante(Integer.parseInt(numerador), divisor);
        } catch (NumberFormatException e) {
            // Si aún así falla, devolvemos una constante 0 en lugar de crashear
            return Termino.crearConstante(0);
        }
    }
}
