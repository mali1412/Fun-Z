package mx.unam.fc.icat.funz.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Module — entidad Room que representa un módulo de aprendizaje.
 */
@Entity(tableName = "modules")
public class Module {

    @PrimaryKey
    public int id;

    @NonNull
    @ColumnInfo(name = "name") public String name;

    @NonNull
    @ColumnInfo(name = "subtitle") public String subtitle;

    @NonNull
    @ColumnInfo(name = "description") public String description;

    @ColumnInfo(name = "order_index") public int orderIndex;

    @ColumnInfo(name = "unlocked") public boolean unlocked = false;

    @ColumnInfo(name = "exercise_count") public int exerciseCount = 3;

    // Campos para contenido dinámico de Info/Ejemplos
    @ColumnInfo(name = "info_title_1") public String infoTitle1;
    @ColumnInfo(name = "info_text_1")  public String infoText1;
    @ColumnInfo(name = "info_title_2") public String infoTitle2;
    @ColumnInfo(name = "info_text_2")  public String infoText2;

    @ColumnInfo(name = "example_equation") public String exampleEquation;
    @ColumnInfo(name = "example_steps")    public String exampleSteps; // JSON Array
}
