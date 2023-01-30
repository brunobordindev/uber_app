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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import br.com.uber.R;
import br.com.uber.databinding.ActivityCadastrarBinding;
import br.com.uber.helper.ConfiguracaoFirebase;
import br.com.uber.helper.UsuarioFirebase;
import br.com.uber.model.Usuario;

public class CadastrarActivity extends AppCompatActivity {

    private ActivityCadastrarBinding binding;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_cadastrar);

        getSupportActionBar().setTitle("Cadastrar uma conta");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.btnCadCadastrar.setOnClickListener(view -> {

            String nome = binding.editCadNome.getText().toString();
            String email = binding.editCadEmail.getText().toString();
            String senha = binding.editCadSenha.getText().toString();

            if (!nome.isEmpty()){
                if (!email.isEmpty()){
                    if (!senha.isEmpty()){

                        Usuario usuario = new Usuario();
                        usuario.setNome(nome);
                        usuario.setEmail(email);
                        usuario.setSenha(senha);
                        usuario.setTipo(verificaTipoUsuario());
                        
                        cadastrarUsuario(usuario);

                    }else{
                        Toast.makeText(getApplicationContext(), "Preencha sua senha!", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "Preencha seu e-mail!", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(getApplicationContext(), "Preencha seu nome!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void cadastrarUsuario(Usuario usuario) {

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(), usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()){

                  try {

                      String id = task.getResult().getUser().getUid();
                      usuario.setId(id);
                      usuario.salvar();

                      //Atualizar o nome no UserProfile
                      UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());

                      /*
                        Redirecionando o usuario de acordo com o seu tipo
                        Motorista - activity requisicoes
                        Passageito - activity Maps
                      */

                      if (verificaTipoUsuario() == "P"){
                          startActivity(new Intent(getApplicationContext(), PassageiroActivity.class));
                          finish();
                          Toast.makeText(getApplicationContext(), "Sucesso ao cadastrar Passageiro!", Toast.LENGTH_SHORT).show();
                      }else{
                          startActivity(new Intent(getApplicationContext(), RequisicoesActivity.class));
                          finish();
                          Toast.makeText(getApplicationContext(), "Sucesso ao cadastrar Motorista!", Toast.LENGTH_SHORT).show();
                      }

                  }catch (Exception e ){
                      e.printStackTrace();
                  }

                }else{

                    String excecao = "";
                    try {
                        throw task.getException();
                    }catch (FirebaseAuthWeakPasswordException e){
                        excecao = "Digite uma senha mais forte";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        excecao = "Digite um e-mail válido";
                    }catch (FirebaseAuthUserCollisionException e){
                        excecao = "E-mail já cadastrado!";
                    }catch (Exception e){
                        excecao = "Erro ao cadastrar usuário: " + e.getMessage();
                        e.printStackTrace();
                    }

                    Toast.makeText(CadastrarActivity.this, excecao, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public String verificaTipoUsuario(){
        return binding.switchTipoUsuario.isChecked() ? "M" : "P";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}