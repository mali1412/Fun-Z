package mx.unam.fc.icat.funz.utils;

import android.app.Activity;
import android.content.Intent;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.ui.main.MainActivity;
import mx.unam.fc.icat.funz.ui.temas.TemasActivity;
import mx.unam.fc.icat.funz.ui.sala.SalasActivity;
import mx.unam.fc.icat.funz.ui.stats.EstadisticasActivity;
import mx.unam.fc.icat.funz.ui.config.ConfiguracionActivity;

public class NavigationUtils {

    public static void setupBottomNavigation(Activity activity, BottomNavigationView nav, int selectedId) {
        nav.setSelectedItemId(selectedId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedId) return true;

            Intent intent = null;
            if (id == R.id.nav_inicio) intent = new Intent(activity, MainActivity.class);
            else if (id == R.id.nav_temas) intent = new Intent(activity, TemasActivity.class);
            else if (id == R.id.nav_salas) intent = new Intent(activity, SalasActivity.class);
            else if (id == R.id.nav_stats) intent = new Intent(activity, EstadisticasActivity.class);
            else if (id == R.id.nav_config) intent = new Intent(activity, ConfiguracionActivity.class);

            if (intent != null) {
                activity.startActivity(intent);
                if (id == R.id.nav_inicio) activity.finish(); // MainActivity suele ser el top de la pila
                return true;
            }
            return false;
        });
    }
}