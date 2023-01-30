package br.com.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import br.com.uber.R;
import br.com.uber.databinding.ActivityLoginBinding;
import br.com.uber.helper.ConfiguracaoFirebase;
import br.com.uber.helper.UsuarioFirebase;
import br.com.uber.model.Usuario;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        getSupportActionBar().setTitle("Acessar minha conta");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.btnLoginEntrar.setOnClickListener(view -> {

            String email = binding.editLoginEmail.getText().toString();
            String senha = binding.editLoginSenha.getText().toString();

            if (!email.isEmpty()){
                if (!senha.isEmpty()){

                    Usuario usuario = new Usuario();
                    usuario.setEmail(email);
                    usuario.setSenha(senha);

                    logarUsuario(usuario);

                }else{
                    Toast.makeText(getApplicationContext(), "Preencha sua senha!", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(getApplicationContext(), "Preencha seu e-mail!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void logarUsuario(Usuario usuario) {

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(), usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()){

                    /*
                    Verificar o tipode usuario logado
                    Motorista ou Passageiro
                     */
                    UsuarioFirebase.redirecionaUsuarioLogado(LoginActivity.this);


                }else{

                    String excecao = "";
                    try {
                        throw task.getException();
                    }catch (FirebaseAuthInvalidUserException e){
                        excecao = "Usuário não está cadastrado";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        excecao = "E-mail e senha não corresponde a um usuário cadastrado";
                    }catch (Exception e){
                        excecao = "Erro ao logar usuário" + e.getMessage();
                    }

                    Toast.makeText(getApplicationContext(), excecao, Toast.LENGTH_SHORT).show();
                }
            }


        });
    }



    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}