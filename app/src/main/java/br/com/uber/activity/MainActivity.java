package br.com.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;

import br.com.uber.R;
import br.com.uber.databinding.ActivityMainBinding;
import br.com.uber.helper.ConfiguracaoFirebase;
import br.com.uber.helper.Permissao;
import br.com.uber.helper.UsuarioFirebase;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String[] permissoes = new  String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

//        auth = ConfiguracaoFirebase.getFirebaseAutenticacao();
//        auth.signOut();

        //validar as permissoes
        Permissao.validarPermissoes(permissoes, this, 1);

        //Esconde a Toolbar
        getSupportActionBar().hide();

        binding.btnEntrar.setOnClickListener(view -> {

            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        });

        binding.btnCadastrar.setOnClickListener(view -> {

            startActivity(new Intent(getApplicationContext(), CadastrarActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        UsuarioFirebase.redirecionaUsuarioLogado(MainActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int permissaoResultado : grantResults){
            if (permissaoResultado == PackageManager.PERMISSION_DENIED){
                alertaValidacaoPermissao();
            }
        }
    }

    private void alertaValidacaoPermissao() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissões Negadas");
        builder.setMessage("Para utilizar o aplicativo é necessário aceitar as permissões");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}