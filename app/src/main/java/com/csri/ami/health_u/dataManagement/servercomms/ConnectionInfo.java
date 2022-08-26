package com.csri.ami.health_u.dataManagement.servercomms;



/**
 * Server communication is specific to a basic php based webserver for http based submission of data
 * This can be updated to other forms of server/database inserting methods
 * Created by daniel on 08/10/2015.
 */
public abstract class ConnectionInfo
{
    public static String url = "...";//server url goes here
    public static String url_all_products = url + "db_readUser_test.php";
    public static String url_deviceIDcheck = url + "deviceIDcheck.php";
    public static String url_deviceIDadd = url + "deviceIDadd.php";
    public static String url_soundFileAdd = url + "soundFileAdd.php";
    public static String url_motionFileAdd = url + "motionFileAdd.php";
    public static String url_locationFileAdd = url + "locationFileAdd.php";
    public static String url_questionnaireAnswersAdd = url + "questionnaireAnswersAdd.php";
    public static String url_getStats = url + "getStats.php";

    // JSON Node names
    public static final String TAG_SUCCESS = "success";
    public static final String TAG_USERS = "user";
    public static final String TAG_UID = "userID";
    public static final String TAG_DEVICE = "deviceID";
    public static final String TAG_HOURSCOUNT_USER = "hourscount";
    public static final String TAG_HOURSCOUNT_ALL = "hourscountall";
    public static final String TAG_USER_COUNT = "usercount";
    public static final String TAG_QUESTIONNAIRE_BY_USER = "questioncount";
    public static final String TAG_QUESTIONNAIRE_BY_ALL = "userstakenQ";


    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_REQUEST_ERROR = "requestError";
    public static final String STATUS_JSON_ERROR = "JSONerror";
    public static final String STATUS_CONNECTION_ERROR = "connectionError";
    public static final String STATUS_FILEREAD_ERROR = "fileReadError";
    public static final String STATUS_USERNOTFOUND_ERROR = "userNotFound";

}
