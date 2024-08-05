package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Models.User;

public class FirebaseRegister {

    public interface FirestoreCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    public static void createUserWithEmailAndPassword(String email, String password, FirebaseAuth mAuth, FirestoreCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public static void saveUserToFirestore(User user, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUserId())
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
}
