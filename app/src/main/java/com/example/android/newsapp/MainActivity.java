package com.example.android.newsapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    //Declaring the base URL to which the search queries will be appended
    String baseURL = "http://content.guardianapis.com/search?";
    //Initializing other Variables
    Uri queryUri;
    String searchQuery;
    String headline;
    String trailText;
    String lastModified;
    String thumbnailUrl;
    String webUrl;
    int i;
    int f;
    // Defined Array values to show in ListView
    ArrayList<Highlight> highlights = new ArrayList<>();
    ArrayList<Bitmap> thumbnails = new ArrayList<>();
    private Parcelable listState;
    private RecyclerView rvHighlights;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            listState = savedInstanceState.getParcelable("ListState");
        }
        setContentView(R.layout.activity_main);
        //Finding and assigning a floatingActionButton as the Search button to a Variable.
        FloatingActionButton searchButton = findViewById(R.id.search_button);
        //Setting an OnClickListener to execute activities on Click.
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Clearing previous image data, if any
                thumbnails.clear();
                //Closing the softkeyboard
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                //Find the view containing the query
                SearchView searchBar = findViewById(R.id.search_bar);
                //Convert the Query to String
                searchQuery = searchBar.getQuery().toString();
                //Getting a search query specific URL
                queryUri = getUri(baseURL);
                Log.v("queryUri", queryUri.toString());
                //Get a reference to the LoaderManager, in order to interact with loaders.
                android.support.v4.app.LoaderManager loaderManager = getSupportLoaderManager();
                //Initialize the loader. If loader with the id doesn't exist, it will use the
                //onCreateLoader to create the loader and restart on second instance of search
                //(no reuse as in initLoader)
                loaderManager.restartLoader(1, null, new jsonLoader());
                //Creating 9 placeholder values.
                for (int f = 0; f < 9; f++) {
                    thumbnails.add(BitmapFactory.decodeResource(getResources(), R.drawable.ic_search_black_48dp));
                }
            }
        });

        if (rvHighlights != null) {
            rvHighlights.getLayoutManager().onRestoreInstanceState(listState);
        }

    }

    /*
     * Method that will create the final query URL
     */
    public Uri getUri(String baseURL) {
        /*
          Building the URL using URI builder
         */
        //Initializing with Uri Builder Variable
        Uri baseUri = Uri.parse(baseURL);
        //Converting the Uri to Uri Builder
        Uri.Builder queryUri = baseUri.buildUpon();

        //Appending the Search Query
        queryUri = queryUri.appendQueryParameter("q", searchQuery);
        //Requesting JSON format
        queryUri = queryUri.appendQueryParameter("format", "json");
        //Appending from date parameter
       // queryUri = queryUri.appendQueryParameter("from-date", "2016-09-30");
        //Appending to date parameter
       // queryUri = queryUri.appendQueryParameter("to-date", "2017-09-30");
        //Appending Order by parameter
      //  queryUri = queryUri.appendQueryParameter("order-by", "newest");
        //Appending the Page number of the search result
      //  queryUri = queryUri.appendQueryParameter("page", "2");
        //Appending the API KEY which is set to TEST
        queryUri = queryUri.appendQueryParameter("api-key", "test");

        //Show the following fields for the search page preview
        queryUri = queryUri.appendQueryParameter("show-fields",
                "trailText,headLine,lastModified,thumbnail");

        //Return the URI with the given attributes
        return queryUri.build();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("ListState", rvHighlights.getLayoutManager().onSaveInstanceState());

    }

    //First LoaderCallBack implementation
    private class jsonLoader implements android.support.v4.app.LoaderManager.LoaderCallbacks<String> {

        @Override
        public android.support.v4.content.Loader<String> onCreateLoader(int i, Bundle argument) {
            return new NewsListLoader(getApplicationContext(), queryUri);
        }

        //The following code will be executed when the Loader has finished Loading.
        @Override
        public void onLoadFinished(android.support.v4.content.Loader<String> loader, String jsonResponse
        ) {
            try {
                JSONObject jsonObjectAsResponse = new JSONObject(jsonResponse);
                JSONObject response = jsonObjectAsResponse.getJSONObject("response");
                JSONArray results = response.getJSONArray("results");
                highlights.clear();
                //for loop to store all items in the array list
                for (i = 0; i < results.length(); i++) {
                    JSONObject webTitleArray = results.getJSONObject(i);
                    headline = webTitleArray.getString("webTitle");
                    JSONObject fields = webTitleArray.getJSONObject("fields");
                    trailText = fields.getString("trailText");
                    lastModified = fields.getString("lastModified");
                    //Link for Thumbnail
                    thumbnailUrl = fields.getString("thumbnail");
                    //Link to open the article
                    webUrl = webTitleArray.getString("webUrl");
                    //Create new object
                    Highlight highlight = new Highlight(headline, trailText, lastModified, webUrl, i);
                    //Add the new highlight to the ArrayList
                    highlights.add(highlight);
                    //Initializing the Loader. Fingers crossed
                    android.support.v4.app.LoaderManager loaderManager = getSupportLoaderManager();
                    //Initialize the loader. If loader with the id doesn't exist, it will use the
                    //onCreateLoader to create the loader and restart on second instance of search
                    //(no reuse as in initLoader)
                    loaderManager.restartLoader(i, null, new thumbnailLoader());
                }
                // Lookup the recyclerview in activity layout
                rvHighlights = (RecyclerView) findViewById(R.id.headlines_recycler);

                // Create adapter passing in the Json fields
                HighlightsAdapter adapter = new HighlightsAdapter(getApplicationContext(), highlights);
                // Attach the adapter to the recyclerview to populate items
                rvHighlights.setAdapter(adapter);
                // Set layout manager to position the items
                rvHighlights.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            } catch (JSONException e) {
                Log.e("JsonTextError", "Some problem procuring the jsonresponse");
            }

        }

        //Compulsory code. Specifies what happens when the Loader is reset.
        @Override
        public void onLoaderReset(android.support.v4.content.Loader loader) {
            //Nothing here. On purpose.
        }
    }

    //Second Image LoaderCallback implementation
    private class thumbnailLoader implements android.support.v4.app.LoaderManager.LoaderCallbacks<Bitmap> {
        int positionStore;

        @Override
        public android.support.v4.content.Loader<Bitmap> onCreateLoader(int i, Bundle argument) {
            positionStore = i;
            return new ThumbnailLoader(getApplicationContext(), thumbnailUrl);
        }

        //To be executed on finishing Loading
        @Override
        public void onLoadFinished(android.support.v4.content.Loader<Bitmap> loader, Bitmap thumbnail) {
            //If the images belong to 1 to 9 positions, then the position simply needs to be
            //replaced by another bitmap. If 10th, add.
            if (positionStore < 9) {
                thumbnails.set(positionStore, thumbnail);
            } else {
                thumbnails.add(positionStore, thumbnail);
            }
            //When all 10 places are filled, the following conditions of code are true.
            if (thumbnails.size() == highlights.size()) {
                // Lookup the recyclerview in activity layout
                rvHighlights = (RecyclerView) findViewById(R.id.headlines_recycler);
                //Create adapter passing in the Bitmap images
                ThumbnailAdapter thumbnailsAdapter = new ThumbnailAdapter(getApplicationContext(),
                        highlights, thumbnails);
                // Attach the adapter to the recyclerview to populate items
                rvHighlights.setAdapter(thumbnailsAdapter);
                // Set layout manager to position the items
                rvHighlights.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            } else {
                return;
            }
        }

        //Compulsory code. Specifies what happens when the Loader is reset.
        @Override
        public void onLoaderReset(android.support.v4.content.Loader loader) {
            //Nothing here. On purpose.
        }
    }
}

