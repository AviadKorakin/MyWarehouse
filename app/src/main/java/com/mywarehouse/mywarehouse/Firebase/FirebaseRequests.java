package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;

import java.util.ArrayList;
import java.util.List;

public class FirebaseRequests extends FirebaseManager {

    public interface FetchCallback<T> {
        void onSuccess(List<T> items);
        void onFailure(Exception e);
    }

    public static void listenToTransactionRequests(FetchCallback<TransactionRequest> callback) {
        db.collection("transactionRequests")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            callback.onFailure(e);
                            return;
                        }
                        if (snapshots != null) {
                            List<TransactionRequest> transactionRequestList = new ArrayList<>();
                            for (DocumentSnapshot document : snapshots.getDocuments()) {
                                TransactionRequest request = document.toObject(TransactionRequest.class);
                                if (request != null) {
                                    transactionRequestList.add(request);
                                }
                            }
                            callback.onSuccess(transactionRequestList);
                        }
                    }
                });
    }
}
