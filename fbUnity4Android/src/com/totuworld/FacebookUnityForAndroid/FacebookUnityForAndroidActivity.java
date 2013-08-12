package com.totuworld.FacebookUnityForAndroid;

import com.unity3d.player.*;
import android.app.Activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.content.Intent;
import android.util.Log;

import com.facebook.*;
import com.facebook.model.*;

import org.json.JSONException;
import org.json.JSONObject;

public class FacebookUnityForAndroidActivity extends Activity
{
    private UnityPlayer mUnityPlayer;


    private String mEventHandler = "@Facebook";
    private String mAppId = "148764025321750";
    private boolean loginFlag = false;
    private String myHash;


    // UnityPlayer.init() should be called before attaching the view to a layout.
    // UnityPlayer.quit() should be the last thing called; it will terminate the process and not return.
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mUnityPlayer = new UnityPlayer(this);

        if (mUnityPlayer.getSettings ().getBoolean ("hide_status_bar", true))
            getWindow ().setFlags (WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int glesMode = mUnityPlayer.getSettings().getInt("gles_mode", 1);
        boolean trueColor8888 = false;
        mUnityPlayer.init(glesMode, trueColor8888);

        View playerView = mUnityPlayer.getView();
        setContentView(playerView);
        playerView.requestFocus();



        // for facebook
        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        Session session =Session.getActiveSession();
        if (session == null)
        {
            if (savedInstanceState != null) {
                session = Session.restoreSession(this,
                        null, statusCallback, savedInstanceState);
                Log.d("Unity::onCreate()", "Session.restoreSession()");
            }
            if (session == null) {
                session = new Session.Builder(this).setApplicationId(mAppId).build();
                Log.d("Unity::onCreate()", "new Session.Builder(this)");
            }

            Session.setActiveSession(session);
            Log.i("Unity", UnityPlayer.currentActivity.toString());
            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
                session.openForRead(
                        new Session.OpenRequest(this).setCallback(statusCallback));
                Log.d("Unity::onCreate()", "openForRead()");
            }
        }

    }
    protected void onDestroy ()
    {
        super.onDestroy();
        mUnityPlayer.quit();
    }

    // onPause()/onResume() must be sent to UnityPlayer to enable pause and resource recreation on resume.
    protected void onPause()
    {
        super.onPause();
        mUnityPlayer.pause();
        if (isFinishing())
            mUnityPlayer.quit();
    }
    protected void onResume()
    {
        super.onResume();
        mUnityPlayer.resume();
    }
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    // Pass any keys not handled by (unfocused) views straight to UnityPlayer
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event)
    {
        return mUnityPlayer.onKeyMultiple(keyCode, count, event);
    }
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        return mUnityPlayer.onKeyDown(keyCode, event);
    }
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        return mUnityPlayer.onKeyUp(keyCode, event);
    }


    /*
     * for facebook method
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }

    public void FacebookLogin()
    {
        Session session = Session.getActiveSession();
        loginFlag = true;

        if (!session.isOpened() && !session.isClosed())
        {
            Log.d("Unity::onLogin()","session.openForRead()");
            session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));
        }
        else if (session !=null)
        {
            Log.d("Unity::onLogin()","Session.openActiveSession()");
            //regenerate session

            Session newsession = new Session.Builder(this).setApplicationId(mAppId).build();
            Session.setActiveSession(newsession);
            newsession.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));

        }
    }

    public void logout()
    {
        Log.d("Unity::onLogout()", "on logout called !");
        Session session = Session.getActiveSession();
        if (!session.isClosed()) {
            session.closeAndClearTokenInformation();
            Log.d("Unity::onLogout()", "session.closeAndClearTokenInformation()");
        }
    }

    public void dispose()
    {
        Log.d("Unity::dispose()", "dispose called");
    }

    public boolean isSessionOpened()
    {
        Session session = Session.getActiveSession();

        return (session != null) && (session.isOpened()) && (session.getAccessToken() != null);
    }

    public String getHash()
    {
        return this.myHash;
    }

    public void requestMe() {
        final Session session =Session.getActiveSession();
        runOnUiThread(new Runnable() {
            public void run() {
                Request readMeRequest = new Request(session.getActiveSession(), "me", null, HttpMethod.GET,
                        new Request.Callback() {

                            public void onCompleted(Response response) {
                                FacebookRequestError error = response.getError();
                                if(error != null) {
                                    Log.e("Unity::requestMe", error.getErrorMessage());
                                }
                                else{
                                    GraphObject graphObject = response.getGraphObject();
                                    if ( graphObject != null)
                                    {
                                        String arr =graphObject.getInnerJSONObject().toString();
                                        JSONObject jObj = null;
                                        try {
                                            jObj = new JSONObject(arr);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        try {
                                            String id = jObj.getString("id");
                                            Log.i("Unity::requestMe", "get id:\n"+ id);
                                            UnityPlayer.UnitySendMessage(mEventHandler, "didLogin", id );
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }


                                    }
                                }
                            }
                        });
                Request.executeBatchAsync(readMeRequest);
            }//run
        });
    }

    private Session.StatusCallback statusCallback = new SessionStatusCallback();
    private class SessionStatusCallback implements Session.StatusCallback {
        public void call(Session session, SessionState state, Exception exception) {
            Log.d("Unity::onSessionStateChange--", state.toString());
            if (state.isOpened())
            {
                Log.d("Unity::onSessionStateChange()", "state.isOpened()");
                if ( mEventHandler != null ){
                    if (loginFlag){
                        requestMe();
                    }
                }
            }
            else
            {
                Log.d("Unity::onSessionStateChange()", "state.isOpened() ===false");
                if ( mEventHandler != null ){
                    Log.d("Unity::callbackState", state.toString());
                    if ((state == state.CLOSED || state == state.CLOSED_LOGIN_FAILED) ){
                        UnityPlayer.UnitySendMessage(mEventHandler, "errorPrint", "error");
                    }
                }
            }
        }
    }

    /*
        facebook end
     */

}
