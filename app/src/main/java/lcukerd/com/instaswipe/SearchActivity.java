package lcukerd.com.instaswipe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;

import lcukerd.com.instaswipe.Database.DbInteract;
import lcukerd.com.instaswipe.Utils.BottomNavigationViewHelper;
import lcukerd.com.instaswipe.Utils.Scrapper;
import lcukerd.com.instaswipe.adapter.UserListAdapter;
import lcukerd.com.instaswipe.models.User;

/**
 * Created by User on 5/28/2017.
 */

public class SearchActivity extends AppCompatActivity
{
    private static final String TAG = "SearchActivity";
    private static final int ACTIVITY_NUM = 1;
    private RecyclerView userlist;
    private ArrayList<User> usersArrayList;
    private UserListAdapter adapter;
    private DbInteract interact;
    private ProgressBar progressBar;
    private boolean stop = false;

    private Context mContext = SearchActivity.this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemewithmenu);
        setContentView(R.layout.activity_home);

        Log.d(TAG, "onCreate: started.");

        interact = new DbInteract(this);

        findViewById(R.id.relLayout1).setVisibility(View.GONE);

        userlist = (RecyclerView) findViewById(R.id.recycler_users);
        progressBar = (ProgressBar) findViewById(R.id.searchProgressBar);
        userlist.setVisibility(View.VISIBLE);
        usersArrayList = new ArrayList<>();
        usersArrayList.addAll(interact.readfromDB());

        adapter = new UserListAdapter(usersArrayList, this, progressBar);
        userlist.setLayoutManager(new LinearLayoutManager(this));
        userlist.setAdapter(adapter);

        setupBottomNavigationView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.toolbar_search, menu);

        MenuItem searchIcon = menu.findItem(R.id.toolbar_searcher);
        SearchView searchView = new SearchView(this);
        searchView.setBackground(getResources().getDrawable(R.drawable.et_border));
        SearchView.SearchAutoComplete searchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setTextColor(Color.BLACK);
        searchAutoComplete.setHint("Search user");
        searchAutoComplete.setHintTextColor(Color.GRAY);
        MenuItemCompat.setShowAsAction(searchIcon, MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(searchIcon, searchView);

        SearchView search = (SearchView) searchIcon.getActionView();
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                query = query.replaceAll(" ","+");
                Log.d(TAG, "Search Text " + query + " https://www.instagram.com/web/search/topsearch/?context=blended&query="
                        + query + "&rank_token=0.624286187601405");
                stop = true;
                progressBar.setVisibility(View.VISIBLE);
                Ion.with(getApplicationContext())
                        .load("https://www.instagram.com/web/search/topsearch/?context=blended&query="
                                + query + "&rank_token=0.624286187601405")
                        .asString().setCallback(new FutureCallback<String>()
                {
                    @Override
                    public void onCompleted(Exception e, String result)
                    {
                        adapter.clear();
                        stop = false;
                        ImageLoader imageLoader = ImageLoader.getInstance();
                        imageLoader.init(ImageLoaderConfiguration.createDefault(getApplicationContext()));
                        try
                        {
                            final ArrayList<User> templist = Scrapper.getUsersfromsearch(result);
                            for (int i = 0; i < templist.size(); i++)
                            {
                                final User u = templist.get(i);
                                imageLoader.loadImage(u.url, new SimpleImageLoadingListener()
                                {
                                    @Override
                                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage)
                                    {
                                        if (stop == true)
                                        {
                                            adapter.clear();
                                            return;
                                        }
                                        progressBar.setVisibility(View.GONE);
                                        if (u.verfied)
                                            adapter.add(new User(loadedImage,
                                                u.name + u.isprivate + "✔", u.url, u.query));
                                        else
                                            adapter.add(new User(loadedImage,
                                                    u.name + u.isprivate, u.url, u.query));
                                    }
                                });
                                if (stop == true)
                                {
                                    adapter.clear();
                                    return;
                                }
                            }
                        } catch (StringIndexOutOfBoundsException ex)
                        {
                            Log.d(TAG, "", ex);
                            Toast.makeText(getApplicationContext(), "User not found!!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return true;
            }

            @Override
            public boolean onQueryTextChange(final String newText)
            {
                if (TextUtils.isEmpty(newText))
                {
                    adapter.refill(interact.readfromDB());
                }
                return true;
            }
        });
        return true;
    }

    /**
     * BottomNavigationView setup
     */
    private void setupBottomNavigationView()
    {
        Log.d(TAG, "setupBottomNavigationView: setting up BottomNavigationView");
        BottomNavigationViewEx bottomNavigationViewEx = (BottomNavigationViewEx) findViewById(R.id.bottomNavViewBar);
        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationViewEx);
        BottomNavigationViewHelper.enableNavigation(mContext, this, bottomNavigationViewEx);
        Menu menu = bottomNavigationViewEx.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
        menuItem.setChecked(true);
    }
}
