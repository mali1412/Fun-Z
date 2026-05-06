package mx.unam.fc.icat.funz.db;

import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;

/**
 * DbSeeder — callback de Room que inserta los datos iniciales
 * (módulos y ejercicios) solo la primera vez que se crea la base de datos.
 */
public class DbSeeder extends RoomDatabase.Callback {

    @Override
    public void onCreate(@NonNull SupportSQLiteDatabase db) {
        super.onCreate(db);
        Executors.newSingleThreadExecutor().execute(DbSeeder::seedDatabase);
    }

    private static void seedDatabase() {
        // La implementación real ocurre en seed() llamado desde FunZDatabase
    }

    /**
     * Semilla completa de datos. Llamado desde FunZDatabase con acceso a los DAOs.
     * Solo inserta si las tablas están vacías (idempotente).
     */
    public static void seed(ModuleDao moduleDao, ExerciseDao exerciseDao) {
        if (moduleDao.count() > 0) return; // ya sembrado

        // ── Módulo 1 ──
        Module mod1 = buildModule(1,"Módulo 1: Introducción","x+a=b · 3 métodos",
            "Aprende a resolver ecuaciones lineales simples usando tres métodos: balanza, clásico y algebra tiles.",
            1, true, 3,
            "¿Qué es una ecuación?", "Una igualdad matemática con una o más incógnitas. El objetivo es encontrar el valor de x que hace verdadera la igualdad.",
            "Forma simple: x + a = b", "Solo se necesita sumar o restar en ambos lados para despejar x. El principio clave: lo que haces a un lado, debes hacerlo al otro.",
            "x + 3 = 7",
            new String[]{
                "Identificar el término a eliminar del lado izquierdo: +3",
                "Aplicar operación inversa: x + 3 − 3 = 7 − 3",
                "Simplificar: x = 4 ✓"
            });
        moduleDao.insertAll(Collections.singletonList(mod1));
        exerciseDao.insertAll(Arrays.asList(
            buildBalanza(1,1,"x + 5 = 10","5","x+5","10","-5","x","5",
                new String[]{"-5","+5","×2","÷2"},
                "Paso 1: Resta 5 a ambos lados.\n  x+5−5 = 10−5\n\nPaso 2: Simplifica.\n  x = 5 ✓"),
            buildClasico(1,2,"3x + 5 = 20","5",
                new String[]{"Transponer +5 al otro lado:","  3x = 20 − 5 = 15",
                             "Dividir entre el coeficiente 3:","  x = 15 ÷ 3 = 5 ✓"},
                "Paso 1: Transponer +5 → 3x = 15\nPaso 2: Dividir entre 3 → x = 5 ✓"),
            buildTiles(1,3,"x + 2 = 12","10",
                new String[]{"x","+1","+1"},
                new String[]{"+1","+1","+1","+1","+1","+1","+1","+1","+1","+1","+1","+1"},
                "Mueve los +1 del lado izquierdo.\nCuando solo quede x: cuenta los +1 del derecho → x = 10 ✓")
        ));

        // ── Módulo 2 ──
        Module mod2 = buildModule(2,"Módulo 2: Coeficientes","ax+b=c · coeficiente >1",
            "Resuelve ecuaciones donde x tiene un coeficiente mayor a 1.",
            2, false, 3,
            "El coeficiente de x", "Es el número que multiplica a la variable. Por ejemplo, en 2x, el coeficiente es 2.",
            "Despeje en dos pasos", "Primero elimina sumas o restas (términos independientes), luego divide entre el coeficiente para dejar a x sola.",
            "2x + 4 = 10",
            new String[]{
                "Restar 4 a ambos lados: 2x = 6",
                "Dividir entre el coeficiente 2: x = 6 / 2",
                "Resultado: x = 3 ✓"
            });
        moduleDao.insertAll(Collections.singletonList(mod2));
        exerciseDao.insertAll(Arrays.asList(
            buildBalanza(2,1,"2x + 3 = 11","4","2x+3","11","-3","2x","8",
                new String[]{"-3","+3","÷2","×2"},
                "Paso 1: Resta 3 → 2x = 8\nPaso 2: Divide entre 2 → x = 4 ✓"),
            buildClasico(2,2,"5x − 10 = 20","6",
                new String[]{"Transponer −10:","  5x = 20 + 10 = 30","Dividir entre 5:","  x = 30 ÷ 5 = 6 ✓"},
                "Paso 1: Transponer −10 → 5x = 30\nPaso 2: Dividir entre 5 → x = 6 ✓"),
            buildTiles(2,3,"2x + 1 = 7","3",
                new String[]{"x","x","+1"},
                new String[]{"+1","+1","+1","+1","+1","+1","+1"},
                "Mueve el +1 del izquierdo. Quedan 2x = 6. x = 3 ✓")
        ));

        // ── Módulo 3 ──
        Module mod3 = buildModule(3,"Módulo 3: Fracciones","x/a+b=c · eliminar denominador",
            "Resuelve ecuaciones con fracciones multiplicando ambos lados.",
            3, false, 3,
            "Denominadores", "Indican una división de la variable. Para eliminarla, usamos la operación inversa: la multiplicación.",
            "Método de eliminación", "Multiplica toda la ecuación por el denominador común para trabajar solo con números enteros.",
            "x/2 - 3 = 1",
            new String[]{
                "Sumar 3 a ambos lados: x/2 = 4",
                "Multiplicar por el denominador 2: x = 4 * 2",
                "Resultado: x = 8 ✓"
            });
        moduleDao.insertAll(Collections.singletonList(mod3));
        exerciseDao.insertAll(Arrays.asList(
            buildBalanza(3,1,"x/2 + 1 = 5","8","x/2+1","5","-1","x/2","4",
                new String[]{"-1","+1","×2","÷2"},
                "Paso 1: Resta 1 → x/2 = 4\nPaso 2: Multiplica por 2 → x = 8 ✓"),
            buildClasico(3,2,"x/3 + 2 = 6","12",
                new String[]{"Transponer +2:","  x/3 = 6 − 2 = 4","Multiplicar por 3:","  x = 4 × 3 = 12 ✓"},
                "Paso 1: Transponer +2 → x/3 = 4\nPaso 2: Multiplicar por 3 → x = 12 ✓"),
            buildTiles(3,3,"x/2 = 3","6",
                new String[]{"x/2"},
                new String[]{"+1","+1","+1"},
                "El tile x/2 vale la mitad de x. Multiplica por 2. x = 6 ✓")
        ));

        // ── Módulo 4: Paréntesis ──
        Module mod4 = buildModule(4,"Módulo 4: Paréntesis","a(x+b)=c · distributiva",
            "Aprende a eliminar paréntesis usando la propiedad distributiva antes de resolver.",
            4, false, 3,
            "Propiedad Distributiva", "El número fuera del paréntesis multiplica a cada término dentro: a(b+c) = ab + ac.",
            "Orden de resolución", "1. Eliminar paréntesis. 2. Agrupar términos. 3. Despejar x.",
            "3(x + 2) = 15",
            new String[]{
                "Distribuir el 3: 3x + 6 = 15",
                "Restar 6: 3x = 9",
                "Dividir entre 3: x = 3 ✓"
            });
        moduleDao.insertAll(Collections.singletonList(mod4));
        exerciseDao.insertAll(Arrays.asList(
            buildClasico(4,1,"2(x + 3) = 10","2",
                new String[]{"Aplicar propiedad distributiva:","  2x + 6 = 10","Restar 6 a ambos lados:","  2x = 4","Dividir entre 2:","  x = 2 ✓"},
                "Paso 1: 2*x + 2*3 = 10 → 2x+6 = 10\nPaso 2: 2x = 4\nPaso 3: x = 2 ✓"),
            buildBalanza(4,2,"3(x - 1) = 9","4","3x-3","9","+3","3x","12",
                new String[]{"+3","-3","÷3","×3"},
                "Paso 1: Distribuye → 3x - 3 = 9\nPaso 2: Suma 3 → 3x = 12\nPaso 3: Divide entre 3 → x = 4 ✓"),
            buildClasico(4,3,"5(x + 2) = 20","2",
                new String[]{"Distribución:","  5x + 10 = 20","Transponer +10:","  5x = 10","Resultado:","  x = 2 ✓"},
                "Distribuye el 5 y luego resuelve como en el Módulo 2.")
        ));

        // ── Módulo 5: Variables en ambos lados ──
        Module mod5 = buildModule(5,"Módulo 5: Variables a ambos lados","ax+b=cx+d · agrupar x",
            "Mueve todos los términos con x a un lado de la ecuación y los números al otro.",
            5, false, 3,
            "Agrupación", "Mueve todos los términos con 'x' al lado izquierdo y los números al derecho.",
            "Transposición", "Al mover un término al otro lado del '=', su operación cambia (suma a resta y viceversa).",
            "5x + 2 = 2x + 11",
            new String[]{
                "Restar 2x en ambos lados: 3x + 2 = 11",
                "Restar 2: 3x = 9",
                "Dividir entre 3: x = 3 ✓"
            });
        moduleDao.insertAll(Collections.singletonList(mod5));
        exerciseDao.insertAll(Arrays.asList(
            buildClasico(5,1,"3x + 2 = x + 10","4",
                new String[]{"Restar x a ambos lados:","  2x + 2 = 10","Restar 2:","  2x = 8","Dividir entre 2:","  x = 4 ✓"},
                "Agrupa las x en el lado izquierdo restando 'x'."),
            buildBalanza(5,2,"4x = 2x + 6","3","4x","2x+6","-2x","2x","6",
                new String[]{"-2x","+2x","÷2","×2"},
                "Paso 1: Resta 2x para agrupar las variables → 2x = 6\nPaso 2: Divide entre 2 → x = 3 ✓"),
            buildClasico(5,3,"5x - 4 = 2x + 5","3",
                new String[]{"Agrupar x:","  3x - 4 = 5","Transponer -4:","  3x = 9","Resultado:","  x = 3 ✓"},
                "Mueve el 2x al izquierdo y el -4 al derecho.")
        ));

        // ── Módulo 6: Expansión y agrupación ──
        Module mod6 = buildModule(6,"Módulo 6: Expansión y agrupación","a(x+b)=cx+d · expandir y agrupar",
                "Primero expande los paréntesis y luego mueve las variables al mismo lado.",
                6, false, 3,
                "Expansión", "Convierte expresiones con paréntesis en sumas o restas simples.",
                "Agrupación", "Mueve todos los términos con 'x' al lado izquierdo y los números al derecho.",
                "2(x+3) = x + 9",
                new String[]{
                        "Expandir: 2x + 6 = x + 9",
                        "Restar x: x + 6 = 9",
                        "Restar 6: x = 3 ✓"
                });
        moduleDao.insertAll(Collections.singletonList(mod6));
        exerciseDao.insertAll(Arrays.asList(
                buildClasico(6,1,"2(x + 3) = x + 9","3",
                        new String[]{
                                "Expandir el paréntesis:","  2x + 6 = x + 9",
                                "Restar x:","  x + 6 = 9",
                                "Restar 6:","  x = 3 ✓"},
                        "Primero expande el paréntesis y luego agrupa las x."),
                buildBalanza(6,2,"3x + 4 = 2x + 10","6","3x+4","2x+10","-2x","x+4","10",
                        new String[]{"-2x","-4","÷2","×2","+4","+2x"},
                        "Paso 1: Resta 2x → x + 4 = 10\nPaso 2: Resta 4 → x = 6 ✓"),
                buildClasico(6,3,"4(x - 2) = 2x","4",
                        new String[]{
                                "Expandir:","  4x - 8 = 2x",
                                "Restar 2x:","  2x - 8 = 0",
                                "Sumar 8:","  2x = 8",
                                "Dividir entre 2:","  x = 4 ✓"},
                        "Expande el paréntesis y luego simplifica.")
        ));

    }

    private static Module buildModule(int id, String name, String subtitle,
            String desc, int order, boolean unlocked, int count,
            String it1, String ix1, String it2, String ix2,
            String eq, String[] steps) {
        Module m = new Module();
        m.id = id; m.name = name; m.subtitle = subtitle;
        m.description = desc; m.orderIndex = order;
        m.unlocked = unlocked; m.exerciseCount = count;
        m.infoTitle1 = it1; m.infoText1 = ix1;
        m.infoTitle2 = it2; m.infoText2 = ix2;
        m.exampleEquation = eq;
        m.exampleSteps = toJson(steps);
        return m;
    }

    private static Exercise buildBalanza(int moduleId, int step, String eq,
            String answer, String lhs, String rhs, String correctOp,
            String lhsAfter, String rhsAfter, String[] ops, String hint) {
        Exercise e = new Exercise();
        e.moduleId = moduleId; e.stepOrder = step;
        e.type = Exercise.TYPE_BALANZA;
        e.equation = eq; e.correctAnswer = answer; e.hintText = hint;
        e.lhsExpr = lhs; e.rhsExpr = rhs;
        e.correctOp = correctOp; e.lhsAfterOp = lhsAfter; e.rhsAfterOp = rhsAfter;
        e.ops = toJson(ops);
        return e;
    }

    private static Exercise buildClasico(int moduleId, int step, String eq,
            String answer, String[] steps, String hint) {
        Exercise e = new Exercise();
        e.moduleId = moduleId; e.stepOrder = step;
        e.type = Exercise.TYPE_CLASICO;
        e.equation = eq; e.correctAnswer = answer; e.hintText = hint;
        e.solutionSteps = toJson(steps);
        return e;
    }

    private static Exercise buildTiles(int moduleId, int step, String eq,
            String answer, String[] left, String[] right, String hint) {
        Exercise e = new Exercise();
        e.moduleId = moduleId; e.stepOrder = step;
        e.type = Exercise.TYPE_TILES;
        e.equation = eq; e.correctAnswer = answer; e.hintText = hint;
        e.tilesLeft = toJson(left); e.tilesRight = toJson(right);
        return e;
    }

    private static String toJson(String[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append("\"").append(arr[i].replace("\"", "\\\"")).append("\"");
            if (i < arr.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }
}
