package com.csri.ami.health_u.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import com.csri.ami.health_u.R;
import com.csri.ami.health_u.dataManagement.servercomms.ConnectionInfo;
import com.csri.ami.health_u.dataManagement.servercomms.JSONParser;
import com.csri.ami.health_u.dataManagement.servercomms.Register;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Questionnaire extends AppCompatActivity
{
    JSONParser jParser = new JSONParser();
    private ProgressDialog progressDialog;
    int[] answersForUploads=null;
    double[] questionnaireResults;
    double questionnaireResult_physical=-1;
    double getQuestionnaireResult_mental=-1;

    String[] questionnaireResultStrings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent mIntent = getIntent();
        boolean isAutoLaunched = mIntent.getBooleanExtra(InformedConsent.IS_QUESTIONNAIRE_OPENED_AUTOMATICALLY,false);

        if(isAutoLaunched)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);//, R.style.MyDialogTheme);
            builder.setTitle("Anonymous Questionnaire");
            String line = "Dear Research Participant,\nThank you for downloading our App and taking part in our research study.\n\nWe would be very grateful if you could please take few minutes to answers some questions about your daily life. (The questionnaire is 100% anonymous, and we will never ask for your identity or contact info)\nThe answers you give will be vital in helping us understand how Smartphones could be used to measure health.\n\nThank you. (Let's do some Science!)";
            builder.setMessage(line);

            builder.setPositiveButton("Answer Questions", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialogInterface,int id)
                {
                    dialogInterface.dismiss();
                }
            });

            builder.setNegativeButton("Not now", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialogInterface,int id)
                {
                    finish();
                }
            });
            builder.show();
        }

        setContentView(R.layout.activity_questionnaire);


        //ListView lv = (ListView) this.findViewById(R.id.q_list);
        questionnaireResultStrings = new String[]{
                getResources().getString(R.string.category1),
                getResources().getString(R.string.category2),
                getResources().getString(R.string.category3),
                getResources().getString(R.string.category4),
                getResources().getString(R.string.category5),
                getResources().getString(R.string.category6),
                getResources().getString(R.string.category7),
                getResources().getString(R.string.category8)};

    }

    public void sendMessage(View view)
    {
        int[] answers = GetResponses();
        int count = CountValidAnswers(answers);
        answersForUploads = answers;
        if(count < 36 || Response_gender <0 || Response_age <0)
        {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);//, R.style.MyDialogTheme);
            builder.setTitle(getResources().getString(R.string.notcomplete));
            String line = GetWrongAnswersString(answers);
            builder.setMessage(line + "\n\n" + getResources().getString(R.string.notcomplete_mes));

            builder.setPositiveButton(getResources().getString(R.string.submit), new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialogInterface,int id)
                {
                    Submit();
                }
            });

            builder.setNegativeButton(getResources().getString(R.string.recheck), null);
            builder.show();
        }
        else
        {
            Submit();


        }
    }

    public void CloseActivity()
    {
        finish();
    }

    public void Submit()
    {
        int[] scores = new int[answersForUploads.length];
        for(int i=0;i<scores.length;i++)
        {
            scores[i] = GetScore(i+1,answersForUploads[i]);
        }



        questionnaireResults = CalculateQuestionnaireResults(scores);

        answersForUploads = scores;

        new QuestionnaireUpload().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_quatyionnaire, menu);
        return true;
    }

    private String GetWrongAnswersString(int[] answers)
    {
        int[] wrongAnserQnos = CheckWrongAnswers(answers);

        String line = "";

        if(wrongAnserQnos.length == 1)
        {
            String qid = Integer.toString(wrongAnserQnos[0]);
            if(wrongAnserQnos[0] == 12)
            {
                qid = "A";
            }
            else if(wrongAnserQnos[0] == 13)
            {
                qid = "B";
            }


            line = getResources().getString(R.string.pleasecheck_singular) + " " + qid;
        }
        else
        {
            line = getResources().getString(R.string.pleasecheck_plural) + " ";
            for(int i=0;i<wrongAnserQnos.length;i++)
            {
                String qid = Integer.toString(wrongAnserQnos[i]);
                if(wrongAnserQnos[i] == 12)
                {
                    qid = "A";
                }
                else if(wrongAnserQnos[i] == 13)
                {
                    qid = "B";
                }
                if(i > 0)
                {
                    if(wrongAnserQnos[i] != wrongAnserQnos[i-1])
                    {
                        line += qid + " ";
                    }
                }
                else
                {
                    line += qid + " ";
                }
            }
        }

        return line;
    }

    private int[] CheckWrongAnswers(int[] answers)
    {
        ArrayList<Integer> questionNumbers = new ArrayList<Integer>();
        int count =0;

        if(Response_age <0)
        {
            questionNumbers.add(12);
        }
        if(Response_gender <0)
        {
            questionNumbers.add(13);
        }
        for(int i=0;i<answers.length;i++)
        {
            if(answers[i] == -1)
            {
                if(i ==0){questionNumbers.add(1);}
                else if(i ==1){questionNumbers.add(2);}
                else if(i >=2 && i <=11){questionNumbers.add(3);}
                else if(i >=12 && i <=15){questionNumbers.add(4);}
                else if(i >=16 && i <=18){questionNumbers.add(5);}
                else if(i == 19){questionNumbers.add(6);}
                else if(i == 20){questionNumbers.add(7);}
                else if(i == 21){questionNumbers.add(8);}
                else if(i >=22 && i <=30){questionNumbers.add(9);}
                else if(i == 31){questionNumbers.add(10);}
                else if(i >=32 && i <=35){questionNumbers.add(11);}
            }
        }

        int[] badQs = new int[questionNumbers.size()];
        for(int i=0;i<badQs.length;i++)
        {
            badQs[i] = questionNumbers.get(i);
        }

        return badQs;
    }

    private int CountValidAnswers(int[] answers)
    {
        int count =0;
        for(int i=0;i<answers.length;i++)
        {
            if(answers[i] != -1)
            {
                count++;
            }
        }

        return count;
    }

    private double[] CalculateQuestionnaireResults(int[] scores)
    {
        double[] results = new double[8];//8 different functionion scores according to RAND

        //physcial functioning
        if(scores[2] != -1 && scores[3] != -1 && scores[4] != -1 && scores[5] != -1 && scores[6] != -1 && scores[7] != -1 && scores[8] != -1 && scores[9] != -1 && scores[10] != -1 && scores[11] != -1)
        {
            results[0] = (scores[2] + scores[3] + scores[4] + scores[5] + scores[6] + scores[7] + scores[8] + scores[9] + scores[10] + scores[11]) / 10;
        }
        else {results[0] = -1;}

        //role limitations due to physical
        if(scores[12] != -1 && scores[13] != -1 && scores[14] != -1 && scores[15] != -1)
        {
            results[1] = (scores[12] + scores[13] + scores[14] + scores[15]) / 4;
        }
        else {results[1] = -1;}

        //physcial functioning due to emotional
        if(scores[16] != -1 && scores[17] != -1 && scores[18] != -1)
        {
            results[2] = (scores[16] + scores[17] + scores[18]) / 3;
        }
        else {results[2] = -1;}

        //energy fatigue
        if(scores[22] != -1 && scores[26] != -1 && scores[28] != -1 && scores[30] != -1)
        {
            results[3] = (scores[22] + scores[26] + scores[28] + scores[30]) / 4;
        }
        else {results[3] = -1;}


        //emotional well being
        if(scores[23] != -1 && scores[24] != -1 && scores[25] != -1 && scores[27] != -1 && scores[29] != -1)
        {
            results[4] = (scores[23] + scores[24] + scores[25] + scores[27] + scores[29]) / 5;
        }
        else {results[4] = -1;}

        //social functioning
        if(scores[19] != -1 && scores[31] != -1)
        {
            results[5] = (scores[19] + scores[31]) / 2;
        }
        else {results[5] = -1;}

        //social functioning
        if(scores[20] != -1 && scores[21] != -1)
        {
            results[6] = (scores[20] + scores[21]) / 2;
        }
        else {results[6] = -1;}

        //general health
        if(scores[0] != -1 && scores[32] != -1 && scores[33] != -1 && scores[34] != -1 && scores[35] != -1)
        {
            results[7] = (scores[0] + scores[32] + scores[33] + scores[34] + scores[35]) / 5;
        }
        else {results[7] = -1;}

        int[] physical_index = new int[]{2,3,4,5,6,7,8,9,10,11,12,13,14,15,20,21,0,35,33,32,34,22,26,28,30};
        int[] mental_index = new int[]{0,35,33,32,34,22,26,28,30,31,19,16,17,18,23,24,25,27,29};

        double count =0;
        double avg=0;
        for(int i=0;i<physical_index.length;i++)
        {
            int index = physical_index[i];
            if(scores[index] != -1)
            {
                avg+=scores[index];
                count++;
            }
        }
        if(count > 1)
        {
            avg = avg / count;
        }

        questionnaireResult_physical=avg;

        count=0;
        avg=0;
        for(int i=0;i<mental_index.length;i++)
        {
            int index = mental_index [i];
            if(scores[index] != -1)
            {
                avg+=scores[index];
                count++;
            }
        }
        if(count > 1)
        {
            avg = avg / count;
            getQuestionnaireResult_mental = avg;
        }

        return results;
    }

    int Response_age=-1;
    int Response_gender =-1;

    private int[] GetResponses()
    {
        int[] responses = new int[36];
        Response_age = GetAnswer(R.id.radioGroup_01);
        Response_gender = GetAnswer(R.id.radioGroup_02);
        responses[0] = GetAnswer(R.id.radioGroup_1);
        responses[1] = GetAnswer(R.id.radioGroup_2);
        responses[2]= GetAnswer(R.id.radioGroup_3);
        responses[3] = GetAnswer(R.id.radioGroup_4);
        responses[4] = GetAnswer(R.id.radioGroup_5);
        responses[5] = GetAnswer(R.id.radioGroup_6);
        responses[6] = GetAnswer(R.id.radioGroup_7);
        responses[7] = GetAnswer(R.id.radioGroup_8);
        responses[8] = GetAnswer(R.id.radioGroup_9);
        responses[9]= GetAnswer(R.id.radioGroup_10);
        responses[10] = GetAnswer(R.id.radioGroup_11);
        responses[11] = GetAnswer(R.id.radioGroup_12);
        responses[12] = GetAnswer(R.id.radioGroup_13);
        responses[13] = GetAnswer(R.id.radioGroup_14);
        responses[14] = GetAnswer(R.id.radioGroup_15);
        responses[15] = GetAnswer(R.id.radioGroup_16);
        responses[16] = GetAnswer(R.id.radioGroup_17);
        responses[17] = GetAnswer(R.id.radioGroup_18);
        responses[18] = GetAnswer(R.id.radioGroup_19);
        responses[19] = GetAnswer(R.id.radioGroup_20);
        responses[20] = GetAnswer(R.id.radioGroup_21);
        responses[21] = GetAnswer(R.id.radioGroup_22);
        responses[22] = GetAnswer(R.id.radioGroup_23);
        responses[23] = GetAnswer(R.id.radioGroup_24);
        responses[24] = GetAnswer(R.id.radioGroup_25);
        responses[25] = GetAnswer(R.id.radioGroup_26);
        responses[26] = GetAnswer(R.id.radioGroup_27);
        responses[27] = GetAnswer(R.id.radioGroup_28);
        responses[28] = GetAnswer(R.id.radioGroup_29);
        responses[29] = GetAnswer(R.id.radioGroup_30);
        responses[30] = GetAnswer(R.id.radioGroup_31);
        responses[31] = GetAnswer(R.id.radioGroup_32);
        responses[32] = GetAnswer(R.id.radioGroup_33);
        responses[33] = GetAnswer(R.id.radioGroup_34);
        responses[34] = GetAnswer(R.id.radioGroup_35);
        responses[35] = GetAnswer(R.id.radioGroup_36);

        return responses;
    }

    private int GetScore(int QuestionId,int response)
    {
        if(QuestionId >= 1 && QuestionId <= 36) {
            if (QuestionId == 1 || QuestionId == 2 || QuestionId == 20 || QuestionId == 22 || QuestionId == 34 || QuestionId == 36) {
                if (response == 1) {
                    return 100;
                } else if (response == 2) {
                    return 75;
                } else if (response == 3) {
                    return 50;
                } else if (response == 4) {
                    return 25;
                } else if (response == 5) {
                    return 0;
                }
            } else if (QuestionId == 3 || QuestionId == 4 || QuestionId == 5 || QuestionId == 6 || QuestionId == 7 || QuestionId == 8 || QuestionId == 9 || QuestionId == 10 || QuestionId == 11 || QuestionId == 12) {
                if (response == 1) {
                    return 0;
                } else if (response == 2) {
                    return 50;
                } else if (response == 3) {
                    return 100;
                }
            } else if (QuestionId >= 13 && QuestionId <= 19) {
                if (response == 1) {
                    return 0;
                } else if (response == 2) {
                    return 25;
                } else if (response == 3) {
                    return 50;
                } else if (response == 4) {
                    return 75;
                } else if (response == 5) {
                    return 100;
                }
            } else if (QuestionId == 21) {
                if (response == 1) {
                    return 100;
                } else if (response == 2) {
                    return 80;
                } else if (response == 3) {
                    return 60;
                } else if (response == 4) {
                    return 40;
                } else if (response == 5) {
                    return 20;
                } else if (response == 6) {
                    return 0;
                }
            } else if (QuestionId == 23 || QuestionId == 26 || QuestionId == 27 || QuestionId == 30) {
                if (response == 1) {
                    return 100;
                } else if (response == 2) {
                    return 75;
                } else if (response == 3) {
                    return 50;
                } else if (response == 4) {
                    return 25;
                } else if (response == 5) {
                    return 0;
                }
            } else if (QuestionId == 24 || QuestionId == 25 || QuestionId == 28 || QuestionId == 29 || QuestionId == 31) {
                if (response == 1) {
                    return 0;
                } else if (response == 2) {
                    return 25;
                } else if (response == 3) {
                    return 50;
                } else if (response == 4) {
                    return 75;
                } else if (response == 5) {
                    return 100;
                }
            } else if (QuestionId == 32 || QuestionId == 33 || QuestionId == 35) {
                if (response == 1) {
                    return 0;
                } else if (response == 2) {
                    return 25;
                } else if (response == 3) {
                    return 50;
                } else if (response == 4) {
                    return 75;
                } else if (response == 5) {
                    return 100;
                }
            } else {
                return -1;
            }
        }
        else
        {
            return -1;
        }

        return -1;
    }

    private int GetAnswer(int radioGroupId)
    {
        RadioGroup rb1 = (RadioGroup)this.findViewById(radioGroupId);
        int rb1ID = rb1.getCheckedRadioButtonId();
        int answer = -1;
        if(rb1ID != -1)
        {

            View radioButton = rb1.findViewById(rb1ID);
            answer = rb1.indexOfChild(radioButton) + 1;
        }

        return answer;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button_left, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    class QuestionnaireUpload extends AsyncTask<String, String, String>
    {





        //class UploadMotionFile extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            progressDialog = new ProgressDialog(Questionnaire.this);//,R.style.MyDialogTheme);
            progressDialog.setMessage(getResources().getString(R.string.uploading));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        /**
         * getting All products from url
         */
        protected String doInBackground(String... args)
        {
            String status = "";
            int userID = Register.GetUserID(getApplication());
            if(userID == -1)
            {
                try{Thread.sleep(2000);}catch (InterruptedException ex){}//if no id found...wait 2 seconds and see if a new one has been regestered
            }
            userID = Register.GetUserID(getApplication());
            String userID_s = Integer.toString(userID);



            if(userID != -1)
            {


                if(answersForUploads != null)
                {
                    String line = DateTime.now().toString("yyyy-MM-dd HH:mm:ss") + ",";
                    for(int i=0;i<answersForUploads.length;i++)
                    {
                        line += answersForUploads[i] + ",";
                    }
                    line += questionnaireResult_physical + ",";//result goes here
                    line += getQuestionnaireResult_mental + ",";
                    line += Response_age + ",";
                    line += Response_gender;

                    List<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("userID", userID_s));
                    params.add(new BasicNameValuePair("data", line));
                    //  params.add(new BasicNameValuePair("datetime", time));
                    //  params.add(new BasicNameValuePair("f1", "1"));
                    //  params.add(new BasicNameValuePair("f2", "2"));
                    //  params.add(new BasicNameValuePair("f3", "3"));
                    //  params.add(new BasicNameValuePair("f4", "4"));
                    // getting JSON string from URL
                    JSONObject json = jParser.makeHttpRequest(ConnectionInfo.url_questionnaireAnswersAdd, "POST", params);
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





        /**
         * After completing background task Dismiss the progress dialog
         **/
        protected void onPostExecute(String uploadStatus) {
           progressDialog.dismiss();

            if(uploadStatus == ConnectionInfo.STATUS_SUCCESS)
            {
                SharedPreferences prefs = getSharedPreferences(InformedConsent.PREFS_INFORMED_CONSENT, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(InformedConsent.PREFS_INFORMED_TAKEN_QUESTIONNAIRE, true);
                editor.commit();

                AlertDialog.Builder builder = new AlertDialog.Builder(Questionnaire.this);//, R.style.MyDialogTheme);
                builder.setTitle(getResources().getString(R.string.qfinished));


                SpannableString message = SpannableString.valueOf("Thank you for completing the questionnaire. To ensure that you become an official research participant, we would really appreciate it if you keep the app installed and switched on for at least 3 days. By doing this, you could be helping improve the treatment for patients suffering from chronic illnesses.\n");

                if(questionnaireResults != null)
                {
                    int count =0;
                    for(int i=0;i<questionnaireResults.length;i++)
                    {
                        if(questionnaireResults[i] != -1)
                        {
                            count++;
                            int score = (int)Math.round(questionnaireResults[i]);

                            int scoreRank = GetScoreRank(score,i);
                            int color = GetScoreColor(scoreRank);

                            SpannableString current= generateCenterSpannableText(Integer.toString(score) + "%", "-" + questionnaireResultStrings[i],color);// questionnaireResultStrings[i] + "=\t" + score + "%\n";

                            Spanned message1 = (Spanned)TextUtils.concat(message,"\n",current);

                            message = new SpannableString(message1);
                        }
                    }
                    if(count == 0)
                    {
                        message = new SpannableString(getResources().getString(R.string.qfinished_notenough));
                    }
                }
                else
                {
                    message = new SpannableString(getResources().getString(R.string.qfinished_notenough));
                }

                builder.setMessage(message);
                builder.setNeutralButton(getResources().getString(R.string.done), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int id) {
                        CloseActivity();
                    }
                });
                builder.show();
            }
            else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(Questionnaire.this);//, R.style.MyDialogTheme);
                builder.setTitle(getResources().getString(R.string.noconnection));
                builder.setMessage(getResources().getString(R.string.noconnection_mes));
                builder.setNeutralButton(getResources().getString(R.string.close), null);
                builder.show();
            }

        }



        private int GetScoreRank(double score,int index)
        {
            double[] averageSF36 = new double[]{85.8,82.1,84,65.8,77.5,86,75.6,77};
            double[] sdSF36 = new double[]{20,33.2,31.7,18,15.3,19.8,23,17.7};

            double q_75th = (sdSF36[index] * 0.67) + averageSF36[index];
            double q_25th = (sdSF36[index] * -0.67) + averageSF36[index];
            double q_12th = (sdSF36[index] * -1.1503) + averageSF36[index];

            double q_50th = averageSF36[index];

            if(score >= q_75th) // > 75
            {
                return 1;
            }
            else if(score < q_75th && score >= q_50th) // 50-75
            {
                return 2;
            }
            else if(score < q_50th && score >= q_25th)//25-50
            {
                return 3;
            }
            else if(score < q_25th && score >= q_12th)//12-25
            {
                return 4;
            }
            else if(score < q_12th)
            {
                return 5;
            }
            else
            {
                return -1;
            }

        }

        private int GetScoreColor(int rank)
        {
            if(rank == 1)
            {
                return ContextCompat.getColor(getApplication(), R.color.tfScale1);
            }
            else if(rank == 2)
            {
                return ContextCompat.getColor(getApplication(), R.color.tfScale2);
            }
            else if(rank == 3)
            {
                return ContextCompat.getColor(getApplication(), R.color.tfScale3);
            }
            else if(rank == 4)
            {
                return ContextCompat.getColor(getApplication(), R.color.tfScale4);
            }
            else if(rank == 5)
            {
                return ContextCompat.getColor(getApplication(), R.color.tfScale5);
            }
            else
            {
                return ContextCompat.getColor(getApplication(), R.color.colorPrimaryDark);
            }
        }

        private SpannableString generateCenterSpannableText(String l1,String l2,int scoreColor) {

            int change = l1.length();
            String l = l1 + " " + l2;
            SpannableString s = new SpannableString(l);
//        s.setSpan(new RelativeSizeSpan(1.0f), 0, change, 0);
//
//        s.setSpan(new StyleSpan(Typeface.NORMAL), change, s.length() , 0);
//        s.setSpan(new ForegroundColorSpan(Color.GRAY), change, s.length() , 0);
//        s.setSpan(new RelativeSizeSpan(1.4f), change, s.length(), 0);
//
//        s.setSpan(new StyleSpan(Typeface.ITALIC), change, s.length(), 0);
//        s.setSpan(new ForegroundColorSpan(ColorTemplate.getHoloBlue()), change, s.length(), 0);

            int c = scoreColor;// ContextCompat.getColor(getApplication(), R.color.colorPrimaryDark);

            int c2 = Color.parseColor("#B3000000");

            s.setSpan(new ForegroundColorSpan(c), 0, l1.length(), 0);
            s.setSpan(new RelativeSizeSpan(1.3f), 0, l1.length(), 0);

            s.setSpan(new ForegroundColorSpan(c2), l1.length(), l.length(), 0);
            s.setSpan(new RelativeSizeSpan(0.8f), l1.length(), l.length(), 0);
            s.setSpan(new StyleSpan(Typeface.BOLD),l1.length(), l.length(),0);

            s.setSpan(new StyleSpan(Typeface.NORMAL), l1.length(), l.length(), 0);




            return s;
        }


        //}

    }
}
