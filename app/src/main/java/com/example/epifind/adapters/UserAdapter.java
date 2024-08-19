package com.example.epifind.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.epifind.R;
import com.example.epifind.models.UserProfile;

import java.util.List;

/**
 * UserAdapter is a RecyclerView.Adapter that displays a list of UserProfiles.
 * It binds each UserProfile to a UserViewHolder, which displays the user's name and status.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<UserProfile> users;

    /**
     * Constructor for the UserAdapter.
     *
     * @param users The list of UserProfiles to be displayed.
     */
    public UserAdapter(List<UserProfile> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserProfile user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * UserViewHolder is a RecyclerView.ViewHolder that represents a single list item.
     * It displays the user's name and their current response status.
     */
    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView statusTextView;

        /**
         * Constructor for the UserViewHolder.
         *
         * @param itemView The view corresponding to the list item.
         */
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }

        /**
         * Binds a UserProfile to the ViewHolder, updating the UI elements with the user's data.
         *
         * @param user The UserProfile to bind to the ViewHolder.
         */
        void bind(UserProfile user) {
            nameTextView.setText(user.getName());
            statusTextView.setText(getStatusString(user.getResponseStatus()));
        }

        /**
         * Converts the UserProfile.ResponseStatus enum into a readable string.
         *
         * @param status The response status to convert.
         * @return A string representing the response status.
         */
        private String getStatusString(UserProfile.ResponseStatus status) {
            switch (status) {
                case RESPONDING:
                    return "Coming!";
                case UNAVAILABLE:
                    return "Unavailable";
                case AVAILABLE:
                default:
                    return "Available";
            }
        }
    }
}
