package com.paragdev.popularmovies;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FetchMovieData extends AsyncTask<String, Void, String> {

    private final String API_KEY = "94b7ce5a595aa7c3aab83bce0fb0db3d";
    HttpURLConnection urlConnection = null;
    BufferedReader reader = null;
    String MovieDBjson = null;

    @Override
    protected String doInBackground(String... params) {
        try {
            String sort = params[0];

            final String BASE_URL = "http://api.themoviedb.org/3/discover/movie";
            final String API = "api_key";
            final String KEY_SORTBY = "sort_by";
            Uri builtUri;
            builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(KEY_SORTBY, sort)
                    .appendQueryParameter(API, API_KEY)
                    .build();
            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();  //stores all data fetched
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return null;
            }

            MovieDBjson = buffer.toString();
            Log.d("result", MovieDBjson);
        } catch (IOException e) {
            Log.e("PlaceholderFragment", "Error " + e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }

        return MovieDBjson;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }
}
