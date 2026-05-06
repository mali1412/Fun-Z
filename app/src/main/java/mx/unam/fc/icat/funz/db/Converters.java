package mx.unam.fc.icat.funz.db;

import androidx.room.TypeConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import mx.unam.fc.icat.funz.model.Ecuacion;
import mx.unam.fc.icat.funz.model.Termino;

public class Converters {
    @TypeConverter
    public static String fromEcuacion(Ecuacion ecuacion) {
        if (ecuacion == null) return null;
        try {
            JSONArray array = new JSONArray();
            for (Termino t : ecuacion.getTerminos()) {
                JSONObject obj = new JSONObject();
                obj.put("id", t.getId());
                obj.put("tipo", t.getTipo().name());
                obj.put("simbolo", t.getSimbolo());
                obj.put("coef", t.getCoeficiente());
                obj.put("val", t.getValor());
                obj.put("pos", t.isPositivo());
                array.put(obj);
            }
            return array.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @TypeConverter
    public static Ecuacion toEcuacion(String data) {
        if (data == null) return null;
        try {
            JSONArray array = new JSONArray(data);
            List<Termino> terminos = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                terminos.add(Termino.reconstruir(
                    o.getString("id"),
                    Termino.TipoTermino.valueOf(o.getString("tipo")),
                    o.getString("simbolo"),
                    o.getInt("coef"),
                    o.getInt("val"),
                    o.getBoolean("pos")
                ));
            }
            return new Ecuacion(terminos);
        } catch (Exception e) {
            return null;
        }
    }
}
