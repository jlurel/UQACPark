package com.katsuo.uqacpark.profile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.katsuo.uqacpark.ImmatriculationActivity;
import com.katsuo.uqacpark.R;
import com.katsuo.uqacpark.base.BaseActivity;
import com.katsuo.uqacpark.dao.UserDAO;
import com.katsuo.uqacpark.models.User;

import butterknife.BindView;
import butterknife.OnClick;

public class ProfileActivity extends BaseActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    @BindView(R.id.profile_activity_imageview_profile)
    ImageView imageViewProfile;

    @BindView(R.id.profile_activity_edit_text_username)
    TextInputEditText textInputEditTextUsername;

    @BindView(R.id.profile_activity_text_view_email)
    TextView textViewEmail;

    @BindView(R.id.profile_activity_progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.edit_text_immatriculation)
    EditText editTextImmatriculation;

    //Identifiants Http Request
    private static final int SIGN_OUT_TASK = 10;
    private static final int DELETE_USER_TASK = 20;
    private static final int UPDATE_USERNAME = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.configureToolbar();
        this.updateUIWhenCreating();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            final String plaque = data.getExtras().getString(ImmatriculationActivity.PLAQUE);
            editTextImmatriculation.setText(plaque);
        }
    }

    @Override
    public int getFragmentLayout() {
        return R.layout.activity_profile;
    }

    @OnClick(R.id.profile_activity_button_update)
    public void onClickUpdateButton() {
        this.updateUsernameInFirebase();
    }

    @OnClick(R.id.profile_activity_button_sign_out)
    public void onClickSignOutButton() {
        this.signOutUserFromFirebase();
    }

    @OnClick(R.id.profile_activity_button_delete)
    public void onClickDeleteButton() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.popup_message_confirmation_delete_account)
                .setPositiveButton(R.string.popup_message_choice_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteUserFromFirebase();
                    }
                })
                .setNegativeButton(R.string.popup_message_choice_no, null)
                .show();
    }

    @OnClick(R.id.button_immatriculation)
    public void onClickScanImmatriculationButton() {
        Intent intent = new Intent(this, ImmatriculationActivity.class);
        startActivityForResult(intent, 1);
    }


    private void updateUIWhenCreating() {

        if (this.getCurrentUser() != null){

            //Get picture URL from Firebase
            if (this.getCurrentUser().getPhotoUrl() != null) {
                Glide.with(this)
                        .load(this.getCurrentUser().getPhotoUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .into(imageViewProfile);
            }

            //Get email & username from Firebase
            String email = TextUtils.isEmpty(this.getCurrentUser().getEmail()) ? getString(R.string.info_no_email_found) : this.getCurrentUser().getEmail();
            this.textViewEmail.setText(email);

            UserDAO.getUser(this.getCurrentUser().getUid()).addOnSuccessListener(
                    new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    User currentUser = documentSnapshot.toObject(User.class);
                    String username = TextUtils.isEmpty(currentUser.getUsername()) ?
                            getString(R.string.info_no_username_found) : currentUser.getUsername();
                    textInputEditTextUsername.setText(username);
                }
            });
        }
    }


    private void updateUsernameInFirebase(){

        this.progressBar.setVisibility(View.VISIBLE);
        String username = this.textInputEditTextUsername.getText().toString();

        if (this.getCurrentUser() != null){
            if (!username.isEmpty() &&  !username.equals(getString(R.string.info_no_username_found))){
                UserDAO.updateUsername(username, this.getCurrentUser().getUid())
                        .addOnFailureListener(this.onFailureListener()).addOnSuccessListener(
                                this.updateUIAfterRESTRequestsCompleted(UPDATE_USERNAME)
                        );
            }
        }
    }

    private void signOutUserFromFirebase(){
        AuthUI.getInstance()
                .signOut(this)
                .addOnSuccessListener(this, this.updateUIAfterRESTRequestsCompleted(SIGN_OUT_TASK));
    }

    private void deleteUserFromFirebase(){
        if (this.getCurrentUser() != null) {
            UserDAO.deleteUser(this.getCurrentUser().getUid())
                    .addOnFailureListener(this.onFailureListener());
            AuthUI.getInstance()
                    .delete(this)
                    .addOnSuccessListener(this, this.updateUIAfterRESTRequestsCompleted(DELETE_USER_TASK));

        }
    }

    private OnSuccessListener<Void> updateUIAfterRESTRequestsCompleted(final int origin){
        return new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                switch (origin){
                    case SIGN_OUT_TASK:
                        finish();
                        break;
                    case DELETE_USER_TASK:
                        finish();
                        break;
                    case UPDATE_USERNAME:
                        progressBar.setVisibility(View.INVISIBLE);
                        break;
                    default:
                        break;
                }
            }
        };
    }
}
