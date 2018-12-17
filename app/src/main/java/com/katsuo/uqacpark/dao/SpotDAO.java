package com.katsuo.uqacpark.dao;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.katsuo.uqacpark.models.Spot;

public class SpotDAO {
    private static final String COLLECTION_NAME = "spots";

    public static CollectionReference getAllSpotsForParking(String parking) {
        return ParkingDAO.getParkingCollection().document(parking).collection(COLLECTION_NAME);
    }

    public static Query getAllAvailableSpotsForParking(String parking) {
        return SpotDAO.getAllSpotsForParking(parking).whereEqualTo("available", true);
    }

    public static Query getSpot(String parking, String spotId) {
        return SpotDAO.getAllSpotsForParking(parking).whereEqualTo("spotId", spotId);
    }

    public static Task<Void> updateIsAvailable(String parking, String spotId, boolean isAvailable) {
        return SpotDAO.getAllSpotsForParking(parking).document(spotId).update("available", isAvailable);
    }
    
    public static Task<Void> deleteSpot(String parking, String spotId) {
        return SpotDAO.getAllSpotsForParking(parking).document(spotId).delete();
    }
}
