package uclan.ac.uk.weatherapp;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import uclan.ac.uk.weatherapp.db.DatabaseOpenHelper;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private View myView;
    private Typeface weatherFont;
    private TextView cityFieldShwd;
    private TextView cityField;
    private TextView updatedFieldShwd;
    private TextView updatedField;
    private TextView detailsField;
    private TextView weatherIcon;
    private TextView noHomeSet;
    private CardView cityWeather;
    private CardView mapWeather;
    private Button wikiButton;
    private GoogleMap mMap;
    private String msgCity;
    private String lng;
    private String lat;

    private Handler handler;

    public HomeFragment() {
        handler = new Handler();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.home_city, container, false);
        cityFieldShwd = (TextView) myView.findViewById(R.id.city_fieldShwd);
        cityField = (TextView) myView.findViewById(R.id.city_field);
        updatedFieldShwd = (TextView) myView.findViewById(R.id.updated_fieldShwd);
        updatedField = (TextView) myView.findViewById(R.id.updated_field);
        detailsField = (TextView) myView.findViewById(R.id.details_field);
        weatherIcon = (TextView) myView.findViewById(R.id.weather_icon);
        noHomeSet = (TextView) myView.findViewById(R.id.noHomeSetText);
        cityWeather = (CardView) myView.findViewById(R.id.card_weather);
        mapWeather = (CardView) myView.findViewById(R.id.card_map);
        wikiButton = (Button) myView.findViewById(R.id.wikiButton);


        msgCity = "";

        DatabaseOpenHelper doh = new DatabaseOpenHelper(this.getActivity());
        SQLiteDatabase db = doh.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT home FROM home_city WHERE home NOT NULL", null);

        if (cursor.moveToFirst()) {
            int columnHomeIndex = cursor.getColumnIndex("home");
            msgCity = cursor.getString(columnHomeIndex);
            cursor.close();

            wikiButton.setText("Wikipedia for " + msgCity);
            updateWeatherData(msgCity);
            weatherIcon.setTypeface(weatherFont);
            noHomeSet.setVisibility(View.INVISIBLE);
        } else {
            cityFieldShwd.setVisibility(View.INVISIBLE);
            cityField.setVisibility(View.INVISIBLE);
            updatedFieldShwd.setVisibility(View.INVISIBLE);
            updatedField.setVisibility(View.INVISIBLE);
            detailsField.setVisibility(View.INVISIBLE);
            weatherIcon.setVisibility(View.INVISIBLE);
            cityWeather.setVisibility(View.INVISIBLE);
            mapWeather.setVisibility(View.INVISIBLE);
            wikiButton.setVisibility(View.INVISIBLE);
        }

        return myView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weatherFont = Typeface.createFromAsset(getActivity().getAssets(), "fonts/weather.ttf");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        try {
            MapFragment fragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWeatherData(final String city) {
        new Thread() {
            public void run() {
                final JSONObject json = WeatherFetch.getJSON(getActivity(), city.trim());
                if (json == null) {
                    handler.post(() -> Toast.makeText(getActivity(),
                            getActivity().getString(R.string.place_not_found),
                            Toast.LENGTH_SHORT).show());
                } else {
                    handler.post(() -> renderWeather(json));
                }
            }
        }.start();
    }

    private void renderWeather(JSONObject json) {
        try {
            lat = json.getJSONObject("coord").getString("lat");
            lng = json.getJSONObject("coord").getString("lon");

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            MapFragment mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            final String cityName = json.getString("name");
            final String countryName = json.getJSONObject("sys").getString("country");
            wikiButton.setOnClickListener(v -> {
                Uri uri = Uri.parse("http://www.google.com/search?&sourceid=navclient&btnI=I&q=" + cityName + " " + countryName + " wikipedia"); // go to wiki page for city
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            });

            cityFieldShwd.setText(json.getString("name").toUpperCase(Locale.US) +
                    ", " +
                    json.getJSONObject("sys").getString("country"));

            cityField.setText(json.getString("name").toUpperCase(Locale.US) +
                    ", " +
                    json.getJSONObject("sys").getString("country"));

            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");
            detailsField.setText(
                    details.getString("description").toUpperCase(Locale.US) +
                            "\n" + "Temp: " + String.format("%.2f", main.getDouble("temp")) + " ℃" +
                            "\n" + "Humidity: " + main.getString("humidity") + "%" +
                            "\n" + "Pressure: " + main.getString("pressure") + " hPa" +
                            "\n" + "Wind: " + json.getJSONObject("wind").getString("speed") + " mps");

            DateFormat df = DateFormat.getDateTimeInstance();
            String updatedOn = df.format(new Date(json.getLong("dt") * 1000));
            updatedFieldShwd.setText("Updated: " + updatedOn);
            updatedField.setText("Updated: " + updatedOn);

            setWeatherIcon(details.getInt("id"),
                    json.getJSONObject("sys").getLong("sunrise") * 1000,
                    json.getJSONObject("sys").getLong("sunset") * 1000);

        } catch (Exception e) {
            Log.e("Weather", "One or more fields not found in the JSON data");
        }
    }

    private void setWeatherIcon(int actualId, long sunrise, long sunset) {
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            long currentTime = new Date().getTime();
            if (currentTime >= sunrise && currentTime < sunset) {
                icon = getActivity().getString(R.string.weather_sunny);
            } else {
                icon = getActivity().getString(R.string.weather_clear_night);
            }
        } else {
            switch (id) {
                case 2:
                    icon = getActivity().getString(R.string.weather_thunder);
                    break;
                case 3:
                    icon = getActivity().getString(R.string.weather_drizzle);
                    break;
                case 7:
                    icon = getActivity().getString(R.string.weather_foggy);
                    break;
                case 8:
                    icon = getActivity().getString(R.string.weather_cloudy);
                    break;
                case 6:
                    icon = getActivity().getString(R.string.weather_snowy);
                    break;
                case 5:
                    icon = getActivity().getString(R.string.weather_rainy);
                    break;
            }
        }
        weatherIcon.setText(icon);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng wCity = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
        mMap.addMarker(new MarkerOptions().position(wCity).title("Marker in " + msgCity));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(wCity));
    }
}
