package com.csri.ami.health_u.dataManagement.servercomms;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import com.csri.ami.health_u.dataManagement.analyze.graphing.GraphValue;
import com.csri.ami.health_u.dataManagement.analyze.graphing.GraphValueDay;
import com.csri.ami.health_u.dataManagement.analyze.graphing.LocationGraphValue;
import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.LocationAnalyser;
import com.csri.ami.health_u.dataManagement.analyze.dataAnalytics.sensorDataAnalysis.MotionFileProcessor;
import com.csri.ami.health_u.dataManagement.record.SensorRecorder;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 08/10/2015.
 */
public class LocationAnalysisUpload extends AsyncTask<String, String, String>
{
    JSONParser jParser = new JSONParser();
    Context t;
    UploadManager upMan;



    public LocationAnalysisUpload(Context context,UploadManager uploadManager)
    {
        upMan = uploadManager;
        t = context;
        //new UploadLocationFile().execute();
    }




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
            String[] fileData = getLocationAnalyisFileData();

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

                JSONObject json = jParser.makeHttpRequest(ConnectionInfo.url_locationFileAdd, "POST", params);
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

    private static int Find(GraphValue[] data,int index)
    {
        int location =-1;
        for(int i=0;i<data.length;i++)
        {
            if(data[i].timeSlotIndex == index)
            {
                location=i;
                break;
            }
        }
        return location;
    }


    public static String[] getLocationAnalyisFileData()
    {
        LocationGraphValue[] data = LocationGraphValue.Load(LocationAnalyser.LOCATION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD,false);
        if(data != null && data.length > 0)
        {
            GraphValue[][] days = GraphValueDay.GroupByDay(data);
            ArrayList<String> day_strings = new ArrayList<String>();

            int perDay = MotionFileProcessor.SAMPLES_PER_DAY;
            for(int i=0;i<days.length;i++)
            {
                if(days[i] != null && days[i].length > 0)
                {
                    String current = days[i][0].time.toString("dd/MM/yyyy") + ",";
                    for (int j = 0; j < perDay; j++) {
                        int index = Find(days[i], j);
                        if (index == -1) {
                            current += "-1,";
                        } else {
                            current += days[i][index].raw_value + ",";
                        }
                    }

                    day_strings.add(current);
                }
            }
            return  day_strings.toArray(new String[day_strings.size()]);
        }
        else
        {
            return null;
        }

    }

    /**
     * After completing background task Dismiss the progress dialog
     **/
    protected void onPostExecute(String file_url)
    {
        if(upMan != null)
        {
            upMan.LocationFinised(file_url);
        }
    }

    public void Finish()
    {

        File f = Environment.getExternalStorageDirectory();
        String fullfilename = f.getAbsoluteFile() + SensorRecorder.SAVEFOLDER;



        File fileToDel = new File(fullfilename, LocationAnalyser.LOCATION_FEATURES_SAVE_FILE_NAME_FOR_UPLOAD);
        if(fileToDel.exists())
        {
           fileToDel.delete();
        }
    }

}
