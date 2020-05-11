package com.sas.test.cloudvision;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static String accessToken;
    static final int REQUEST_GALLERY_IMAGE = 10;
    static final int REQUEST_CODE_PICK_ACCOUNT = 11;
    static final int REQUEST_ACCOUNT_AUTHORIZATION = 12;
    static final int REQUEST_PERMISSIONS = 13;
    private final String LOG_TAG = "MainActivity";
    Account mAccount;


    Toolbar toolbar;
    TextView labelHeader;
    private ImageView ivSelectedImage;
    private TextView tvResults;

    int currentSelection = 0;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        labelHeader = findViewById(R.id.tv_labels);

        Button btnSelectImage = (Button) findViewById(R.id.btn_choose_image);

        ivSelectedImage = (ImageView) findViewById(R.id.iv_imageView);
        tvResults = (TextView) findViewById(R.id.tv_label_results);
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /** OAuth Based **/

//                ActivityCompat.requestPermissions(HomeActivity.this,
//                        new String[]{Manifest.permission.GET_ACCOUNTS},
//                        REQUEST_PERMISSIONS);

                /** OAuth Based end **/


                launchImagePicker();

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.opt_text_detection:
                currentSelection = 0;
                setActionBarAndResultHeader(R.string.text_detection, R.string.text_identified);
                return true;
            case R.id.opt_label_detection:
                currentSelection = 1;
                setActionBarAndResultHeader(R.string.lable_detection, R.string.text_lables);
                return true;
            case R.id.opt_landmark_detection:
                currentSelection = 2;
                setActionBarAndResultHeader(R.string.landmark_det, R.string.text_landmark);
                return true;
            case R.id.opt_facial_detection:
                currentSelection = 3;
                setActionBarAndResultHeader(R.string.facial_detection, R.string.text_face);
                return true;
            case R.id.opt_logo_detection:
                currentSelection = 4;
                setActionBarAndResultHeader(R.string.logo_detection, R.string.text_logo);
                return true;
            case R.id.opt_safe_search_detection:
                currentSelection = 5;
                setActionBarAndResultHeader(R.string.safe_search, R.string.text_safe);
                return true;
            case R.id.opt_web_detection:
                currentSelection = 6;
                setActionBarAndResultHeader(R.string.web_detection, R.string.text_web_links);
                return true;
            case R.id.opt_img_properties:
                currentSelection = 7;
                setActionBarAndResultHeader(R.string.img_props, R.string.text_img_porps);
                return true;
            case R.id.opt_obj_locale:
                currentSelection = 8;
                setActionBarAndResultHeader(R.string.object_locale, R.string.text_obj_locale);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setActionBarAndResultHeader(int stringID, int text_identified) {
        toolbar.setTitle(getString(stringID));
        setSupportActionBar(toolbar);
        labelHeader.setText(getString(text_identified));
    }

    private void launchImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select an image"),
                REQUEST_GALLERY_IMAGE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getAuthToken();
                } else {
                    Toast.makeText(HomeActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK && data != null) {
            openProgressDialog();
            uploadImage(data.getData());
        } else if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                AccountManager am = AccountManager.get(this);
                Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
                for (Account account : accounts) {
                    if (account.name.equals(email)) {
                        mAccount = account;
                        break;
                    }
                }
                getAuthToken();
            } else if (resultCode == RESULT_CANCELED) {
                closeProgressDialog();
                Toast.makeText(this, "No Account Selected", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == REQUEST_ACCOUNT_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                Bundle extra = data.getExtras();
                onTokenReceived(extra.getString("authtoken"));
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Authorization Failed", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                Bitmap bitmap = resizeBitmap(
                        MediaStore.Images.Media.getBitmap(getContentResolver(), uri));
                callCloudVision(bitmap);
                ivSelectedImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                closeProgressDialog();
                Log.e(LOG_TAG, e.getMessage());
            }
        } else {
            closeProgressDialog();
            Log.e(LOG_TAG, "Null image was returned.");
        }
    }
    private void callCloudVision(final Bitmap bitmap) throws IOException {
        tvResults.setText("Retrieving results from cloud");
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {

                    /** OAuth Based **/

//                    GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
//                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
//                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
//                    Vision.Builder builder = new Vision.Builder
//                            (httpTransport, jsonFactory, credential);
//                    Vision vision = builder.build();
                    /** OAuth Based **/



                    /** API KEY **/
                    Vision.Builder visionBuilder = new Vision.Builder(
                            new NetHttpTransport(),
                            new AndroidJsonFactory(),
                            null);

                    visionBuilder.setVisionRequestInitializer(
                            new VisionRequestInitializer("YOUR_API_KEY"));
                    Vision vision = visionBuilder.build();

                    /** API KEY End**/


                    List<Feature> featureList = new ArrayList<>();

                    switch (currentSelection){
                        case 0:     //Text Detection
                            Feature textDetection = new Feature();
                            textDetection.setType("TEXT_DETECTION");
                            textDetection.setMaxResults(10);
                            featureList.add(textDetection);
                            break;
                        case 1:     //Label Detection
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(10);
                            featureList.add(labelDetection);
                            break;
                        case 2:     //Landmark Detection
                            Feature landmarkDetection = new Feature();
                            landmarkDetection.setType("LANDMARK_DETECTION");
                            landmarkDetection.setMaxResults(10);
                            featureList.add(landmarkDetection);
                            break;
                        case 3:     //Facial Detection
                            Feature desiredFeature = new Feature();
                            desiredFeature.setType("FACE_DETECTION");
                            desiredFeature.setMaxResults(10);
                            featureList.add(desiredFeature);
                        case 4:     //Logo Detection
                            Feature logoDetection = new Feature();
                            logoDetection.setType("LOGO_DETECTION");
                            logoDetection.setMaxResults(10);
                            featureList.add(logoDetection);
                            break;
                        case 5:
                            break;
                        case 6:
                            break;
                        case 7:
                            break;
                    }

                    List<AnnotateImageRequest> imageList = new ArrayList<>();
                    AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
                    Image base64EncodedImage = getBase64EncodedJpeg(bitmap);
                    annotateImageRequest.setImage(base64EncodedImage);
                    annotateImageRequest.setFeatures(featureList);
                    imageList.add(annotateImageRequest);
                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(imageList);
                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(LOG_TAG, "sending request");
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response , currentSelection);
                } catch (GoogleJsonResponseException e) {
                    Log.e(LOG_TAG, "Request failed: " + e.getContent());
                } catch (IOException e) {
                    Log.d(LOG_TAG, "Request failed: " + e.getMessage());
                }
                return "Cloud Vision API request failed.";
            }
            protected void onPostExecute(String result) {
                tvResults.setText(result);
                closeProgressDialog();

            }
        }.execute();
    }

    //TODO
    private String convertResponseToString(BatchAnnotateImagesResponse response , int currentSelection) {
        StringBuilder message = new StringBuilder("Results:\n\n");

        switch (currentSelection){
            case 0:
                message.append("Texts:\n");
                List<EntityAnnotation> texts = response.getResponses().get(0)
                        .getTextAnnotations();
                if (texts != null) {
                    for (EntityAnnotation text : texts) {
                        message.append(String.format(Locale.getDefault(), "%s: %s",
                                text.getLocale(), text.getDescription()));
                        message.append("\n");
                    }
                } else {
                    message.append("nothing\n");
                }
                break;
            case 1:
                List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
                if (labels != null) {
                    for (EntityAnnotation label : labels) {
                        message.append(String.format(Locale.getDefault(), "%.3f: %s",
                                label.getScore(), label.getDescription()));
                        message.append("\n");
                    }
                } else {
                    message.append("nothing\n");
                }
                break;
            case 2:
                message.append("Landmarks:\n");
                List<EntityAnnotation> landmarks = response.getResponses().get(0)
                        .getLandmarkAnnotations();
                if (landmarks != null) {
                    for (EntityAnnotation landmark : landmarks) {
                        message.append(String.format(Locale.getDefault(), "%.3f: %s",
                                landmark.getScore(), landmark.getDescription()));
                        message.append("\n");
                    }
                } else {
                    message.append("nothing\n");
                }
                break;
            case 3:
                message.append("Face Detection:\n");
                List<FaceAnnotation> faces = response.getResponses().get(0)
                        .getFaceAnnotations();
                if (faces != null) {
                    for (FaceAnnotation face : faces) {
                        message.append(String.format(Locale.getDefault(), "%s",
                                face.getJoyLikelihood()));
                        message.append("\n");
                    }
                } else {
                    message.append("nothing\n");
                }
                break;
            case 4:
                message.append("Logo Detection:\n");
                List<EntityAnnotation> logos = response.getResponses().get(0)
                        .getLogoAnnotations();
                if (logos != null) {
                    for (EntityAnnotation logo : logos) {
                        message.append(String.format(Locale.getDefault(), "%s",
                                logo.getDescription()));
                        message.append("\n");
                    }
                } else {
                    message.append("nothing\n");
                }
                break;


        }

        return message.toString();
    }
    public Bitmap resizeBitmap(Bitmap bitmap) {
        int maxDimension = 1024;
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }
    public Image getBase64EncodedJpeg(Bitmap bitmap) {
        Image image = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        image.encodeContent(imageBytes);
        return image;
    }
    private void pickUserAccount() {
        String[] accountTypes = new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }
    private void getAuthToken() {
        String SCOPE = "oauth2:https://www.googleapis.com/auth/cloud-platform";
        if (mAccount == null) {
            pickUserAccount();
        } else {
            new GetOAuthToken(HomeActivity.this, mAccount, SCOPE, REQUEST_ACCOUNT_AUTHORIZATION)
                    .execute();
        }
    }
    public void onTokenReceived(String token){
        accessToken = token;
        launchImagePicker();
    }

    public void openProgressDialog(){
        progress=new ProgressDialog(this);
        progress.setMessage("Processing in Cloud");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setProgress(0);
        progress.show();
    }
    public void closeProgressDialog(){
        progress.hide();
    }
}