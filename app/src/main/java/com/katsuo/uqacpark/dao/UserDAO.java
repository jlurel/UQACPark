package com.katsuo.uqacpark.dao;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.katsuo.uqacpark.models.User;

public class UserDAO {
    private static final String COLLECTION_NAME = "users";

    // --- COLLECTION REFERENCE ---

    public static CollectionReference getUsersCollection(){
        return FirebaseFirestore.getInstance().collection(COLLECTION_NAME);
    }

    // --- CREATE ---

    public static Task<Void> createUser(String userId, String username, String urlPicture) {
        User userToCreate = new User(userId, username, urlPicture);
        return UserDAO.getUsersCollection().document(userId).set(userToCreate);
    }

    // --- GET ---

    public static Task<DocumentSnapshot> getUser(String userId){
        return UserDAO.getUsersCollection().document(userId).get();
    }

    // --- UPDATE ---

    public static Task<Void> updateUsername(String username, String userId) {
        return UserDAO.getUsersCollection().document(userId).update("username", username);
    }

    public static Task<Void> updateIsAdmin(String userId, Boolean isAdmin) {
        return UserDAO.getUsersCollection().document(userId).update("isAdmin", isAdmin);
    }

    // --- DELETE ---

    public static Task<Void> deleteUser(String userId) {
        return UserDAO.getUsersCollection().document(userId).delete();
    }
}
