package mx.unam.fc.icat.funz.db;

import androidx.room.TypeConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import mx.unam.fc.icat.funz.model.Ecuacion;
import mx.unam.fc.icat.funz.model.Termino;
import mx.unam.fc.icat.funz.model.TerminoFactory;

/**
 * <p>
 * Permite mapear y transformar estructuras de datos complejas no primitivas (como la entidad
 * {@link Ecuacion}) en cadenas de texto formateadas en JSON para su almacenamiento directo
 * en las columnas de la base de datos SQLite, garantizando una reconstrucción fidedigna e
 * inmutable al recuperar los registros.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
public class Converters {
    /**
     * Serializa una instancia de {@link Ecuacion} traduciendo su listado interno de términos
     * algebraicos en una estructura plana embebida en una cadena JSON.
     *
     * @param ecuacion Objeto con la estructura algebraica a persistir. Puede ser nulo.
     * @return Cadena de caracteres en formato JSON lista para base de datos, o {@code null} si ocurre un error o la entrada es nula.
     */
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
                obj.put("div", t.getDivisor());
                obj.put("pos", t.isPositivo());
                array.put(obj);
            }
            return array.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Deserializa una cadena con formato JSON recuperada de SQLite para reconstruir de forma
     * íntegra la instancia matemática de la {@link Ecuacion} mediante la fábrica de términos.
     *
     * @param data Cadena de texto JSON recuperada desde las columnas de Room. Puede ser nula.
     * @return Instancia matemática de {@link Ecuacion} poblada con sus respectivos términos, u {@code null} en caso de corrupción o datos nulos.
     */
    @TypeConverter
    public static Ecuacion toEcuacion(String data) {
        if (data == null) return null;
        try {
            JSONArray array = new JSONArray(data);
            List<Termino> terminos = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                terminos.add(TerminoFactory.reconstruir(
                    o.getString("id"),
                    Termino.TipoTermino.valueOf(o.getString("tipo")),
                    o.getString("simbolo"),
                    o.getInt("coef"),
                    o.getInt("val"),
                    o.optInt("div", 1),
                    o.getBoolean("pos")
                ));
            }
            return new Ecuacion(terminos);
        } catch (Exception e) {
            return null;
        }
    }
}
