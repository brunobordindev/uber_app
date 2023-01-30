package br.com.uber.model;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

import br.com.uber.helper.ConfiguracaoFirebase;
import br.com.uber.helper.UsuarioFirebase;

public class Carro extends Usuario{

    private String id;
    private String marca;
    private String modelo;
    private String ano;
    private String placa;

    public Carro() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getAno() {
        return ano;
    }

    public void setAno(String ano) {
        this.ano = ano;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }
}
