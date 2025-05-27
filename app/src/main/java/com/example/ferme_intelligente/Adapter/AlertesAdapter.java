package com.example.ferme_intelligente.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ferme_intelligente.Model.Alerte;
import com.example.ferme_intelligente.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertesAdapter extends RecyclerView.Adapter<AlertesAdapter.AlerteViewHolder> {
    private List<Alerte> alertes;

    public AlertesAdapter(List<Alerte> alertes) {
        this.alertes = alertes;
    }

    @NonNull
    @Override
    public AlerteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alerte, parent, false);
        return new AlerteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlerteViewHolder holder, int position) {
        Alerte alerte = alertes.get(position);
        holder.messageText.setText(alerte.getMessage());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(alerte.getCollection_time()));
        holder.dateText.setText(dateStr);
    }

    @Override
    public int getItemCount() {
        return alertes.size();
    }

    public static class AlerteViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, dateText;

        public AlerteViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textMessage);
            dateText = itemView.findViewById(R.id.textDate);
        }
    }
}

