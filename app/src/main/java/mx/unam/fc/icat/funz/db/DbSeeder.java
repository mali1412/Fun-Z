package mx.unam.fc.icat.funz.db;

import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;

import mx.unam.fc.icat.funz.model.Ecuacion;
import mx.unam.fc.icat.funz.model.ParserEcuacion;

/**
 * DbSeeder — callback de Room que inserta los datos iniciales
 * (módulos y ejercicios) solo la primera vez que se crea la base de datos.
 */
public class DbSeeder extends RoomDatabase.Callback {

    @Override
    public void onCreate(@NonNull SupportSQLiteDatabase db) {
        super.onCreate(db);
    }

    /**
     * Semilla completa de datos. Llamado desde FunZDatabase con acceso a los DAOs.
     * Solo inserta si las tablas están vacías o incompletas (idempotente).
     */
    /**
     * Semilla completa de datos. Llamado desde FunZDatabase con acceso a los DAOs.
     * Solo inserta si las tablas están vacías o incompletas (idempotente).
     */
    public static void seed(FunZDatabase db) {
        db.runInTransaction(() -> {
            ModuleDao moduleDao = db.moduleDao();
            ExerciseDao exerciseDao = db.exerciseDao();

            // Verificamos si la suma total de ejercicios esperada existe (6 módulos * 3 ej = 18)
            if (moduleDao.count() == 6 && exerciseDao.count() >= 18) return;

            // Si hay inconsistencia, limpiamos y empezamos de cero para garantizar integridad
            db.clearAllTables();

            // ── Módulo 1 ──
            Module mod1 = buildModule(1,
                    "Módulo 1: Introducción",
                    "Conceptos básicos e igualdad",
                    "Aprende el concepto de ecuación como una balanza.",
                    1, true,

                    // Bloque 1: Definición
                    "¿Qué es una Ecuación?",
                    "Es una igualdad en la que hay cantidades desconocidas llamadas incógnitas (letras). Solo es verdadera para valores específicos de dichas letras. El signo '=' indica que lo que está a la izquierda pesa exactamente lo mismo que lo que está a la derecha.",

                    // Bloque 2: El Axioma Fundamental
                    "Axioma de Igualdad",
                    "Si a dos cantidades iguales se les aplica la misma operación, la igualdad persiste. Para dejar a la 'x' sola, restamos la misma cantidad en ambos miembros. 'Lo que quitas de un plato de la balanza, debes quitarlo del otro para que no se incline'.",

                    // Bloque 3: La Forma Básica x + a = b
                    "Forma básica: x + a = b",
                    "Es el tipo más sencillo de ecuación. Para resolverla, aplicamos la 'Regla de Transposición': si un número está sumando junto a la x, pasa al otro lado del signo igual realizando la operación opuesta, es decir, restando. \n\nEjemplo: x + 5 = 12 se convierte en x = 12 - 5.",
                    // Ejemplo visual de la sección de info
                    "x + 3 = 7\n" + "x + 15 = 30\n" + "8 + x = 12",
                    new String[]{
                            "Ejemplo A (Básico): x + 3 = 7",
                            "1. El +3 pasa al otro lado restando: x = 7 - 3",
                            "2. Resultado: x = 4 ✓",
                            "", // Espacio visual
                            "Ejemplo B (Cantidades mayores y resta): x - 15 = 30",
                            "1. El -15 pasa sumando al segundo miembro: x = 30 + 15",
                            "2. Resultado: x = 45 ✓",
                            "", // Espacio visual
                            "Ejemplo C (Cambio de orden): 8 + x = 12",
                            "1. El 8 es positivo, pasa al otro lado restando: x = 12 - 8",
                            "2. Resultado: x = 4 ✓"
                    }
            );
            moduleDao.insertAll(Collections.singletonList(mod1));
            exerciseDao.insertAll(Arrays.asList(
                    buildBalanza(1,1,"x + 5 = 10","5","x+5","10","-5","x","5",
                            new String[]{"-5","+5","×2","÷2"},
                            "Paso 1: Resta 5 a ambos lados.\n  x+5-5 = 10-5\n\nPaso 2: Simplifica.\n  x = 5 ✓"),
                    buildClasico(1, 2, "3x + 5 = 20", "5",
                            new String[]{
                                    "Transponer +5 al otro lado:",
                                    "3x = 20 - |5|", // El sistema pondrá un cuadro aquí
                                    "3x = |15|",
                                    "Dividir entre el coeficiente 3:",
                                    "x = 15 / |3|",
                                    "¡Listo! Ingresa el valor final de x abajo:"
                            },
                            "Paso 1: Transponer +5 → 3x = 15\nPaso 2: Dividir entre 3 → x = 5 ✓"),
                    buildTiles(1,3,"x + 2 = 12","10",
                            new String[]{"x","+1","+1"},
                            new String[]{"+1","+1","+1","+1","+1","+1","+1","+1","+1","+1","+1","+1"},
                            "Mueve los +1 del lado izquierdo.\nCuando solo quede x: cuenta los +1 del derecho → x = 10 ✓")
            ));

            // ── Módulo 2 ──
            Module mod2 = buildModule(2,
                    "Módulo 2: Coeficientes",
                    "ax + b = c · El coeficiente",
                    "Resuelve ecuaciones donde x tiene un número multiplicándola.",
                    2, false,

                    // Bloque 1: El Coeficiente
                    "El coeficiente de x",
                    "Es el número que multiplica a la variable. En '2x', el coeficiente es 2. Esto significa que tenemos dos veces la incógnita. Si un número multiplica a la x en un miembro, pasa al otro miembro dividiendo a todo lo que esté allí.",

                    // Bloque 2: Despeje en dos pasos
                    "Prioridad de Despeje",
                    "Para dejar a la x sola, seguimos un orden: primero eliminamos lo que suma o resta (términos independientes) usando transposición, y al final eliminamos el coeficiente usando división. Es como quitar las capas de una cebolla hasta llegar al centro.",

                    // Bloque 3: Reducción a la unidad
                    "Reducción a la unidad",
                    "El objetivo final es que x tenga coeficiente 1 (que aparezca sola). Si tenemos 5x = 30, dividimos ambos miembros entre 5. Según el axioma: si dividimos cantidades iguales por un mismo número, los resultados son iguales.",

                    // Ejemplos visuales de la sección de info (3 Ejemplos)
                    "2x + 4 = 10\n" + "3x - 6 = 12\n" + "5x = 20",
                    new String[]{
                            "Ejemplo A (Dos pasos): 2x + 4 = 10",
                            "1. Pasamos el +4 restando: 2x = 10 - 4 -> 2x = 6",
                            "2. El 2 multiplica a x, pasa dividiendo: x = 6 / 2",
                            "3. Resultado: x = 3 ✓",
                            "",
                            "Ejemplo B (Con resta): 3x - 6 = 12",
                            "1. Pasamos el -6 sumando: 3x = 12 + 6 -> 3x = 18",
                            "2. Pasamos el 3 dividiendo: x = 18 / 3",
                            "3. Resultado: x = 6 ✓",
                            "",
                            "Ejemplo C (Coeficiente solo): 5x = 20",
                            "1. No hay sumas, pasamos el 5 dividiendo: x = 20 / 5",
                            "2. Resultado: x = 4 ✓"
                    }
            );
            moduleDao.insertAll(Collections.singletonList(mod2));
            exerciseDao.insertAll(Arrays.asList(
                    buildBalanza(2,1,"2x + 3 = 11","4","2x+3","11","-3","2x","8",
                            new String[]{"-3","+3","÷2","×2"},
                            "Paso 1: Resta 3 → 2x = 8\nPaso 2: Divide entre 2 → x = 4 ✓"),
                    buildClasico(2, 2, "5x - 10 = 20", "6",
                            new String[]{
                                    "Transponer -10:",
                                    "5x = 20 + |10|",
                                    "5x = |30|",
                                    "Dividir entre 5:",
                                    "x = 30 / |5|",
                                    "¡Listo! Ingresa el valor final de x abajo:"
                            },
                            "Paso 1: Transponer -10 → 5x = 30\nPaso 2: Dividir entre 5 → x = 6 ✓"),
                    buildTiles(2,3,"2x + 1 = 7","3",
                            new String[]{"x","x","+1"},
                            new String[]{"+1","+1","+1","+1","+1","+1","+1"},
                            "Mueve el +1 del izquierdo. Quedan 2x = 6. x = 3 ✓")
            ));

            // ── Módulo 3 ──
            Module mod3 = buildModule(3,
                    "Módulo 3: Fracciones",
                    "x/a + b = c · Eliminar denominador",
                    "Resuelve ecuaciones con fracciones eliminando el divisor.",
                    3, false,

                    // Bloque 1: El Denominador
                    "Concepto de Denominador",
                    "En una ecuación, el denominador indica que la incógnita o un término está siendo dividido. Debemos multiplicar los dos miembros de la ecuación por dicho número. Esto se llama 'linealizar' la ecuación.",

                    // Bloque 2: Operación Inversa
                    "La Multiplicación",
                    "Así como la suma se elimina con resta, la división (fracción) se elimina con la multiplicación. Si tienes x dividido entre 3, al multiplicar por 3 recuperas la x entera. Recuerda: para mantener el equilibrio, debes multiplicar todo el miembro opuesto también.",

                    // Bloque 3: Metodo de Eliminación
                    "Pasos Sugeridos",
                    "1. Primero, realiza la transposición de los términos independientes (sumas y restas).\n2. Cuando la fracción esté sola, multiplica por el denominador.\n3. Si la x tiene un coeficiente arriba (ej. 2x/3), primero multiplica por 3 y luego divide entre 2.",

                    // Ejemplos visuales de la sección de info (3 Ejemplos)
                    "x/2 + 3 = 7\n" + "x/3 - 1 = 4\n" + "x/5 = 2",
                    new String[]{
                            "Ejemplo A (Suma y Fracción): x/2 + 3 = 7",
                            "1. Pasamos el +3 restando: x/2 = 7 - 3 -> x/2 = 4",
                            "2. El 2 divide, pasa multiplicando: x = 4 * 2",
                            "3. Resultado: x = 8 ✓",
                            "",
                            "Ejemplo B (Resta y Fracción): x/3 - 1 = 4",
                            "1. Pasamos el -1 sumando: x/3 = 4 + 1 -> x/3 = 5",
                            "2. Pasamos el 3 multiplicando: x = 5 * 3",
                            "3. Resultado: x = 15 ✓",
                            "",
                            "Ejemplo C (Fracción Directa): x/5 = 2",
                            "1. Multiplicamos directamente por 5: x = 2 * 5",
                            "2. Resultado: x = 10 ✓"
                    }
            );
            moduleDao.insertAll(Collections.singletonList(mod3));
            exerciseDao.insertAll(Arrays.asList(
                    buildBalanza(3,1,"x/2 + 1 = 5","8","x/2+1","5","-1","x/2","4",
                            new String[]{"-1","+1","×2","÷2"},
                            "Paso 1: Resta 1 → x/2 = 4\nPaso 2: Multiplica por 2 → x = 8 ✓"),
                    buildClasico(3, 2, "x/3 + 2 = 6", "12",
                            new String[]{
                                    "Transponer +2:",
                                    "x/3 = 6 - |2|",
                                    "x/3 = |4|",
                                    "Multiplicar por 3:",
                                    "x = 4 * |3|",
                                    "¡Listo! Ingresa el valor final de x abajo:"
                            },
                            "Paso 1: Transponer +2 → x/3 = 4\nPaso 2: Multiplicar por 3 → x = 12 ✓"),
                    buildTiles(3,3,"x/2 = 3","6",
                            new String[]{"x/2"},
                            new String[]{"+1","+1","+1"},
                            "El tile x/2 vale la mitad de x. Multiplica por 2. x = 6 ✓")
            ));

            // ── Módulo 4: Paréntesis ──
            Module mod4 = buildModule(4,
                    "Módulo 4: Paréntesis",
                    "a(x + b) = c · Propiedad Distributiva",
                    "Aprende a eliminar paréntesis multiplicando los términos internos.",
                    4, false,

                    // Bloque 1: Signos de Agrupación
                    "Los Paréntesis",
                    "En álgebra, los paréntesis indican que los términos en su interior forman un solo bloque. Para resolver estas ecuaciones, el primer paso indispensable es 'destruir' o eliminar los paréntesis para poder operar con los términos libremente.",

                    // Bloque 2: Propiedad Distributiva
                    "La Propiedad Distributiva",
                    "Si un número está pegado a un paréntesis, multiplica a cada uno de los elementos dentro. Por ejemplo: 2(x + 3) significa que tienes dos veces la x (2x) y dos veces el tres (6). ¡No olvides multiplicar ambos términos!",

                    // Bloque 3: Orden de Resolución
                    "Jerarquía de Despeje",
                    "El orden recomendado es: 1. Eliminar paréntesis (multiplicar). 2. Transponer los números al segundo miembro. 3. Reducir los términos semejantes. 4. Despejar la x dividiendo entre su coeficiente.",

                    // Ejemplos visuales de la sección de info (3 Ejemplos)
                    "2(x + 3) = 10\n" + "3(x - 2) = 9\n" + "4(x + 1) = 12",
                    new String[]{
                            "Ejemplo A (Suma interna): 2(x + 3) = 10",
                            "1. Multiplicamos 2*x y 2*3: 2x + 6 = 10",
                            "2. Pasamos el 6 restando: 2x = 10 - 6 -> 2x = 4",
                            "3. Dividimos: x = 4 / 2 = 2 ✓",
                            "",
                            "Ejemplo B (Resta interna): 3(x - 2) = 9",
                            "1. Multiplicamos 3*x y 3*(-2): 3x - 6 = 9",
                            "2. Pasamos el -6 sumando: 3x = 9 + 6 -> 3x = 15",
                            "3. Dividimos: x = 15 / 3 = 5 ✓",
                            "",
                            "Ejemplo C (Distribución rápida): 4(x + 1) = 12",
                            "1. Expandimos: 4x + 4 = 12",
                            "2. Transponemos y resolvemos: 4x = 8 -> x = 2 ✓"
                    }
            );

            moduleDao.insertAll(Collections.singletonList(mod4));
            exerciseDao.insertAll(Arrays.asList(
                    buildClasico(4, 1, "2(x + 3) = 10", "2",
                            new String[]{
                                    "Aplicar propiedad distributiva:",
                                    "2x + |6| = 10",
                                    "Pasar el 6 restando:",
                                    "2x = 10 - |6|",
                                    "2x = |4|",
                                    "Dividir entre 2:",
                                    "x = 4 / |2|",
                                    "¡Listo! Ingresa el valor final de x abajo:"
                            },
                            "Paso 1: 2*x + 2*3 = 2x+6.\nPaso 2: 10 - 6 = 4.\nPaso 3: 4 / 2 = 2."),
                    buildBalanza(4, 2, "3(x - 1) = 9", "4", "3x-3", "9", "+3", "3x", "12",
                            new String[]{"+3", "-3", "÷3", "×3"},
                            "Sugerencia: Al expandir 3(x-1) obtienes 3x y 3 pesas negativas (-3). ¡Suma 3 para eliminarlas!"),
                    buildTiles(4, 3, "2(x + 2) = 8", "2",
                            new String[]{"x", "x", "+1", "+1", "+1", "+1"}, // Ya expandido: 2x + 4
                            new String[]{"+1", "+1", "+1", "+1", "+1", "+1", "+1", "+1"},
                            "Tienes dos grupos de (x + 2). En total son 2x y 4 unidades. Quita las 4 unidades y divide lo que quede entre las dos x.")
            ));

            // ── Módulo 5: Variables en ambos lados ──
            Module mod5 = buildModule(5,
                    "Módulo 5: Variables a ambos lados",
                    "ax + b = cx + d · Agrupación",
                    "Aprende a reunir todas las x en un solo lado de la ecuación.",
                    5, false,

                    // Bloque 1: Agrupación de Términos
                    "Términos Semejantes",
                    "Para resolver estas ecuaciones se deben transponer todos los términos que contienen la incógnita al primer miembro (izquierda) y todas las cantidades conocidas al segundo miembro (derecha). Así, podemos reducirlos a un solo término con x.",

                    // Bloque 2: Transposición de la Incógnita
                    "Mover la x",
                    "La x se comporta como cualquier otro número en la transposición. Si tienes una '2x' sumando en la derecha, pasa al lado izquierdo restando (-2x). El objetivo es que todas las x se 'encuentren' en un solo plato de la balanza.",

                    // Bloque 3: Reducción Final
                    "Simplificación",
                    "Una vez agrupadas, sumamos o restamos los coeficientes de las x (ej: 5x - 2x = 3x). Después de esto, la ecuación se convierte en una de las formas simples que ya conoces de los módulos anteriores.",

                    // Ejemplos visuales de la sección de info (3 Ejemplos)
                    "3x + 2 = x + 10\n" + "5x - 4 = 2x + 5\n" + "4x = 2x + 6",
                    new String[]{
                            "Ejemplo A (Agrupación simple): 3x + 2 = x + 10",
                            "1. Pasamos la x restando a la izquierda: 3x - x + 2 = 10",
                            "2. Reducimos y movemos el 2: 2x = 10 - 2 -> 2x = 8",
                            "3. Resultado: x = 4 ✓",
                            "",
                            "Ejemplo B (Con números negativos): 5x - 4 = 2x + 5",
                            "1. Movemos 2x a la izquierda y -4 a la derecha: 5x - 2x = 5 + 4",
                            "2. Reducimos: 3x = 9",
                            "3. Resultado: x = 3 ✓",
                            "",
                            "Ejemplo C (Variables solas): 4x = 2x + 6",
                            "1. Restamos 2x en ambos miembros: 4x - 2x = 6",
                            "2. Reducimos: 2x = 6 -> x = 3 ✓"
                    }
            );

            moduleDao.insertAll(Collections.singletonList(mod5));
            exerciseDao.insertAll(Arrays.asList(
                    // Ejercicio 1: Clásico (Agrupación paso a paso)
                    buildClasico(5, 1, "3x + 2 = x + 10", "4",
                            new String[]{
                                    "Pasamos la x al lado izquierdo restando:",
                                    "3x - |x| + 2 = 10",
                                    "Reducimos términos semejantes (3x - x):",
                                    "|2x| + 2 = 10",
                                    "Pasamos el 2 restando y despejamos:",
                                    "2x = |8|",
                                    "Ingresa el valor final de x abajo:"
                            },
                            "Paso 1: 3x - x = 2x.\nPaso 2: 10 - 2 = 8.\nPaso 3: 8 / 2 = 4."),

                    // Ejercicio 2: Balanza (Eliminación mutua)
                    buildBalanza(5, 2, "4x = 2x + 6", "3", "4x", "2x+6", "-2x", "2x", "6",
                            new String[]{"-2x", "+2x", "÷2", "×2"},
                            "Sugerencia: Tienes x en ambos lados. Si quitas 2 cajas (2x) de cada plato, la balanza seguirá en equilibrio."),

                    // Ejercicio 3: Tiles (Cancelación de variables)
                    buildTiles(5, 3, "2x + 3 = x + 5", "2",
                            new String[]{"x", "x", "+1", "+1", "+1"}, // 2x + 3
                            new String[]{"x", "+1", "+1", "+1", "+1", "+1"}, // x + 5
                            "Quita una 'x' de cada lado. Luego quita las 3 unidades del lado izquierdo. ¿Cuántas unidades quedan para la x solitaria?")
            ));


            Module mod6 = buildModule(6,
                    "Módulo 6: Caso General",
                    "a(x + b) = cx + d · El gran cierre",
                    "Domina la resolución de ecuaciones complejas integrando todo lo aprendido.",
                    6, false,

                    // Bloque 1: El Orden de Baldor
                    "El Método Universal",
                    "Para resolver las ecuaciones más complejas: 1. Suprimir signos de agrupación (paréntesis). 2. Reducir términos semejantes en cada miembro. 3. Transponer términos para agrupar las 'x' a la izquierda. 4. Reducir nuevamente y despejar.",

                    // Bloque 2: Verificación de la Igualdad
                    "La Prueba del Resultado",
                    "Una vez hallado el valor de x, es fundamental la 'Verificación'. Consiste en sustituir la x por el número obtenido en la ecuación original. Si ambos miembros resultan en el mismo valor (ej. 15 = 15), la ecuación está correctamente resuelta.",

                    // Bloque 3: Dominio del Álgebra
                    "Estrategia de Agrupación",
                    "Al tener x en ambos lados, siempre busca mover la x de menor valor hacia donde está la de mayor valor para trabajar con números positivos. Recuerda que al saltar el signo '=', la operación cambia radicalmente.",

                    // Ejemplos visuales de la sección de info (3 Ejemplos combinados)
                    "2(x + 3) = x + 9\n" + "3(x - 1) = 2x + 5\n" + "4(x - 2) = 2x",
                    new String[]{
                            "Ejemplo A (Expansión y Agregación): 2(x + 3) = x + 9",
                            "1. Expandimos: 2x + 6 = x + 9",
                            "2. Agrupamos x: 2x - x = 9 - 6",
                            "3. Resultado: x = 3 ✓",
                            "",
                            "Ejemplo B (Combinado): 3(x - 1) = 2x + 5",
                            "1. Expandimos: 3x - 3 = 2x + 5",
                            "2. Agrupamos: 3x - 2x = 5 + 3",
                            "3. Resultado: x = 8 ✓",
                            "",
                            "Ejemplo C (Variables solas): 4(x - 2) = 2x",
                            "1. Expandimos: 4x - 8 = 2x",
                            "2. Agrupamos: 4x - 2x = 8 -> 2x = 8",
                            "3. Resultado: x = 4 ✓"
                    }
            );

            moduleDao.insertAll(Collections.singletonList(mod6));
            exerciseDao.insertAll(Arrays.asList(
                    // Ejercicio 1: Clásico (El proceso completo)
                    buildClasico(6, 1, "2(x + 3) = x + 9", "3",
                            new String[]{
                                    "Primero, expande el paréntesis multiplicando el 2:",
                                    "|2x| + 6 = x + 9",
                                    "Ahora, agrupa las x en la izquierda (resta x):",
                                    "x + 6 = |9|",
                                    "Finalmente, resta el 6 para despejar x:",
                                    "x = 9 - |6|",
                                    "Ingresa el valor final de x abajo:"
                            },
                            "Paso 1: 2*x + 2*3. Paso 2: 2x - x = x. Paso 3: 9 - 6 = 3."),

                    // Ejercicio 2: Balanza (Visualización de equilibrio dinámico)
                    buildBalanza(6, 2, "3x + 4 = 2x + 10", "6", "3x+4", "2x+10", "-2x", "x+4", "10",
                            new String[]{"-2x", "-4", "+2x", "+4", "÷2"},
                            "Sugerencia: Quita 2 cajas (2x) de cada plato primero para simplificar la balanza."),

                    // Ejercicio 3: Tiles (Manipulación de grupos y negativos)
                    buildTiles(6, 3, "2(x + 1) = x + 5", "3",
                            new String[]{"x", "x", "+1", "+1"}, // Expandido: 2x + 2
                            new String[]{"x", "+1", "+1", "+1", "+1", "+1"}, // x + 5
                            "Suprime una 'x' de cada lado y luego las dos unidades del lado izquierdo. ¿Cuánto queda?")
            ));
        });
    }

    private static Module buildModule(int id, String name, String subtitle, String desc, int order, boolean unlocked,
                                      String it1, String ix1, String it2, String ix2, String it3, String ix3,
                                      String eq, String[] steps) {
        Module m = new Module();
        m.id = id; m.name = name; m.subtitle = subtitle; m.description = desc; m.orderIndex = order;
        m.unlocked = unlocked; m.infoTitle1 = it1; m.infoText1 = ix1;
        m.infoTitle2 = it2; m.infoText2 = ix2; m.infoTitle3 = it3; m.infoText3 = ix3;
        m.exampleEquation = eq; m.exampleSteps = toJson(steps);
        return m;
    }

    private static Exercise buildBalanza(int moduleId, int step, String eq, String answer, String lhs, String rhs, String correctOp, String lhsAfter, String rhsAfter, String[] ops, String hint) {
        Exercise e = new Exercise();
        e.moduleId = moduleId; e.stepOrder = step; e.type = Exercise.TYPE_BALANZA;
        e.equation = eq; e.equationObj = ParserEcuacion.parsear(eq); e.correctAnswer = answer; e.hintText = hint;
        e.lhsExpr = lhs; e.rhsExpr = rhs; e.correctOp = correctOp; e.lhsAfterOp = lhsAfter; e.rhsAfterOp = rhsAfter;
        e.ops = toJson(ops);
        return e;
    }

    private static Exercise buildClasico(int moduleId, int step, String eq, String answer, String[] steps, String hint) {
        Exercise e = new Exercise();
        e.moduleId = moduleId; e.stepOrder = step; e.type = Exercise.TYPE_CLASICO;
        e.equation = eq; e.equationObj = ParserEcuacion.parsear(eq); e.correctAnswer = answer; e.hintText = hint;
        e.solutionSteps = toJson(steps);
        return e;
    }

    private static Exercise buildTiles(int moduleId, int step, String eq, String answer, String[] left, String[] right, String hint) {
        Exercise e = new Exercise();
        e.moduleId = moduleId; e.stepOrder = step; e.type = Exercise.TYPE_TILES;
        e.equation = eq; e.equationObj = ParserEcuacion.parsear(eq); e.correctAnswer = answer; e.hintText = hint;
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
