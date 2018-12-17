package com.katsuo.uqacpark;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.katsuo.uqacpark.base.BaseActivity;
import com.squareup.picasso.Picasso;

import org.openalpr.OpenALPR;
import org.openalpr.model.Results;
import org.openalpr.model.ResultsError;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;

public class ImmatriculationActivity extends BaseActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = ImmatriculationActivity.class.getSimpleName();
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 2;
    public static final String PLAQUE = "plaque";

    @BindView(R.id.image_view)
    ImageView imageView;
    @BindView(R.id.text_view_immatriculation)
    TextView textViewImmatriculation;
    @BindView(R.id.immatriculation_layout)
    LinearLayout layout;

    private static File destination;

    private String ANDROID_DATA_DIR;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ANDROID_DATA_DIR = this.getApplicationInfo().dataDir;

        checkPermission();
    }

    @Override
    public int getFragmentLayout() {
        return R.layout.activity_immatriculation;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (destination != null) {// Picasso does not seem to have an issue with a null value, but to be safe
            Log.d(TAG, destination.getAbsolutePath());
            Picasso.with(getApplicationContext()).load(destination).into(imageView);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if ( progressDialog!=null && progressDialog.isShowing() ){
            progressDialog.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            progressDialog = ProgressDialog.show(this, "Loading", "Parsing result...", true);
            final String openAlprConfFile = ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf";
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 10;

            // Picasso requires permission.WRITE_EXTERNAL_STORAGE_REQUEST_CODE
            Log.d(TAG, destination.getAbsolutePath());
            Picasso.with(getApplicationContext()).load(destination).into(imageView);
            textViewImmatriculation.setText("Processing");

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {

                    String result = OpenALPR.Factory.create(ImmatriculationActivity.this, ANDROID_DATA_DIR)
                            .recognizeWithCountryRegionNConfig("us", "", destination.getAbsolutePath(), openAlprConfFile, 10);

                    Log.d("OPEN ALPR", result);

                    try {
                        final Results results = new Gson().fromJson(result, Results.class);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (results == null || results.getResults() == null || results.getResults().size() == 0) {
                                    Toast.makeText(ImmatriculationActivity.this, "It was not possible to detect the licence plate.", Toast.LENGTH_LONG).show();
                                    textViewImmatriculation.setText("It was not possible to detect the licence plate.");
                                } else {
                                    textViewImmatriculation.setText("Plate: " + results.getResults().get(0).getPlate()
                                            // Trim confidence to two decimal places
                                            + " Confidence: " + String.format("%.2f", results.getResults().get(0).getConfidence()) + "%"
                                            // Convert processing time to seconds and trim to two decimal places
                                            + " Processing time: " + String.format("%.2f", ((results.getProcessingTimeMs() / 1000.0) % 60)) + " seconds");

                                    Intent intent = new Intent();
                                    intent.putExtra(PLAQUE, results.getResults().get(0).getPlate());
                                    setResult(Activity.RESULT_OK, intent);
                                    finish();
                                }
                            }
                        });

                    } catch (JsonSyntaxException exception) {
                        final ResultsError resultsError = new Gson().fromJson(result, ResultsError.class);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textViewImmatriculation.setText(resultsError.getMsg());
                            }
                        });
                    }
                    progressDialog.dismiss();
                }
            });
        }
    }

    @OnClick(R.id.button_scan)
    public void onClickScanButton() {
        checkPermission();
    }


    private void checkPermission() {
        requestCameraPermission();
        requestStoragePermission();
    }

    private void requestCameraPermission() {
        Log.i(TAG, "Camera permission has not been granted. Requesting permission");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, CAMERA_REQUEST_CODE,
                    Manifest.permission.CAMERA, true);
        } else {
            Log.i(TAG,
                    "Camera permissions have already been granted.");
            takePicture();
        }
    }

    private void requestStoragePermission() {
        Log.i(TAG, "Storage permission has not been granted. Requesting permission");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            PermissionUtils.requestPermission(this, STORAGE_REQUEST_CODE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, true);
            PermissionUtils.requestPermission(this, STORAGE_REQUEST_CODE,
                    Manifest.permission.READ_EXTERNAL_STORAGE, true);
        } else {
            Log.i(TAG,
                    "Storage permissions have already been granted.");
            takePicture();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case STORAGE_REQUEST_CODE: {
                Log.i(TAG, "Received response for Storage permission request.");
                if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        && PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(layout, "La permission d'utiliser le stockage a été accordée.",
                            Snackbar.LENGTH_SHORT)
                            .show();
                } else {
                    Log.i(TAG, "Storage permission was NOT granted.");
                    Toast.makeText(this,
                            "Storage permission is needed to analyse the picture.",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
            case CAMERA_REQUEST_CODE: {
                Log.i(TAG, "Received response for Camera permission request.");
                if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.CAMERA)) {
                    Snackbar.make(layout, "La permission d'utiliser l'appareil photo a été accordée.",
                            Snackbar.LENGTH_SHORT)
                            .show();
                } else {
                    Log.i(TAG, "CAMERA permission was NOT granted.");
                    Toast.makeText(this,
                            "Camera permission is needed to take the picture.",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
            default:
                finish();
                break;
        }
    }

    private File createImageFile() throws IOException {
        String name = dateToString(new Date(), "yyyy-MM-dd-hh-mm-ss");
        File folder = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!folder.exists()) {
            folder.mkdir();
        }
        File image = File.createTempFile(name, ".jpg", folder);
        return image;
    }

    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            destination = null;
            try {
                destination = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (destination != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.katsuo.uqacpark.fileprovider",
                        destination);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            }
        }
    }

    public String dateToString(Date date, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format, Locale.getDefault());

        return df.format(date);
    }
}