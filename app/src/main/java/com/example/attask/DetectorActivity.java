package com.example.attask;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.attask.customview.OverlayView;
import com.example.attask.env.BorderedText;
import com.example.attask.env.ImageUtils;
import com.example.attask.env.Logger;
import com.example.attask.tflite.SimilarityClassifier;
import com.example.attask.tflite.TFLiteObjectDetectionAPIModel;
import com.example.attask.tracking.MultiBoxTracker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;


public class DetectorActivity extends CameraActivity implements ImageReader.OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    OutputStream outputStream;
    // MobileFaceNet
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";


    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    //private static final int CROP_SIZE = 320;
    //private static final Size CROP_SIZE = new Size(320, 320);


    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private SimilarityClassifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;
    private boolean addPending = false;
    //private boolean adding = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    //private Matrix cropToPortraitTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;

    // Face detector
    private FaceDetector faceDetector;

    // here the preview image is drawn in portrait way
    private Bitmap portraitBmp = null;

    // here the face is cropped and drawn
    private Bitmap faceBmp = null;

    private FloatingActionButton fabAdd;
    private String TAG = "Alhad";

    String loadImageName = "ken";

    private Integer validator = 0;
    private Integer smileVal = 0;

    //private HashMap<String, Classifier.Recognition> knownFaces = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Real-time contour detection of multiple faces
        FaceDetectorOptions options =

                new FaceDetectorOptions.Builder()
                        .enableTracking()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);
        faceDetector = detector;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        loadImage();
    }

    void loadImageFile(String name)
    {
        try {
            File imgFile = new File("/storage/emulated/0/Pictures/Face_Pics/"+name+".jpg");
            Bitmap bmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            InputImage image = InputImage.fromBitmap(bmp, 0);

            Bitmap finalBmp = bmp;
            faceDetector
                    .process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                        @Override
                        public void onSuccess(List<Face> faces) {
                            ++timestamp;
                            final long currTimestamp = timestamp;
                            if (faces.size() == 0)
                            {
                                //updateResults(currTimestamp, new LinkedList<>());
                                Log.d("FAVALResult","Face Size 0");
                                return;
                            }
                            runInBackground(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            onFacesDetected(currTimestamp, faces, true, true, finalBmp);
                                            addPending = false;
                                        }
                                    });

                        }
                    });
        }
        catch (Exception e)
        {
            Log.d("FAVALResult",e.toString());

        }
    }

    void loadImage()
    {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("attasksession", Context.MODE_PRIVATE);
            String image = sharedPreferences.getString("image", null);
//            "https://kingsmanpower.s3.ap-southeast-1.amazonaws.com/user_profile/20181546-M.jpg"
            URL url = new URL(image);
            Bitmap imagebit = BitmapFactory.decodeStream(url.openConnection().getInputStream());

            FileOutputStream outputStream = null;
            File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File dir = new File(file.getAbsolutePath() + "/Face_Pics");
            dir.mkdirs();

            String filename = String.format( loadImageName+ ".jpg");
            File outFile = new File(dir, filename);
            try {
                outputStream = new FileOutputStream(outFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            imagebit.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            try {
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();
                loadImageFile(loadImageName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            Log.d("FAVALResultdawdawzcszs",e.toString());
        }
    }

    private void onAddClick() {
        addPending = true;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);


        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);


        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        }
        else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888);

        portraitBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        Matrix frameToPortraitTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        targetW, targetH,
                        sensorOrientation, MAINTAIN_ASPECT);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    void saveImage(ImageView ivFace, String name)
    {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) ivFace.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();

        FileOutputStream outputStream = null;
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File dir = new File(file.getAbsolutePath() + "/Face_Pics");
        Log.d("FILE DIRECTORY",file.getAbsolutePath() + "/Face_Pics");
        dir.mkdirs();

        String filename = String.format(name + ".png");
        File outFile = new File(dir, filename);
        try {
            outputStream = new FileOutputStream(outFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        try {
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);

        faceDetector
                .process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.size() == 0) {
                            updateResults(currTimestamp, new LinkedList<>(), false);
                            return;
                        }
                        runInBackground(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        onFacesDetected(currTimestamp, faces, addPending, false,croppedBitmap );
                                        addPending = false;
                                    }
                                });
                    }

                });


    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }


    // Face Processing
    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }
        return matrix;
    }

    private void updateResults(long currTimestamp, final List<SimilarityClassifier.Recognition> mappedRecognitions,boolean isLoadImage) {

        tracker.trackResults(mappedRecognitions, currTimestamp);
        trackingOverlay.postInvalidate();
        computingDetection = false;

        if (mappedRecognitions.size() > 0) {
            SimilarityClassifier.Recognition rec = mappedRecognitions.get(0);
            if(isLoadImage)
            {
                detector.register(loadImageName,rec);
            }
        }
    }

    private void onFacesDetected(long currTimestamp, List<Face> faces, boolean add, boolean isLoadImage, Bitmap onLoadBitmap) {
        if(isLoadImage){
            onLoadBitmap = onLoadBitmap.copy(Bitmap.Config.ARGB_8888, true);
            onLoadBitmap = Bitmap.createBitmap(onLoadBitmap);
            final Canvas canvas = new Canvas(onLoadBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
                case TF_OD_API:
                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    break;
            }

            final List<SimilarityClassifier.Recognition> mappedRecognitions =
                    new LinkedList<SimilarityClassifier.Recognition>();

            // Note this can be done only once
            int sourceW = onLoadBitmap.getWidth();
            int sourceH = onLoadBitmap.getHeight();
            int targetW = portraitBmp.getWidth();
            int targetH = portraitBmp.getHeight();
            Matrix transform = createTransform(
                    sourceW,
                    sourceH,
                    targetW,
                    targetH,
                    360);
            final Canvas cv = new Canvas(portraitBmp);

            // draws the original image in portrait mode.
            cv.drawBitmap(onLoadBitmap, transform, null);
            Bitmap checker = portraitBmp;
            final Canvas cvFace = new Canvas(faceBmp);

            boolean saved = false;

            for (Face face : faces) {

                final RectF boundingBox = new RectF(face.getBoundingBox());

                final boolean goodConfidence = true; //face.get;
                if (boundingBox != null && goodConfidence) {
                    // maps original coordinates to portrait coordinates
                    RectF faceBB = new RectF(boundingBox);

                    float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                    float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                    Matrix matrix = new Matrix();
                    matrix.postTranslate(-faceBB.left, -faceBB.top);
                    matrix.postScale(sx, sy);

                    cvFace.drawBitmap(onLoadBitmap, matrix, null);

                    String label = "";
                    float confidence = -1f;
                    Integer color = Color.BLUE;
                    Object extra = null;
                    Bitmap crop = null;

                    if (add)
                    {
                        crop = Bitmap.createBitmap(onLoadBitmap,
                                (int) faceBB.left,
                                (int) faceBB.top,
                                (int) faceBB.width(),
                                (int) faceBB.height());
                    }

                    final long startTime = SystemClock.uptimeMillis();
                    final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, add);
                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                    Log.d("Score",String.valueOf(resultsAux.size()));
                    if (resultsAux.size() > 0) {

                        SimilarityClassifier.Recognition result = resultsAux.get(0);

                        extra = result.getExtra();

                        float conf = result.getDistance();
                        Log.d("Score",String.valueOf(conf));
                        if (conf <= 0.5f) {
                            confidence = conf;
                            label = result.getTitle();
                            if (result.getId().equals("0")) {
                                color = Color.GREEN;
                            }
                            else {
                                color = Color.RED;
                            }
                        }
                    }

                    final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                            "0", label, confidence, boundingBox);

                    result.setColor(color);
                    result.setLocation(boundingBox);
                    result.setExtra(extra);
                    result.setCrop(crop);
                    mappedRecognitions.add(result);

                    Log.d(TAG,"Distance: " + result.getDistance());
                }
            }
            updateResults(currTimestamp, mappedRecognitions, true);

        }else{
            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
                case TF_OD_API:
                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    break;
            }

            final List<SimilarityClassifier.Recognition> mappedRecognitions =
                    new LinkedList<SimilarityClassifier.Recognition>();

            // Note this can be done only once
            int sourceW = rgbFrameBitmap.getWidth();
            int sourceH = rgbFrameBitmap.getHeight();
            int targetW = portraitBmp.getWidth();
            int targetH = portraitBmp.getHeight();
            Matrix transform = createTransform(
                    sourceW,
                    sourceH,
                    targetW,
                    targetH,
                    sensorOrientation);
            final Canvas cv = new Canvas(portraitBmp);

            // draws the original image in portrait mode.
            cv.drawBitmap(rgbFrameBitmap, transform, null);

            final Canvas cvFace = new Canvas(faceBmp);

            boolean saved = false;

            for (Face face : faces) {
                //results = detector.recognizeImage(croppedBitmap);

                //Liveness Validation
                Log.d("FAVALResult",face.toString());

                if(face.getSmilingProbability() != null && face.getSmilingProbability() < 0.7){
                    smileVal = 1;
                }

                if(face.getSmilingProbability() != null && face.getSmilingProbability() > 0.7 && smileVal == 1){
                    validator++;
                }

                final RectF boundingBox = new RectF(face.getBoundingBox());

                //final boolean goodConfidence = result.getConfidence() >= minimumConfidence;
                final boolean goodConfidence = true; //face.get;
                if (boundingBox != null && goodConfidence) {

                    // maps crop coordinates to original
                    cropToFrameTransform.mapRect(boundingBox);

                    // maps original coordinates to portrait coordinates
                    RectF faceBB = new RectF(boundingBox);
                    transform.mapRect(faceBB);

                    // translates portrait to origin and scales to fit input inference size
                    //cv.drawRect(faceBB, paint);
                    float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                    float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                    Matrix matrix = new Matrix();
                    matrix.postTranslate(-faceBB.left, -faceBB.top);
                    matrix.postScale(sx, sy);

                    cvFace.drawBitmap(portraitBmp, matrix, null);

                    String label = "";
                    float confidence = -1f;
                    Integer color = Color.BLUE;
                    Object extra = null;
                    Bitmap crop = null;

                    if (add) {
                        crop = Bitmap.createBitmap(portraitBmp,
                                (int) faceBB.left,
                                (int) faceBB.top,
                                (int) faceBB.width(),
                                (int) faceBB.height());
                    }

                    final long startTime = SystemClock.uptimeMillis();
                    final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, add);
                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                    if(validator < 1){
                        color = Color.RED;
                    }
                    if (resultsAux.size() > 0) {

                        SimilarityClassifier.Recognition result = resultsAux.get(0);
                        extra = result.getExtra();

                        float conf = result.getDistance();

                        if (conf < 0.6f) {

                            confidence = conf;
                            label = result.getTitle();

                            if(validator < 1){
                                color = Color.YELLOW;
                                label = result.getTitle() + " SMILE!";
                            }else{
                                if (result.getId().equals("0")) {
                                    color = Color.GREEN;

                                    saveImageBitmapSendToSystem(cropCopyBitmap, result.getTitle());

                                    saveImageBitmap(cropCopyBitmap, result.getTitle());
                                }
                                else {
                                    color = Color.RED;
                                }
                            }
                        }
                    }

                    if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {

                        // camera is frontal so the image is flipped horizontally
                        // flips horizontally
                        Matrix flip = new Matrix();
                        if (sensorOrientation == 90 || sensorOrientation == 270) {
                            flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                        }
                        else {
                            flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                        }
                        //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                        flip.mapRect(boundingBox);

                    }
                    LOGGER.i("validator" + color.toString());
                    final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                            "0", label, confidence, boundingBox);

                    result.setColor(color);
                    result.setLocation(boundingBox);
                    result.setExtra(extra);
                    result.setCrop(crop);
                    mappedRecognitions.add(result);
                }
            }
            updateResults(currTimestamp, mappedRecognitions, false);
        }
    }

    void saveImageBitmapSendToSystem(Bitmap bitmap, String name)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

        try {

            SharedPreferences sharedPreferences = getSharedPreferences("attasksession", Context.MODE_PRIVATE);
            String userEmployeeid = sharedPreferences.getString("employeeid", null);

            /*initiate volley request*/
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String backendURL = "http://192.168.2.97/ptihris/index.php/Android_/test";

            JSONObject postData = new JSONObject();
            try {
                postData.put("rememberPassword", false);
                postData.put("ip_address", "1.41");
                postData.put("isCaptchaEnabled", false);
                postData.put("emp_id", "dwadw");
                postData.put("base_64", Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT | Base64.NO_WRAP));
//                Log.d("base64",Base64.encodeToString(outputStream.toByteArray(), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, backendURL, postData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                }
            });

            requestQueue.add(jsonObjectRequest);

            Toast.makeText(this, "Check In Success", Toast.LENGTH_LONG).show();

            Intent returnBtn = new Intent(this,
                    MainActivity.class);
            startActivity(returnBtn);
            finish();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ErrorListener implements Response.ErrorListener{
        @Override
        public void onErrorResponse(VolleyError error){

        }
    }

    void saveImageBitmap(Bitmap bitmap, String name)
    {

        FileOutputStream outputStream = null;
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File dir = new File(file.getAbsolutePath() + "/Face_Pics");
        Log.d("FILE DIRECTORY",file.getAbsolutePath() + "/Face_Pics");
        dir.mkdirs();

        String filename = String.format(name + "Succcess.jpg");
        File outFile = new File(dir, filename);
        try {
            outputStream = new FileOutputStream(outFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        try {
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
