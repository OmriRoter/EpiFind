package com.example.epifind.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.epifind.R;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * AllergyAdapter is a custom ArrayAdapter used to display a list of allergies in a ListView.
 * It handles the display of allergy icons and the selection state of each allergy item.
 */
public class AllergyAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> allergies;
    private final boolean[] selectedAllergies;
    private final Map<String, Integer> allergyIcons;

    /**
     * Constructor for the AllergyAdapter.
     *
     * @param context          The current context.
     * @param allergies        The list of allergies to display.
     * @param selectedAllergies A boolean array indicating which allergies are selected.
     */
    public AllergyAdapter(@NonNull Context context, @NonNull List<String> allergies, boolean[] selectedAllergies) {
        super(context, R.layout.item_allergy, allergies);
        this.context = context;
        this.allergies = allergies;
        this.selectedAllergies = selectedAllergies;
        this.allergyIcons = initAllergyIcons();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_allergy, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String allergy = allergies.get(position);
        holder.textView.setText(allergy);

        setAllergyIcon(holder.imageView, allergy);
        updateSelectionState(holder.textView, position);
        setupClickListener(convertView, holder.textView, position);

        return convertView;
    }

    /**
     * Sets the appropriate icon for a given allergy.
     *
     * @param imageView The ImageView where the icon should be set.
     * @param allergy   The name of the allergy.
     */
    private void setAllergyIcon(ImageView imageView, String allergy) {
        Integer iconResId = allergyIcons.get(allergy);
        if (iconResId != null) {
            Drawable icon = ContextCompat.getDrawable(context, iconResId);
            imageView.setImageDrawable(icon);
        }
    }

    /**
     * Updates the selection state of a CheckedTextView based on its position.
     *
     * @param textView The CheckedTextView to update.
     * @param position The position of the item in the list.
     */
    private void updateSelectionState(CheckedTextView textView, int position) {
        textView.setChecked(selectedAllergies[position]);
    }

    /**
     * Sets up a click listener for each item in the list to toggle the selection state.
     *
     * @param convertView The view corresponding to the list item.
     * @param textView    The CheckedTextView to update on click.
     * @param position    The position of the item in the list.
     */
    private void setupClickListener(View convertView, CheckedTextView textView, int position) {
        convertView.setOnClickListener(v -> {
            selectedAllergies[position] = !selectedAllergies[position];
            textView.setChecked(selectedAllergies[position]);
        });
    }

    /**
     * Initializes a mapping of allergies to their corresponding icons.
     *
     * @return A map where the key is the allergy name and the value is the resource ID of the icon.
     */
    private Map<String, Integer> initAllergyIcons() {
        Map<String, Integer> icons = new HashMap<>();
        icons.put("Peanuts", R.drawable.ic_allergy_peanuts);
        icons.put("Tree nuts", R.drawable.ic_allergy_tree_nuts);
        icons.put("Milk", R.drawable.ic_allergy_milk);
        icons.put("Eggs", R.drawable.ic_allergy_eggs);
        icons.put("Wheat", R.drawable.ic_allergy_wheat);
        icons.put("Soy", R.drawable.ic_allergy_soy);
        icons.put("Fish", R.drawable.ic_allergy_fish);
        icons.put("Shellfish", R.drawable.ic_allergy_shellfish);
        return icons;
    }

    /**
     * ViewHolder class to hold the views for each list item.
     */
    private static class ViewHolder {
        final CheckedTextView textView;
        final ImageView imageView;

        ViewHolder(View view) {
            textView = view.findViewById(R.id.checkedTextView);
            imageView = view.findViewById(R.id.imageView);
        }
    }
}
