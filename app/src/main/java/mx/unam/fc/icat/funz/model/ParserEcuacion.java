package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.List;

/**
 * Utilidad para serialización y parseo de expresiones algebraicas.
 */
public class ParserEcuacion {

    @NonNull
    public static Ecuacion parsear(@NonNull String expresion) {
        String normalized = expresion.replace("−", "-").replace("–", "-").replace("×", "*").replace("÷", "/").trim();
        String spaced = normalized.replaceAll("(?<=[^\\s])([\\+\\-=])", " $1")
                .replaceAll("([\\+\\-=])(?=[^\\s])", "$1 ");

        Ecuacion ec = new Ecuacion();
        String[] tokens = spaced.split("\\s+");

        int currentSign = 1;
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (token.equals("=")) {
                ec.getTerminos().add(TerminoFactory.crearIgual());
                currentSign = 1;
            } else if (token.equals("+")) {
                currentSign = 1;
            } else if (token.equals("-")) {
                currentSign = -1;
            } else if (token.contains("x")) {
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

    private static Termino parseVariable(String token, int currentSign) {
        String clean = token.replace("(", "").replace(")", "");
        int divisor = 1;
        if (clean.contains("/")) {
            String[] dParts = clean.split("/");
            if (dParts.length > 1) {
                try { divisor = Integer.parseInt(dParts[1]); } catch (Exception ignored) {}
            }
            clean = dParts[0];
        }
        String coefStr = clean.replace("x", "");
        int coef = (coefStr.isEmpty() || coefStr.equals("+")) ? 1 :
                (coefStr.equals("-") ? -1 : Integer.parseInt(coefStr));
        return TerminoFactory.crearVariable(coef * currentSign, divisor);
    }

    private static Termino parseConstante(String token, int currentSign) {
        try {
            String clean = token.replace("(", "").replace(")", "");
            if (clean.isEmpty()) return null;
            int divisor = 1;
            if (clean.contains("/")) {
                String[] dParts = clean.split("/");
                if (dParts.length > 1) divisor = Integer.parseInt(dParts[1]);
                clean = dParts[0];
            }
            return TerminoFactory.crearConstante(Integer.parseInt(clean) * currentSign, divisor);
        } catch (Exception e) { return null; }
    }

    public static String terminosAString(List<Termino> lista) {
        if (lista.isEmpty()) return "0";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            Termino t = lista.get(i);
            String s = t.getSimbolo();
            boolean pos = t.isPositivo();
            if (s.startsWith("+")) s = s.substring(1);
            else if (s.startsWith("-") && i > 0) s = s.substring(1);
            if (i > 0) sb.append(pos ? "+ " : "- ");
            if (!s.equals("0") || (lista.size() == 1)) sb.append(s).append(" ");
        }
        return sb.toString().trim();
    }
}