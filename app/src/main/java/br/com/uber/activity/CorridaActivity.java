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
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;

import br.com.uber.R;
import br.com.uber.databinding.ActivityCorridaBinding;
import br.com.uber.helper.ConfiguracaoFirebase;
import br.com.uber.helper.UsuarioFirebase;
import br.com.uber.model.Destino;
import br.com.uber.model.Local;
import br.com.uber.model.Requisicao;
import br.com.uber.model.Usuario;

public class CorridaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityCorridaBinding binding;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localMotorista;
    private LatLng localPassageiro;
    private Usuario motorista;
    private Usuario passageiro;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseRef;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private String statusRequisicao;
    private boolean requisicaoAtiva;
    private Destino destino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_corrida);

        inicializarComponentes();

        //recuperando dados do usuario
        if(getIntent().getExtras().containsKey("idRequisicao")
            && getIntent().getExtras().containsKey("motorista")){

            Bundle extra = getIntent().getExtras();
            motorista = (Usuario) extra.getSerializable("motorista");
            localMotorista = new LatLng(Double.parseDouble(motorista.getLatitude()), Double.parseDouble(motorista.getLongitude()));
            idRequisicao = extra.getString("idRequisicao");
            requisicaoAtiva = extra.getBoolean("requisicaoAtiva");
            verificaStatusRequisicao();

        }

        binding.btnAceitarCorrida.setOnClickListener(view -> {

            //configura requisicao
            requisicao = new Requisicao();
            requisicao.setId(idRequisicao);
            requisicao.setMotorista(motorista);
            requisicao.setStatus(Requisicao.STATUS_A_CAMINHO);

            requisicao.atualizar();
        });

        binding.fabRota.setOnClickListener(view -> {

            String status = statusRequisicao;
            if (status != null && !status.isEmpty()){

                String lat = "";
                String lon = "";


                switch (status){
                    case Requisicao.STATUS_A_CAMINHO:
                        lat = String.valueOf(localPassageiro.latitude);
                        lon = String.valueOf(localPassageiro.longitude);
                        break;
                    case Requisicao.STATUS_VIAGEM:
                        lat = destino.getLatitude();
                        lon = destino.getLongitude();
                        break;
                }

                //Abrir rota
                //mode d = carro, b = biicleta, l = moto, w = caminhada
                String latLong = lat + "," + lon;
                Uri uri = Uri.parse("google.navigation:q=" + latLong + "&mode=d");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.setPackage("com.google.android.apps.maps");
                startActivity(i);
            }


        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        verificaStatusRequisicao();
    }

    private void verificaStatusRequisicao() {

        DatabaseReference requisicoes = firebaseRef.child("requisicoes").child(idRequisicao);
        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //Recupera requisicao
                requisicao = snapshot.getValue(Requisicao.class);

                if (requisicao != null){
                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng( Double.parseDouble(passageiro.getLatitude()), Double.parseDouble(passageiro.getLongitude()));

                    statusRequisicao = requisicao.getStatus();
                    destino = requisicao.getDestino();
                    alteraInterfaceStatusRequisicao(statusRequisicao);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void alteraInterfaceStatusRequisicao(String status){

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
            case Requisicao.STATUS_ENCERRADA:
                requisicaoEncerrada();
                break;
            case Requisicao.STATUS_CANCELADA:
                requisicaoCancelada();
                break;
        }
    }

    private void requisicaoAguardando() {
        binding.btnAceitarCorrida.setText("Aceitar corrida");

        //exibe marcado do motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        centralizarMarcador(localMotorista);

    }

    private void requisicaoACaminho() {
        binding.btnAceitarCorrida.setText("A caminho do passageiro");
        binding.fabRota.setVisibility(View.VISIBLE);

        //exibe marcado do motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //exibe marcado do passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //centralizar dois marcadores - motorista e passageiro
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

        //inicia o monitoramento do motorista /pasageiro
        iniciarMonitoramento(motorista, localPassageiro, Requisicao.STATUS_VIAGEM);

        //foto do passageiro
        binding.imgUsuarioUber.setVisibility(View.VISIBLE);
        recuperarFotoPassageiro(passageiro);
    }

    private void requisicaoViagem() {

        //Altera interface
        binding.fabRota.setVisibility(View.VISIBLE);
        binding.btnAceitarCorrida.setText("A caminho do destino");

        //exibe marcador motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //exibe marcador destino
        LatLng localDestino = new LatLng(Double.parseDouble(destino.getLatitude()), Double.parseDouble(destino.getLongitude()));
        adicionaMarcadorDestino(localDestino, "Destino");

        //centraliza marcodores motorista/passageito
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);

        //inicia o monitoramento do motorista /pasageiro
        iniciarMonitoramento(motorista, localDestino, Requisicao.STATUS_FINALIZADA);

        //foto do passageiro
        binding.imgUsuarioUber.setVisibility(View.VISIBLE);
        recuperarFotoPassageiro(passageiro);
    }

    private void requisicaoFinalizada() {

        //Altera interface
        binding.fabRota.setVisibility(View.GONE);
        requisicaoAtiva = false;

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        if (marcadorDestino != null)
            marcadorDestino.remove();

        //Exibe marcador destino
        LatLng localDestino = new LatLng(Double.parseDouble(destino.getLatitude()), Double.parseDouble(destino.getLongitude()));
        adicionaMarcadorDestino(localDestino, "Destino " + destino.getRua() + ", " + destino.getNumero() );

        centralizarMarcador(localDestino);

        //foto do passageiro
        binding.imgUsuarioUber.setVisibility(View.VISIBLE);
        recuperarFotoPassageiro(passageiro);

        //calcular Distancia
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);

        //valor do litro do combustivel / pela quantidade de km feita por litro, mais o valor de ganho de mao de obra
        float valorDoKm = (5.85f / 7.5f) + 3.55f;
        float valor = distancia * valorDoKm;

        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);

        binding.btnAceitarCorrida.setText("Corrida finalizada - R$ " + resultado);
        binding.btnAceitarCorrida.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Total da viagem")
                    .setMessage("A viagem do passageiro ficou: R$ " + resultado)
                    .setCancelable(false)
                    .setNegativeButton("Encerrar viagem", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                            requisicao.atualizarStatus();

                            finish();

//                            alteraInterfaceStatusRequisicao(Requisicao.STATUS_ENCERRADA);
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();

        });

    }

    private void requisicaoEncerrada(){

        requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
        requisicao.atualizarStatus();
        finish();
//        startActivity(new Intent(CorridaActivity.this, RequisicoesActivity.class));

    }

    private void requisicaoCancelada(){
       finish();
//        startActivity(new Intent(CorridaActivity.this, RequisicoesActivity.class));

    }

    private void centralizarMarcador(LatLng local){

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(local, 17));
    }

    private void iniciarMonitoramento(final Usuario uOrigin, LatLng localDestino, final String status ){

        //Iniciciar Geofire
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        //Adiciona círculo no passageiro;
        final Circle circulo = mMap.addCircle(
                new CircleOptions()
                .center(localDestino)
                .radius(20)// é medido por metro
                .fillColor(Color.argb(90, 255, 153, 0))
                .strokeColor(Color.argb(190, 255, 152, 0))
        );

        final GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(localDestino.latitude, localDestino.longitude),
                0.02 // é em km - 1 é 1 km, 0.5 é 500 metros e 0.05 é 50 metros
        );
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (key.equals(uOrigin.getId())){ //iOrigin sempre é o motorista

                    //Altera status da requisicao
                    requisicao.setStatus(status); // motorista chegou no raio do passageiro
                    requisicao.atualizarStatus();

                    //remove Listener
                    geoQuery.removeAllListeners();
                    circulo.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

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

    private void adicionaMarcadorMotorista(LatLng localizacao, String titulo) {

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
    }


    private void adicionaMarcadorPassageiro(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholder)));
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
                localMotorista = new LatLng(latitude, longitude);

                //Atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                //atualizar localizacao motorista no firebase
                motorista.setLatitude(String.valueOf(latitude));
                motorista.setLongitude(String.valueOf(longitude));
                requisicao.setMotorista(motorista);
                requisicao.atualizarLocalizacaoMotorista();

                alteraInterfaceStatusRequisicao(statusRequisicao);

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

    private void recuperarFotoPassageiro(Usuario usuario){

        DatabaseReference pasRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference passUber = pasRef.child("usuarios").child(usuario.getId());
        passUber.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Usuario user = snapshot.getValue(Usuario.class);
                String urlString = user.getFoto();
                Picasso.get().load(urlString).into(binding.imgUsuarioUber);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void inicializarComponentes() {

        getSupportActionBar().setTitle("Iniciar corrida");

        //Configuracoes iniciais
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (requisicaoAtiva){
            Toast.makeText(getApplicationContext(), "Necessário encerrar corrida atual", Toast.LENGTH_SHORT).show();
        }else{
            startActivity( new Intent( CorridaActivity.this, RequisicoesActivity.class));
        }

        //verifica status da requisicao para encerrar
        if (statusRequisicao != null && !statusRequisicao.isEmpty()){
            requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
            requisicao.atualizarStatus();
        }

        return false;
    }
}