package br.com.uber.helper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import br.com.uber.activity.CorridaActivity;
import br.com.uber.activity.PassageiroActivity;
import br.com.uber.activity.RequisicoesActivity;
import br.com.uber.model.Usuario;

public class UsuarioFirebase {

    public static FirebaseUser getUsuarioAtual(){

        FirebaseAuth usuario = ConfiguracaoFirebase.getFirebaseAutenticacao();
        return usuario.getCurrentUser();
    }

    public static String identificadorUsuario(){
        return getUsuarioAtual().getUid();
    }

    public static void atualizarDadosLocalizacao(double lat, double lon){

        //Define nó de local de usuário
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        //recupera dados do usuario logado
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        //configura localizacao do usuario
        geoFire.setLocation(
                usuarioLogado.getId(),
                new GeoLocation(lat, lon),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                }
        );
    }

    public static boolean atualizarNomeUsuario(String nome){

        try {

            //usuario logado no app
            FirebaseUser user = getUsuarioAtual();

            //Configurar objeto para alteracao do perfil
            UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest
                    .Builder()
                    .setDisplayName(nome)
                    .build();

            user.updateProfile(profileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (!task.isSuccessful()){
                        Log.d("Perfil", "Erro ao atualizar nome do perfil");
                    }
                }
            });

            return true;

        }catch (Exception e ){
            e.printStackTrace();
            return false;
        }
    }

    public static void atualizarFotoUsuario(Uri url){

        try {

            //usuario logado no app
            FirebaseUser user = getUsuarioAtual();

            //Configurar objeto para alteracao do perfil
            UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest
                    .Builder()
                    .setPhotoUri(url)
                    .build();

            user.updateProfile(profileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (!task.isSuccessful()){
                        Log.d("Perfil", "Erro ao atualizar foto do perfil");
                    }
                }
            });

        }catch (Exception e ){
            e.printStackTrace();
        }
    }

    public static Usuario getDadosUsuarioLogado(){

        FirebaseUser firebaseUser = getUsuarioAtual();

        Usuario usuario = new Usuario();
        usuario.setId(firebaseUser.getUid());
        usuario.setNome(firebaseUser.getDisplayName());
        usuario.setEmail(firebaseUser.getEmail());

        if (firebaseUser.getPhotoUrl() == null){
            usuario.setFoto("");
        }else{
            usuario.setFoto(firebaseUser.getPhotoUrl().toString());
        }
        return usuario;
    }

    public static void redirecionaUsuarioLogado(final Activity activity){

        FirebaseUser user = getUsuarioAtual();

        if (user != null){

            DatabaseReference usuarioRef = ConfiguracaoFirebase.getFirebaseDatabase()
                    .child("usuarios")
                    .child(identificadorUsuario());
            usuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    Usuario usuario = snapshot.getValue(Usuario.class);

                    String tipoUsuario = usuario.getTipo();
                    if (tipoUsuario.equals("M") && tipoUsuario != null){
                        activity.startActivity(new Intent(activity, RequisicoesActivity.class));
                    }else{
                        activity.startActivity(new Intent(activity, PassageiroActivity.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }



    }
}
