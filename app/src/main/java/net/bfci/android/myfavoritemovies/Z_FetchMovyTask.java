/**
 * Created by fady on 5/24/16.
 */

package net.bfci.android.myfavoritemovies;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

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
import java.util.Arrays;

public class Z_FetchMovyTask extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = Z_FetchMovyTask.class.getSimpleName();
    private A_MainActivityFragment mainActivityFragment;

    public Z_FetchMovyTask(A_MainActivityFragment a_mainActivityFragment) {
        super();
        mainActivityFragment= a_mainActivityFragment;
    }

    @Override
    protected String[] doInBackground(String... params) {

        //specify sortBy criteria
        String sortBy = X_Constants.POPULAR;
        if(params[0]!=null){
            if(params[0].equals(X_Constants.TOP_RATED)){
                sortBy = X_Constants.TOP_RATED;
            }else if(params[0].equals(X_Constants.FAVORITE)){
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mainActivityFragment.getActivity());
                String favorite = preferences.getString(mainActivityFragment.getActivity().getString(R.string.pref_favorite_key), "[]");
                try {
                    JSONArray favoriteJsonArray= new JSONArray(favorite);
                    JSONArray allMoviewJsonArray = new JSONArray();
                    for(int i=0; i< favoriteJsonArray.length(); i++){
                        sortBy=favoriteJsonArray.getJSONObject(i).getString(X_Constants.MOVY_ID_PARAM_NAME);
                        String str = getMovyDataAsStr(sortBy);
                        if(str!=null){
                            allMoviewJsonArray.put(new JSONObject(str));
                        }
                    }
                    return getMoviesDataFromJson(allMoviewJsonArray);
                }catch (JSONException e){
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }
            }

        }
        String str = getMovyDataAsStr(sortBy);
        if(str!=null){
            try {
                return getMoviesDataFromJson(new JSONObject(str)
                        .getJSONArray(X_Constants.RESULTS_PARAM_NAME));
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
        }

        // This will only happen if there was an error getting or parsing the forecast.
        return null;
    }

    private String getMovyDataAsStr(String relativePath){

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String movyJsonString = null;

        // Construct the URL for the themoviedb.org query
        try {
            String apiUrlUsed = X_Constants.API_BASE_URL+relativePath+"?";
            Uri builtUri = Uri.parse(apiUrlUsed).buildUpon()
                    .appendQueryParameter(X_Constants.APP_KEY_PARAM_NAME, X_Constants.API_KEY)
                    .build();
            URL url = new URL(builtUri.toString());

            // Create the request to TheMovieDb, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
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
                return null;
            }
            return buffer.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the moviedb data, there's no point in attempting
            // to parse it.
            return null;
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
    }

    private String[] getMoviesDataFromJson(JSONArray movyJsonArray) throws JSONException{
        String[] movyStrArray = new String[movyJsonArray.length()];
        for (int i=0;i<movyJsonArray.length();i++){
            movyStrArray[i] = movyJsonArray.getJSONObject(i).toString();
        }
        return movyStrArray;
    }

    @Override
    protected void onPostExecute(String[] movyStrArray) {
        //super.onPostExecute(strings);
        if (movyStrArray != null) {
            String[] imgFullPathStrArray = new String[movyStrArray.length];
            for(int i = 0; i<movyStrArray.length; i++){
                try {
                    imgFullPathStrArray[i]=Uri.parse(X_Constants.IMG_BASE_URL).buildUpon()
                            .appendEncodedPath(new JSONObject(movyStrArray[i])
                                    .getString(X_Constants.IMG_PATH_PARAM_NAME))
                            .build().toString();
                }  catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }
            }
            mainActivityFragment.getMovyArrayAdapter().clear();
            mainActivityFragment.getMovyArrayAdapter().addAll(imgFullPathStrArray);
            A_MainActivityFragment.movyArrayList= new ArrayList<String>(Arrays.asList(movyStrArray));
        }
        // New data is back from the server.
    }

}