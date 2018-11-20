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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.ImageView;
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
    private static final int REQUEST_IMAGE = 100;
    private static final int STORAGE = 2;
    public static final String PLAQUE = "plaque";

    @BindView(R.id.image_view)
    ImageView imageView;
    @BindView(R.id.text_view_immatriculation)
    TextView textViewImmatriculation;
    private File destination;

    private String ANDROID_DATA_DIR;

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            final ProgressDialog progress = ProgressDialog.show(this, "Loading", "Parsing result...", true);
            final String openAlprConfFile = ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf";
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 10;

            // Picasso requires permission.WRITE_EXTERNAL_STORAGE
            Log.d(TAG, destination.getAbsolutePath());
            Picasso.with(getApplicationContext()).load(destination).into(imageView);
            textViewImmatriculation.setText("Processing");

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    String result = OpenALPR.Factory.create(ImmatriculationActivity.this, ANDROID_DATA_DIR).recognizeWithCountryRegionNConfig("us", "", destination.getAbsolutePath(), openAlprConfFile, 10);

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
                    progress.dismiss();
                }
            });
        }
    }

    @OnClick(R.id.button_scan)
    public void onClickScanButton() {
        checkPermission();
    }


    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the camera is missing.
            PermissionUtils.requestPermission(this, REQUEST_IMAGE,
                    Manifest.permission.CAMERA, true);
        } else {
            takePicture();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case STORAGE: {
                if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    takePicture();
                } else {
                    Toast.makeText(this, "Storage permission is needed to analyse the picture.", Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_IMAGE: {
                if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.CAMERA)) {
                    takePicture();
                } else {
                    Toast.makeText(this, "Camera permission is needed to take the picture.", Toast.LENGTH_LONG).show();
                }
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
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        }
    }

    public String dateToString(Date date, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format, Locale.getDefault());

        return df.format(date);
    }
}
