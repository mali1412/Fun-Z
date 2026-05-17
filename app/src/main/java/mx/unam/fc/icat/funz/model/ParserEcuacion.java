package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.List;

import mx.unam.fc.icat.funz.utils.AlgebraTokens;

/**
 * Clase utilitaria encargada del análisis léxico, tokenización y formateo de expresiones algebraicas.
 * <p>
 * Provee los métodos estáticos indispensables para parsear cadenas de texto planas (ej. "3x + 5 = 20")
 * aislando operadores y operandos mediante expresiones regulares, inflándolos como un objeto estructural
 * de tipo {@link Ecuacion}. Asimismo, realiza el proceso inverso (serialización), traduciendo listas de
 * componentes de tipo {@link Termino} a cadenas de texto sanitizadas y legibles para la interfaz de usuario.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class ParserEcuacion {

    /**
     * Analiza una cadena de texto para estructurar y construir un objeto {@link Ecuacion}.
     * <p>
     * El proceso normaliza variantes tipográficas de signos menos o barras de división, inyecta
     * un espaciado uniforme a través de expresiones regulares y segmenta la expresión en tokens discretos.
     * Recorre cada token modificando el estado del signo actual para asociarlo de manera correcta al
     * coeficiente o valor del término resultante.
     * </p>
     *
     * @param expresion Cadena de texto con la ecuación lineal a parsear. No debe ser nula.
     * @return Una instancia de {@link Ecuacion} con su árbol de términos internos poblado.
     */
    @NonNull
    public static Ecuacion parsear(@NonNull String expresion) {
        String normalized = expresion.replace(AlgebraTokens.MINUS_SIGN, AlgebraTokens.MINUS)
                .replace(AlgebraTokens.EN_DASH, AlgebraTokens.MINUS)
                .replace(AlgebraTokens.MUL_SYMBOL, AlgebraTokens.MUL_ASCII)
                .replace(AlgebraTokens.DIV_SYMBOL, AlgebraTokens.DIV_ASCII)
                .trim();
        String spaced = normalized.replaceAll("(?<=[^\\s])([\\+\\-=])", " $1")
                .replaceAll("([\\+\\-=])(?=[^\\s])", "$1 ");

        Ecuacion ec = new Ecuacion();
        String[] tokens = spaced.split("\\s+");

        int currentSign = 1;
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (token.equals(AlgebraTokens.EQUALS)) {
                ec.getTerminos().add(TerminoFactory.crearIgual());
                currentSign = 1;
            } else if (token.equals(AlgebraTokens.PLUS)) {
                currentSign = 1;
            } else if (token.equals(AlgebraTokens.MINUS)) {
                currentSign = -1;
            } else if (token.contains(AlgebraTokens.X_SYMBOL)) {
                ec.getTerminos().add(parseVariable(token, currentSign));
                currentSign = 1;
            } else {
                Termino t = parseConstante(token, currentSign);
                if (t != null) ec.getTerminos().add(t);
                currentSign = 1;
            }
        }
        return ec;
    }

    /**
     * Parsea un token específico identificado como variable para extraer su coeficiente y denominador.
     */
    private static Termino parseVariable(String token, int currentSign) {
        String clean = token.replace(AlgebraTokens.OPEN_PAREN, "").replace(AlgebraTokens.CLOSE_PAREN, "");
        int divisor = 1;
        if (clean.contains(AlgebraTokens.DIV_ASCII)) {
            String[] dParts = clean.split(AlgebraTokens.DIV_ASCII);
            if (dParts.length > 1) {
                try { divisor = Integer.parseInt(dParts[1]); } catch (Exception ignored) {}
            }
            clean = dParts[0];
        }
        String coefStr = clean.replace(AlgebraTokens.X_SYMBOL, "");
        int coef = (coefStr.isEmpty() || coefStr.equals(AlgebraTokens.PLUS)) ? 1 :
                (coefStr.equals(AlgebraTokens.MINUS) ? -1 : Integer.parseInt(coefStr));
        return TerminoFactory.crearVariable(coef * currentSign, divisor);
    }

    /**
     * Parsea un token específico identificado como cantidad escalar constante independiente.
     */
    private static Termino parseConstante(String token, int currentSign) {
        try {
            String clean = token.replace(AlgebraTokens.OPEN_PAREN, "").replace(AlgebraTokens.CLOSE_PAREN, "");
            if (clean.isEmpty()) return null;
            int divisor = 1;
            if (clean.contains(AlgebraTokens.DIV_ASCII)) {
                String[] dParts = clean.split(AlgebraTokens.DIV_ASCII);
                if (dParts.length > 1) divisor = Integer.parseInt(dParts[1]);
                clean = dParts[0];
            }
            return TerminoFactory.crearConstante(Integer.parseInt(clean) * currentSign, divisor);
        } catch (Exception e) { return null; }
    }

    /**
     * Convierte una colección indexada de elementos estructurales de tipo {@link Termino} en una cadena
     * de texto sanitizada, formateada y perfectamente espaciada para su despliegue en las pantallas de la app.
     * <p>
     * El algoritmo remueve signos unarios redundantes al principio de los miembros e inyecta operadores
     * binarios de manera estética basándose en las propiedades booleanas de positividad de los elementos.
     * </p>
     *
     * @param lista Colección o sublista de componentes algebraicos pertenecientes a un miembro. No debe ser nula.
     * @return Cadena de caracteres formateada con sintaxis algebraica limpia (ej. "2x + 4").
     */
    public static String terminosAString(List<Termino> lista) {
        if (lista.isEmpty()) return AlgebraTokens.ZERO;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            Termino t = lista.get(i);
            String s = t.getSimbolo();
            boolean pos = t.isPositivo();
            if (s.startsWith(AlgebraTokens.PLUS)) s = s.substring(1);
            else if (s.startsWith(AlgebraTokens.MINUS) && i > 0) s = s.substring(1);
            if (i > 0) sb.append(pos ? "+ " : "- ");
            if (!s.equals(AlgebraTokens.ZERO) || (lista.size() == 1)) sb.append(s).append(" ");
        }
        return sb.toString().trim();
    }
}