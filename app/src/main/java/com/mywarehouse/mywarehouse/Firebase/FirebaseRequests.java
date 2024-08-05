package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;

import java.util.ArrayList;
import java.util.List;

public class FirebaseRequests {

    public interface FetchCallback<T> {
        void onSuccess(List<T> items);
        void onFailure(Exception e);
    }

    public static void fetchTransactionRequests(FirebaseFirestore db, FetchCallback<TransactionRequest> callback) {
        db.collection("transactionRequests").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<TransactionRequest> transactionRequestList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    TransactionRequest request = document.toObject(TransactionRequest.class);
                    transactionRequestList.add(request);
                }
                callback.onSuccess(transactionRequestList);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }
}
