package com.example.ferme_intelligente.Adapter;

import android.graphics.Color;
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

    private final List<Alerte> alertes;

    public AlertesAdapter(List<Alerte> alertes) {
        this.alertes = alertes;
    }

    @NonNull
    @Override
    public AlerteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alerte, parent, false);
        return new AlerteViewHolder(view);
    }

    public static class AlerteViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public TextView dateText;
        public View containerLayout; // <-- ajouter

        public AlerteViewHolder(View view) {
            super(view);
            messageText = view.findViewById(R.id.textMessage);
            dateText = view.findViewById(R.id.textDate);
            containerLayout = view.findViewById(R.id.containerLayout);  // <-- récupérer
        }
    }

    @Override
    public void onBindViewHolder(@NonNull AlerteViewHolder holder, int position) {
        Alerte alerte = alertes.get(position);

        holder.messageText.setText(alerte.getFormattedMessage());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.dateText.setText(sdf.format(new Date(alerte.getTimestamp())));

        // Reset couleurs pour éviter effets recyclage
        holder.containerLayout.setBackgroundColor(Color.WHITE);
        holder.messageText.setTextColor(Color.BLACK);
        holder.dateText.setTextColor(Color.DKGRAY);

        switch (alerte.getType()) {
            case "temperature":
                holder.containerLayout.setBackgroundResource(R.color.alerte_temperature);
                holder.messageText.setTextColor(Color.RED);   // Ex. texte rouge
                break;
            case "maladie":
                holder.containerLayout.setBackgroundResource(R.color.alerte_maladie);
                holder.messageText.setTextColor(Color.RED);
                break;
            case "pompe":
                holder.containerLayout.setBackgroundResource(R.color.alerte_pompe);
                holder.messageText.setTextColor(Color.BLUE);
                break;
        }
    }


    @Override
    public int getItemCount() {
        return alertes.size();
    }


}