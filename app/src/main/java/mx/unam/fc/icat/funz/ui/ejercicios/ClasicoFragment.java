package mx.unam.fc.icat.funz.ui.ejercicios;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import mx.unam.fc.icat.funz.R;
import mx.unam.fc.icat.funz.db.Exercise;
import mx.unam.fc.icat.funz.viewmodel.ExerciseViewModel;

public class ClasicoFragment extends Fragment {

    private ExerciseViewModel vm;
    private LinearLayout llSteps;
    private TextView tvEquation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_exercise_clasico, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(ExerciseViewModel.class);
        llSteps = view.findViewById(R.id.ll_solution_steps);
        tvEquation = view.findViewById(R.id.tv_equation_display);

        vm.exercise.observe(getViewLifecycleOwner(), exercise -> {
            if (exercise != null && Exercise.TYPE_CLASICO.equals(exercise.type)) {
                tvEquation.setText(exercise.equation);
                List<String> steps = ExerciseViewModel.parseJson(exercise.solutionSteps);
                llSteps.removeAllViews();
                mostrarSiguientePaso(steps, 0);
            }
        });
    }

    private void mostrarSiguientePaso(List<String> steps, int index) {
        if (index >= steps.size()) return;

        String stepText = steps.get(index);
        if (index == steps.size() - 1) {
            // El último paso suele ser el resultado final que se pone en el etAnswer de la Activity
            // Podríamos disparar un evento o simplemente dejar que el usuario lo escriba abajo.
            return;
        }

        if (!stepText.contains("|")) {
            TextView tvInstruction = new TextView(requireContext());
            tvInstruction.setText(stepText);
            tvInstruction.setPadding(dimenPx(R.dimen.margin_small), dimenPx(R.dimen.margin_small), dimenPx(R.dimen.margin_small), dimenPx(R.dimen.margin_tiny));
            tvInstruction.setTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            llSteps.addView(tvInstruction);
            mostrarSiguientePaso(steps, index + 1);
            return;
        }

        String[] parts = stepText.split("\\|");
        View stepView = getLayoutInflater().inflate(R.layout.item_step_clasico, llSteps, false);
        TextView tvPre = stepView.findViewById(R.id.tv_step_prefix);
        EditText etInput = stepView.findViewById(R.id.et_step_input);
        TextView tvPost = stepView.findViewById(R.id.tv_step_suffix);
        Button btnCheck = stepView.findViewById(R.id.btn_step_verify);

        tvPre.setText(parts[0]);
        if (parts.length > 2) tvPost.setText(parts[2]);

        String correctAnswer = parts[1];

        btnCheck.setOnClickListener(v -> {
            if (etInput.getText().toString().trim().equals(correctAnswer)) {
                // Feedback táctil y sonoro se maneja mejor en la Activity o via VM si es común
                etInput.setEnabled(false);
                etInput.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.success_bg)));
                btnCheck.setVisibility(View.GONE);
                tvPre.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green));
                mostrarSiguientePaso(steps, index + 1);
            } else {
                etInput.setError(getString(R.string.input_error_incorrect));
                etInput.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.shake));
            }
        });

        llSteps.addView(stepView);
    }

    private int dimenPx(int resId) { return getResources().getDimensionPixelSize(resId); }
    private int resolveThemeColor(int attr) { TypedValue tv = new TypedValue(); requireActivity().getTheme().resolveAttribute(attr, tv, true); return tv.data; }
}
