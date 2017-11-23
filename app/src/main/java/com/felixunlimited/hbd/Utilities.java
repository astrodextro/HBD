package com.felixunlimited.hbd;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created on 01/11/2017.by Ando
 */

class Utilities {

    private static long getPrimaryCalendarId(Context context) {
        long calId = 1;
        String selection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            selection = CalendarContract.Calendars.VISIBLE + " = 1 AND "
                    + CalendarContract.Calendars.IS_PRIMARY + " = 1";
        } else {
            selection = CalendarContract.Calendars.VISIBLE + " = 1";
        }

        Uri calendarUri = CalendarContract.Calendars.CONTENT_URI;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return calId;
        }
        Cursor cur = context.getContentResolver().query(calendarUri, null, selection, null, null);

        if (cur != null && cur.moveToFirst()) {
            calId = cur.getLong(Integer.parseInt(CalendarContract.Calendars._ID));
            cur.close();
        }

        return calId;
    }
//    static void updateCalendar(Context context, Calendar calendar, String name) {
//        String where =  CalendarContract.Events.CALENDAR_ID+" = 3 AND  "+
//                CalendarContract.Events.TITLE+" = "+name;
//
//        Uri uri = context.getContentResolver().query(CalendarContract.Events.CONTENT_URI,null, where, null, null );
//    }

    static void addToCalendar(Context context, Calendar calendar, String name) {

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, calendar.getTimeInMillis());

        Cursor cursor = getCalendarInstanceCursor(context, name);

        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() > 0) {
                // deal with conflict
                String where = CalendarContract.Events.TITLE+" = ?";
                String[] selectionArgs = {name+"'s Birthday"};
                long rows = context.getContentResolver().update(CalendarContract.Events.CONTENT_URI, values, where, selectionArgs);
            }
            else {
                values.put(CalendarContract.Events.CALENDAR_ID, getPrimaryCalendarId(context));
                values.put(CalendarContract.Events.TITLE, name+"'s Birthday");
                values.put(CalendarContract.Events.STATUS, 1);
                values.put(CalendarContract.Events.DESCRIPTION, "Today is the birthday of " + name);
                values.put(CalendarContract.Events.DURATION, "+P1D");
                values.put(CalendarContract.Events.ALL_DAY, 1);
                values.put(CalendarContract.Events.RRULE, "FREQ=YEARLY");
                values.put(CalendarContract.Events.HAS_ALARM, 1);
                values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
                values.put(CalendarContract.Events.EVENT_COLOR, Color.YELLOW);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Uri uri = context.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values);
                // get the event ID that is the last element in the Uri
                long eventID;
                if (uri != null) {
                    eventID = Long.parseLong(uri.getLastPathSegment());

                    // add 10 minute reminder for the event
                    ContentValues reminders = new ContentValues();
                    reminders.put(CalendarContract.Reminders.EVENT_ID, eventID);
                    reminders.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                    reminders.put(CalendarContract.Reminders.MINUTES, 3*24*60);

                    Uri uriRem =  context.getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, reminders);
                    if (uriRem == null) {
                        reminders.remove(CalendarContract.Reminders.EVENT_ID);
                        context.getContentResolver().update(CalendarContract.Reminders.CONTENT_URI, reminders, CalendarContract.Reminders.EVENT_ID+" = "+eventID, null);
                    }
                }
            }
        }
    }

    static Cursor getCalendarInstanceCursor(Context context, String name) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        return context.getContentResolver().query(CalendarContract.Events.CONTENT_URI, null, CalendarContract.Events.TITLE+" = ?", new String[] {name+"'s Birthday"}, null);
//        long begin = calendar.getTimeInMillis(); // starting time in milliseconds
//        long end = calendar.getTimeInMillis()+1000*86400; // ending time in milliseconds
//        String[] proj = new String[]{
//                    CalendarContract.Instances._ID,
//                    CalendarContract.Instances.BEGIN,
//                    CalendarContract.Instances.END,
//                    CalendarContract.Instances.EVENT_ID
//        };
//
//
//        return CalendarContract.Instances.query(context.getContentResolver(), proj, begin, end, name+"'s Birthday");
    }

    private static long getRawContactId(Context context, String contactId) {
        long rawContactId = -1;
        Cursor c = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.CONTACT_ID + "=?",
                new String[]{String.valueOf(contactId)}, null);
        if (c != null && c.moveToFirst()) {
            rawContactId = c.getLong(0);
            c.close();
        }
        return rawContactId;
    }

    static void updateContactBirthday(Context context, String contactId, String name, int month, int day) {
        Uri uri = ContactsContract.Data.CONTENT_URI;

        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactsContract.CommonDataKinds.Event.START_DATE, "--"+month+"-"+day);

        String where =
                ContactsContract.Data.CONTACT_ID+" = ? AND "+
                        ContactsContract.CommonDataKinds.Event.RAW_CONTACT_ID+" = ? AND "+
                        ContactsContract.Data.MIMETYPE + "= ? AND " +
                        ContactsContract.CommonDataKinds.Event.TYPE + "=" +
                        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY;

        String[] selectionArgs = new String[]{
                contactId,
                String.valueOf(getRawContactId(context, contactId)),
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
        };

        long id = context.getContentResolver().update(uri,contentValues, where, selectionArgs);
        if (id != -1)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, month-1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            addToCalendar(context, calendar, name);
        }
    }

    static void insertContactBirthday (Context context, String contactId, String name, int month, int day) {
        Uri uri = ContactsContract.Data.CONTENT_URI;

        ContentValues contentValues = new ContentValues();
        contentValues.put(ContactsContract.CommonDataKinds.Event.RAW_CONTACT_ID, getRawContactId(context, contactId));
        contentValues.put(ContactsContract.CommonDataKinds.Event.START_DATE, "--"+month+"-"+day);
        contentValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
        contentValues.put(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);

        Uri uri1 = context.getContentResolver().insert(uri,contentValues);
        long id = -1;
        if (uri1 != null) {
            id = Long.parseLong(uri1.getLastPathSegment());
        }
        if (id != -1)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, month-1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            addToCalendar(context, calendar, name);
        }
    }

    // method to get name, contact id, and birthday
    private static Cursor getContactBirthdayCursor(Context context, String contactID) {
        Uri uri = ContactsContract.Data.CONTENT_URI;

        String[] projection = new String[] {
                ContactsContract.CommonDataKinds.Event.START_DATE,
                ContactsContract.CommonDataKinds.Event.RAW_CONTACT_ID
        };

        String where =
                ContactsContract.Data.CONTACT_ID+" = ? AND "+
                        ContactsContract.Data.MIMETYPE + "= ? AND " +
                        ContactsContract.CommonDataKinds.Event.TYPE + "=" +
                        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY;
        String[] selectionArgs = new String[] {
                contactID,
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
        };

        return context.getContentResolver().query(uri, projection, where, selectionArgs, null);
    }

    static Calendar getContactsBirthdays(Context context, String contactID) {
        Calendar calendar = null;
        Cursor birthdayCursor = getContactBirthdayCursor(context, contactID);
        if (birthdayCursor != null) {
            if (birthdayCursor.getCount() > 0 && birthdayCursor.moveToNext()) {
                calendar = Calendar.getInstance();
                String birthday = birthdayCursor.getString(0);
                String[] bdSplit = birthday.split("-");
                int month = Integer.parseInt(((bdSplit.length == 3) ? bdSplit[1] : bdSplit[2]));
                int day = Integer.parseInt(((bdSplit.length == 3) ? bdSplit[2] : bdSplit[3]));
                calendar.set(Calendar.MONTH, month-1);
                calendar.set(Calendar.DAY_OF_MONTH, day);
            }
            birthdayCursor.close();
        }
        return calendar;
    }

    static Cursor allContactsCursor(Context context, String searchText) {
        Cursor cur;
        ContentResolver cr = context.getContentResolver();
        Uri uri = ContactsContract.Contacts.CONTENT_URI;

        if (!searchText.isEmpty()) {
            String[] mProjection = {ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME};

            String selection = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = new String[]{"%"+searchText+"%"};
            cur = cr.query(uri, mProjection, selection, selectionArgs, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");
//            cur =  cr.query(Uri.withAppendedPath(uri, searchText), null,null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");
        }
        else
            cur =  cr.query(uri, null,null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");

        return cur;
    }

    static void addAllBirthdaysToCalendar(Context context) {
        Cursor cursor = allContactsCursor(context, "");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String contact_id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts. _ID ));
                String name = cursor.getString(cursor.getColumnIndex( ContactsContract.Contacts.DISPLAY_NAME ));

                Calendar calendar = getContactsBirthdays(context, contact_id);
                if (calendar != null) {
                    Cursor calendarEventCursor = getCalendarInstanceCursor(context, name);
                    if (calendarEventCursor != null) {
                        if (calendarEventCursor.getCount() == 0)
                            addToCalendar(context, calendar, name);
                        calendarEventCursor.close();
                    }
                }
            }
            cursor.close();
        }
    }

//    pDialog = new ProgressDialog(this);
//        pDialog.setMessage("Reading contacts...");
//        pDialog.setCancelable(false);
//        pDialog.show();
//    mListView = (ListView) findViewById(R.id.list);
//    updateBarHandler =new Handler();
//
//    // Set onclicklistener to the list item.
//        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view,
//        int position, long id) {
//            //TODO Do whatever you want with the list data
//            Toast.makeText(getApplicationContext(), "item clicked : \n"+contactList.get(position), Toast.LENGTH_SHORT).show();
//        }
//    });
//    public void getContacts() {
//        contactList = new ArrayList<String>();
//        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
//        String _ID = ContactsContract.Contacts._ID;
//        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
//        StringBuffer output;
//        ContentResolver contentResolver = getContentResolver();
//        cursor = contentResolver.query(CONTENT_URI, null,null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");
//        // Iterate every contact in the phone
//        if (cursor.getCount() > 0) {
//            counter = 0;
//            while (cursor.moveToNext()) {
//                output = new StringBuffer();
//                // Update the progress message
//                updateBarHandler.post(new Runnable() {
//                    public void run() {
//                        pDialog.setMessage("Reading contacts : "+ counter++ +"/"+cursor.getCount());
//                    }
//                });
//                String contact_id = cursor.getString(cursor.getColumnIndex( _ID ));
//                String name = cursor.getString(cursor.getColumnIndex( DISPLAY_NAME ));
////                String phoneNumber = null;
////                String email = null;
////                String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
////                Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
////                String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
////                String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
////                Uri EmailCONTENT_URI =  ContactsContract.CommonDataKinds.Email.CONTENT_URI;
////                String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
////                String DATA = ContactsContract.CommonDataKinds.Email.DATA;
//////                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex( HAS_PHONE_NUMBER )));
//////                if (hasPhoneNumber > 0) {
//////                    output.append("First Name:").append(name);
//////                    //This is to read multiple phone numbers associated with the same contact
//////                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[] { contact_id }, null);
//////                    while (phoneCursor.moveToNext()) {
//////                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
//////                        output.append("\n Phone number:").append(phoneNumber);
//////                    }
//////                    phoneCursor.close();
//////                    // Read every email id associated with the contact
//////                    Cursor emailCursor = contentResolver.query(EmailCONTENT_URI,    null, EmailCONTACT_ID+ " = ?", new String[] { contact_id }, null);
//////                    while (emailCursor.moveToNext()) {
//////                        email = emailCursor.getString(emailCursor.getColumnIndex(DATA));
//////                        output.append("\n Email:").append(email);
//////                    }
//////                    emailCursor.close();
//////                }
////
////                // iterate through all Contact's Birthdays and print in log
//                Cursor birthdayCursor = getContactsBirthdays(contact_id);
//                int bDayColumn = birthdayCursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE);
//                while (birthdayCursor.moveToNext()) {
//                    String bDay = birthdayCursor.getString(bDayColumn);
//                    Calendar calendar = Calendar.getInstance();
//                    calendar.set(Calendar.MONTH, Integer.parseInt(bDay.split("-")[1]));
//                    String month = Calendar.getInstance().getDisplayName(Calendar.MONTH).;
//                    output.append("First Name:").append(name);
//                    output.append("\n Birthday:").append(bDay);
//                    String[] split = bDay.split("-");
//                    for (String str :
//                            split) {
//                        output.append("\n").append(str);
//                    }
//                }
//                birthdayCursor.close();
//
//                // Add the contact to the ArrayList
//                contactList.add(output.toString());
//            }
//
//            // ListView has to be updated using a ui thread
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.contact_item, R.id.contact_name, contactList);
//                    mListView.setAdapter(adapter);
//                }
//            });
//
//            // Dismiss the progressbar after 500 millisecondds
//            updateBarHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    pDialog.cancel();
//                }
//            }, 500);
//        }
//    }
}
