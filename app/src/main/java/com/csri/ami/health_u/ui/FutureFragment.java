package com.csri.ami.health_u.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.csri.ami.health_u.R;
import com.csri.ami.health_u.dataManagement.servercomms.ConnectionInfo;
import com.csri.ami.health_u.dataManagement.servercomms.JSONParser;
import com.csri.ami.health_u.dataManagement.servercomms.Register;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class FutureFragment extends Fragment{

    int hoursUserCount=-1;
    int hoursAllCount=-1;
    int UserCount=-1;
    int questionnaireByAll=-1;
    int questionnaireByUser=-1;

    public FutureFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume()
    {
        super.onResume();
        LoadStats();

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(statsFromServer != null)
        {
            statsFromServer.cancel(true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_future, container, false);

        Button b = (Button)v.findViewById(R.id.buttonQues);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                boolean connectionFound = SummaryDataUploadAlarm.ConnectivityStatus(getActivity());
                if(connectionFound) {
                    Intent intent = new Intent(getActivity(), Questionnaire.class);

                    startActivity(intent);
                }
                else
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.noconnection));
                    builder.setMessage(getResources().getString(R.string.noconnection_mes));
                    builder.setNeutralButton(getResources().getString(R.string.close), null);
                    builder.show();
                }
            }
        });

        // Inflate the layout for this fragment
        return v;
    }

    boolean downloadingCurrently = false;

    GetStatsFromServer statsFromServer;

    private void LoadStats()
    {
        if(UserCount == -1 && hoursAllCount == -1)
        {
            if(!downloadingCurrently)
            {
                statsFromServer = new GetStatsFromServer();
                statsFromServer.execute();
            }
        }
        else
        {
            LoadText();
        }
    }

    @Override
    public void setUserVisibleHint(boolean visible)
    {
        super.setUserVisibleHint(visible);
        if(visible)
        {
            LoadStats();
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

//        if(UserCount == -1 && hoursAllCount == -1) {
//            new GetStatsFromServer().execute();
//        }
//        else
//        {
//            LoadText();
//        }
    }




    class GetStatsFromServer extends AsyncTask<String, String, String> {

        JSONParser jParser = new JSONParser();


        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute()
        {

        }

        /**
         * getting All products from url
         */
        protected String doInBackground(String... args)
        {
            downloadingCurrently = true;
            int userID = Register.GetUserID(getContext());
            if(userID == -1)
            {
                try{Thread.sleep(2000);}catch (InterruptedException ex){}//if no id found...wait 2 seconds and see if a new one has been regestered
            }
            userID = Register.GetUserID(getContext());
            String userID_s = Integer.toString(userID);

            if(userID != -1) {
                // Building Parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("userID", userID_s));
                // getting JSON string from URL
                try
                {
                    JSONObject json = jParser.makeHttpRequest(ConnectionInfo.url_getStats, "POST", params);

                    // Check your log cat for JSON reponse
                    if(json != null) {
                        try {
                            // Checking for SUCCESS TAG
                            int success = json.getInt(ConnectionInfo.TAG_SUCCESS);


                            if (success == 5)//user found
                            {
                                JSONArray stats = json.getJSONArray("stats");
                                JSONObject o1 = stats.getJSONObject(0);


                                hoursUserCount = Integer.parseInt(o1.getString(ConnectionInfo.TAG_HOURSCOUNT_USER));
                                hoursAllCount = Integer.parseInt(o1.getString(ConnectionInfo.TAG_HOURSCOUNT_ALL));
                                UserCount = Integer.parseInt(o1.getString(ConnectionInfo.TAG_USER_COUNT));
                                questionnaireByAll = Integer.parseInt(o1.getString(ConnectionInfo.TAG_QUESTIONNAIRE_BY_ALL));
                                questionnaireByUser = Integer.parseInt(o1.getString(ConnectionInfo.TAG_QUESTIONNAIRE_BY_USER));
                            } else if (success == 0)//connection was good, but no user found....means we need to add user
                            {


                            } else//error occured
                            {

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            downloadingCurrently = false;
                        }
                    }

                }
                catch (Exception ex)
                {
                    downloadingCurrently = false;
                }
            }

            downloadingCurrently = false;
            return null;
        }



        /**
         * After completing background task Dismiss the progress dialog
         **/
        protected void onPostExecute(String file_url)
        {

            LoadText();

        }
    }

    private void LoadText()
    {

        try {
            TextView youText = (TextView) getView().findViewById(R.id.youText);
            TextView allUsersText = (TextView) getView().findViewById(R.id.allUsersText);

            TextView heading = (TextView)getView().findViewById(R.id.expprogress);
            TextView heading_you = (TextView)getView().findViewById(R.id.expprogress_you);
            TextView heading_all = (TextView)getView().findViewById(R.id.expprogress_all);


            if (hoursAllCount != -1)
            {
                int minHoursneeded = 72;

                SpannableString hoursUserString = null; generateCenterSpannableText(Integer.toString(hoursUserCount), "hours of data have been uploaded by you.");
                if(hoursUserCount > minHoursneeded)
                {
                    hoursUserString = generateCenterSpannableText(Integer.toString(hoursUserCount), "hours of data have been uploaded by you.");
                }
                else if(questionnaireByUser > 0 && hoursUserCount > 0 && hoursUserCount < minHoursneeded)
                {
                    int remaining = minHoursneeded - hoursUserCount;
                    hoursUserString = generateCenterSpannableText(Integer.toString(remaining), "more hours of data required by you. Well done, you are almost an official contributor to a scientific study.");
                }
                else if(hoursUserCount > 0 && hoursUserCount < minHoursneeded)
                {
                    int remaining = minHoursneeded - hoursUserCount;
                    hoursUserString = generateCenterSpannableText(Integer.toString(remaining), "more hours of data required by you.");
                }
                else if(hoursUserCount == 0)
                {

                    hoursUserString = generateCenterSpannableText(Integer.toString(minHoursneeded), "hours of data is required by you. Thank you for contributing to our scientific study.");
                }

                SpannableString questionnaireUserString=null;
                if(questionnaireByUser > 0)
                {
                    questionnaireUserString = generateCenterSpannableText_bold("You have submitted the questionnaire. Thank you!");

                }
                else
                {
                    questionnaireUserString = generateCenterSpannableText_bold("You have not submitted the questionnaire yet. Please remember to take the questionnaire");

                }

                Spanned message1 = (Spanned) TextUtils.concat(hoursUserString, "\n", questionnaireUserString);

                SpannableString message = new SpannableString(message1);

                youText.setText(message);

                SpannableString usersString = generateCenterSpannableText(Integer.toString(UserCount), "users are currently taking part in this experiment.");
                SpannableString hoursAllString = generateCenterSpannableText(Integer.toString(hoursAllCount), "hours of data have been uploaded in total.");
                SpannableString questionnaireAllString = generateCenterSpannableText(Integer.toString(questionnaireByAll), "users have currently submitted questionnaires.");

                Spanned message2 = (Spanned) TextUtils.concat(usersString, "\n", hoursAllString, "\n", questionnaireAllString);

                SpannableString m = new SpannableString(message2);

                allUsersText.setText(m);

                SpannableString expProgress = generateCenterSpannableText("Experiment Progress");
                heading.setText(expProgress);

                SpannableString expProgress_you = generateCenterSpannableText("You:");
                heading_you.setText(expProgress_you);

                SpannableString expProgress_all = generateCenterSpannableText("All Users:");
                heading_all.setText(expProgress_all);


            }
        }
        catch (Exception ex){}
    }

    private SpannableString generateCenterSpannableText_bold(String l1) {


        SpannableString s = new SpannableString(l1);

        Typeface chalk = Typeface.createFromAsset(getContext().getAssets(), "Denne.ttf");
//        s.setSpan(new RelativeSizeSpan(1.0f), 0, change, 0);
//
//        s.setSpan(new StyleSpan(Typeface.NORMAL), change, s.length() , 0);
//        s.setSpan(new ForegroundColorSpan(Color.GRAY), change, s.length() , 0);
//        s.setSpan(new RelativeSizeSpan(1.4f), change, s.length(), 0);
//
//        s.setSpan(new StyleSpan(Typeface.ITALIC), change, s.length(), 0);
//        s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), change, s.length(), 0);

        int c = Color.WHITE;// ContextCompat.getColor(getContext(), Color.WHITE);

        s.setSpan(new CustomTypefaceSpan("", chalk), 0, l1.length(), 0);
        s.setSpan(new ForegroundColorSpan(c), 0, l1.length(), 0);
        s.setSpan(new RelativeSizeSpan(0.8f), 0, l1.length(), 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, l1.length(), 0);

        return s;
    }


    private SpannableString generateCenterSpannableText(String l1) {


        SpannableString s = new SpannableString(l1);

        Typeface chalk = Typeface.createFromAsset(getContext().getAssets(), "Denne.ttf");
//        s.setSpan(new RelativeSizeSpan(1.0f), 0, change, 0);
//
//        s.setSpan(new StyleSpan(Typeface.NORMAL), change, s.length() , 0);
//        s.setSpan(new ForegroundColorSpan(Color.GRAY), change, s.length() , 0);
//        s.setSpan(new RelativeSizeSpan(1.4f), change, s.length(), 0);
//
//        s.setSpan(new StyleSpan(Typeface.ITALIC), change, s.length(), 0);
//        s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), change, s.length(), 0);

        int c = Color.WHITE;// ContextCompat.getColor(getContext(), Color.WHITE);

        s.setSpan(new CustomTypefaceSpan("", chalk), 0, l1.length(), 0);
        s.setSpan(new ForegroundColorSpan(c), 0, l1.length(), 0);
        s.setSpan(new RelativeSizeSpan(1.0f), 0, l1.length(), 0);
        return s;
    }

    public void fabClicked()
    {

    }

    private SpannableString generateCenterSpannableText(String l1,String l2) {

        int change = l1.length();
        String l = l1 + " " + l2;
        SpannableString s = new SpannableString(l);

        Typeface chalk = Typeface.createFromAsset(getContext().getAssets(),"Denne.ttf");
//        s.setSpan(new RelativeSizeSpan(1.0f), 0, change, 0);
//
//        s.setSpan(new StyleSpan(Typeface.NORMAL), change, s.length() , 0);
//        s.setSpan(new ForegroundColorSpan(Color.GRAY), change, s.length() , 0);
//        s.setSpan(new RelativeSizeSpan(1.4f), change, s.length(), 0);
//
//        s.setSpan(new StyleSpan(Typeface.ITALIC), change, s.length(), 0);
//        s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), change, s.length(), 0);

        int c = Color.WHITE;// ContextCompat.getColor(getContext(), Color.WHITE);

        s.setSpan(new CustomTypefaceSpan("",chalk),0, l1.length(), 0);
        s.setSpan(new ForegroundColorSpan(c), 0, l1.length(), 0);
        s.setSpan(new RelativeSizeSpan(1.2f), 0, l1.length(), 0);
        s.setSpan(new StyleSpan(Typeface.BOLD),0, l1.length(),0);


        s.setSpan(new CustomTypefaceSpan("",chalk), l1.length(), l.length(), 0);
        s.setSpan(new ForegroundColorSpan(c), l1.length(), l.length(), 0);
        s.setSpan(new RelativeSizeSpan(0.8f), l1.length(), l.length(), 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), l1.length(), l.length(), 0);




        return s;
    }





}
