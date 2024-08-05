package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mywarehouse.mywarehouse.Models.User;

public class FirebaseLogin {

    public interface FirestoreCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    public interface UserCallback {
        void onCallback(User user);
    }

    public static void signInWithEmailAndPassword(String email, String password, FirebaseAuth mAuth, FirestoreCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public static void fetchUserRole(String userId, UserCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        User user = task.getResult().toObject(User.class);
                        callback.onCallback(user);
                    }
                });
    }
}
