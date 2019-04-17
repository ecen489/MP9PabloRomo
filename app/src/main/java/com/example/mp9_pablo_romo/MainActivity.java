// Resources:
// https://www.youtube.com/watch?v=-W3qpuYr3lk

package com.example.mp9_pablo_romo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ImageView img;
    Button text;
    Button fc;
    ListView outputlist;

    static final int CAPTURE_IMAGE_REQUEST = 1;

    File photoFile = null;
    private String mCurrentPhotoPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        img = findViewById(R.id.imageView);
        text = findViewById(R.id.textrec);
        fc = findViewById(R.id.fcrec);
        outputlist = findViewById(R.id.listy);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Bundle extras = data.getExtras();
        //Bitmap imageBitmap = (Bitmap) extras.get("data");
        //img.setImageBitmap(imageBitmap);
        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Bitmap myBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            //displayMessage(getBaseContext(),"Path to file is: " + mCurrentPhotoPath);
            img.setImageBitmap(myBitmap);
        } else {
            displayMessage(getBaseContext(),"Request cancelled or something went wrong");
        }
    }

    public void isfc(View view) {
        // Uses MLKit and a custom tensor model to determine if the photo is of a flower or cat
    }

    public void istext(View view) {
        // Uses MLKit to figure what text is in the picture
        runTextRecognition();
    }

    public void getpic(View view) {
        // Calls captureImage to take a new photo
        captureImage();
    }

    private void captureImage() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        }
        else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(takePictureIntent.resolveActivity(getPackageManager()) != null ) {

                // Create File where photo should go
                try{
                    photoFile = createImageFile();
                    //displayMessage(getBaseContext(),photoFile.getAbsolutePath());

                    if(photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,"com.example.mp9_pablo_romo", photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
                        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
                    }
                }
                catch (Exception ex) {
                    displayMessage(getBaseContext(),ex.getMessage());
                }
            }
            //startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void displayMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // TEXT RECOGNITION CODE
    private void runTextRecognition() {
        Bitmap myBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(myBitmap);
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        text.setEnabled(false);
        recognizer.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                text.setEnabled(true);
                                processTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                text.setEnabled(true);
                                e.printStackTrace();
                            }
                        });
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) {
        List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            displayMessage(getBaseContext(),"No text found");
            return;
        }

        int count = 0;

        // get size of array to add to listview
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();

            count += lines.size();
//            for (int j = 0; j < lines.size(); j++) {
//
//                // This was for taking the individual elements of the lines found
//                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
//                for (int k = 0; k < elements.size(); k++) {
//                    //displayMessage(getBaseContext(),elements.get(k).getText());
//                }
//            }
        }

        String[] itemsFound = new String[count];
        count = 0;

        // add the lines to the listview
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for(int j = 0; j < lines.size(); j++) {
                itemsFound[count] = lines.get(j).getText();
                count++;
            }
        }

        ArrayAdapter<String> myadapter = new ArrayAdapter<>(getBaseContext(),android.R.layout.simple_list_item_1,itemsFound);
        outputlist.setAdapter(myadapter);
    }
}
