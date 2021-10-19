package com.civilsafety.civilassess.activitys;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.civilsafety.civilassess.Common.Common;
import com.civilsafety.civilassess.Common.GpsTracker;
import com.civilsafety.civilassess.LocalManage.DatabaseHelper;
import com.civilsafety.civilassess.LocalManage.ElementValueDatabaeHelper;
import com.civilsafety.civilassess.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class SuccessActivity extends AppCompatActivity implements LocationListener {
    private SQLiteDatabase db,VDb;
    private SQLiteOpenHelper openHelper,ElementValueopenHeloer;
    RequestQueue queue;
    String Token, formid, Vid;
    static int recordId = 0;
    TextView textView;
    RelativeLayout loading;
    HashMap<String, String> formData = new HashMap<String, String>();
    HashMap<String, String> photoData = new HashMap<String, String>();
    double longitude;
    double latitude;

    // The minimum time between updates in milliseconds
    public static final long UPDATE_INTERVAL = 1000;
    // The minimum distance to change updates in meters
    public static final long MIN_DISTANCE = 5;
    // To Store LocationManager.GPS_PROVIDER or LocationManager.NETWORK_PROVIDER
    private String PROVIDER_NAME;

    public static final int LOCATION_PERMISSION = 100;
    protected LocationManager locationManager;
    private static  String TAG = SuccessActivity.class.getName();
    boolean  isGPSTrackingEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        getSupportActionBar().hide();

        FormActivity.sigleElementArray.clear();
        FormActivity.numberElementArray.clear();
        FormActivity.emailElementArray.clear();

        Intent intent = getIntent();
        formData = (HashMap<String, String>) FormActivity.element_data;
        Log.e("elementData", String.valueOf(formData));
        formid = intent.getStringExtra("FormId");

        photoData = (HashMap<String, String>) FormActivity.elementPhotos_send;

        for (Map.Entry<String, Bitmap> entry : FormActivity.elementSignature.entrySet()) {
            String key = entry.getKey();
            Bitmap value = entry.getValue();
            String image = "data:image/png;base64," + toBase64(value);
            formData.put(key, image);
        }

        openHelper = new DatabaseHelper(this);
        ElementValueopenHeloer = new ElementValueDatabaeHelper(this);
        db = openHelper.getWritableDatabase();
        VDb = ElementValueopenHeloer.getWritableDatabase();

        final Cursor cursor = db.rawQuery("SELECT *FROM " + DatabaseHelper.TABLE_NAME,  null);
        if(cursor != null){
            if (cursor.moveToFirst()){
                do{
                    Token = cursor.getString(cursor.getColumnIndex("token"));
                }while(cursor.moveToNext());
            }
            cursor.close();
        }

        loading = findViewById(R.id.loadingSucees);
        textView = findViewById(R.id.successText);
        TextView back = findViewById(R.id.back_main);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SuccessActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        if (checkPermission()) {
            getLocation();
        }
    }

    @Override
    public void onLocationChanged(Location mLastLocation) {
    	if(mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
     	}
    	Log.d("longitude", String.valueOf(longitude));
    	Log.d("latitude", String.valueOf(latitude));
        sendcheck();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    public void getLocation() {
    	try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // Try to get location if you GPS Service is enabled
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                isGPSTrackingEnabled = true;;
                // To store service provider name
                PROVIDER_NAME = LocationManager.GPS_PROVIDER;
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                isGPSTrackingEnabled = true;;
                // To store service provider name
                PROVIDER_NAME = LocationManager.NETWORK_PROVIDER;
            }

            // Application can use GPS or Network Provider
            if (!PROVIDER_NAME.isEmpty() && locationManager != null && checkPermission()) {
                locationManager.requestLocationUpdates(PROVIDER_NAME, UPDATE_INTERVAL, MIN_DISTANCE, (LocationListener) this);
            }

        } catch (NullPointerException ed) {
            ed.printStackTrace();
        }
        // if GPS is not enabled
        if (!isGPSTrackingEnabled) {
            dialogSettings();
        }
    }


    public void dialogSettings() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("GPS Error");
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings?");
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    //runtime location permission
    public boolean checkPermission() {
    	int ACCESS_FINE_LOCATION = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
    	int ACCESS_COARSE_LOCATION = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
    	if (ACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED && ACCESS_COARSE_LOCATION != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION);
            return false;
    	}
    	return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    	super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        }
    }

    private void sendcheck() {
        String url = Common.getInstance().getSaveUrl();
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(response);
                            String result = jsonObject.getString("success");
                            if (result.equals("true")){
                                Vid = jsonObject.getString("id");
                                PsendData(Vid);
                                Log.e("Vid value", Vid );
                                FormActivity.elementSignature.clear();
                                FormActivity.element_data.clear();
                            } else {
                                loading.setVisibility(View.GONE);
                                textView.setText("false");
                            }
                        } catch (JSONException e) {
                            loading.setVisibility(View.GONE);
                            e.printStackTrace();
                            Toast.makeText(SuccessActivity.this, "Request faild", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(SuccessActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("senddata Error:", String.valueOf(error));
                        loading.setVisibility(View.GONE);
                        Toast.makeText(SuccessActivity.this, getResources().getString(R.string.send_faild), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SuccessActivity.this, MainActivity.class);
                        startActivity(intent);
                        textView.setText(getResources().getString(R.string.send_faild));

                        recordId += 1;
                        for (Map.Entry<String, String> entry : formData.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            insertData(key, value, formid);
                        }

                        for (Map.Entry<String, String> entry : photoData.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            insertData(key, value, formid);
                        }
//                        Toast.makeText(SuccessActivity.this, getResources().getString(R.string.offline_text), Toast.LENGTH_LONG).show();

                        FormActivity.elementPhotos.clear();
                        FormActivity.elementPhotos_send.clear();
                        FormActivity.element_filePath.clear();
                        FormActivity.element_data.clear();
                        FormActivity.elementSignature.clear();
                    }
                }){

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("token", Token);
                return headers;
            }

            @Override
            protected Map<String, String> getParams()
            {
                formData.put("formId", formid);
                formData.put("id", "0");
                formData.put("longitude", String.valueOf(longitude));
                formData.put("latitude", String.valueOf(latitude));
                Log.d("formdata:", String.valueOf(formData));
                Log.d("token:", Token);
                return formData;
            }
        };
        queue = Volley.newRequestQueue(SuccessActivity.this);
        queue.add(postRequest);
    }

    private void PsendData(final String id) {
        String url = Common.getInstance().getSaveUrl();
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onResponse(String response) {
                        Log.e("server request", response );
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(response);
                            String result = jsonObject.getString("success");
                            if (result.equals("true")){
                                loading.setVisibility(View.GONE);
                                FormActivity.elementPhotos_send.clear();
                                FormActivity.elementPhotos.clear();
                                FormActivity.element_filePath.clear();

                                textView.setText(getResources().getString(R.string.success));
                                Toast.makeText(SuccessActivity.this, getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SuccessActivity.this, MainActivity.class);
                                startActivity(intent);

                            } else {
                                loading.setVisibility(View.GONE);
                                Toast.makeText(SuccessActivity.this, "Oops, Request failed.", Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
//                            e.printStackTrace();
                            loading.setVisibility(View.GONE);
                            Log.e("send res okay",e.toString() );
                            Toast.makeText(SuccessActivity.this, "request faild", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loading.setVisibility(View.GONE);
                        System.out.println(error);
                        Intent intent = new Intent(SuccessActivity.this, MainActivity.class);
                        startActivity(intent);
                        Toast.makeText(SuccessActivity.this, getResources().getString(R.string.offline_text), Toast.LENGTH_LONG).show();
                    }
                }){

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("token", Token);
                return headers;
            }

            @Override
            protected Map<String, String> getParams()
            {
                photoData.put("formId", formid);
                photoData.put("id", id);
                photoData.put("Final", "end");
                return photoData;
            }
        };
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                2500000, 0, 1f
        ));
        queue = Volley.newRequestQueue(SuccessActivity.this);
        queue.add(postRequest);
    }

    public String toBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void insertData(String elementkye, String elementValue, String elementformid) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ElementValueDatabaeHelper.VCOL_2, elementkye);
        contentValues.put(ElementValueDatabaeHelper.VCOL_3, elementValue);
        contentValues.put(ElementValueDatabaeHelper.VCOL_4, elementformid);
        contentValues.put(ElementValueDatabaeHelper.VCOL_5, String.valueOf(recordId));
        Log.e(String.valueOf(recordId), String.valueOf(contentValues));
        VDb.insert(ElementValueDatabaeHelper.VTABLE_NAME,null,contentValues);
    }
}
