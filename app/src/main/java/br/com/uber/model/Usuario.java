package br.com.uber.model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import br.com.uber.helper.ConfiguracaoFirebase;
import br.com.uber.helper.UsuarioFirebase;

public class Usuario implements Serializable {

    private String id;
    private String nome;
    private String email;
    private String senha;
    private String tipo;
    private String foto;
    private Carro carro;

    private String latitude;
    private String longitude;

    public Usuario() {
    }

    public void salvar(){

        DatabaseReference reference = ConfiguracaoFirebase.getFirebaseDatabase();
        reference.child("usuarios")
                .child(getId())
                .setValue(this);
    }


    public void atualizar(){

        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        Map objeto = new HashMap();

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        objeto.put("/usuarios/" + getId() + "/nome", getNome());
        objeto.put("/usuarios/" + getId() + "/foto", getFoto());

        firebaseRef.updateChildren(objeto);

    }

    public void atualizarMotorista(){

        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        Map objeto = new HashMap();

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        objeto.put("/usuarios/" + getId() + "/nome", getNome());
        objeto.put("/usuarios/" + getId() + "/foto", getFoto());
        objeto.put("/usuarios/" + getId() + "/carro", getCarro());

        firebaseRef.updateChildren(objeto);

    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Exclude
    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public Carro getCarro() {
        return carro;
    }

    public void setCarro(Carro carro) {
        this.carro = carro;
    }
}
