package mx.unam.fc.icat.funz.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad de persistencia para Room que modela un módulo o unidad temática dentro del mapa
 * de aprendizaje del sistema FunZ.
 * <p>
 * Representa la estructura de contenido de alto nivel inspirada en los bloques del Álgebra de Baldor.
 * Almacena los metadatos de progreso, el estado de bloqueo dinámico y encapsula de forma relacional
 * el contenido teórico/práctico indexado (títulos informativos, descripciones didácticas y
 * ejemplos resueltos en formato serializado JSON) para su consumo dinámico en la UI.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
 */
@Entity(tableName = "modules")
public class Module {

    // Atributos

    @PrimaryKey
    public int id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    @NonNull
    @ColumnInfo(name = "subtitle")
    public String subtitle;

    @NonNull
    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "order_index")
    public int orderIndex;

    @ColumnInfo(name = "unlocked")
    public boolean unlocked = false;

    @ColumnInfo(name = "exercise_count")
    public int exerciseCount = 3;

    // Campos para contenido dinámico de Info/Ejemplos
    @ColumnInfo(name = "info_title_1") public String infoTitle1;
    @ColumnInfo(name = "info_text_1")  public String infoText1;
    @ColumnInfo(name = "info_title_2") public String infoTitle2;
    @ColumnInfo(name = "info_text_2")  public String infoText2;
    @ColumnInfo(name = "info_title_3") public String infoTitle3;
    @ColumnInfo(name = "info_text_3")  public String infoText3;

    // Sección de ejemplos.
    @ColumnInfo(name = "example_equation") public String exampleEquation;
    @ColumnInfo(name = "example_steps")    public String exampleSteps; // JSON Array
}
