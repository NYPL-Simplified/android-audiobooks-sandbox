/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.uamp.ui;

import android.app.ActivityOptions;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.android.uamp.R;
import com.example.android.uamp.model.ManifestModel;
import com.example.android.uamp.model.ManifestReader;
import com.example.android.uamp.utils.LogHelper;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract activity with toolbar, navigation drawer and cast support. Needs to be extended by
 * any activity that wants to be shown as a top level activity.
 *
 * The requirements for a subclass is to call {@link #initializeToolbar()} on onCreate, after
 * setContentView() is called and have three mandatory layout elements:
 * a {@link android.support.v7.widget.Toolbar} with id 'toolbar',
 * a {@link android.support.v4.widget.DrawerLayout} with id 'drawerLayout' and
 * a {@link android.widget.ListView} with id 'drawerList'.
 */
public abstract class ActionBarCastActivity extends AppCompatActivity {

    private static final String TAG = LogHelper.makeLogTag(ActionBarCastActivity.class);

    private static final int DELAY_MILLIS = 1000;

    private CastContext mCastContext;
    private MenuItem mMediaRouteMenuItem;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    private boolean mToolbarInitialized;

    private int menuItemToOpenWhenDrawerCloses = -1;

    private CastStateListener mCastStateListener = new CastStateListener() {
        @Override
        public void onCastStateChanged(int newState) {
            if (newState != CastState.NO_DEVICES_AVAILABLE) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mMediaRouteMenuItem.isVisible()) {
                            LogHelper.d(TAG, "Cast Icon is visible");
                            showFtu();
                        }
                    }
                }, DELAY_MILLIS);
            }
        }
    };


    /**
     * Is called when user clicks on an item in the burger left navigation menu.
     * Sees which menu item got clicked on, and acts accordingly.
     * TODO:  make my new chapter items have valid menuItemToOpenWhenDrawerCloses ids
     * TODO:  when a chapter item is clicked, it is highlighted correctly.  when the next one is clicked, the previous one doe not get un-highlighted.
     */
    private final DrawerLayout.DrawerListener mDrawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerClosed(View drawerView) {
            LogHelper.d(TAG, "menuItemToOpenWhenDrawerCloses=" + menuItemToOpenWhenDrawerCloses);

            if (mDrawerToggle != null) mDrawerToggle.onDrawerClosed(drawerView);
            if (menuItemToOpenWhenDrawerCloses >= 0) {
                Bundle extras = ActivityOptions.makeCustomAnimation(
                    ActionBarCastActivity.this, R.anim.fade_in, R.anim.fade_out).toBundle();

                Class activityClass = null;
                switch (menuItemToOpenWhenDrawerCloses) {
                    case R.id.navigation_back_to_catalog:
                        // TODO: display a message that says "you went back to library catalog"
                        // TODO: make sure there's a catalog id in R to use
                        // case R.id.navigation_allmusic:
                        //activityClass = MusicPlayerActivity.class;
                        break;
                    case R.id.navigation_audiobook_settings:
                        // TODO: display a settings activity
                        // TODO: make sure there's a settings activity for me in R
                        // TODO:  how are individual songs indicated?  not in the drawer, are they?  I think they're in the outside inteface.
                        // the placeholder is for the playlists functionality, which isn't included in the sample,
                        // only the all music is coded for.

                        // case R.id.navigation_playlists:
                        //activityClass = PlaceholderActivity.class;
                        break;
                }
                if (activityClass != null) {
                    startActivity(new Intent(ActionBarCastActivity.this, activityClass), extras);
                    finish();
                }
            }
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerStateChanged(newState);
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (mDrawerToggle != null) mDrawerToggle.onDrawerOpened(drawerView);
            if (getSupportActionBar() != null) getSupportActionBar()
                    .setTitle(R.string.app_name);
        }
    };

    private final FragmentManager.OnBackStackChangedListener mBackStackChangedListener =
        new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateDrawerToggle();
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        //int playServicesAvailable =
        //        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        //if (playServicesAvailable == ConnectionResult.SUCCESS) {
        mCastContext = CastContext.getSharedInstance(this);
        //}
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mToolbarInitialized) {
            throw new IllegalStateException("You must run super.initializeToolbar at " +
                "the end of your onCreate method");
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if (mCastContext != null) {
            mCastContext.addCastStateListener(mCastStateListener);
        }
        
        // Whenever the fragment back stack changes, we may need to update the
        // action bar toggle: only top level screens show the hamburger-like icon, inner
        // screens - either Activities or fragments - show the "Up" icon instead.
        getFragmentManager().addOnBackStackChangedListener(mBackStackChangedListener);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCastContext != null) {
            mCastContext.removeCastStateListener(mCastStateListener);
        }
        getFragmentManager().removeOnBackStackChangedListener(mBackStackChangedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);

        if (mCastContext != null) {
            mMediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
                    menu, R.id.media_route_menu_item);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // If not handled by drawerToggle, home needs to be handled by returning to previous
        if (item != null && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the drawer is open, back will close it
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }
        // Otherwise, it may return to the previous fragment stack
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // Lastly, it will rely on the system behavior for back
            super.onBackPressed();
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mToolbar.setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        mToolbar.setTitle(titleId);
    }

    protected void initializeToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar == null) {
            throw new IllegalStateException("Layout is required to include a Toolbar with id " + "'toolbar'");
        }
        mToolbar.inflateMenu(R.menu.main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout != null) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            if (navigationView == null) {
                throw new IllegalStateException("Layout requires a NavigationView " + "with id 'nav_view'");
            }

            // Create an ActionBarDrawerToggle that will handle opening/closing of the drawer:
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.open_content_drawer, R.string.close_content_drawer);
            mDrawerLayout.setDrawerListener(mDrawerListener);
            populateDrawerItems(navigationView);
            setSupportActionBar(mToolbar);
            updateDrawerToggle();
        } else {
            setSupportActionBar(mToolbar);
        }

        mToolbarInitialized = true;
    }

    private void populateDrawerItems(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        menuItemToOpenWhenDrawerCloses = menuItem.getItemId();
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });

        // from web:
        //navigationView.getMenu().clear(); //clear old inflated items.
        //navigationView.inflateMenu(R.menu.new_navigation_drawer_items); //inflate new items.

        // read the manifest file for list of chapter titles
        // TODO: the logic for where the assets are stored should be moved out of here,
        // and eventually hooked up to the SimplyE app.
        Context appContext = getApplicationContext();
        File assetDirectory = appContext.getFilesDir();
        Log.d(TAG, "assetDirectory=" + assetDirectory.getAbsolutePath()); // data/data/appname/stuff

        File testAssetDirectory = new File("file:///android_asset", "21_gun_salute");
        Log.d(TAG, "testAssetDirectory=" + testAssetDirectory.getAbsolutePath());

        AssetManager assetManager = appContext.getAssets();
        InputStream manifestFileStream = null;
        try {
            manifestFileStream = assetManager.open("21_gun_salute/audiobook_manifest.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * Returns the absolute path on the filesystem where a file created with {@link #openFileOutput} is stored.
        public abstract File getFileStreamPath(String name);

         * Returns the absolute path to the directory on the filesystem where files created with {@link #openFileOutput} are stored.
         public abstract File getFilesDir();
         *
         FileOutputStream fOut = context.openFileOutput(filename, appContext.MODE_PRIVATE); // if internal

         * Return a String array of all the assets at the given path.
         *
         * @param path A relative path within the assets, i.e., "docs/home.html".
         *
         * @return String[] Array of strings, one for each asset.  These file names are relative to 'path'.  You can open the file by
         *         concatenating 'path' and a name in the returned string (via File) and passing that to open().
         public native final String[] list(String path)
         */

        ManifestReader manifestReader = new ManifestReader();
        String fileContents = manifestReader.readManifest(manifestFileStream);
        ManifestModel manifestModel = manifestReader.parseManifest(fileContents);

        for (ManifestModel.Spine spineElement : manifestModel.getSpine()) {
            navigationView.getMenu().add(manifestModel.sanitizeString(spineElement.getTitle()));
        }

        //if (MusicPlayerActivity.class.isAssignableFrom(getClass())) {
        navigationView.setCheckedItem(R.id.navigation_back_to_catalog);
        //} else if (PlaceholderActivity.class.isAssignableFrom(getClass())) {
        //    navigationView.setCheckedItem(R.id.navigation_playlists);
        //}


    }

    protected void updateDrawerToggle() {
        if (mDrawerToggle == null) {
            return;
        }
        boolean isRoot = getFragmentManager().getBackStackEntryCount() == 0;
        mDrawerToggle.setDrawerIndicatorEnabled(isRoot);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(!isRoot);
            getSupportActionBar().setDisplayHomeAsUpEnabled(!isRoot);
            getSupportActionBar().setHomeButtonEnabled(!isRoot);
        }
        if (isRoot) {
            mDrawerToggle.syncState();
        }
    }

    /**
     * Shows the Cast First Time User experience to the user (an overlay that explains what is
     * the Cast icon)
     */
    private void showFtu() {
        Menu menu = mToolbar.getMenu();
        View view = menu.findItem(R.id.media_route_menu_item).getActionView();
        if (view != null && view instanceof MediaRouteButton) {
            IntroductoryOverlay overlay = new IntroductoryOverlay.Builder(this, mMediaRouteMenuItem)
                    .setTitleText(R.string.touch_to_cast)
                    .setSingleTime()
                    .build();
            overlay.show();
        }
    }
}
