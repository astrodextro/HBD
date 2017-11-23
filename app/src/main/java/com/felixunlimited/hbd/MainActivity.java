package com.felixunlimited.hbd;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_CONTACTS = 1;
    private static final int MY_PERMISSIONS_REQUEST_REAS_CONTACTS = 2;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_CALENDAR = 3;
    private static final int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 4;
    ContactDisplayAdapter contactDisplayAdapter;
    SearchView searchView;
//    ProgressDialog progressDialog;
//    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
//        progressBar = (ProgressBar) findViewById(R.id.progress);
//        progressBar.setVisibility(View.VISIBLE);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(R.mipmap.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Utilities.addAllBirthdaysToCalendar(this);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {

            //progressDialog = ProgressDialog.show(this, "Please wait", "Fetching contacts");
            ListView mListView = (ListView) findViewById(R.id.list);
            mListView.setTextFilterEnabled(true);
            ListView mBDListView = (ListView) findViewById(R.id.contacts_with_bd);
            contactDisplayAdapter = new ContactDisplayAdapter(this, Utilities.allContactsCursor(this, ""), 0);
            contactDisplayAdapter.setFilterQueryProvider(new FilterQueryProvider() {
                @Override
                public Cursor runQuery(CharSequence constraint) {
                    return Utilities.allContactsCursor(MainActivity.this, constraint.toString());
                }
            });
            mListView.setAdapter(contactDisplayAdapter);
//            contactDisplayAdapter.setFilterQueryProvider(new FilterQueryProvider() {
//                @Override
//                public Cursor runQuery(CharSequence charSequence) {
//                    return getCu(charSequence.toString());
//                }
//            });
//            mBDListView.setAdapter(contactDisplayAdapter);
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_WRITE_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR},
                    MY_PERMISSIONS_REQUEST_WRITE_CALENDAR);
        }
        //CursorJoiner cursorJoiner = new CursorJoiner()
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);
        searchView.setSubmitButtonEnabled(true);
        searchView.setIconified(true);

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_CONTACTS) {
            if (grantResults.length < 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Not granted", Toast.LENGTH_SHORT).show();
                onDestroy();
            }
        }
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_CALENDAR) {
            if (grantResults.length < 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Not granted", Toast.LENGTH_SHORT).show();
                onDestroy();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        contactDisplayAdapter.getFilter().filter(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        contactDisplayAdapter.getFilter().filter(newText);
//        Cursor contacts = Utilities.allContactsCursor(this, newText);
//        ContactDisplayAdapter cursorAdapter = new ContactDisplayAdapter(this, contacts, 0);
//        searchView.setSuggestionsAdapter(cursorAdapter);
        return true;
    }

    private class ContactDisplayAdapter extends CursorAdapter{

        ContactDisplayAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
//            progressDialog.setCancelable(false);
//            progressDialog.dismiss();
        }

        class ViewHolder {
            final TextView nameView;
            final TextView birthdayView;

            ViewHolder(View view) {
                nameView = view.findViewById(R.id.contact_name);
                birthdayView = view.findViewById(R.id.cantact_birthday);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.contact_item, parent, false);

            ViewHolder viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);

            return view;
        }

        @Override
        public void bindView(View view, final Context context, final Cursor cursor) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
//            if (getCount() > 10)
//                progressBar.setVisibility(View.GONE);
//                progressDialog.dismiss();

            final String contact_id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts. _ID ));
            final String name = cursor.getString(cursor.getColumnIndex( ContactsContract.Contacts.DISPLAY_NAME ));
            String birthday = "NONE";

            Calendar calendar = Utilities.getContactsBirthdays(context, contact_id);
            if (calendar != null) {
                birthday = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + " " + calendar.get(Calendar.DAY_OF_MONTH);
                Cursor calendarEventCursor = Utilities.getCalendarInstanceCursor(context, name);
                if (calendarEventCursor != null) {
                    if (calendarEventCursor.getCount() == 0)
                        Utilities.addToCalendar(context, calendar, name);
                    calendarEventCursor.close();
                }
            }
            viewHolder.nameView.setText(name);
            viewHolder.birthdayView.setText(birthday);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Calendar calendar = Utilities.getContactsBirthdays(MainActivity.this, contact_id);
                    final DatePicker datePicker = new DatePicker(MainActivity.this);
                    if (calendar != null) {
                        datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
                    }

                    AlertDialog birthdayDialog = new AlertDialog.Builder(MainActivity.this)
                            .setTitle(name+"'s birthday")
                            .setView(datePicker)
                            .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (calendar == null)
                                        Utilities.insertContactBirthday(context, contact_id, name, datePicker.getMonth()+1, datePicker.getDayOfMonth());
                                    else {
                                        int month = datePicker.getMonth();
                                        int dayOfMonth = datePicker.getDayOfMonth();
                                        int bMonth = calendar.get(Calendar.MONTH);
                                        int bDay = calendar.get(Calendar.DATE);
                                        if ((bMonth != month) || (bDay != dayOfMonth))
                                            Utilities.updateContactBirthday(context, contact_id, name, datePicker.getMonth() + 1, datePicker.getDayOfMonth());
                                    }
                                    ContactDisplayAdapter.this.notifyDataSetChanged();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .create();

                    birthdayDialog.show();
                }
            });
        }
    }
}