package com.example.root.kickflip;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.root.kickflip.sdk.Kickflip;
import com.example.root.kickflip.sdk.Util;
import com.example.root.kickflip.sdk.av.RecordListener;
import com.example.root.kickflip.sdk.av.SessionConfig;
import com.example.root.kickflip.sdk.exception.KickflipException;


public class MainActivity extends ActionBarActivity {

    private ImageButton imageRecord;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageRecord = (ImageButton) findViewById(R.id.imageButtonRecord);

        SessionConfig config = Util.create720pSessionConfig(Util.PATH_APP);

        Kickflip.setSessionConfig(config);
        Kickflip.startRecordActivity(this, new RecordListener() {
            @Override
            public void onRecordStart() {
                Toast.makeText(getApplicationContext(), "onRecordStart", Toast.LENGTH_LONG).show();
                Log.d("MainActivity", "onRecordStart");
            }

            @Override
            public void onRecordStop() {
                Toast.makeText(getApplicationContext(), "onRecordStop", Toast.LENGTH_LONG).show();
                Log.d("MainActivity", "onRecordStop");
            }

            @Override
            public void onRecordError(KickflipException error) {
                Toast.makeText(getApplicationContext(), "onRecordError", Toast.LENGTH_LONG).show();
                Log.d("MainActivity", "onRecordError");
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
