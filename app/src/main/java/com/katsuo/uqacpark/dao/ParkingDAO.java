package com.katsuo.uqacpark.dao;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.katsuo.uqacpark.models.Parking;

public class ParkingDAO {
    private static final String COLLECTION_NAME = "parkings";

    public static CollectionReference getParkingCollection() {
        return FirebaseFirestore.getInstance().collection(COLLECTION_NAME);
    }

    public static Task<Void> createParking(String parkingId, String name, int nbSpotsAvailable, double lat, double lng) {
        Parking parking = new Parking(parkingId, name, nbSpotsAvailable, lat, lng);
        return ParkingDAO.getParkingCollection().document(parkingId).set(parking);
    }

    public static Task<DocumentSnapshot> getParking(String parkingId) {
        return ParkingDAO.getParkingCollection().document(parkingId).get();
    }

    public static Task<Void> updateName(String parkingId, String name) {
        return ParkingDAO.getParkingCollection().document(parkingId).update("name", name);
    }

    public static Task<Void> updateNbSpotsAvailable(String parkingId, int nbSpotsAvailable) {
        return ParkingDAO.getParkingCollection().document(parkingId).update("nbSpotsAvailable", nbSpotsAvailable);
    }

    public static Task<Void> deleteParking(String parkingId) {
        return ParkingDAO.getParkingCollection().document(parkingId).delete();
    }
}
