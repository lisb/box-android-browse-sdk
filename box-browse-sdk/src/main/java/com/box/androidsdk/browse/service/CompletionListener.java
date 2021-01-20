package com.box.androidsdk.browse.service;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxLogUtils;

public class CompletionListener implements BoxFutureTask.OnCompletedListener {

    private static final String TAG = CompletionListener.class.getName();

    private final LocalBroadcastManager mBroadcastManager;
    private final boolean mFromCache;

    /**
     * Instantiates a new Completion listener.
     *
     * @param broadcastManager the broadcast manager
     */
    public CompletionListener(LocalBroadcastManager broadcastManager, boolean fromCache) {
        mBroadcastManager = broadcastManager;
        mFromCache = fromCache;
    }

    @Override
    public void onCompleted(BoxResponse response) {
        final BoxResponseIntent intent = new BoxResponseIntent(response, mFromCache);
        if (!response.isSuccess()) {
            BoxLogUtils.e(TAG, response.getException());
        }

        mBroadcastManager.sendBroadcast(intent);
    }

}
