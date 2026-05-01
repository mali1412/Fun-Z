package mx.unam.fc.icat.funz.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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
    @ColumnInfo(name = "step_order") public int    stepOrder;   // 1, 2, 3 …
    @NonNull
    @ColumnInfo(name = "type")       public String type;        // TYPE_* constante

    // ── Datos comunes ─────────────────────────────────────────────────────────
    /** Ecuación que se muestra en pantalla, p.ej. "x + 5 = 10" */
    @NonNull
    @ColumnInfo(name = "equation")       public String equation;

    /** Respuesta correcta como String; se convierte a int en el ViewModel. */
    @NonNull
    @ColumnInfo(name = "correct_answer") public String correctAnswer;

    /** Texto de pista que se muestra en el BottomSheet. */
    @NonNull
    @ColumnInfo(name = "hint_text")      public String hintText;

    /** Puntos ganados respondiendo sin pista. */
    @ColumnInfo(name = "points_correct") public int pointsCorrect = 100;

    /** Puntos ganados respondiendo con pista. */
    @ColumnInfo(name = "points_hint")    public int pointsHint    =  50;

    // ── Campos BALANZA ────────────────────────────────────────────────────────
    /** Expresión inicial del plato izquierdo, p.ej. "x+5". Null si no es BALANZA. */
    @ColumnInfo(name = "lhs_expr")    public String lhsExpr;

    /** Expresión inicial del plato derecho, p.ej. "10". Null si no es BALANZA. */
    @ColumnInfo(name = "rhs_expr")    public String rhsExpr;

    /** Operación que aísla x, p.ej. "-5". */
    @ColumnInfo(name = "correct_op")  public String correctOp;

    /** Expresión LHS tras aplicar la op correcta, p.ej. "x". */
    @ColumnInfo(name = "lhs_after")   public String lhsAfterOp;

    /** Expresión RHS tras aplicar la op correcta, p.ej. "5". */
    @ColumnInfo(name = "rhs_after")   public String rhsAfterOp;

    /**
     * JSON array de operaciones disponibles en los botones, p.ej.
     * ["-5","+5","x2","/2"]. Máximo 4 operaciones.
     */
    @ColumnInfo(name = "ops") public String ops;

    // ── Campos CLASICO ────────────────────────────────────────────────────────
    /**
     * JSON array de strings con los pasos de la solución, p.ej.
     * ["Transponer +5: 3x = 15", "Dividir entre 3: x = 5"].
     */
    @ColumnInfo(name = "solution_steps") public String solutionSteps;

    // ── Campos TILES ──────────────────────────────────────────────────────────
    /**
     * JSON array de etiquetas para el lado izquierdo, p.ej. ["x","+1","+1"].
     * La Activity construye las vistas de tile a partir de esta lista.
     */
    @ColumnInfo(name = "tiles_left")  public String tilesLeft;

    /**
     * JSON array de etiquetas para el lado derecho, p.ej.
     * ["+1","+1",...] (12 elementos para x+2=12).
     */
    @ColumnInfo(name = "tiles_right") public String tilesRight;
}
