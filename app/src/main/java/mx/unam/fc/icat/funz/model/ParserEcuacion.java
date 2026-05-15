package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.List;

import mx.unam.fc.icat.funz.utils.AlgebraTokens;

/**
 * Utilidad para serialización y parseo de expresiones algebraicas.
 */
public class ParserEcuacion {

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