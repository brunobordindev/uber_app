package br.com.uber.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;

import com.bumptech.glide.Glide;
import br.com.uber.R;
import br.com.uber.databinding.ActivityConfiguracaoBinding;
import br.com.uber.helper.ConfiguracaoFirebase;
import br.com.uber.helper.Permissao;
import br.com.uber.helper.UsuarioFirebase;
import br.com.uber.model.Usuario;

public class ConfiguracaoActivity extends AppCompatActivity {

    private ActivityConfiguracaoBinding binding;
    private DatabaseReference usuarioRef;
    private Usuario usuarioLogado;
    private StorageReference storageRef;
    private String identificadorUsuario;
    private String[] permissoes = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final int SELECAO_GALERIA = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_configuracao );

        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        storageRef = ConfiguracaoFirebase.getFirebaseStorage();
        identificadorUsuario = UsuarioFirebase.identificadorUsuario();

        //validar permissoes
        Permissao.validarPermissoes(permissoes, this, 1 );

        //Recuperar os dados do usuario
        FirebaseUser user = UsuarioFirebase.getUsuarioAtual();
        binding.editNomeConfig.setText(user.getDisplayName());

        //Recuperar os dados do usuario da foto
        Uri url  =  user.getPhotoUrl();
        if (url != null){
            Glide.with(ConfiguracaoActivity.this)
                    .load(url)
                    .into(binding.editarImagemConfig);
        }else{

            binding.editarImagemConfig.setImageResource(R.drawable.person);
        }

        binding.btnAlterar.setOnClickListener(view -> {

            String nomeAlterado = binding.editNomeConfig.getText().toString();

            //Atualizar nome do perfil
            UsuarioFirebase.atualizarNomeUsuario(nomeAlterado);
            usuarioLogado.setNome(nomeAlterado);
            usuarioLogado.atualizar();
            finish();
        });

        binding.btnGaleria.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (intent.resolveActivity(getPackageManager()) != null){
                startActivityForResult(intent, SELECAO_GALERIA);
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            Bitmap imagem = null;

            try {

                //Seleciona apenas da galeria
                switch (requestCode){
                    case SELECAO_GALERIA:
                        Uri localImagemSelecionada = data.getData();
                        imagem = MediaStore.Images.Media.getBitmap(getContentResolver(), localImagemSelecionada);
                        break;
                }

                //caso tenha sido escolhido uma imagem
                if (imagem != null){

                    //configura imagem para aparecer na tela
                    binding.editarImagemConfig.setImageBitmap(imagem);

                    //recuperar dados da imagem para o firebase
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG, 72, baos);
                    byte[] dadosImagem = baos.toByteArray();


                    //salvar imagem no firebase
                    final StorageReference imageRef = storageRef
                            .child("imagens")
                            .child("perfil")
                            .child(identificadorUsuario + ".jpeg");
                    UploadTask uploadTask = imageRef.putBytes(dadosImagem);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            Toast.makeText(getApplicationContext(), "Erro ao fazer upload da imagem", Toast.LENGTH_SHORT).show();

                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            imageRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    Uri url = task.getResult();
                                    atualizarFotoUsuario(url);
                                }

                            });

                            Toast.makeText(getApplicationContext(), "Sucesso ao fazer upload da imagem", Toast.LENGTH_SHORT).show();

                        }
                    });
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void atualizarFotoUsuario(Uri url) {

        //atualizar foto no perfil
        UsuarioFirebase.atualizarFotoUsuario(url);

        //Atualizar foto no Firebase
        usuarioLogado.setFoto(url.toString());
        usuarioLogado.atualizar();

        Toast.makeText(getApplicationContext(), "Sua foto foi atualizada!", Toast.LENGTH_SHORT).show();
    }
}