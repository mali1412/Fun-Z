package mx.unam.fc.icat.funz.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * ModuleDao — acceso a datos para la tabla 'modules'.
 */
@Dao
public interface ModuleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Module> modules);

    @Update
    void update(Module module);

    /** Todos los módulos en orden, para la pantalla TemasActivity. */
    @Query("SELECT * FROM modules ORDER BY order_index ASC")
    LiveData<List<Module>> getAllModules();

    /** Síncrono — usado por el seeder. */
    @Query("SELECT COUNT(*) FROM modules")
    int count();

    /** Desbloquear el siguiente módulo por ID. */
    @Query("UPDATE modules SET unlocked = 1 WHERE id = :moduleId")
    void unlock(int moduleId);

    @Query("SELECT * FROM modules WHERE id = :moduleId LIMIT 1")
    Module getModuleSync(int moduleId);
}
