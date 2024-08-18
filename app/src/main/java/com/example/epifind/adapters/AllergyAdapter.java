package com.example.epifind.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.widget.ArrayAdapter;

import com.example.epifind.R;

import java.util.List;

public class AllergyAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> allergies;
    private final boolean[] selectedAllergies;

    public AllergyAdapter(@NonNull Context context, @NonNull List<String> allergies, boolean[] selectedAllergies) {
        super(context, R.layout.item_allergy, allergies);
        this.context = context;
        this.allergies = allergies;
        this.selectedAllergies = selectedAllergies;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_allergy, parent, false);
        }

        String allergy = allergies.get(position);
        CheckedTextView textView = convertView.findViewById(R.id.checkedTextView);
        ImageView imageView = convertView.findViewById(R.id.imageView);

        textView.setText(allergy);

        Drawable icon = null;
        switch (allergy) {
            case "Peanuts":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_allergy_peanuts);
                break;
            case "Tree nuts":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_allergy_tree_nuts);
                break;
            case "Milk":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_allergy_milk);
                break;
            case "Eggs":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_allergy_eggs);
                break;
            case "Wheat":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_allergy_wheat);
                break;
            case "Soy":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_allergy_soy);
                break;
            case "Fish":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_allergy_fish);
                break;
            case "Shellfish":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_allergy_shellfish);
                break;
        }

        if (icon != null) {
            imageView.setImageDrawable(icon);
        }

        // עדכון מצב הבחירה
        textView.setChecked(selectedAllergies[position]);

        // מאזין ללחיצות על הפריט
        convertView.setOnClickListener(v -> {
            selectedAllergies[position] = !selectedAllergies[position];
            textView.setChecked(selectedAllergies[position]);
        });

        return convertView;
    }
}
