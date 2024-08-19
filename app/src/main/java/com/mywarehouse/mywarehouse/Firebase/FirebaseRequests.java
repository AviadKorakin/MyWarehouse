package com.mywarehouse.mywarehouse.Firebase;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.mywarehouse.mywarehouse.Models.TransactionRequest;



public class FirebaseRequests extends FirebaseManager {

    private static ListenerRegistration requestListener;

    public interface TransactionRequestCallback {
        void onRequestAdded(TransactionRequest request);
        void onRequestModified(TransactionRequest request);
        void onRequestRemoved(String requestId);
        void onFailure(Exception e);
    }

    public interface RequestCountCallback {
        void onRequestCountFetched(int count);
        void onFailure(Exception e);
    }

    public static void fetchTotalRequestCount(RequestCountCallback callback) {
        AggregateQuery countQuery = db.collection("transactionRequests").count();
        countQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AggregateQuerySnapshot snapshot = task.getResult();
                callback.onRequestCountFetched((int) snapshot.getCount());
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public static void listenToTransactionRequests(TransactionRequestCallback callback) {
        if (requestListener != null) {
            requestListener.remove(); // Remove any existing listener to avoid duplicates
        }

        requestListener = db.collection("transactionRequests")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        callback.onFailure(e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            TransactionRequest request = dc.getDocument().toObject(TransactionRequest.class);
                            switch (dc.getType()) {
                                case ADDED:
                                    callback.onRequestAdded(request);
                                    break;
                                case MODIFIED:
                                    callback.onRequestModified(request);
                                    break;
                                case REMOVED:
                                    callback.onRequestRemoved(dc.getDocument().getId());
                                    break;
                            }
                        }
                    }
                });
    }

    public static void removeAllListeners() {
        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }
    }
}