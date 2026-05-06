package mx.unam.fc.icat.funz.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * FunZDatabase — base de datos Room de la aplicación.
 */
@Database(
    entities  = { Module.class, Exercise.class },
    version   = 4,
    exportSchema = false
)
@TypeConverters({ Converters.class })
public abstract class FunZDatabase extends RoomDatabase {

    public abstract ModuleDao   moduleDao();
    public abstract ExerciseDao exerciseDao();

    private static volatile FunZDatabase INSTANCE;

    public static FunZDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FunZDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            FunZDatabase.class,
                            "funz_db"
                    )
                    .fallbackToDestructiveMigration() // Para desarrollo, recrea la DB al cambiar versión
                    .addCallback(new DbSeeder())
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
