package com.example.tsunamiudacity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String USGS_REQUEST_URL =
            "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2012-01-01&endtime=2012-12-01&minmagnitude=6";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TsunamiAsyncTask tsunamiAsyncTask = new TsunamiAsyncTask();
        tsunamiAsyncTask.execute();

    }

    public class TsunamiAsyncTask extends AsyncTask<URL, Void, Event> {

        @Override
        protected Event doInBackground(URL... urls) {
            URL url = createUrl(USGS_REQUEST_URL);
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (Exception e) {

            }

            Event earthquake = extractFeatureFromJson(jsonResponse);
            return earthquake;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onPostExecute(Event earthquake) {
            if (earthquake == null) {
                return;
            }
            updateUi(earthquake);
        }

        private String makeHttpRequest(URL url) {

            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;

            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.connect();
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);

            } catch (Exception e) {

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {

                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return jsonResponse;

        }

        private String readFromStream(InputStream inputStream) {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                try {
                    String line = reader.readLine();
                    while (line != null) {
                        output.append(line);
                        line = reader.readLine();
                    }

                } catch (Exception e) {

                }
            }
            return output.toString();
        }

        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error with creating URL", e);
                return null;
            }
            return url;
        }
    }

    private Event extractFeatureFromJson(String earthquakeJSON) {
        try {
            JSONObject baseJSONObject = new JSONObject(earthquakeJSON);
            JSONArray featureArray = baseJSONObject.getJSONArray("features");
            if (featureArray.length() > 0) {
                JSONObject firstFeature = featureArray.getJSONObject(0);
                JSONObject properties = firstFeature.getJSONObject("properties");
                String title = properties.getString("title");
                long time = properties.getLong("time");
                int tsunamiAlert = properties.getInt("tsunami");
                return new Event(title, time, tsunamiAlert);

            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateUi(Event earthquake) {
        // Display the earthquake title in the UI
        TextView titleTextView = (TextView) findViewById(R.id.title);
        titleTextView.setText(earthquake.title);

        // Display the earthquake date in the UI
        TextView dateTextView = (TextView) findViewById(R.id.date);
        dateTextView.setText(getDateString(earthquake.time));

        // Display whether or not there was a tsunami alert in the UI
        TextView tsunamiTextView = (TextView) findViewById(R.id.tsunami_alert);
        tsunamiTextView.setText(getTsunamiAlertString(earthquake.tsunamiAlert));
    }

    private String getTsunamiAlertString(int tsunamiAlert) {
        switch (tsunamiAlert) {
            case 0:
                return getString(R.string.alert_no);
            case 1:
                return getString(R.string.alert_yes);
            default:
                return getString(R.string.alert_not_available);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String getDateString(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z");
        return formatter.format(time);
    }
}