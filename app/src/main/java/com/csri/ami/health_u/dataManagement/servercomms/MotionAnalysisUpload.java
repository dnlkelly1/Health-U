package com.csri.ami.health_u.dataManagement.servercomms;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.MotionFileProcessor;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 08/10/2015.
 */
public class MotionAnalysisUpload extends AsyncTask<String, String, String>
{
    JSONParser jParser = new JSONParser();
    Context t;
    UploadManager upMan;



    public MotionAnalysisUpload(Context context,UploadManager uploadManager)
    {
        upMan = uploadManager;
        t = context;
        //new UploadMotionFile().execute();
    }


    //class UploadMotionFile extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute() {

        }

        /**
         * getting All products from url
         */
        protected String doInBackground(String... args)
        {
            String status = "";
            int userID = Register.GetUserID(t);
            if(userID == -1)
            {
                try{Thread.sleep(2000);}catch (InterruptedException ex){}//if no id found...wait 2 seconds and see if a new one has been regestered
            }
            userID = Register.GetUserID(t);
            String userID_s = Integer.toString(userID);



            if(userID != -1)
            {
                String[] fileData = getMotionAnalyisFileData();

                if(fileData != null && fileData.length > 0)
                {
                    String line = "";
                    for(int i=0;i<fileData.length;i++)
                    {

                        line += fileData[i] + "\n";

                    }

                    List<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("userID", userID_s));
                    params.add(new BasicNameValuePair("data", line));
                    //  params.add(new BasicNameValuePair("datetime", time));
                    //  params.add(new BasicNameValuePair("f1", "1"));
                    //  params.add(new BasicNameValuePair("f2", "2"));
                    //  params.add(new BasicNameValuePair("f3", "3"));
                    //  params.add(new BasicNameValuePair("f4", "4"));
                    // getting JSON string from URL
                    JSONObject json = jParser.makeHttpRequest(ConnectionInfo.url_motionFileAdd, "POST", params);
                    if(json != null)
                    {
                        try {
                            int success = json.getInt(ConnectionInfo.TAG_SUCCESS);
                            if (success == 1) {
                                status = ConnectionInfo.STATUS_SUCCESS;
                            } else {
                                status = ConnectionInfo.STATUS_REQUEST_ERROR;
                            }
                        } catch (JSONException ex) {
                            status = ConnectionInfo.STATUS_JSON_ERROR;
                        }
                    }
                    else
                    {
                        status = ConnectionInfo.STATUS_CONNECTION_ERROR;
                    }
                }
                else
                {
                    status = ConnectionInfo.STATUS_FILEREAD_ERROR;
                }
            }
            else
            {
                status = ConnectionInfo.STATUS_USERNOTFOUND_ERROR;
            }

            return status;
        }

        public void Finish()
        {

            File f = Environment.getExternalStorageDirectory();
            String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;


            File fileToDel = new File(fullfilename, MotionFileProcessor.MOTION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD);
            if(fileToDel.exists())
            {
                fileToDel.delete();
            }
        }

        protected String[] getMotionAnalyisFileData()
        {
            ArrayList<String> data = new ArrayList<String>();

            File f = Environment.getExternalStorageDirectory();
            String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;

            File fileToRead = new File(fullfilename, MotionFileProcessor.MOTION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD);
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(fileToRead));

                String line = reader.readLine();

                while (line != null)
                {
                    data.add(line);
                    line = reader.readLine();
                }
                reader.close();

            }
            catch (java.io.IOException ex){}
            //Scanner scanner = new Scanner(fileToRead);


            return data.toArray(new String[data.size()]);
        }

        /**
         * After completing background task Dismiss the progress dialog
         **/
        protected void onPostExecute(String file_url) {

            if(upMan != null)
            {
                upMan.MotionFinised(file_url);
            }

        }
    //}

}
