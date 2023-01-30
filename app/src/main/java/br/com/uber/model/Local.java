package br.com.uber.model;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

public class Local {

    public static float calcularDistancia(LatLng latLngInicial, LatLng latLngFinal){

        Location localInicial = new Location("Local inicial");
        localInicial.setLatitude(latLngInicial.latitude);
        localInicial.setLongitude(latLngInicial.longitude);

        Location localFinal = new Location("Local final");
        localFinal.setLatitude(latLngFinal.latitude);
        localFinal.setLongitude(latLngFinal.longitude);

        //Calcula distancia - resultados em metros
        //dividir por 1000 para converter em Km
        float distancia = localInicial.distanceTo(localFinal) / 1000;

        return distancia;
    }

    public static String formarDistancia(float distancia){

        String distanciaFormatada;
        if (distancia < 1 ){
            distancia = distancia * 1000; //converter em metros
            distanciaFormatada = Math.round(distancia) + "metros";
        }else {
            DecimalFormat decimalFormat = new DecimalFormat("0.000");
            distanciaFormatada = decimalFormat.format(distancia) + " km";
        }

        return distanciaFormatada;
    }
}
