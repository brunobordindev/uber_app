package br.com.uber.model;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

import br.com.uber.helper.ConfiguracaoFirebase;

public class Requisicao {

    private String id;
    private String status;
    private Usuario passageiro;
    private Usuario motorista;
    private Destino destino;

    public static final String STATUS_AGUARDANDO = "aguardando!";
    public static final String STATUS_A_CAMINHO = "a caminho!";
    public static final String STATUS_VIAGEM = "viagem!";
    public static final String STATUS_FINALIZADA = "finalizada!";
    public static final String STATUS_ENCERRADA = "encerrada!";
    public static final String STATUS_CANCELADA = "cancelada!";

    public Requisicao() {
    }

    public void salvar(){

        DatabaseReference firabeseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference requisicoes = firabeseRef.child("requisicoes");

        String idRequisicao = requisicoes.push().getKey();
        setId(idRequisicao);

        requisicoes.child(getId()).setValue(this);
    }

    public void atualizar(){

        DatabaseReference firabeseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference requisicoes = firabeseRef.child("requisicoes");

        DatabaseReference requisicao = requisicoes.child(getId());

        Map objeto = new HashMap();
        objeto.put("motorista", getMotorista());
        objeto.put("status", getStatus());

        requisicao.updateChildren(objeto);
    }

    public void atualizarLocalizacaoMotorista(){

        DatabaseReference firabeseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference requisicoes = firabeseRef.child("requisicoes");

        DatabaseReference requisicao = requisicoes.child(getId()).child("motorista");

        Map objeto = new HashMap();
        objeto.put("latitude", getMotorista().getLatitude());
        objeto.put("longitude", getMotorista().getLongitude());

        requisicao.updateChildren(objeto);
    }

    public void atualizarStatus(){

        DatabaseReference firabeseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference requisicoes = firabeseRef.child("requisicoes");

        DatabaseReference requisicao = requisicoes.child(getId());

        Map objeto = new HashMap();
        objeto.put("status", getStatus());

        requisicao.updateChildren(objeto);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Usuario getPassageiro() {
        return passageiro;
    }

    public void setPassageiro(Usuario passageiro) {
        this.passageiro = passageiro;
    }

    public Usuario getMotorista() {
        return motorista;
    }

    public void setMotorista(Usuario motorista) {
        this.motorista = motorista;
    }

    public Destino getDestino() {
        return destino;
    }

    public void setDestino(Destino destino) {
        this.destino = destino;
    }
}
