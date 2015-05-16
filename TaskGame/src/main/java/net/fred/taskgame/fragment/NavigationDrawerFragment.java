/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.taskgame.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import net.fred.taskgame.R;
import net.fred.taskgame.activity.MainActivity;
import net.fred.taskgame.activity.SettingsActivity;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.NavigationItem;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.adapters.NavDrawerAdapter;
import net.fred.taskgame.model.adapters.NavDrawerCategoryAdapter;
import net.fred.taskgame.utils.CroutonHelper;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.Display;
import net.fred.taskgame.utils.Navigation;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.ThrottledFlowContentObserver;
import net.fred.taskgame.view.NonScrollableListView;

import java.util.ArrayList;
import java.util.List;


/**
 * Fragment used for managing interactions for and presentation of a navigation
 * drawer. See the <a href=
 * "https://developer.android.com/design/patterns/navigation-drawer.html#Interaction"
 * > design guidelines</a> for a complete explanation of the behaviors
 * implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    public ActionBarDrawerToggle mDrawerToggle;
    DrawerLayout mDrawerLayout;
    String[] mNavigationArray;
    TypedArray mNavigationIconsArray;
    TypedArray mNavigationIconsSelectedArray;
    private NonScrollableListView mDrawerList;
    private NonScrollableListView mDrawerCategoriesList;
    private View categoriesListHeader;
    private View settingsView, settingsViewCat;
    private MainActivity mActivity;
    private CharSequence mTitle;

    // Categories list scrolling
    private int listViewPosition;
    private int listViewPositionOffset;

    private ThrottledFlowContentObserver mContentObserver = new ThrottledFlowContentObserver(100) {
        @Override
        public void onChangeThrottled() {
            init();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("listViewPosition")) {
                listViewPosition = savedInstanceState.getInt("listViewPosition");
                listViewPositionOffset = savedInstanceState.getInt("listViewPositionOffset");
            }
        }

        // registers for callbacks from the specified tables
        mContentObserver.registerForContentChanges(inflater.getContext(), Task.class);
        mContentObserver.registerForContentChanges(inflater.getContext(), Category.class);

        return inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
    }

    @Override
    public void onDestroyView() {
        mContentObserver.unregisterForContentChanges(getView().getContext());

        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (MainActivity) getActivity();

        init();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        refreshListScrollPosition();
        outState.putInt("listViewPosition", listViewPosition);
        outState.putInt("listViewPositionOffset", listViewPositionOffset);
    }


    private void refreshListScrollPosition() {
        if (mDrawerCategoriesList != null) {
            listViewPosition = mDrawerCategoriesList.getFirstVisiblePosition();
            View v = mDrawerCategoriesList.getChildAt(0);
            listViewPositionOffset = (v == null) ? 0 : v.getTop();
        }
    }


    /**
     * Initialization of compatibility navigation drawer
     */
    private void init() {

        mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawer_layout);
        mDrawerLayout.setFocusableInTouchMode(false);

        // Setting specific bottom margin for Kitkat with translucent nav bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View leftDrawer = getView().findViewById(R.id.left_drawer);
            int leftDrawerBottomPadding = Display.getNavigationBarHeightKitkat(getActivity());
            leftDrawer.setPadding(leftDrawer.getPaddingLeft(), leftDrawer.getPaddingTop(),
                    leftDrawer.getPaddingRight(), leftDrawerBottomPadding);
        }

        buildMainMenu();
        buildCategoriesMenu();

        // ActionBarDrawerToggle± ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(mActivity,
                mDrawerLayout,
                getMainActivity().getToolbar(),
                R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                // Saves the scrolling of the categories list
                refreshListScrollPosition();
                // Restore title
                mActivity.getSupportActionBar().setTitle(mTitle);
                // Call to onPrepareOptionsMenu()
                mActivity.supportInvalidateOptionsMenu();
            }


            public void onDrawerOpened(View drawerView) {
                // Commits all pending actions
                mActivity.commitPending();
                // Finishes action mode
                mActivity.finishActionMode();
                mTitle = mActivity.getSupportActionBar().getTitle();
                mActivity.getSupportActionBar().setTitle(mActivity.getApplicationContext().getString(R.string
                        .app_name));
                mActivity.supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // just styling option
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }


    private void buildCategoriesMenu() {
        // Retrieves data to fill tags list
        List<Category> categories = DbHelper.getCategories();

        mDrawerCategoriesList = (NonScrollableListView) getView().findViewById(R.id.drawer_tag_list);

        // Inflater used for header and footer
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        // Inflation of header view
        if (categoriesListHeader == null) {
            categoriesListHeader = inflater.inflate(R.layout.drawer_category_list_header, null);
        }

        // Inflation of Settings view
        if (settingsView == null) {
            settingsView = ((ViewStub) getActivity().findViewById(R.id.settings_placeholder)).inflate();
        }
        settingsView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        if (settingsViewCat == null) {
            settingsViewCat = inflater.inflate(R.layout.drawer_category_list_footer, null);
        }
        settingsViewCat.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        if (mDrawerCategoriesList.getAdapter() == null) {
            mDrawerCategoriesList.addFooterView(settingsViewCat);
        }
        if (categories.size() == 0) {
            categoriesListHeader.setVisibility(View.GONE);
            settingsViewCat.setVisibility(View.GONE);
            settingsView.setVisibility(View.VISIBLE);
        } else if (categories.size() > 0) {
            settingsView.setVisibility(View.GONE);
            categoriesListHeader.setVisibility(View.VISIBLE);
            settingsViewCat.setVisibility(View.VISIBLE);
        }

        mDrawerCategoriesList.setAdapter(new NavDrawerCategoryAdapter(mActivity, categories));

        // Sets click events
        mDrawerCategoriesList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                // Commits pending deletion or archiviation
                mActivity.commitPending();
                // Stops search service
                if (mActivity.getSearchMenuItem() != null && MenuItemCompat.isActionViewExpanded(mActivity
                        .getSearchMenuItem()))
                    MenuItemCompat.collapseActionView(mActivity.getSearchMenuItem());

                Object item = mDrawerCategoriesList.getAdapter().getItem(position);
                // Ensuring that clicked item is not the ListView header
                if (item != null) {
                    Category tag = (Category) item;
                    selectNavigationItem(mDrawerCategoriesList, position);
                    mActivity.updateNavigation(String.valueOf(tag.id));
                    mDrawerCategoriesList.setItemChecked(position, true);
                    if (mDrawerList != null)
                        mDrawerList.setItemChecked(0, false); // Forces redraw
                    mActivity.initTasksList(mActivity.getIntent());
                }
            }
        });

        // Sets long click events
        mDrawerCategoriesList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long arg3) {
                if (mDrawerCategoriesList.getAdapter() != null) {
                    Object item = mDrawerCategoriesList.getAdapter().getItem(position);
                    // Ensuring that clicked item is not the ListView header
                    if (item != null) {
                        mActivity.editTag((Category) item);
                    }
                } else {
                    getMainActivity().showMessage(R.string.category_deleted, CroutonHelper.ALERT);
                }
                return true;
            }
        });

        // Restores listview position when turning back to list
        if (mDrawerCategoriesList != null && categories.size() > 0) {
            if (mDrawerCategoriesList.getCount() > listViewPosition) {
                mDrawerCategoriesList.setSelectionFromTop(listViewPosition, listViewPositionOffset);
            } else {
                mDrawerCategoriesList.setSelectionFromTop(0, 0);
            }
        }

        mDrawerCategoriesList.justifyListViewHeightBasedOnChildren();
    }


    private void buildMainMenu() {
        // Sets the adapter for the MAIN navigation list view
        mDrawerList = (NonScrollableListView) getView().findViewById(R.id.drawer_nav_list);
        mNavigationArray = getResources().getStringArray(R.array.navigation_list);
        mNavigationIconsArray = getResources().obtainTypedArray(R.array.navigation_list_icons);
        mNavigationIconsSelectedArray = getResources().obtainTypedArray(R.array.navigation_list_icons_selected);

        final List<NavigationItem> items = new ArrayList<>();
        for (int i = 0; i < mNavigationArray.length; i++) {
            if (!checkSkippableItem(i)) {
                NavigationItem item = new NavigationItem(i, mNavigationArray[i], mNavigationIconsArray.getResourceId(i,
                        0), mNavigationIconsSelectedArray.getResourceId(i, 0));
                items.add(item);
            }
        }
        mDrawerList.setAdapter(new NavDrawerAdapter(mActivity, items));

        // Sets click events
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                mActivity.commitPending();
                String navigation = getResources().getStringArray(R.array.navigation_list_codes)[items.get(position)
                        .getArrayIndex()];
                selectNavigationItem(mDrawerList, position);
                mActivity.updateNavigation(navigation);
                mDrawerList.setItemChecked(position, true);
                if (mDrawerCategoriesList != null)
                    mDrawerCategoriesList.setItemChecked(0, false); // Called to force redraw
                // Reset intent
                mActivity.getIntent().setAction(Intent.ACTION_MAIN);

                // Call method to update tasks list
                mActivity.initTasksList(mActivity.getIntent());
            }
        });

        mDrawerList.justifyListViewHeightBasedOnChildren();
    }


    private boolean checkSkippableItem(int i) {
        boolean skippable = false;
        boolean dynamicMenu = PrefUtils.getBoolean(PrefUtils.PREF_DYNAMIC_MENU, true);
        switch (i) {
            case Navigation.REMINDERS:
                if (DbHelper.getTasksWithReminder(false).size() == 0 && dynamicMenu)
                    skippable = true;
                break;
            case Navigation.TRASH:
                if (DbHelper.getTasksTrashed().size() == 0 && dynamicMenu)
                    skippable = true;
                break;
        }
        return skippable;
    }


    /**
     * Swaps fragments in the main content view
     */
    private void selectNavigationItem(ListView list, int position) {
        Object itemSelected = list.getItemAtPosition(position);
        if (itemSelected.getClass().isAssignableFrom(NavigationItem.class)) {
            mTitle = ((NavigationItem) itemSelected).getText();
            // Is a category
        } else {
            mTitle = ((Category) itemSelected).name;
        }
        // Navigation drawer is closed after a while to avoid lag
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportActionBar().setTitle(mTitle);
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }
        }, 500);

    }


    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }
}