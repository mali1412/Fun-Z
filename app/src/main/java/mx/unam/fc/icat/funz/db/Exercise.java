package mx.unam.fc.icat.funz.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import mx.unam.fc.icat.funz.model.Ecuacion;

/**
 * Entidad de persistencia para Room que modela de forma abstracta e interactiva un desafío
 * algebraico individual perteneciente a un módulo didáctico del sistema.
 * <p>
 * Implementa un esquema polimórfico ligero: el campo {@link #type} actúa como el discriminador
 * que dicta a la capa visual ({@code ExerciseActivity}) qué tipo de panel, lógica e interacciones
 * sensoriales (Balanza, Clásico o Algebra Tiles) deben ser infladas y renderizadas en la UI.
 * </p>
 * <p>
 * Cuenta con restricciones de integridad referencial rígidas mediante claves foráneas acopladas
 * en cascada con la entidad {@link Module}, e índices únicos para evitar colisiones operacionales
 * en el mapeo secuencial de pasos.
 * </p>
 *
 * @author Alan Kevin Cano Tenorio
 * @author Malinalli Escobedo Irineo
 * @author Marco Antonio Chávez Martínez
 * @version 1.0
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

    // Constantes para identificar el tipo de ejercicio.

    public static final String TYPE_BALANZA = "BALANZA";
    public static final String TYPE_CLASICO = "CLASICO";
    public static final String TYPE_TILES   = "TILES";

    // Atributos y clabes

    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "module_id")
    public int moduleId;
    @ColumnInfo(name = "step_order")
    public int stepOrder;

    @NonNull
    @ColumnInfo(name = "type")
    public String type;

    // Campos matemmaticos

    /** Estructura abstracta desglosada de la ecuación persistida mediante serialización JSON. */
    @ColumnInfo(name = "equation_obj")
    public Ecuacion equationObj;

    /** Representación en cadena de texto legible de la ecuación original (ej. "3x + 5 = 20"). */
    @NonNull
    @ColumnInfo(name = "equation")
    public String equation;
    @NonNull
    @ColumnInfo(name = "correct_answer")
    public String correctAnswer;
    @NonNull
    @ColumnInfo(name = "hint_text")
    public String hintText;

    // Campos para la interfaz visual

    // Metáfora de la Balanza: Árbol de expresiones y operaciones objetivo espejo
    public String lhsExpr;
    public String rhsExpr;
    public String correctOp;
    public String lhsAfterOp;
    public String rhsAfterOp;
    public String ops; // Arreglo JSON con los botones de operaciones válidas de la balanza

    // Metáfora Modo Clásico: Secuencia procedural de pasos intermedios con cajas de texto
    public String solutionSteps;

    // Metáfora Algebra Tiles: Estado y volumen de distribución inicial de los azulejos
    public String tilesLeft;  // Arreglo JSON de strings (ej. ["x", "+1", "-1"]) para miembro izquierdo
    public String tilesRight; // Arreglo JSON de strings para miembro derecho
}