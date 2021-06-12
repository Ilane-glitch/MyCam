package com.dev.mycamera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    ImageView selectedImage;
    /*
    Button cameraBtn, galleryBtn;
     */
    String currentPhotoPath;
    StorageReference storageReference;

    //codes d'acces
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedImage = findViewById(R.id.displayImageView);
        /* ancien button d'action
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
         */

        FloatingActionButton fab_gallery = findViewById(R.id.fab_gallery);
        FloatingActionButton fab_photo = findViewById(R.id.fab_photo);

        storageReference = FirebaseStorage.getInstance().getReference();

        //click sur le bouton photo
        fab_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "click sur Bouton de camera", Toast.LENGTH_SHORT).show();
                askCameraPermissions();
            }
        });
        //click sur le bouton gallery
        fab_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "click sur Bouton de galerie", Toast.LENGTH_SHORT).show();
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });

    }

    //demande de permission
    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }else {
            dispatchTakePictureIntent();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                /* open camera*/
                dispatchTakePictureIntent();
                /*
                openCamera();
                 */
            } else {
                Toast.makeText(this, "Permission necessaire pour utiliser la caméra", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //méthode pour ouverture de caméra
    private void openCamera() {
        Toast.makeText(this, "Ouverture de la caméra...", Toast.LENGTH_SHORT).show();
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera,CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //traitement image photo
        if (requestCode == CAMERA_REQUEST_CODE){
            /*Bitmap image = (Bitmap) data.getExtras().get("data");
            selectedImage.setImageBitmap(image);*/
            if (resultCode == Activity.RESULT_OK){
                File f = new File(currentPhotoPath);
                /*
                selectedImage.setImageURI(Uri.fromFile(f));
                 */
                Log.d("onActivity","Absolute Url of image is "+Uri.fromFile(f));

                //ajout d'une photo dans la galerie
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                //creation du nom de l'image
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
                //upload de la photo dans le serveur firebase
                uploadImageToFirebase(f.getName(),contentUri);
            }
        }
        //traitement image galerie
        if (requestCode == GALLERY_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                //creation du nom de l'image
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_"+getFileExt(contentUri);
                Log.d("Gallery request ","onActivityResult: Gallery Image Uri "+ imageFileName);
                /*
                selectedImage.setImageURI(contentUri);
                 */
                //upload de l'image dans le serveur firebase
                uploadImageToFirebase(imageFileName,contentUri);
            }
        }
    }

    //méthode d'ajout sur serveur firebase
    private void uploadImageToFirebase(String name, Uri contentUri) {
        //endroit de stockage dans le serveur -> modifier et gérer plusieurs "name" ?
        StorageReference image = storageReference.child("pictures/"+ name);
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        //download de l'image précédement upload -> permet de savoir si elle a bien été upload via la bibliothèque Picasso
                        Log.d("tag", "onSuccess: Uploaded Image URl is " + uri.toString());
                        Picasso.get().load(uri).into(selectedImage);
                    }
                });

                Toast.makeText(MainActivity.this, "Image Is Uploaded.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //cas d'echec de l'upload
                Toast.makeText(MainActivity.this, "Upload Failled.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //métohode pour ouvrir la caméra -> dispo sur documentation
    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }

    //métohode pour ouvrir la caméra -> dispo sur documentation
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        /* ancienne technique de stockage
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
         */
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    //métohode pour ouvrir la caméra -> dispo sur documentation
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.dev.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }
}
