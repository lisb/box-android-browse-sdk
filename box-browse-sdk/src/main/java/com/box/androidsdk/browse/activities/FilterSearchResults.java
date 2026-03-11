package com.box.androidsdk.browse.activities;

import static androidx.core.view.WindowInsetsCompat.Type.displayCutout;
import static androidx.core.view.WindowInsetsCompat.Type.systemBars;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.box.androidsdk.browse.R;
import com.box.androidsdk.browse.fragments.BoxFilterSearchResultsFragment;
import com.box.androidsdk.browse.models.BoxSearchFilters;

/**
 * The type Filter search results.
 */
public class FilterSearchResults extends AppCompatActivity {
    private static String EXTRA_FILTERS = "extraFilters";
    private BoxSearchFilters mFilters;
    private BoxFilterSearchResultsFragment mFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_filter_search_results2);
        setSupportActionBar(findViewById(R.id.box_action_bar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final View root = findViewById(R.id.root);
        final View toolbar = findViewById(R.id.box_action_bar);
        final View fragmentContainer = findViewById(R.id.fragmentContainer);
        ViewGroupCompat.installCompatInsetsDispatch(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            final Insets insets = windowInsets.getInsets(displayCutout() | systemBars());
            root.setPadding(insets.left, 0, insets.right, 0);
            toolbar.setPadding(0, insets.top, 0, 0);
            ViewCompat.dispatchApplyWindowInsets(fragmentContainer, windowInsets.inset(insets.left, insets.top, insets.right, 0));
            return WindowInsetsCompat.CONSUMED;
        });

        if (savedInstanceState != null) {
            mFilters = (BoxSearchFilters) savedInstanceState.getSerializable(EXTRA_FILTERS);
        }
        else {
            mFilters = (BoxSearchFilters) getIntent().getExtras().getSerializable(EXTRA_FILTERS);
        }

        mFragment = (BoxFilterSearchResultsFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (mFragment == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            mFragment = BoxFilterSearchResultsFragment.newInstance(mFilters);
            ft.add(R.id.fragmentContainer, mFragment);
            ft.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        if (mFragment != null) {
            mFilters = mFragment.getCurrentFilters();
        }

        savedInstanceState.putSerializable(EXTRA_FILTERS, mFilters);
        super.onSaveInstanceState(savedInstanceState);
    }


    /**
     * New filter search results intent intent.
     *
     * @param context the context
     * @param filters current selected filters
     * @return the intent
     */
    public static Intent newFilterSearchResultsIntent(final Context context, BoxSearchFilters filters) {
        Intent intent = new Intent(context, FilterSearchResults.class);
        intent.putExtra(EXTRA_FILTERS, filters == null? new BoxSearchFilters(): filters);
        return intent;
    }

}

