package mx.unam.fc.icat.funz.model;import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * TraductorEcuacion — Algoritmo puente que transforma la disposición visual
 * de los elementos en pantalla hacia la jerarquía lógica del modelo.
 */
public class TraductorEcuacion {

    /**
     * Define los niveles de precedencia matemática (PEMDAS).
     * Nivel 3: Potencias (Máxima prioridad)
     * Nivel 2: Multiplicación y División
     * Nivel 1: Suma y Resta
     */
    private static int obtenerPrecedencia(String op) {
        switch (op) {
            case "(": case ")": return 0;
            case "+": case "-": return 1;
            case "×": case "÷": return 2;
            case "^":           return 3;
            default: return -1;
        }
    }

    /**
     * Traduce una lista de símbolos (disposición visual) a un objeto Ecuacion
     * con una estructura lógica jerárquica (Notación Polaca Inversa).
     *
     * @param tokens Secuencia de strings obtenida de la interfaz (ej. ["(", "x", "+", "2", ")", "^", "2"])
     * @return Ecuacion con términos ordenados lógicamente por precedencia.
     */
    public static Ecuacion traducirSecuencia(List<String> tokens) {
        List<Termino> resultado = new ArrayList<>();
        Stack<String> operadores = new Stack<>();

        for (String token : tokens) {
            String t = token.trim();

            if (t.equals("=")) {
                // Al llegar al igual, procesamos lo pendiente del lado izquierdo
                vaciarPila(operadores, resultado);
                resultado.add(Termino.crearIgual());
            } else if (t.equals("(")) {
                operadores.push(t);
            } else if (t.equals(")")) {
                // Resolvemos la jerarquía dentro del paréntesis
                while (!operadores.isEmpty() && !operadores.peek().equals("(")) {
                    resultado.add(crearTerminoEspecial(operadores.pop()));
                }
                if (!operadores.isEmpty()) operadores.pop(); // Quitar el "("
            } else if (esOperador(t)) {
                // Aplicamos reglas de precedencia
                while (!operadores.isEmpty() &&
                        obtenerPrecedencia(operadores.peek()) >= obtenerPrecedencia(t)) {
                    resultado.add(crearTerminoEspecial(operadores.pop()));
                }
                operadores.push(t);
            } else {
                // Es un operando (variable o constante)
                resultado.add(parsearOperando(t));
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
        return t.equals("+") || t.equals("-") || t.equals("×") || t.equals("÷") || t.equals("^");
    }

    private static Termino crearTerminoEspecial(String op) {
        if (op.equals("^")) return Termino.crearPotencia();
        return Termino.crearOperador(op);
    }

    private static Termino parsearOperando(String t) {
        if (t.contains("x")) {
            String clean = t.replace("(", "").replace(")", "");
            String coefStr = clean.replace("x", "");
            int coef = (coefStr.isEmpty() || coefStr.equals("+")) ? 1 :
                    (coefStr.equals("-") ? -1 : Integer.parseInt(coefStr));
            return Termino.crearVariable(coef);
        } else {
            return Termino.crearConstante(Integer.parseInt(t.replace("(", "").replace(")", "")));
        }
    }
}