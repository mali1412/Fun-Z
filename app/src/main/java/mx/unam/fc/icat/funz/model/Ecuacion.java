package mx.unam.fc.icat.funz.model;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ecuacion — Motor algebraico dinámico con soporte para fracciones.
 */
public class Ecuacion {

    private final List<Termino> terminos = new ArrayList<>();

    public Ecuacion() {}

    public Ecuacion(@NonNull List<Termino> terminos) {
        this.terminos.addAll(terminos);
    }

    public List<Termino> getTerminos() {
        return terminos;
    }

    public String getLhsString() { return terminosAString(getLadoIzquierdo()); }
    public String getRhsString() { return terminosAString(getLadoDerecho()); }
   
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
                ec.terminos.add(Termino.crearIgual());
                currentSign = 1;
            } else if (token.equals("+")) {
                currentSign = 1;
            } else if (token.equals("-")) {
                currentSign = -1;
            } else if (token.contains("x")) {
                String clean = token.replace("(", "").replace(")", "");
                int divisor = 1;
                if (clean.contains("/")) {
                    String[] dParts = clean.split("/");
                    if (dParts.length > 1) {
                        try { divisor = Integer.parseInt(dParts[1]); } catch (NumberFormatException ignored) {}
                    }
                    clean = dParts[0];
                }
                String[] parts = clean.split("x");
                String coefStr = parts.length > 0 ? parts[0] : "";
                int coef;
                if (coefStr.isEmpty() || coefStr.equals("+")) coef = 1;
                else if (coefStr.equals("-")) coef = -1;
                else {
                    try {
                        coef = Integer.parseInt(coefStr);
                    } catch (NumberFormatException e) {
                        coef = 1;
                    }
                }
                ec.terminos.add(Termino.crearVariable(coef * currentSign, divisor));
                currentSign = 1;
            } else {
                try {
                    String clean = token.replace("(", "").replace(")", "");
                    if (!clean.isEmpty()) {
                        int divisor = 1;
                        if (clean.contains("/")) {
                            String[] dParts = clean.split("/");
                            if (dParts.length > 1) {
                                try { divisor = Integer.parseInt(dParts[1]); } catch (NumberFormatException ignored) {}
                            }
                            clean = dParts[0];
                        }
                        int val = Integer.parseInt(clean);
                        ec.terminos.add(Termino.crearConstante(val * currentSign, divisor));
                        currentSign = 1;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return ec;
    }

    public void aplicarOperacion(String opStr) {
        String clean = opStr.replace("−", "-").replace("–", "-").replace("×", "*").replace("÷", "/").trim();
        if (clean.length() < 2) return;

        char symbol = clean.charAt(0);
        String rest = clean.substring(1);

        try {
            if (symbol == '+' || symbol == '-') {
                Termino t;
                if (rest.contains("x")) {
                    int divisor = 1;
                    if (rest.contains("/")) {
                        String[] dParts = rest.split("/");
                        if (dParts.length > 1) divisor = Integer.parseInt(dParts[1]);
                        rest = dParts[0];
                    }
                    String coefStr = rest.replace("x", "");
                    int coef = (coefStr.isEmpty() || coefStr.equals("+")) ? 1 :
                            (coefStr.equals("-") ? -1 : Integer.parseInt(coefStr));
                    if (symbol == '-') coef = -coef;
                    t = Termino.crearVariable(coef, divisor);
                } else {
                    int divisor = 1;
                    if (rest.contains("/")) {
                        String[] dParts = rest.split("/");
                        if (dParts.length > 1) divisor = Integer.parseInt(dParts[1]);
                        rest = dParts[0];
                    }
                    int val = Integer.parseInt(rest);
                    if (symbol == '-') val = -val;
                    t = Termino.crearConstante(val, divisor);
                }

                int igualIdx = indiceIgual();
                if (igualIdx >= 0) {
                    terminos.add(igualIdx, t.copiar());
                    terminos.add(t.copiar());
                }
            } else if (symbol == '*' || symbol == '/') {
                int value = Integer.parseInt(rest);
                if (value == 0) return;
                
                if (symbol == '*') {
                    for (Termino t : terminos) {
                        if (t.esVariable()) {
                            int nCoef = t.getCoeficiente() * value;
                            int nDiv = t.getDivisor();
                            int common = gcd(nCoef, nDiv);
                            t.setCoeficiente(nCoef / common);
                            t.setDivisor(nDiv / common);
                        } else if (t.esConstante()) {
                            int nVal = t.getValor() * value;
                            int nDiv = t.getDivisor();
                            int common = gcd(nVal, nDiv);
                            t.setValor(nVal / common);
                            t.setDivisor(nDiv / common);
                        }
                    }
                } else { // división
                    for (Termino t : terminos) {
                        if (t.esVariable()) {
                            int nCoef = t.getCoeficiente();
                            int nDiv = t.getDivisor() * value;
                            int common = gcd(nCoef, nDiv);
                            t.setCoeficiente(nCoef / common);
                            t.setDivisor(nDiv / common);
                        } else if (t.esConstante()) {
                            int nVal = t.getValor();
                            int nDiv = t.getDivisor() * value;
                            int common = gcd(nVal, nDiv);
                            t.setValor(nVal / common);
                            t.setDivisor(nDiv / common);
                        }
                    }
                }
            }
        } catch (NumberFormatException ignored) {}

        simplificar();
    }

    public void simplificar() {
        int igualIdx = indiceIgual();
        if (igualIdx < 0) {
            terminos.add(Termino.crearIgual());
            igualIdx = terminos.size() - 1;
        }
        
        // Simplificar lado izquierdo
        List<Termino> lhs = new ArrayList<>(terminos.subList(0, igualIdx));
        Simplificacion resultadoL = simplificarLado(lhs);
        
        // Simplificar lado derecho
        List<Termino> rhs = new ArrayList<>(terminos.subList(igualIdx + 1, terminos.size()));
        Simplificacion resultadoR = simplificarLado(rhs);
        
        terminos.clear();
        // Reconstruir LHS
        if (resultadoL.coef != 0) terminos.add(Termino.crearVariable(resultadoL.coef, resultadoL.divCoef));
        if (resultadoL.val != 0 || resultadoL.coef == 0) terminos.add(Termino.crearConstante(resultadoL.val, resultadoL.divVal));
        
        terminos.add(Termino.crearIgual());
        
        // Reconstruir RHS
        if (resultadoR.coef != 0) terminos.add(Termino.crearVariable(resultadoR.coef, resultadoR.divCoef));
        if (resultadoR.val != 0 || resultadoR.coef == 0) terminos.add(Termino.crearConstante(resultadoR.val, resultadoR.divVal));
    }

    private static class Simplificacion {
        int coef, divCoef, val, divVal;
    }

    private Simplificacion simplificarLado(List<Termino> lado) {
        Simplificacion s = new Simplificacion();
        s.divCoef = 1; s.divVal = 1;
        
        for (Termino t : lado) {
            if (t.esVariable()) s.divCoef = lcm(s.divCoef, t.getDivisor());
            else if (t.esConstante()) s.divVal = lcm(s.divVal, t.getDivisor());
        }
        
        for (Termino t : lado) {
            if (t.esVariable()) {
                s.coef += t.getCoeficiente() * (s.divCoef / t.getDivisor());
            } else if (t.esConstante()) {
                s.val += t.getValor() * (s.divVal / t.getDivisor());
            }
        }
        
        // Simplificar fracciones finales
        int commonCoef = gcd(s.coef, s.divCoef);
        s.coef /= commonCoef; s.divCoef /= commonCoef;
        
        int commonVal = gcd(s.val, s.divVal);
        s.val /= commonVal; s.divVal /= commonVal;
        
        return s;
    }

    private String terminosAString(List<Termino> lista) {
        if (lista.isEmpty()) return "0";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            Termino t = lista.get(i);
            String s = t.getSimbolo();
            boolean pos = t.isPositivo();

            if (s.startsWith("+")) s = s.substring(1);
            else if (s.startsWith("-") && i > 0) s = s.substring(1);

            if (i > 0) {
                sb.append(pos ? "+ " : "- ");
            }

            if (!s.equals("0") || (lista.size() == 1)) {
                sb.append(s).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public double evaluarLado(double valorX, boolean izquierdo) {
        List<Termino> lado = izquierdo ? getLadoIzquierdo() : getLadoDerecho();
        double total = 0;
        for (Termino t : lado) {
            if (t.esVariable()) total += (double) t.getCoeficiente() * valorX / t.getDivisor();
            else if (t.esConstante()) total += (double) t.getValor() / t.getDivisor();
        }
        return total;
    }

    public boolean xEstaAislada() {
        List<Termino> lhs = getLadoIzquierdo();
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

    public int valorRHS() { 
        return (int) Math.round(evaluarLado(0, false)); 
    }

    public List<Termino> getLadoIzquierdo() {
        int idx = indiceIgual();
        return idx < 0 ? terminos : new ArrayList<>(terminos.subList(0, idx));
    }

    public List<Termino> getLadoDerecho() {
        int idx = indiceIgual();
        return (idx < 0 || idx == terminos.size() - 1) ? Collections.emptyList() : new ArrayList<>(terminos.subList(idx + 1, terminos.size()));
    }

    private int indiceIgual() {
        for (int i = 0; i < terminos.size(); i++) if (terminos.get(i).esIgual()) return i;
        return -1;
    }

    private int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b > 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a == 0 ? 1 : a;
    }

    private int lcm(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a * b) / gcd(a, b);
    }
}
