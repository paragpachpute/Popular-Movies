package com.paragdev.popularmovies;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import com.github.paolorotolo.expandableheightlistview.ExpandableHeightListView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MovieDetailActivity extends AppCompatActivity {
    public static final String REVIEW_QUERY_KEY = "reviews";
    
    private long movieID;
    private final String MOVIE_ID = "id";

    private String movieTitle;
    private String date;
    private String plotSynopsis;
    private float vote;
    private String posterPath;

    private ArrayList<Review> reviews;
    private boolean reviewQuery;
    private boolean movieQuery;

    private ReviewAdapter  reviewAdapter;

    @Bind(R.id.imageView) ImageView poster;
    @Bind(R.id.title) TextView title;
    @Bind(R.id.release_date) TextView releaseDate;
    @Bind(R.id.plot) TextView plot;
    @Bind(R.id.rating) TextView rating;
    @Bind(R.id.review) TextView reviewTitle;
    @Bind(R.id.ratingBar) RatingBar ratingBar;
    @Bind(R.id.expandable_listview) ExpandableHeightListView reviewList;
    @Bind(R.id.progressBar) ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        ButterKnife.bind(this);
        reviewList.setExpanded(true);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(MOVIE_ID)){
                movieID = intent.getLongExtra(MOVIE_ID, 168259);
            }
        }

        if(savedInstanceState != null){
            movieTitle = savedInstanceState.getString("title");
            plotSynopsis = savedInstanceState.getString("plot");
            date = savedInstanceState.getString("date");
            posterPath = savedInstanceState.getString("posterPath");
            vote = savedInstanceState.getFloat("rating");
            reviewTitle.setText(savedInstanceState.getString("reviewTitle"));
            loadMovieInfo();
            reviews = savedInstanceState.getParcelableArrayList("reviews");

        }else{
            reviews = new ArrayList<>();
            GetMovieTask getMovieTask = new GetMovieTask();
            getMovieTask.execute(String.valueOf(movieID));

            GetMovieTask getMovieReviews = new GetMovieTask();
            getMovieReviews.execute(String.valueOf(movieID), REVIEW_QUERY_KEY);

        }

        reviewAdapter = new ReviewAdapter(this, reviews);
        reviewList.setAdapter(reviewAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("title", movieTitle);
        outState.putString("plot", plotSynopsis);
        outState.putString("date", date);
        outState.putString("posterPath", posterPath);
        outState.putFloat("rating", vote);
        outState.putString("reviewTitle", reviewTitle.getText().toString());
        outState.putParcelableArrayList("reviews", reviews);
    }

    public void loadMovieInfo(){
        getSupportActionBar().setTitle(movieTitle);
        title.setText(movieTitle);
        releaseDate.setText(date);
        rating.setText("" + vote + "/10");
        ratingBar.setRating(vote / 2);
        plot.setText(plotSynopsis);
        Picasso.with(this)
                .load(posterPath)
                .placeholder(R.drawable.backgroundwithtext)
                .into(poster);
    }

    public class GetMovieTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String movieInfoJsonStr = null;
            try {

                final String BASE_URL = "http://api.themoviedb.org/3/movie/";
                final String API = "api_key";
                Uri builtUri;

                if(params.length > 1){
                    builtUri = Uri.parse(BASE_URL).buildUpon()
                            .appendPath(String.valueOf(params[0]))
                            .appendPath(String.valueOf(params[1]))
                            .appendQueryParameter(API, getString(R.string.api_key))
                            .build();

                    reviewQuery = true;

                }else {
                    builtUri = Uri.parse(BASE_URL).buildUpon()
                            .appendPath(String.valueOf(params[0]))
                            .appendQueryParameter(API, getString(R.string.api_key))
                            .build();
                    movieQuery = true;
                }

                URL url = new URL(builtUri.toString());


                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    movieInfoJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    movieInfoJsonStr = null;
                }
                movieInfoJsonStr = buffer.toString();

            } catch (IOException e) {
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                movieInfoJsonStr = null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                    }
                }
            }

            return movieInfoJsonStr;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }


        @Override
        protected void onPostExecute(String movieInfoJsonStr) {
            super.onPostExecute(movieInfoJsonStr);
            final String DATE = "release_date";
            final String RATING = "vote_average";
            final String POSTER_PATH = "poster_path";
            final String OVERVIEW = "overview";
            final String TITLE = "original_title";
            final String RESULTS = "results";
            final String REVIEW_KEY = "content";
            final String REVIEW_AUTHOR_KEY = "author";
            if(movieQuery) {
                try {
                    JSONObject movieJson = new JSONObject(movieInfoJsonStr);
                    movieTitle = movieJson.getString(TITLE);
                    date = movieJson.getString(DATE);
                    Double ratingValue = movieJson.getDouble(RATING);
                    vote = ratingValue.floatValue();
                    plotSynopsis = movieJson.getString(OVERVIEW);
                    posterPath = "http://image.tmdb.org/t/p/w185/" + movieJson.getString(POSTER_PATH);
                    loadMovieInfo();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if(reviewQuery) {
                try {
                    JSONObject reviewJson = new JSONObject(movieInfoJsonStr);
                    JSONArray reviewArray = reviewJson.getJSONArray(RESULTS);
                    if(reviewArray.length()!=0) {
                        for (int i = 0; i < reviewArray.length(); i++) {
                            JSONObject reviewData = reviewArray.getJSONObject(i);
                            String content = reviewData.getString(REVIEW_KEY);
                            String author = reviewData.getString(REVIEW_AUTHOR_KEY);
                            Review review = new Review(author, content);
                            reviews.add(review);
                        }
                        reviewAdapter.notifyDataSetChanged();
                    }else{
                        reviewTitle.setText(R.string.no_review);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            progressBar.setVisibility(View.GONE);
        }

    }

}
