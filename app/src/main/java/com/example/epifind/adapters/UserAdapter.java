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

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<UserProfile> users;

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
        holder.nameTextView.setText(user.getName());
        holder.statusTextView.setText(getStatusString(user.getResponseStatus()));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

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

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView statusTextView;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }
    }
}