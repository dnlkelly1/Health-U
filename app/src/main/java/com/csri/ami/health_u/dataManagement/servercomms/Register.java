package com.csri.ami.health_u.dataManagement.servercomms;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by daniel on 08/10/2015.
 */
public class Register
{
    public static String PREFS_REGISTER = "prefsregister";
    public static String PREFS_REGISTER_DEVICE_ID = "prefsregister_DEVID";
    public static String PREFS_REGISTER_USER_ID = "prefsregister";

    JSONArray userInfo = null;
    JSONParser jParser = new JSONParser();

    String deviceId = null;
    String CountryID = "";
    boolean showDialog;

    ProgressDialog progressDialog;

    Context context;

    public Register(Context t,boolean dialog)
    {
        showDialog = dialog;
        context = t;
        TelephonyManager telephonyManager = (TelephonyManager)t.getSystemService(Context.TELEPHONY_SERVICE);

        String android_id = Settings.Secure.getString(t.getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceId = MD5(android_id);

        if(telephonyManager != null) {
            //deviceId = telephonyManager.getDeviceId();


            try
            {
                CountryID = telephonyManager.getSimCountryIso().toUpperCase();
                CountryID = CountryID.trim();

            }
            catch (Exception ex){}

        }



        new UpdateCurrentDeviceOnServer().execute();

    }

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static int GetUserID(Context t)
    {
        int id_int=-1;
        try {
            SharedPreferences pref = t.getSharedPreferences(PREFS_REGISTER, Context.MODE_PRIVATE);
            String id = pref.getString(PREFS_REGISTER_USER_ID, "-1");

            id_int = Integer.parseInt(id);

            if (id_int == -1) {
                Register r = new Register(t, false);
            }



        }
        catch (Exception ex)
        {

        }
        finally
        {
            return id_int;
        }

    }

    ////////////////
    class UpdateCurrentDeviceOnServer extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute()
        {
//            super.onPreExecute();
//            if(showDialog)
//            {
//                progressDialog = new ProgressDialog(context, R.style.MyDialogTheme);
//                progressDialog.setMessage(context.getResources().getString(R.string.uploading));
//                progressDialog.setCancelable(false);
//                progressDialog.show();
//            }
        }

        /**
         * getting All products from url
         */
        protected String doInBackground(String... args) {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("devID",deviceId));

            // getting JSON string from URL
            JSONObject json = jParser.makeHttpRequest(ConnectionInfo.url_deviceIDcheck, "POST", params);


            if(json != null) {
                try {

                    // Checking for SUCCESS TAG
                    int success = json.getInt(ConnectionInfo.TAG_SUCCESS);

                    if (success == 1)//user found
                    {
                        // products found
                        // Getting Array of Products
                        userInfo = json.getJSONArray(ConnectionInfo.TAG_USERS);

                        AddUserInfoToPreferences();
                    } else if (success == 0)//connection was good, but no user found....means we need to add user
                    {
                        List<NameValuePair> params2 = new ArrayList<NameValuePair>();
                        params2.add(new BasicNameValuePair("devID",deviceId));
                        params2.add(new BasicNameValuePair("countryID",CountryID));

                        JSONObject json_add = jParser.makeHttpRequest(ConnectionInfo.url_deviceIDadd, "POST", params2);

                        int success_add = json_add.getInt(ConnectionInfo.TAG_SUCCESS);

                        if (success_add == 1) {
                            userInfo = json_add.getJSONArray(ConnectionInfo.TAG_USERS);
                            AddUserInfoToPreferences();
                        }


                    } else//error occured
                    {

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        protected void AddUserInfoToPreferences()
        {
            try {
                for (int i = 0; i < userInfo.length(); i++) {
                    JSONObject c = userInfo.getJSONObject(i);

                    // Storing each json item in variable

                    String id = c.getString(ConnectionInfo.TAG_UID);
                    String name = c.getString(ConnectionInfo.TAG_DEVICE);

                    if (deviceId.compareTo(name) == 0) {
                        SharedPreferences.Editor pref = context.getSharedPreferences(PREFS_REGISTER, Context.MODE_PRIVATE).edit();
                        pref.putString(PREFS_REGISTER_DEVICE_ID, name);
                        pref.putString(PREFS_REGISTER_USER_ID, id);
                        pref.apply();
                    }

                }
            }
            catch(JSONException ex){}
        }

        /**
         * After completing background task Dismiss the progress dialog
         **/
        protected void onPostExecute(String file_url) {
//            if(showDialog)
//            {
//                progressDialog.dismiss();
//            }

        }
    }
    ////////////////
}
