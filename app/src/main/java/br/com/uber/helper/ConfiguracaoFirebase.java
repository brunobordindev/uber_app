package br.com.uber.helper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ConfiguracaoFirebase {

    private static FirebaseAuth referenciaAuth;
    private static DatabaseReference referenciaFirebase;
    private static StorageReference storageReference;

    public  static String getIdUsuario(){
        FirebaseAuth auth = getFirebaseAutenticacao();
        return  auth.getCurrentUser().getUid();
    }

    public static FirebaseAuth getFirebaseAutenticacao(){
        if (referenciaAuth == null){
            referenciaAuth = FirebaseAuth.getInstance();
        }
        return referenciaAuth;
    }

    public static DatabaseReference getFirebaseDatabase(){
        if (referenciaFirebase == null){
            referenciaFirebase = FirebaseDatabase.getInstance().getReference();
        }
        return referenciaFirebase;
    }

    public static StorageReference getFirebaseStorage(){
        if (storageReference == null){
            storageReference = FirebaseStorage.getInstance().getReference();
        }
        return storageReference;
    }
}
