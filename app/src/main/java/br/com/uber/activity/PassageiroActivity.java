package br.com.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import br.com.uber.R;
import br.com.uber.databinding.ActivityPassageiroBinding;
import br.com.uber.helper.ConfiguracaoFirebase;
import br.com.uber.helper.UsuarioFirebase;
import br.com.uber.model.Destino;
import br.com.uber.model.Local;
import br.com.uber.model.Requisicao;
import br.com.uber.model.Usuario;

public class PassageiroActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityPassageiroBinding binding;
    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;
    private LatLng localMotorista;
    private boolean cancelarUber = false;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;
    private Usuario passageiro;
    private Usuario motorista;
    private String statusRequisicao;
    private Destino destino;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_passageiro);

        inicializarComponentes();

        //Adiciona um listener para o status da requisiao
        verificaStatusRequisicao();

        binding.btnChamarUber.setOnClickListener(view -> {

            //false o uber nao pode ser cancelado ainda
            //true o uber pode ser cancelado

            if (cancelarUber){//uber pode ser cancelado

                //cancelar requisicao
                requisicao.setStatus(Requisicao.STATUS_CANCELADA);
                requisicao.atualizarStatus();

            }else{

                String enderecoDestino = binding.editDestino.getText().toString();

                if( !enderecoDestino.equals("") || enderecoDestino != null ){

                    Address addressDestino = recuperarEndereco( enderecoDestino );
                    if( addressDestino != null ){

                        final Destino destino = new Destino();
                        destino.setCidade( addressDestino.getSubAdminArea() );
                        destino.setCep( addressDestino.getPostalCode() );
                        destino.setBairro( addressDestino.getSubLocality() );
                        destino.setRua( addressDestino.getThoroughfare() );
                        destino.setNumero( addressDestino.getFeatureName() );
                        destino.setLatitude( String.valueOf(addressDestino.getLatitude()) );
                        destino.setLongitude( String.valueOf(addressDestino.getLongitude()) );

                        StringBuilder mensagem = new StringBuilder();
                        mensagem.append( "Cidade: " + destino.getCidade() );
                        mensagem.append( "\nRua: " + destino.getRua() );
                        mensagem.append( "\nBairro: " + destino.getBairro() );
                        mensagem.append( "\nNúmero: " + destino.getNumero() );
                        mensagem.append( "\nCep: " + destino.getCep() );

                        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                .setTitle("Confirme seu endereco!")
                                .setMessage(mensagem)
                                .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        //salvar requisição
                                        salvarRequisicao(destino);

                                    }
                                }).setNegativeButton("cancelar", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();

                    }

                }else {
                    Toast.makeText(this,
                            "Informe o endereço de destino!",
                            Toast.LENGTH_SHORT).show();
                }
            }

        });
    }

    private void verificaStatusRequisicao() {

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo(usuarioLogado.getId());
        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Requisicao> lista = new ArrayList<>();

                for (DataSnapshot ds: snapshot.getChildren()){
                    lista.add(ds.getValue(Requisicao.class));
                }

                Collections.reverse(lista);
                if (lista != null && lista.size() > 0){

                    requisicao = lista.get(0);

                    if (requisicao != null){
                        if (!requisicao.getStatus().equals(Requisicao.STATUS_ENCERRADA)){
                            passageiro = requisicao.getPassageiro();
                            localPassageiro = new LatLng( Double.parseDouble(passageiro.getLatitude()), Double.parseDouble(passageiro.getLongitude()));

                            statusRequisicao = requisicao.getStatus();
                            destino = requisicao.getDestino();

                            if (requisicao.getMotorista() != null){
                                motorista= requisicao.getMotorista();
                                localMotorista = new LatLng( Double.parseDouble(motorista.getLatitude()), Double.parseDouble(motorista.getLongitude()));
                            }

                            alteraInterfaceStatusRequisicao(statusRequisicao);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void alteraInterfaceStatusRequisicao(String status){
        if (status != null && !status.isEmpty()){
            cancelarUber = false;
            switch (status){
                case Requisicao.STATUS_AGUARDANDO:
                    requisicaoAguardando();
                    break;
                case Requisicao.STATUS_A_CAMINHO:
                    requisicaoACaminho();
                    break;
                case Requisicao.STATUS_VIAGEM:
                    requisicaoViagem();
                    break;
                case Requisicao.STATUS_FINALIZADA:
                    requisicaoFinalizada();
                    break;
                case Requisicao.STATUS_CANCELADA:
                    requisicaoCancelada();
                    break;
            }
        }else {
            //add marcador de passageiro
            adicionaMarcadorPassageiro(localPassageiro, "seu local");
            centralizarMarcador(localPassageiro);
        }
    }

    private void requisicaoAguardando(){

        binding.linearLayoutDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setText("Cancelar Uber");
        cancelarUber = true;

        //add marcador passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());
        centralizarMarcador(localPassageiro);

    }

    private void requisicaoACaminho(){

        binding.linearLayoutDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setText("Motorista a caminho");
        binding.btnChamarUber.setEnabled(false);

        //add marcado passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //add marcado motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //centralizar motorista | paasageiro
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

        //foto do motorista
        binding.imgMotoristaUber.setVisibility(View.VISIBLE);
        recuperarFotoPassageiro(motorista);
    }

    private void requisicaoViagem(){

        binding.linearLayoutDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setText("A caminho do destino");
        binding.btnChamarUber.setEnabled(false);

        //add marcado motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //add o marcador de destino
        LatLng localDestino = new LatLng(Double.parseDouble(destino.getLatitude()), Double.parseDouble(destino.getLongitude()));
        adicionaMarcadorDestino(localDestino, "Destino:" + destino.getRua() + ", " + destino.getNumero());

        //centralizar motorista | paasageiro
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

        //foto do motorista
        binding.imgMotoristaUber.setVisibility(View.VISIBLE);
        recuperarFotoPassageiro(motorista);
    }

    private void requisicaoFinalizada(){

        binding.linearLayoutDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setEnabled(false);

        //add o marcador de destino
        LatLng localDestino = new LatLng(Double.parseDouble(destino.getLatitude()), Double.parseDouble(destino.getLongitude()));
        adicionaMarcadorDestino(localDestino, "Destino:" + destino.getRua() + ", " + destino.getNumero());

        centralizarMarcador(localDestino);

        //foto do motorista
        binding.imgMotoristaUber.setVisibility(View.VISIBLE);
        recuperarFotoPassageiro(motorista);

        //calcular Distancia
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);

        //valor do litro do combustivel / pela quantidade de km feita por litro, mais o valor de ganho de mao de obra
        float valorDoKm = (5.85f / 7.5f) + 3.55f;
        float valor = distancia * valorDoKm;

        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);

        binding.btnChamarUber.setText("Corrida finalizada - R$ " + resultado);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Total da viagem")
                .setMessage("Sua viagem ficou: R$ " + resultado)
                .setCancelable(false)
                .setNegativeButton("Encerrar viagem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                        requisicao.atualizarStatus();

                        finish();
//                        startActivity(new Intent(getIntent()));
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void requisicaoCancelada(){
        binding.linearLayoutDestino.setVisibility(View.VISIBLE);
        binding.btnChamarUber.setText("Chamar Uber");
        binding.editDestino.setText("");
        cancelarUber = false;

    }

    private void adicionaMarcadorPassageiro(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholder)));
    }

    private void adicionaMarcadorMotorista(LatLng localizacao, String titulo) {

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
    }

    private void adicionaMarcadorDestino(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        if (marcadorDestino != null)
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino)));

    }

    private void centralizarMarcador(LatLng local){

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(local, 16));
    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        //recupera informcaoes do android studio
        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.20);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, espacoInterno));

    }

    private void salvarRequisicao(Destino destino) {

        Requisicao requisicao = new Requisicao();
        requisicao.setDestino(destino);

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude(String.valueOf(localPassageiro.latitude));
        usuarioPassageiro.setLongitude(String.valueOf(localPassageiro.longitude));

        requisicao.setPassageiro(usuarioPassageiro);
        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        binding.linearLayoutDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setText("Cancelar Uber");
    }

    private Address recuperarEndereco(String endereco){

        Geocoder geocoder = new Geocoder(PassageiroActivity.this, Locale.getDefault());
        List<Address> listaEnderecos;
        try {
            listaEnderecos = geocoder.getFromLocationName(endereco.toString(), 1);
            if( listaEnderecos != null && listaEnderecos.size() > 0 ){
                Address address = listaEnderecos.get(0);

                return address;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    private void recuperarFotoPassageiro(Usuario usuario){

        DatabaseReference pasRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference passUber = pasRef.child("usuarios").child(usuario.getId());
        passUber.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Usuario user = snapshot.getValue(Usuario.class);
                String urlString = user.getFoto();
                Picasso.get().load(urlString).into(binding.imgMotoristaUber);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void inicializarComponentes() {

        getSupportActionBar().setTitle("Iniciar uma viagem");

        //Configuracoes iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //recuperar localizacao do usuario
        recuperarLocalizacaoUsuario();

    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

                //recuperar laritude e Longitude
                Double latitude = location.getLatitude();
                Double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                //Atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                //alterar interface de acordo com o status
                alteraInterfaceStatusRequisicao(statusRequisicao);

                if (statusRequisicao != null && !statusRequisicao.isEmpty()){
                    if (statusRequisicao.equals(Requisicao.STATUS_VIAGEM) || statusRequisicao.equals(Requisicao.STATUS_FINALIZADA)){
                        locationManager.removeUpdates(locationListener);
                    }else {
                        //Solicitar atualizacoes de localizacao
                        if (ActivityCompat.checkSelfPermission(PassageiroActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    10000,
                                    10,
                                    locationListener);
                        }
                    }
                }


            }
        };

        //Solicitar atualizacoes de localizacao
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case  R.id.menu_sair_p:
                autenticacao.signOut();
                finish();
                break;
            case  R.id.menu_configuracao_p:
                startActivity(new Intent(PassageiroActivity.this, ConfiguracaoActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}