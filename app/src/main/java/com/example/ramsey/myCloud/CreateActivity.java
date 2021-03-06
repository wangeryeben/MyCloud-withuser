package com.example.ramsey.myCloud;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateActivity extends AppCompatActivity {
    private EditText inputTitle, inputSource, inputPosition, inputDescription;
    private TextInputLayout inputLayoutTitle, inputLayoutSource,  inputLayoutPosition, inputLayoutDescription;
    private FloatingActionButton fab_delete, fab_upload;
    private Button b_photo;
    private SQLiteHandler db;
    private ProgressDialog pDialog;
    private String image_uid;

    private static final String TAG = CreateActivity.class.getSimpleName();

    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;

    public static final int MEDIA_TYPE_IMAGE = 1;

    private Uri fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // file url to store image

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);
        Toolbar toolbar2 = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar2);


        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();


        inputLayoutTitle = (TextInputLayout) findViewById(R.id.create_layout_title);
        inputLayoutSource = (TextInputLayout) findViewById(R.id.create_layout_source);
        inputLayoutPosition = (TextInputLayout) findViewById(R.id.create_layout_position);
        inputLayoutDescription = (TextInputLayout) findViewById(R.id.create_layout_description);

        inputTitle = (EditText) findViewById(R.id.create_title);
        inputSource = (EditText) findViewById(R.id.create_source);
        inputPosition = (EditText) findViewById(R.id.create_position);
        inputDescription = (EditText) findViewById(R.id.create_description);

        fab_delete = (FloatingActionButton) findViewById(R.id.btn_delete);
        fab_upload = (FloatingActionButton) findViewById(R.id.btn_upload);

        // SQLite database handler
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        b_photo=(Button) findViewById(R.id.btn_photo);

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Fetching user details from sqlite
        HashMap<String, String> user = db.getUserDetails();

        Intent intent =getIntent();
        final String image_uid=intent.getStringExtra("image_uid");
        Log.d(TAG, "onCreate: "+image_uid );

        final String finder = user.get("uid");
        final String process = user.get("process");
        Log.d(TAG, "onCreate: "+finder+"   "+process);
        fab_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String title = inputTitle.getText().toString().trim();
                String prob_describe = inputDescription.getText().toString().trim();
                String prob_source = inputSource.getText().toString().trim();
                String position = inputPosition.getText().toString().trim();

                if (submitForm()) {
                    uploadProblem(title, prob_source, prob_describe, position, process, finder, image_uid);
                }

                else{
                    Toast.makeText(CreateActivity.this, "出现错误！", Toast.LENGTH_SHORT).show();
                }
            }
        });
        fab_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view,"即将清空所有文本框",Snackbar.LENGTH_SHORT).setAction("确定",new View.OnClickListener(){
                    @Override
                    public void onClick(View v)
                    {
                        delete();
                        Toast.makeText(CreateActivity.this,"已清空所有文本框",Toast.LENGTH_SHORT).show();
                    }
                }).show();
            }
        });
        b_photo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // capture picture
                captureImage();
                Toast.makeText(CreateActivity.this,"拍照",Toast.LENGTH_SHORT).show();
            }
        });

        // Checking camera availability
        if (!isDeviceSupportCamera()) {
            Toast.makeText(getApplicationContext(),
                    "设备不支持相机！",
                    Toast.LENGTH_LONG).show();
            // will close the app if the device does't have camera
            finish();
        }
    }



    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    private boolean submitForm() {
        if(validate())
        {
            return true;
        }
        else
            return false;
    }


    private boolean validate() {
        if (inputTitle.getText().toString().trim().isEmpty()) {
            inputLayoutTitle.setError(getString(R.string.err_msg_title));
            return false;
        } else {
            inputLayoutTitle.setErrorEnabled(false);
            if (inputSource.getText().toString().trim().isEmpty()) {
                inputLayoutSource.setError(getString(R.string.err_msg_source));
                return false;
            } else {
                inputLayoutSource.setErrorEnabled(false);
                if (inputDescription.getText().toString().trim().isEmpty()) {
                    inputLayoutDescription.setError(getString(R.string.err_msg_description));
                    return false;
                } else {
                    inputLayoutDescription.setErrorEnabled(false);
                    {
                        if (inputPosition.getText().toString().trim().isEmpty()) {
                            inputLayoutPosition.setError(getString(R.string.err_msg_position));
                            return false;
                        } else {
                            inputLayoutPosition.setErrorEnabled(false);
                            return true;
                        }
                    }
                }
            }
        }
    }

    private void uploadProblem(final String title, final String prob_source,
                               final String prob_describe, final String position, final String process,
                               final String finder, final String image_uid){
        // Tag used to cancel the request
        String tag_string_req = "req_upload";

        Log.d(TAG, "uploadProblem: image_uid"+image_uid)
        ;
        pDialog.setMessage("Uploading ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_CREATE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Uploading Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        Toast.makeText(getApplicationContext(), "成功提交", Toast.LENGTH_SHORT).show();

                        // Launch user activity
                        Intent intent = new Intent(
                                CreateActivity.this,
                                User.class);
                        startActivity(intent);
                        finish();
                    }else {

                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Upload Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap();
                params.put("title", title);
                params.put("prob_source", prob_source);
                params.put("prob_describe", prob_describe);
                params.put("finder", finder);
                params.put("process", process);
                params.put("image_uid", image_uid);
                params.put("position", position);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }


    private void delete() {
        inputDescription.setText(null);
        inputSource.setText(null);
        inputTitle.setText(null);
        inputPosition.setText(null);
    }


    /**
     * Here we store the file url as it will be null after returning from camera
     * app
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on screen orientation
        // changes
        outState.putParcelable("file_uri", fileUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        fileUri = savedInstanceState.getParcelable("file_uri");
    }



    /**
     * Receiving activity result method will be called after closing the camera
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){

            // if the result is capturing Image
            case CAMERA_CAPTURE_IMAGE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {

                    // successfully captured the image
                    // launching upload activity
                    launchUploadActivity(true);


                } else if (resultCode == RESULT_CANCELED) {

                    // user cancelled Image capture
                    Toast.makeText(getApplicationContext(),
                            "User cancelled image capture", Toast.LENGTH_SHORT)
                            .show();

                } else {
                    // failed to capture image
                    Toast.makeText(getApplicationContext(),
                            "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                            .show();
                }

//            接收上传成功界面传过来的image_uid;
            case 10:
                if (resultCode ==200) {
                    image_uid = data.getStringExtra("image_uid");
                    Log.d(TAG, "onActivityResult: "+image_uid);
                }

        }
    }

    private void launchUploadActivity(boolean isImage){
        Intent i = new Intent(CreateActivity.this, UploadActivity.class);
        i.putExtra("filePath", fileUri.getPath());
        i.putExtra("isImage", isImage);
        startActivityForResult(i,10);
        Log.d(TAG, "launchUploadActivity: "+fileUri.getPath());
    }


    /**
     * ------------ Helper Methods ----------------------
     * */



    /**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                AppConfig.IMAGE_DIRECTORY_NAME);

        Log.d(TAG, "getOutputMediaFile: "+ mediaStorageDir);
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create "
                        + AppConfig.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    public void onBackPressed(){
        Intent reg_to_login=new Intent(CreateActivity.this,User.class);
        startActivity(reg_to_login);
        finish();
    }
}