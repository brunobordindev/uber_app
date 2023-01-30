package br.com.uber.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.picasso.Picasso;

import java.util.List;

import br.com.uber.R;
import br.com.uber.model.Local;
import br.com.uber.model.Requisicao;
import br.com.uber.model.Usuario;
import de.hdodenhof.circleimageview.CircleImageView;

public class RequisicoesAdapter extends RecyclerView.Adapter<RequisicoesAdapter.MyViewHolder> {

    private List<Requisicao> requisicoes;
    private Context context;
    private Usuario motorista;

    public RequisicoesAdapter(List<Requisicao> requisicaos, Context context, Usuario motorista) {
        this.requisicoes = requisicaos;
        this.context = context;
        this.motorista = motorista;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_requisicoes, parent, false);
        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Requisicao requisicao = requisicoes.get(position);
        Usuario passageiro = requisicao.getPassageiro();

        holder.nome.setText(passageiro.getNome());

        if (passageiro.getFoto() != null){

            Uri uri = Uri.parse(passageiro.getFoto());
            Glide.with(context).load(uri).into(holder.foto);

        }else{
            holder.foto.setImageResource(R.drawable.person);
        }

        if (motorista != null){

            LatLng localPassageiro = new LatLng(
                    Double.parseDouble(passageiro.getLatitude()),
                    Double.parseDouble(passageiro.getLongitude())
            );

            LatLng localMotorista = new LatLng(
                    Double.parseDouble(motorista.getLatitude()),
                    Double.parseDouble(motorista.getLongitude())
            );

            float distancia = Local.calcularDistancia(localPassageiro, localMotorista);
            String distanciaFormatada  = Local.formarDistancia(distancia);

            holder.distancia.setText(distanciaFormatada + " - aproximadamente");

        }

    }

    @Override
    public int getItemCount() {
        return requisicoes.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView nome, distancia;
        CircleImageView foto;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            nome = itemView.findViewById(R.id.text_requisicao_nome);
            distancia = itemView.findViewById(R.id.text_requisicao_distancia);
            foto = itemView.findViewById(R.id.edit_img_requisicao);
        }
    }
}
