package com.example.ferme_intelligente.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ferme_intelligente.Activities.EditPlante;
import com.example.ferme_intelligente.Activities.RecommendationActivity;
import com.example.ferme_intelligente.Model.Plante;
import com.example.ferme_intelligente.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PlanteAdapter extends RecyclerView.Adapter<PlanteAdapter.PlanteViewHolder> {

    private Context context;
    private List<Plante> planteList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public PlanteAdapter(Context context, List<Plante> planteList) {
        this.context = context;
        this.planteList = planteList;
    }

    @NonNull
    @Override
    public PlanteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_plante, parent, false);
        return new PlanteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanteViewHolder holder, int position) {
        Plante plante = planteList.get(position);
        Log.d("PlanteAdapter", "Affichage de la plante : " + plante.getNom());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        holder.nom.setText(plante.getNom());
        holder.description.setText(plante.getDescription());
        holder.periodePlantation.setText(plante.getPeriodePlantation());
        holder.type.setText(plante.getType());

        // Bouton modifier
        holder.btnModifier.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                String emailFermier = user.getEmail();
                if (emailFermier != null) {
                    Intent intent = new Intent(context, EditPlante.class);
                    intent.putExtra("fermierId", emailFermier);
                    intent.putExtra("planteId", plante.getId());
                    intent.putExtra("nomPlante", plante.getNom());
                    intent.putExtra("descriptionPlante", plante.getDescription());
                    intent.putExtra("periodePlantationPlante", plante.getPeriodePlantation());
                    intent.putExtra("typePlante", plante.getType());
                    intent.putExtra("imagePlante", plante.getImage());

                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Erreur : Email du fermier introuvable", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Erreur : Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            }
        });

        // Bouton supprimer
        String planteId = plante.getId();
        holder.btnSupprimer.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Confirmation")
                    .setMessage("Voulez-vous vraiment supprimer cette plante ?")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        FirebaseUser user = auth.getCurrentUser();
                        String emailFermier = user != null ? user.getEmail() : null;

                        if (emailFermier != null && planteId != null) {
                            supprimerPlante(emailFermier, planteId);
                        } else {
                            Log.e("PlanteAdapter", "Impossible de supprimer, ID null !");
                        }
                    })
                    .setNegativeButton("Non", null)
                    .show();

            if (planteId == null || planteId.isEmpty()) {
                Toast.makeText(context, "Erreur : ID de la plante invalide", Toast.LENGTH_SHORT).show();
            }
        });

        // Bouton Consulter État
        holder.btnConsulterEtat.setOnClickListener(v -> {
            Log.d("PlanteAdapter", "Consultation état de la plante : " + plante.getId());

            Intent intent = new Intent(context, RecommendationActivity.class);
            intent.putExtra("planteId", plante.getId()); // facultatif
            context.startActivity(intent);
        });

        // Chargement de l'image
        int imageResource = context.getResources().getIdentifier(plante.getImage(), "mipmap", context.getPackageName());
        if (imageResource != 0) {
            Glide.with(context)
                    .load(imageResource)
                    .placeholder(R.mipmap.plante1)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.mipmap.plante1);
        }
    }

    @Override
    public int getItemCount() {
        return planteList.size();
    }

    public static class PlanteViewHolder extends RecyclerView.ViewHolder {
        TextView nom, description, periodePlantation, type;
        ImageView imageView;
        Button btnModifier, btnSupprimer, btnConsulterEtat;

        public PlanteViewHolder(@NonNull View itemView) {
            super(itemView);
            nom = itemView.findViewById(R.id.tvNom);
            description = itemView.findViewById(R.id.tvDescription);
            periodePlantation = itemView.findViewById(R.id.tvPeriodePlantation);
            type = itemView.findViewById(R.id.tvType);
            imageView = itemView.findViewById(R.id.ivPlante);
            btnModifier = itemView.findViewById(R.id.btnModifier);
            btnSupprimer = itemView.findViewById(R.id.btnSupprimer);
            btnConsulterEtat = itemView.findViewById(R.id.btnConsulterEtat); // Nouveau bouton
        }
    }

    public void supprimerPlante(String idFermier, String idPlante) {
        if (idFermier == null || idFermier.isEmpty() || idPlante == null || idPlante.isEmpty()) {
            Log.e("PlanteAdapter", "Erreur : ID(s) null ou vide !");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("fermiers")
                .document(idFermier)
                .collection("plantes")
                .document(idPlante)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("PlanteAdapter", "Plante supprimée avec succès !"))
                .addOnFailureListener(e -> Log.e("PlanteAdapter", "Erreur lors de la suppression", e));
    }
}
