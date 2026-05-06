package mx.unam.fc.icat.funz.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import mx.unam.fc.icat.funz.model.Ecuacion;

/**
 * Exercise — entidad Room que representa UN ejercicio dentro de un módulo.
 *
 * El campo {@link #type} determina qué panel de la UI se renderiza
 * en {@code ExerciseActivity}. Para agregar un nuevo tipo de ejercicio
 * basta con definir el valor de tipo aquí y crear el layout correspondiente.
 *
 * Campos específicos por tipo:
 *   BALANZA → lhsExpr, rhsExpr, correctOp, lhsAfterOp, rhsAfterOp, ops
 *   CLASICO → solutionSteps (JSON array de strings para mostrar en pantalla)
 *   TILES   → tilesLeft, tilesRight (JSON arrays de etiquetas "x"/"+1"/"-1")
 *
 * Para agregar un ejercicio nuevo: insertar una fila en esta tabla.
 * Para agregar un módulo nuevo: insertar en 'modulos' + las filas de ejercicios.
 */
@Entity(
    tableName = "exercises",
    foreignKeys = @ForeignKey(
        entity    = Module.class,
        parentColumns = "id",
        childColumns  = "module_id",
        onDelete  = ForeignKey.CASCADE
    ),
    indices = { @Index(value = {"module_id", "step_order"}, unique = true) }
)
public class Exercise {

    // ── Tipo de ejercicio ─────────────────────────────────────────────────────
    /** Constantes de tipo — el Activity infla el panel correspondiente. */
    public static final String TYPE_BALANZA = "BALANZA";
    public static final String TYPE_CLASICO = "CLASICO";
    public static final String TYPE_TILES   = "TILES";

    // ── Clave primaria ────────────────────────────────────────────────────────
    @PrimaryKey(autoGenerate = true)
    public int id;

    // ── Clasificación ─────────────────────────────────────────────────────────
    @ColumnInfo(name = "module_id")  public int    moduleId;
    @ColumnInfo(name = "step_order") public int    stepOrder;

    @NonNull
    @ColumnInfo(name = "type")       public String type;

    // ── Datos comunes ─────────────────────────────────────────────────────────
    /** Ecuación que se muestra en pantalla, p.ej. "x + 5 = 10" */
    @ColumnInfo(name = "equation_obj")
    public Ecuacion equationObj;
    @NonNull
    @ColumnInfo(name = "equation")       public String equation;

    /** Respuesta correcta como String; se convierte a int en el ViewModel. */
    @NonNull
    @ColumnInfo(name = "correct_answer") public String correctAnswer;

    /** Texto de pista que se muestra en el BottomSheet. */
    @NonNull
    @ColumnInfo(name = "hint_text")      public String hintText;

    public int pointsCorrect = 100;
    public int pointsHint    = 50;

    // Campos específicos
    public String lhsExpr;
    public String rhsExpr;
    public String correctOp;
    public String lhsAfterOp;
    public String rhsAfterOp;
    public String ops;
    public String solutionSteps;
    public String tilesLeft;
    public String tilesRight;
}
