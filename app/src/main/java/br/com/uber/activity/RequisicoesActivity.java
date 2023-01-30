package br.com.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import br.com.uber.R;
import br.com.uber.adapter.RequisicoesAdapter;
import br.com.uber.databinding.ActivityRequisicoesBinding;
import br.com.uber.helper.ConfiguracaoFirebase;
import br.com.uber.helper.RecyclerItemClickListener;
import br.com.uber.helper.UsuarioFirebase;
import br.com.uber.model.Requisicao;
import br.com.uber.model.Usuario;

public class RequisicoesActivity extends AppCompatActivity {

    private ActivityRequisicoesBinding binding;
    private FirebaseAuth autenticacao;
    private DatabaseReference firebaseRef;
    private List<Requisicao> listaRequisicoes = new ArrayList<>();
    private RequisicoesAdapter adapter;
    private Usuario motorista;

    private LocationManager locationManager;
    private LocationListener locationListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_requisicoes);

        inicializarComponentes();

        //recuperar localizacao do usuario
        recuperarLocalizacaoUsuario();

    }

    @Override
    protected void onStart() {
        super.onStart();
        verificaStatusRequisicao();
    }


    private void verificaStatusRequisicao(){

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        DatabaseReference requisicoes = firebaseRef.child("requisicoes");

        Query requisicoesPesquisa = requisicoes.orderByChild("motorista/id")
                .equalTo(usuarioLogado.getId());

        requisicoesPesquisa.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){

                    Requisicao requisicao = ds.getValue(Requisicao.class);

                    if (requisicao.getStatus().equals(Requisicao.STATUS_A_CAMINHO)
                            || requisicao.getStatus().equals(Requisicao.STATUS_VIAGEM)
                            || requisicao.getStatus().equals(Requisicao.STATUS_FINALIZADA)
                    || requisicao.getStatus().equals(Requisicao.STATUS_CANCELADA)){

                        motorista = requisicao.getMotorista();
                        abrirTelaCorridaActivity(requisicao.getId(), motorista, true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

                //recuperar laritude e Longitude
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());

                //Atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao(location.getLatitude(), location.getLongitude());

                if (!latitude.isEmpty() && !longitude.isEmpty()){
                    motorista.setLatitude(latitude);
                    motorista.setLongitude(longitude);

                    adicionaEventoDeCliqueRecycler();
                    locationManager.removeUpdates(locationListener);
                    adapter.notifyDataSetChanged();
                }

            }
        };

        //Solicitar atualizacoes de localizacao
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_motorista, menu);;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case  R.id.menu_sair:
                autenticacao.signOut();
                finish();
                break;
            case  R.id.menu_configuracao:
                startActivity(new Intent(RequisicoesActivity.this, ConfiguracaoMotoristaActivity.class) );
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void abrirTelaCorridaActivity( String idRequisicao, Usuario motorista, boolean requisicaoAtiva){

        Intent intent = new Intent(RequisicoesActivity.this, CorridaActivity.class);
        intent.putExtra("idRequisicao", idRequisicao);
        intent.putExtra("motorista", motorista);
        intent.putExtra("requisicaoAtiva", requisicaoAtiva);
        startActivity(intent);

    }

    private void inicializarComponentes() {

        getSupportActionBar().setTitle("Requisições");

        //configuracoes iniciais
        motorista = UsuarioFirebase.getDadosUsuarioLogado();
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        //RecyclerView Configuracoes
        adapter = new RequisicoesAdapter(listaRequisicoes, getApplicationContext(), motorista);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        binding.recyclerRequisicoes.setLayoutManager(layoutManager);
        binding.recyclerRequisicoes.setHasFixedSize(true);
        binding.recyclerRequisicoes.setAdapter(adapter);
        
        recuperarRequisicoes();
    }

    private void adicionaEventoDeCliqueRecycler(){

        //Adiciona evento de clique no recycler
        binding.recyclerRequisicoes.addOnItemTouchListener(new RecyclerItemClickListener(
                getApplicationContext(),
                binding.recyclerRequisicoes,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                        Requisicao requisicao = listaRequisicoes.get(position);
                        abrirTelaCorridaActivity(requisicao.getId(), motorista, false);

                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    }
                }

        ));
    }

    private void recuperarRequisicoes() {

        DatabaseReference requisicoes = firebaseRef.child("requisicoes");

        Query requisicaoPesquisa = requisicoes.orderByChild("status")
                .equalTo(Requisicao.STATUS_AGUARDANDO);
        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.getChildrenCount() > 0){
                    binding.textResultado.setVisibility(View.GONE);
                    binding.recyclerRequisicoes.setVisibility(View.VISIBLE);
                }else{
                    binding.textResultado.setVisibility(View.VISIBLE);
                    binding.recyclerRequisicoes.setVisibility(View.GONE);
                }

                listaRequisicoes.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    Requisicao requisicao = ds.getValue(Requisicao.class);
                    listaRequisicoes.add(requisicao);
                }

                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}