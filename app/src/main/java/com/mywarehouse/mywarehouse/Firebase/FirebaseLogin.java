package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.auth.FirebaseAuth;

import com.mywarehouse.mywarehouse.Models.User;
import com.mywarehouse.mywarehouse.Utilities.MyUser;

public class FirebaseLogin extends FirebaseManager {

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
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        User user = task.getResult().toObject(User.class);
                        MyUser.getInstance().setUser(user);
                        callback.onCallback(user);
                    }
                });
    }
}
