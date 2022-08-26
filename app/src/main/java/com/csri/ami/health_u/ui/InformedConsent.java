package com.csri.ami.health_u.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.csri.ami.health_u.R;
import com.csri.ami.health_u.dataManagement.servercomms.Register;

/**
 * Created by daniel on 27/10/2015.
 */
public class InformedConsent
{
    private IconTextTabsActivity mContext;
    public static String PREFS_INFORMED_CONSENT = "informedconsentprefs";
    public static String PREFS_INFORMED_CONSENT_ACCEPTED = "informedconsentaccepted";
    public static String PREFS_INFORMED_TAKEN_QUESTIONNAIRE = "questionnairecompleted";
    public static String PREFS_INFORMED_LAST_QUESTIONNAIRE_REMINDER_TIME= "questionnaireremindtime";
    boolean newlyAccepted = false;

    public static String IS_QUESTIONNAIRE_OPENED_AUTOMATICALLY = "openquesauto";

    public InformedConsent(IconTextTabsActivity t)
    {
        mContext = t;
    }

    public void show()
    {

        final SharedPreferences prefs = mContext.getSharedPreferences(PREFS_INFORMED_CONSENT, Context.MODE_PRIVATE);
        boolean accepted = prefs.getBoolean(PREFS_INFORMED_CONSENT_ACCEPTED, false);

        if(!accepted)
        {
            String message = mContext.getResources().getString(R.string.informedconsent);

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                    .setTitle("Participant Information")
                    .setMessage(message)
                    .setCancelable(false)
                    .setIcon(R.drawable.ic_launcher)
                    .setPositiveButton(R.string.accept,
                            new Dialog.OnClickListener() {

                                @Override
                                public void onClick(
                                        DialogInterface dialogInterface, int i) {
                                    // Mark this version as read.
                                    newlyAccepted = true;
                                    SharedPreferences.Editor editor = prefs
                                            .edit();
                                    editor.putBoolean(PREFS_INFORMED_CONSENT_ACCEPTED, true);
                                    editor.commit();

                                    Register r = new Register(mContext, true);
                                    // Close dialog
                                    dialogInterface.dismiss();

                                    mContext.ConsentFinished(true);

                                    Bundle b = new Bundle();
                                    b.putBoolean(IS_QUESTIONNAIRE_OPENED_AUTOMATICALLY,true);

                                    Intent intent = new Intent(mContext, Questionnaire.class);
                                    intent.putExtra(IS_QUESTIONNAIRE_OPENED_AUTOMATICALLY,true);

                                    mContext.startActivity(intent);

                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new Dialog.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    // Close the activity as they have declined
                                    // the EULA
                                    mContext.finish();

                                }

                            });

            builder.create().show();
        }
    }
}
