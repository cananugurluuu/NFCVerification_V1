package com.sagittarius.nfc;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ActionBar actionbar;
    private NavigationView navigationView;
    private View parent_view;
    private DrawerLayout drawerLayout;
    private NavigationView navHeaderView;
    private String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.NFC
    };
    private Tag nfcTag;
    private IntentFilter writeTagFilters[];
    private boolean writeMode;
    private NfcAdapter adapter;
    private PendingIntent pendingIntent;
    public static Context context;
    private Button btnWrite;
    private static final Charset US_ASCII=Charset.forName("US-ASCII");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        initToolbar();
        initDrawerMenu();
        setupDrawerLayout();
        displayFragment(R.id.nav_home, getString(R.string.app_name));
        Tools.systemBarLolipop(this);

        adapter = NfcAdapter.getDefaultAdapter(this);
        //If no NfcAdapter, display that the device has no NFC
        if (adapter == null){
            Toast.makeText(this,"No NFC Support", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!adapter.isEnabled()) {
                Toast.makeText(this,"Enable NFC, Please!", Toast.LENGTH_SHORT).show();
            } else {
                pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
                writeTagFilters = new IntentFilter[] { tagDetected };
            }
        }
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeButtonEnabled(true);
    }

    private void initDrawerMenu() {
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            public void onDrawerOpened(View drawerView) {
                //hideKeyboard();
                super.onDrawerOpened(drawerView);
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);
                displayFragment(menuItem.getItemId(), menuItem.getTitle().toString());
                drawer.closeDrawers();
                return true;
            }
        });
    }

    private void setupDrawerLayout() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        FragmentHome fragment = null;
        NavigationView view = (NavigationView) findViewById(R.id.nav_view);
        navigationView.bringToFront();

        view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                if (menuItem.getTitle().equals(getString(R.string.title_nav_logout))) {
                    onBackPressed();
                }
                return true;
            }
        });
    }

    private void displayFragment(int id, String title) {
        actionbar.setDisplayShowCustomEnabled(false);
        actionbar.setDisplayShowTitleEnabled(true);
        actionbar.setTitle(title);
        FragmentHome fragment = null;
        switch (id) {
            case R.id.nav_home:
                fragment = new FragmentHome();
                break;
        }
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame_content, fragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            FragmentHome fg = new FragmentHome();
            nfcTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] tagId = nfcTag.getId();
            String seriNo = "";
            for (int i = 0; i < tagId.length; i++) {
                String a = Integer.toHexString(tagId[i] & 0xFF);
                if (a.length() == 1) {
                    a = '0' + a;
                }
                seriNo += a;
            }
            seriNo = ConvertCardID(seriNo);
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            String tagRecordStr = "";
            if (rawMsgs != null) {
                for (int i = 0; i < rawMsgs.length; i++) {
                    NdefMessage ndefMessage = (NdefMessage) rawMsgs[i];
                    NdefRecord[] records = ndefMessage.getRecords();
                    for (NdefRecord record : records) {
                        tagRecordStr = tagRecordStr + new String(record.getPayload(), US_ASCII);
                    }
                }
            }

            ((TextView) findViewById(R.id.tag_id)).setText(seriNo);
            ((TextView) findViewById(R.id.tag_record)).setText(tagRecordStr);

            ((Button) findViewById(R.id.btnWrite)).setOnClickListener((View.OnClickListener) v -> {
                String msgStr = "";
                msgStr = ((TextView) findViewById(R.id.writenTxt)).getText().toString();
                NdefMessage msg = new NdefMessage(NdefRecord.createApplicationRecord(msgStr));
                if (WriteTag(nfcTag, msg)) {
                    ((TextView) findViewById(R.id.tag_record)).setText(msgStr);
                    Ndef ndef = Ndef.get(nfcTag);
                    if(ndef.canMakeReadOnly()){
                        try {
                            ndef.connect();
                            ndef.makeReadOnly();
                            ndef.close();
                            Toast.makeText(context, "Read Only", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
            });
        }
    }

    public static String ConvertCardID(String cardid) {
        return ("000000000000000" + cardid).substring(cardid.length());
    }

    public boolean WriteTag(Tag tag, NdefMessage message) {
        if (tag != null) {
            try {
                Ndef ndef = Ndef.get(tag);
                if(ndef == null) {
                    NdefFormatable ndefFormatable = NdefFormatable.get(tag);
                    if (ndefFormatable != null) {
                        ndefFormatable.connect();
                        ndefFormatable.format(message);
                        ndefFormatable.close();
                    }
                    return false;
                } else {
                    ndef.connect();
                    if (ndef.isWritable()) {
                        ndef.writeNdefMessage(message);
                        Toast.makeText(context, getText(R.string.successfull).toString(), Toast.LENGTH_SHORT).show();
                        ndef.close();
                        return true;
                    } else {
                        Toast.makeText(context, getText(R.string.notWritable).toString(), Toast.LENGTH_SHORT).show();
                        ndef.close();
                        return false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, getText(R.string.failed).toString(), Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory( Intent.CATEGORY_HOME );
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WriteModeOn();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //WriteModeOff();
    }

    private void WriteModeOn(){
        writeMode = true;
        adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    /*private void WriteModeOff(){
        writeMode = false;
        adapter.disableForegroundDispatch(this);
    }*/
}