package uclan.ac.uk.weatherapp;

import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Locale;

import uclan.ac.uk.weatherapp.db.DatabaseOpenHelper;

public class AddCitiesFragment extends Fragment {

    private View myView;
    private EditText titleEditText;
    private Button addButton;
    private String myCity;
    private Handler handler;

    public AddCitiesFragment() {
        handler = new Handler();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.add_cities, container, false);
        titleEditText = (EditText) myView.findViewById(R.id.editTextTitle);
        addButton = (Button) myView.findViewById(R.id.addButton);

        addButton.setOnClickListener(v -> updateWeatherData());
        return myView;
    }

    private void updateWeatherData() {
        new Thread() {
            public void run() {
                myCity = titleEditText.getText().toString().trim();
                Log.d("co2509", "myCity: " + myCity);
                final JSONObject json = WeatherFetch.getJSON(getActivity(), myCity);
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
            String query = "Select * FROM entries WHERE LOWER(title)= '" + myCity.toLowerCase() + "'";

            DatabaseOpenHelper doh = new DatabaseOpenHelper(this.getActivity());
            SQLiteDatabase db = doh.getWritableDatabase();

            Cursor cursor = db.rawQuery(query, null);

            String opCityName = json.getString("name").toUpperCase(Locale.US);
            String inCityName = myCity.toUpperCase(Locale.US).trim();

            Log.d("co2509", opCityName + " " + inCityName);

            if (!cursor.moveToFirst() && opCityName.equals(inCityName)) {
                // store in DB
                ContentValues contentValues = new ContentValues(); // a map data structure
                contentValues.put("title", json.getString("name").toUpperCase(Locale.US));
                db.insert("entries", null, contentValues);

                Toast.makeText(this.getActivity(), myCity.toUpperCase(Locale.US) + " has been added to your Favourite Cities!",
                        Toast.LENGTH_SHORT).show();

                titleEditText.setText("");

                // hide keyboard
                // Check if no view has focus:
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

            } else if (cursor.moveToFirst()) {
                Toast.makeText(this.getActivity(), titleEditText.getText() + " already exists in your cities!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(),
                        getActivity().getString(R.string.place_not_found),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Weather", "One or more fields not found in the JSON data");
        }
    }
}
