package com.katsuo.uqacpark.dao;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SpotDAO {
    private static final String COLLECTION_NAME = "spots";

    public static CollectionReference getAllSpotsForParking(String parking) {
        return ParkingDAO.getParkingCollection().document(parking).collection(COLLECTION_NAME);
    }

    public static Task<DocumentSnapshot> getSpot(String parking, String spotId) {
        return SpotDAO.getAllSpotsForParking(parking).document(spotId).get();
    }

    public static Task<Void> updateIsAvailable(String parking, String spotId, boolean isAvailable) {
        return SpotDAO.getAllSpotsForParking(parking).document(spotId).update("isAvailable", isAvailable);
    }
    
    public static Task<Void> deleteSpot(String parking, String spotId) {
        return SpotDAO.getAllSpotsForParking(parking).document(spotId).delete();
    }
}
