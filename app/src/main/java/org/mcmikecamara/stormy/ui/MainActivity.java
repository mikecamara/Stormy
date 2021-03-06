package org.mcmikecamara.stormy.ui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mcmikecamara.stormy.R;
import org.mcmikecamara.stormy.weather.Current;
import org.mcmikecamara.stormy.weather.Day;
import org.mcmikecamara.stormy.weather.Forecast;
import org.mcmikecamara.stormy.weather.Hour;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String DAILY_FORECAST = "DAILY_FORECAST";
    public static final String HOURLY_FORECAST = "HOURLY_FORECAST";
    /*
         * Define a request code to send to Google Play services
         * This code is returned in Activity.onActivityResult
         */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Forecast mForecast;


    @InjectView(R.id.timeLabel)
    TextView mTimeLabel;
    @InjectView(R.id.temperatureLabel)
    TextView mTemperatureLabel;
    @InjectView(R.id.humidityValue)
    TextView mDayTempMax;
    @InjectView(R.id.precipValue)
    TextView mDayTemperatureMin;
    @InjectView(R.id.summaryLabel)
    TextView mSummaryLabel;
    @InjectView(R.id.iconImageView)
    ImageView mIconImageView;
    @InjectView(R.id.refreshImageView)
    ImageView mRefreshImageView;
    @InjectView(R.id.progressBar)
    ProgressBar mProgressBar;
    @InjectView(R.id.locationLabel)
    TextView mTimezone;
    @InjectView(R.id.ChanceOfRainValue)
    TextView mChanceOfRain;
    @InjectView(R.id.windSpeedValue)
    TextView mWindSpeed;
    @InjectView(R.id.moonPhaseValue)
    TextView mMoonPhase;
    @InjectView(R.id.sunriseTimeValue)
    TextView mSunriseTime;
    @InjectView(R.id.sunsetTimeValue)
    TextView mSunsetTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        ButterKnife.inject(this);

        mProgressBar.setVisibility(View.INVISIBLE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }


    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        final double currentLatitude = location.getLatitude();
        final double currentLongitude = location.getLongitude();

        getForecast(currentLatitude, currentLongitude);

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(currentLatitude, currentLongitude);

            }
        });

    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
    }

    private void getForecast(double currentLatitude, double currentLongitude) {

        //Mike's apiKey for forecast.io website
        String apiKey = "d27540fae83b3983cab20efd226287e5";

        String forecastUrl = "https://api.forecast.io/forecast/"
                + apiKey + "/" + currentLatitude + ","
                + currentLongitude;


        if (isNetworkAvailable()) {
            toggleRefresh();

            //I would have to test the connection network before the next line
            OkHttpClient client = new OkHttpClient();

            //1 - create a request
            Request request = new Request.Builder().url(forecastUrl).build();

            //2 - create a call
            Call call = client.newCall(request);

            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });


                    try {

                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);

                        //4 - get a string from the response
                        if (response.isSuccessful()) {

                            mForecast = parseForecastDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();

                                }
                            });

                        } else {
                            alertUserAboutError();
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);

                    }

                }


            });
        } else {
            Toast.makeText(this, getString(R.string.unavailable_network_message), Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {

        Current current = mForecast.getCurrent();


        mTimezone.setText(current.getTimezone() + "");
        mTemperatureLabel.setText(current.getTemperature() + "°");
        mTimeLabel.setText("Now - " + current.getFormattedTime());
        mDayTempMax.setText(this.mForecast.getDailyForecast()[0].getTemperatureMax() + "°");


        RelativeLayout layout = (RelativeLayout) findViewById(R.id.relativeLayout);

        // Mike's Cunneen piece of code
        // this will check if current hour is > 18pm and < 6am, so it's night
        if (current.isNight()) {
            layout.setBackground(getDrawable(R.drawable.bg_gradient_night));

        } else {
            layout.setBackground(getDrawable(R.drawable.bg_gradient_day));
        }

        //check which phase of the moon it is
        //I will to implement the other 3 conditions
        if (this.mForecast.getDailyForecast()[0].getMoon() > 0) {
            mMoonPhase.setText("New moon");
        } else {
        }

        mDayTemperatureMin.setText(this.mForecast.getDailyForecast()[0].getTemperatureMin() + "°");
        mChanceOfRain.setText(this.mForecast.getDailyForecast()[0].getChanceRain() + "%");
        mWindSpeed.setText(current.getWind() + "");
        mSunriseTime.setText(this.mForecast.getDailyForecast()[0].getFormattedSunriseTime() + "");
        mSunsetTime.setText(this.mForecast.getDailyForecast()[0].getFormattedSunsetTime() + "");
        mSummaryLabel.setText(current.getSummary());
        mTimezone.setText(current.getTimezone());

        Drawable drawable = getResources().getDrawable(current.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private Forecast parseForecastDetails(String jsonData) throws JSONException {
        Forecast forecast = new Forecast();
        forecast.setCurrent(getCurrentDetails(jsonData));
        forecast.setHourlyForecast(getHourlyForecast(jsonData));
        forecast.setDailyForecast(getDailyForecast(jsonData));
        return forecast;
    }

    public Day[] getDailyForecast(String jsonData) throws JSONException {


        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray data = daily.getJSONArray("data");

        Day[] days = new Day[data.length()];


        for (int i = 0; i < data.length(); i++) {
            JSONObject jsonDay = data.getJSONObject(i);
            Day day = new Day();
            if (jsonDay != null) {
                day.setSummary(jsonDay.getString("summary"));
                day.setIcon(jsonDay.getString("icon"));
                day.setTemperatureMax(jsonDay.getDouble("temperatureMax"));
                day.setTime(jsonDay.getLong("time"));
                day.setChanceRain(jsonDay.getDouble("precipProbability"));
                day.setTimezone(timezone);
                day.setSunriseTime(jsonDay.getLong("sunriseTime"));
                day.setTemperatureMin(jsonDay.getDouble("temperatureMin"));
                day.setSunsetTime(jsonDay.getLong("sunsetTime"));
                day.setMoon(jsonDay.getDouble("moonPhase"));

                days[i] = day;

            }
        }
        return days;
    }


    private Hour[] getHourlyForecast(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        JSONObject hourly = forecast.getJSONObject("hourly");
        JSONArray data = hourly.getJSONArray("data");

        Hour[] hours = new Hour[data.length()];

        for (int i = 0; i < data.length(); i++) {
            JSONObject jsonHour = data.getJSONObject(i);
            Hour hour = new Hour();
            hour.setSummary(jsonHour.getString("summary"));
            hour.setIcon(jsonHour.getString("icon"));
            hour.setTemperature(jsonHour.getDouble("temperature"));
            hour.setTime(jsonHour.getLong("time"));
            hour.setTimezone(timezone);
            hours[i] = hour;
        }
        return hours;
    }


    private Current getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        Log.i(TAG, "From JSON: " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");

        Current current = new Current();
        current.setHumidity(currently.getDouble("humidity"));
        current.setTime(currently.getLong("time"));
        current.setIcon(currently.getString("icon"));
        current.setPrecipChance(currently.getDouble("precipProbability"));
        current.setSummary(currently.getString("summary"));
        current.setTemperature(currently.getDouble("temperature"));
        current.setTimezone(timezone);
        current.setWind(currently.getDouble("windSpeed"));

        return current;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;
    }

    private void alertUserAboutError() {

        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    //click on this button uses a Intent to open Daily forecast Activity
    @OnClick(R.id.dailyButton)
    public void startDailyActivity(View view) {
        Intent intent = new Intent(this, DailyForecastActivity.class);
        intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
        startActivity(intent);
    }

    //click on this button uses a Intent to open Hourly forecast Activity
    @OnClick(R.id.hourlyButton)
    public void startHourlyActivity(View view) {
        Intent intent = new Intent(this, HourlyForecastActivity.class);
        intent.putExtra(HOURLY_FORECAST, mForecast.getHourlyForecast());
        startActivity(intent);
    }
}