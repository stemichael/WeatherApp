package uclan.ac.uk.weatherapp;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import uclan.ac.uk.weatherapp.db.DatabaseOpenHelper;

public class ListCitiesFragment extends Fragment {

    private View myView;
    private ListView listView;
    private TextView noCitiesAddedInTheListText;
    private FloatingActionButton addCityButton;
    private ArrayList<String> list_items = new ArrayList<>();
    private int counter = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.list_cities, container, false);
        listView = (ListView) myView.findViewById(R.id.listView);
        noCitiesAddedInTheListText = (TextView) myView.findViewById(R.id.noCitiesText);
        addCityButton = (FloatingActionButton) myView.findViewById(R.id.fab_add);
        setHasOptionsMenu(true);
        return myView;
    }

    @Override
    public void onResume() {
        super.onResume();

        addCityButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame
                            , new AddCitiesFragment(), "ADD_CITIES")
                    .addToBackStack(null)
                    .commit();
        });

        DatabaseOpenHelper doh = new DatabaseOpenHelper(this.getActivity());
        SQLiteDatabase db = doh.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title FROM entries", null);
        int numOfRows = cursor.getCount();

        final String[] titles = new String[numOfRows];

        if (cursor.moveToFirst()) {
            noCitiesAddedInTheListText.setVisibility(View.INVISIBLE);
            int columnTitleIndex = cursor.getColumnIndex("title");
            for (int i = 0; i < numOfRows; i++) {
                titles[i] = cursor.getString(columnTitleIndex);
                cursor.moveToNext();
            }
            cursor.close();
        } else {
            noCitiesAddedInTheListText.setVisibility(View.VISIBLE);
        }

        Arrays.sort(titles);

        final ArrayAdapter arrayAdapter = new ArrayAdapter<>(this.getActivity(),
                R.layout.list_adapter, titles);
        listView.setAdapter(arrayAdapter);


        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if (!list_items.contains(titles[position])) {
                    list_items.add(titles[position]);
                    counter++;
                } else if (counter != 0) {
                    list_items.remove(titles[position]);
                    counter--;
                }

                mode.setTitle(counter + " items selected");
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                ListCitiesFragment.this.getActivity().getWindow().setStatusBarColor(Color.parseColor("#303F9F"));

                android.view.MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.delete, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete_id:

                        AlertDialog alertDialog = new AlertDialog.Builder(ListCitiesFragment.this.getActivity()).create();
                        alertDialog.setTitle("Delete");
                        alertDialog.setMessage("Are you sure you want to delete the selected items?");
                        alertDialog.setButton(-1, "DELETE", (dialog, which) -> {
                            for (String msg : list_items) {
                                deleteEntry(msg);
                            }

                            // explicitly update UI after modifying the list view
                            arrayAdapter.notifyDataSetChanged();

                            Toast.makeText(ListCitiesFragment.this.getActivity(), "The cities have been removed!",
                                    Toast.LENGTH_SHORT).show();
                            counter = 0;
                            mode.finish();

                            // Refresh Fragment
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            ft.replace(R.id.content_frame
                                    , new ListCitiesFragment())
                                    .commit();
                        });
                        alertDialog.setButton(-2, "CANCEL", (dialog, which) -> {
                        });

                        alertDialog.show();

                        return true;

                    default:
                        return false;
                }

            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                counter = 0;
                list_items.clear();
                mode.finish();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            DatabaseOpenHelper doh1 = new DatabaseOpenHelper(getActivity());
            SQLiteDatabase db1 = doh1.getWritableDatabase();
            db1.delete("current_city", null, null);
            String query = "INSERT INTO current_city (current) VALUES ('" + titles[position] + "')";
            db1.execSQL(query);

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame
                            , new ShowWeatherFragment(), "SHOW_WEATHER")
                    .addToBackStack(null)
                    .commit();
        });
    }

    public void deleteEntry(String title) {
        DatabaseOpenHelper doh = new DatabaseOpenHelper(this.getActivity());
        SQLiteDatabase db = doh.getWritableDatabase();
        db.delete("entries", "title=" + DatabaseUtils.sqlEscapeString(title) + "", null);
    }
}
