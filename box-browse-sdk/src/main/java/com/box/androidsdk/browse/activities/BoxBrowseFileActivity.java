package com.box.androidsdk.browse.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.models.BoxSessionDto;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxBookmark;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestUpdateSharedItem;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;
import com.eclipsesource.json.JsonObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

/**
 * This activity is launched to navigate to a boxfile
 */
public class BoxBrowseFileActivity extends BoxBrowseActivity {
    /**
     * Extra serializable intent parameter that adds a {@link com.box.androidsdk.content.models.BoxFile} to the intent
     */
    public static final String EXTRA_BOX_FILE = "extraBoxFile";

    /**
     * Extra intent parameter used to filter the returned files by extension type
     */
    public static final String EXTRA_BOX_EXTENSION_FILTER = "extraBoxExtensionFilter";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.box_browsesdk_activity_file);
        initToolbar();
        initRecentSearches();
        if (getSupportFragmentManager().getBackStackEntryCount() < 1){
            final BoxFolder item = (BoxFolder) getIntent().getSerializableExtra(EXTRA_FOLDER);
            onItemClick(item);
            setTitle(item);
        } else {
            final BoxFolder currentFolder = getCurrentFolder();
            if (currentFolder != null) {
                setTitle(currentFolder);
            }
        }

    }

    @Override
    public void onItemClick(BoxItem item) {
        super.onItemClick(item);
        if (item instanceof BoxFile || item instanceof BoxBookmark) {
            if (item.getSharedLink() != null && item.getSharedLink().getURL() != null) {
                finishWithResult(item);
            } else {
                getApiExecutor(getApplication()).execute(mController.getCreatedSharedLinkRequest(item).toTask());
            }
        }
    }

    private void finishWithResult(BoxItem item) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_BOX_FILE, item);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    protected void handleBoxResponse(BoxResponse response) {
        if (response.getRequest() instanceof BoxRequestUpdateSharedItem) {
            if (response.isSuccess()) {
                finishWithResult((BoxItem) response.getResult());
            } else {
                if (response.getException() instanceof BoxException) {
                    int responseCode = ((BoxException) response.getException()).getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                        return;
                    }

                    if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                        Toast.makeText(this, R.string.box_sharesdk_insufficient_permissions, Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                Toast.makeText(this, R.string.box_sharesdk_unable_to_modify_toast, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.box_browsesdk_menu_file, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Create an intent to launch an instance of this activity to browse folders.
     *
     * @param context current context.
     * @param folder  folder to browse
     * @param session a session, should be already authenticated.
     * @return an intent to launch an instance of this activity.
     */
    public static Intent getLaunchIntent(Context context, final BoxFolder folder, final BoxSession session) {
        if (folder == null || SdkUtils.isBlank(folder.getId()))
            throw new IllegalArgumentException("A valid folder must be provided to browse");
        if (session == null || session.getUser() == null || SdkUtils.isBlank(session.getUser().getId()))
            throw new IllegalArgumentException("A valid user must be provided to browse");

        Intent intent = new Intent(context, BoxBrowseFileActivity.class);
        intent.putExtra(EXTRA_FOLDER, folder);
        intent.putExtra(EXTRA_SESSION, BoxSessionDto.marshal(session));
        return intent;
    }


    /**
     * Create a builder object that can be used to construct an intent to launch an instance of this activity.
     *
     * @param context current context.
     * @param session a session, should be already authenticated.
     * @return a builder object to use to construct an instance of this class.
     */
    public static BoxBrowseFileActivity.IntentBuilder createIntentBuilder(final Context context, final BoxSession session){
        return new IntentBuilder(context, session);
    }


    /**
     * The type Intent builder.
     */
    public static class IntentBuilder extends BoxBrowseActivity.IntentBuilder<IntentBuilder>{

        ArrayList<String> mAllowedExtensions;

        /**
         * Instantiates a new Intent builder.
         *
         * @param context the context
         * @param session the session
         */
        protected IntentBuilder(final Context context, final BoxSession session){
            super(context, session);
        }

        /**
         * Sets a list of allowed extensions to filter files by.
         *
         * @param allowedExtensions extension of files which should be shown while browsing.
         * @return the intent builder
         */
        public IntentBuilder setAllowedExtensions(final ArrayList<String> allowedExtensions){
            mAllowedExtensions = allowedExtensions;
            return this;
        }

        @Override
        protected void addExtras(Intent intent) {
            if (mAllowedExtensions != null){
                intent.putExtra(EXTRA_BOX_EXTENSION_FILTER, mAllowedExtensions);
            }
            super.addExtras(intent);
        }

        @Override
        protected Intent createLaunchIntent() {
            if (mFolder == null){
                mFolder = BoxFolder.createFromId("0");
            }
            return getLaunchIntent(mContext, mFolder, mSession);
        }


    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders.
     * This method is deprecated use the createIntentBuilder instead.
     *
     * @param context  current context.
     * @param folderId folder id to navigate.
     * @param session  session.
     * @return an intent to launch an instance of this activity.
     */
    @Deprecated
    public static Intent getLaunchIntent(Context context, final String folderId, final BoxSession session) {
        return createIntentBuilder(context,session).setStartingFolder(BoxFolder.createFromId(folderId)).createIntent();
    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders. This version will immediately show the given name in the navigation spinner
     * to before information about it has been fetched from the server.
     * This method is deprecated use the createIntentBuilder instead.
     *
     * @param context    current context.
     * @param folderName Name to show in the navigation spinner. Should be name of the folder.
     * @param folderId   folder id to navigate.
     * @param session    session.
     * @return an intent to launch an instance of this activity.
     */
    @Deprecated
    public static Intent getLaunchIntent(Context context, final String folderName, final String folderId, final BoxSession session) {
        BoxFolder folder = BoxFolder.createFromId(folderId);
        JsonObject jsonObject = folder.toJsonObject().add(BoxItem.FIELD_NAME, folderName);

        return createIntentBuilder(context,session).setStartingFolder(new BoxFolder(jsonObject)).createIntent();
    }

    /**
     * Create an intent to launch an instance of this activity to navigate folders. This version will disable all files with extension types not included in the extensions list.
     * This method is deprecated use the createIntentBuilder instead.
     *
     * @param context           current context.
     * @param folderId          folder id to navigate.
     * @param allowedExtensions extension types to enable.
     * @param session           session.
     * @return an intent to launch an instance of this activity.
     */
    @Deprecated
    public static Intent getLaunchIntent(Context context, final String folderId, final ArrayList<String> allowedExtensions, final BoxSession session) {
       return createIntentBuilder(context,session).setStartingFolder(BoxFolder.createFromId(folderId)).setAllowedExtensions(allowedExtensions).createIntent();
    }

}
