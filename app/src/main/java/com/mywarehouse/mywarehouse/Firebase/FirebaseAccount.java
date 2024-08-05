package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class FirebaseAccount {

    public interface FetchCallback {
        void onSuccess(DocumentSnapshot document);
        void onFailure(Exception e);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void fetchUserData(FirebaseFirestore db, String userId, FetchCallback callback) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        callback.onSuccess(task.getResult());
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public static void updateUserData(FirebaseFirestore db, FirebaseAuth mAuth, String userId, Map<String, Object> updates, String password, UpdateCallback callback) {
        db.collection("users").document(userId).update(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.updatePassword(password).addOnCompleteListener(passwordTask -> {
                                if (passwordTask.isSuccessful()) {
                                    callback.onSuccess();
                                } else {
                                    callback.onFailure(passwordTask.getException());
                                }
                            });
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
}
