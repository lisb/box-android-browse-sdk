package com.box.androidsdk.browse.activities;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.models.BoxSessionDto;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Base class for all activities that make API requests through the Box Content SDK. This class is responsible for
 * showing a loading spinner while a request is executing and then hiding it when the request is complete.
 * All BoxRequest tasks should be submitted to getApiExecutor and then handled by overriding handleBoxResponse
 */
public abstract class BoxThreadPoolExecutorActivity extends AppCompatActivity {
    public static final String EXTRA_ITEM = "extraItem";
    public static final String EXTRA_SESSION = "extraSession";

    protected BoxSession mSession;
    protected BoxItem mItem;

    protected static final int DEFAULT_TIMEOUT = 30 * 1000;
    private static final int  DEFAULT_SPINNER_DELAY = 500;

    protected static final String ACTION_STARTING_TASK = "com.box.androidsdk.share.starting_task";
    protected static final String ACTION_ENDING_TASK = "com.box.androidsdk.share.ending_task";

    private ProgressDialog mDialog;
    private LastRunnableHandler mDialogHandler;

    /**
     * Broadcast receiver for handling when the spinner should be shown or hidden
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_STARTING_TASK)){
                showSpinner();
            }
            else if (intent.getAction().equals(ACTION_ENDING_TASK)) {
                handleTaskEnd();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BoxConfig.IS_FLAG_SECURE){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        mDialogHandler = new LastRunnableHandler();
        if (savedInstanceState != null && savedInstanceState.getSerializable(EXTRA_ITEM) != null){
            mSession = BoxSessionDto.unmarshal(this, (BoxSessionDto) savedInstanceState.getSerializable(EXTRA_SESSION));
            mItem = (BoxItem)savedInstanceState.getSerializable(EXTRA_ITEM);

        } else if (getIntent() != null) {
            mSession = BoxSessionDto.unmarshal(this, (BoxSessionDto) getIntent().getSerializableExtra(EXTRA_SESSION));
            mItem = (BoxItem)getIntent().getSerializableExtra(EXTRA_ITEM);
        }

        if (mSession == null || mSession.getUser() == null || SdkUtils.isBlank(mSession.getUser().getId())) {
            Toast.makeText(this, R.string.box_browsesdk_session_is_not_authenticated, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (mItem == null){
            Toast.makeText(this, R.string.box_browsesdk_no_item_selected, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mSession.setSessionAuthListener(new BoxAuthentication.AuthListener() {
            @Override
            public void onRefreshed(BoxAuthentication.BoxAuthenticationInfo info) {

            }

            @Override
            public void onAuthCreated(BoxAuthentication.BoxAuthenticationInfo info) {

            }

            @Override
            public void onAuthFailure(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
                finish();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BoxThreadPoolExecutorActivity.this, R.string.box_browsesdk_session_is_not_authenticated, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onLoggedOut(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
                
            }
        });
        mSession.authenticate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STARTING_TASK);
        filter.addAction(ACTION_ENDING_TASK);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mBroadcastReceiver, filter);
        resumeSpinnerIfNecessary();

    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissSpinner();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mBroadcastReceiver);
        mSession.setSessionAuthListener(null);
        super.onDestroy();
    }

    /**
     * Returns the api executor for the activity
     *
     * @param application the application
     * @return api executor
     */
    public abstract ThreadPoolExecutor getApiExecutor(final Application application);

    /**
     * Returns the queue that is used for holding the responses of completed {@link com.box.androidsdk.content.requests.BoxRequest}
     *
     * @return Response queue of completed requests
     */
    public abstract Queue<BoxResponse> getResponseQueue();


    /**
     * Handler that will be called after a BoxRequest completes. This method should be overridden
     * to handle the appropriate request/response
     *
     * @param response the response object returned after a BoxRequest completes
     */
    protected abstract void handleBoxResponse(final BoxResponse response);


    /**
     * Checks if the API executor currently ahs a task in progress
     *
     * @return true is the task is in progress, false otherwise.
     */
    protected boolean isTaskInProgress(){
        return getApiExecutor(getApplication()) != null && (getApiExecutor(getApplication()).getActiveCount() > 0 || getApiExecutor(getApplication()).getQueue().size() > 0);
    }

    private void handleTaskEnd(){
        BoxResponse response = getResponseQueue().poll();
        dismissSpinner();
        if (response == null){
            return;
        }
        else {
            handleBoxResponse(response);
        }
    }

    /**
     * Check to see if the threadpool executor is currently in the middle of a task and if so, show a spinner dialog again.
     */
    protected void resumeSpinnerIfNecessary(){
        if (getApiExecutor(getApplication()) == null){
            return;
        } else  if (getApiExecutor(getApplication()).getActiveCount() > 0) {
            showSpinner();
        }
    }

    /**
     * Convenient accessor for the BoxItem that will be interacted with
     *
     * @return the target box item the user is performing actions on.
     */
    protected BoxItem getMainItem(){
        return mItem;
    }

    /**
     * Sets the main BoxItem that will be interacted with
     *
     * @param boxItem the box item
     */
    protected void setMainItem(final BoxItem boxItem){
        mItem = boxItem;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
       outState.putSerializable(EXTRA_ITEM, mItem);
       outState.putSerializable(EXTRA_SESSION, BoxSessionDto.marshal(mSession));
        super.onSaveInstanceState(outState);
    }


    /**
     * Dismisses the spinner if it is currently showing
     */
    protected void dismissSpinner(){
        if (mDialog != null && mDialog.isShowing()){
            mDialog.dismiss();
        }
        mDialogHandler.cancelLastRunnable();
    }

    /**
     * Shows the spinner with the default wait messaging
     */
    protected void showSpinner(){
        showSpinner(R.string.boxsdk_Please_wait, R.string.boxsdk_Please_wait);
    }

    /**
     * Shows the spinner with a custom title and description
     *
     * @param stringTitleRes string resource for the spinner title
     * @param stringRes      string resource for the spinner description
     */
    protected void showSpinner(final int stringTitleRes, final int stringRes) {
        mDialogHandler.queue(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mDialog != null && mDialog.isShowing()) {
                        return;
                    }
                    mDialog = ProgressDialog.show(BoxThreadPoolExecutorActivity.this, getText(stringTitleRes), getText(stringRes));
                    mDialog.show();
                } catch (Exception e) {
                    // WindowManager$BadTokenException will be caught and the app would not display
                    // the 'Force Close' message
                    mDialog = null;
                    return;
                }
            }
        },DEFAULT_SPINNER_DELAY);

    }

    /**
     * Creates a static ThreadPoolExecutor that is meant to be used in {@link #getApiExecutor(android.app.Application) getApiExecutor}.
     * The ThreadPoolExecutor returned should be set to a static variable so that it is not recreated each time
     *
     * @param application   application context needed for the LocalBroadcastManager
     * @param responseQueue the queue that will hold all responses after the requests are completed. This should be provided through {@link #getResponseQueue()}
     * @return the ThreadPoolExecutor used to execute API requests
     */
    protected static ThreadPoolExecutor createTaskMessagingExecutor(final Application application, final Queue<BoxResponse> responseQueue){
        return new ThreadPoolExecutor(1, 10, 3600, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()){

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                Intent intent = new Intent();
                intent.setAction(ACTION_STARTING_TASK);
                LocalBroadcastManager.getInstance(application).sendBroadcast(intent);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Intent intent = new Intent();
                intent.setAction(ACTION_ENDING_TASK);
                if (r instanceof BoxFutureTask && ((BoxFutureTask) r).isDone()){
                    try {
                        BoxResponse response = (BoxResponse) ((BoxFutureTask) r).get();
                        responseQueue.add(response);
                    } catch (Exception e){
                    }
                }
                LocalBroadcastManager.getInstance(application).sendBroadcast(intent);
            }
        };
    }

    /**
     * Helper method to hide a view ie. set the visibility to View.GONE
     *
     * @param view the view that should be hidden
     */
    protected void hideView(View view){
        view.setVisibility(View.GONE);
    }

    /**
     * Helper method to show a view ie. set the visibility to View.VISIBLE
     *
     * @param view the view that should be shown
     */
    protected void showView(View view){
        view.setVisibility(View.VISIBLE);
    }

    /**
     * Helper method that returns formatted text that is meant to be shown in a Button
     *
     * @param title       the title text that should be emphasized
     * @param description the description text that should be de-emphasized
     * @return Spannable that is the formatted text
     */
    protected Spannable createTitledSpannable(final String title, final String description){
        String combined = title +"\n"+ description;
        Spannable accessSpannable = new SpannableString(combined);

        accessSpannable.setSpan(new TextAppearanceSpan(this, R.style.Base_TextAppearance_AppCompat_Body1), title.length(),combined.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        accessSpannable.setSpan(new ForegroundColorSpan(R.color.box_hint_foreground), title.length(),combined.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return accessSpannable;
    }

    /**
     * Helper class used to keep track of last runnable queued.
     */
    private class LastRunnableHandler extends Handler {
        private Runnable mLastRunable;


        public void queue(final Runnable runnable, final int delay){
            cancelLastRunnable();
            postDelayed(runnable, delay);
            mLastRunable = runnable;
        }

        public void cancelLastRunnable(){
            if (mLastRunable != null){
                removeCallbacks(mLastRunable);
            }
        }

    }

}
