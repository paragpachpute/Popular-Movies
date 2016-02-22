package com.paragdev.popularmovies;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    final CharSequence[] items = {" Most Popular ", " Highest Rated "};
    private final String KEY_MOST_POPULAR = "popularity.desc";
    private final String KEY_HIGHLY_RATED = "vote_count.desc";
    private final String KEY_MOVIE_ID = "id";
    private final String KEY_RESULTS = "results";
    private final String KEY_IMAGE_BASE_URL = "http://image.tmdb.org/t/p/w185/";
    private final String KEY_POSTER_PATH = "poster_path";
    private final String KEY_SIS_POSITION = "position"; //Key SavedInstanceState Position


    private GridView gridView;
    private String resultJSON = null;
    private String[] imgUrl = new String[20];
    private AlertDialog choice;
    private String FLAG_CURRENT = KEY_MOST_POPULAR;
    private JSONArray movieDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateView(FLAG_CURRENT);

        if (savedInstanceState != null) {
            int temp = savedInstanceState.getInt(KEY_SIS_POSITION);
            gridView.setSelection(temp);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateView(FLAG_CURRENT);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateView(FLAG_CURRENT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mapMenu:
                showChoices();
                break;
        }
        return true;
    }

    private void showChoices() {

        choice = new AlertDialog.Builder(MainActivity.this)
                .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0:
                                updateView(KEY_MOST_POPULAR);
                                break;
                            case 1:
                                updateView(KEY_HIGHLY_RATED);
                                break;
                        }
                        choice.dismiss();
                    }
                }).setTitle(getString(R.string.choose_filter))
                .show();
    }

    private void updateView(String type) {
        FLAG_CURRENT = type;

        if (FetchMovie()) {
            gridView = (GridView) findViewById(R.id.movie_grid);
            gridView.setAdapter(new ImageAdapter(this, imgUrl));
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    try {
                        JSONObject object = movieDetails.getJSONObject(position);
                        long movieId = object.getLong(KEY_MOVIE_ID);

                        Intent intent = new Intent(getApplicationContext(), MovieDetailActivity.class);
                        intent.putExtra(KEY_MOVIE_ID, movieId);

                        startActivity(intent);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        showErrorDialog();
                    }

                }
            });
        } else {
            showErrorDialog();
        }
    }

    private void showErrorDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setCancelable(true)
                .setMessage(getString(R.string.error_msg))
                .setPositiveButton(getString(R.string.retry_msg), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(getString(R.string.okay_msg), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }

                }).show();
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int position = savedInstanceState.getInt(KEY_SIS_POSITION);
        gridView.setSelection(position);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        int position = gridView.getFirstVisiblePosition();
        outState.putInt(KEY_SIS_POSITION, position);
    }

    private boolean FetchMovie() {
        FetchMovieData fetchMovieData = new FetchMovieData();

        try {
            resultJSON = fetchMovieData.execute(FLAG_CURRENT).get();

            if (resultJSON != null) {
                JSONObject movie = new JSONObject(resultJSON);
                movieDetails = movie.getJSONArray(KEY_RESULTS);

                for (int i = 0; i < movieDetails.length(); i++) {
                    JSONObject currentMovie = movieDetails.getJSONObject(i);
                    imgUrl[i] = KEY_IMAGE_BASE_URL + currentMovie.getString(KEY_POSTER_PATH);
                }

                return true;
            } else
                return false;

        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

    }
}
