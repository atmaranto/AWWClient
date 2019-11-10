package edu.utdallas.tonym.awwclient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private static final String sharedPreferencesString = "awwData";
    private static final String desiredTempKey = "desiredTemp";
    private static final String fOrCKey = "fahrenheitOrCelsius";

    private float DEFAULT_TEMP;
    private long TEXT_FADE_DURATION;
    private long BAR_SHIFT_DURATION;

    private EditText desiredTemp;
    private EditText occupancy;
    private Button submit;
    private Switch fOrC;

    private ProgressBar bar;
    private TextView barText;
    private TextView recommendText;

    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;

    private ColorStateList defaultColor;

    private Logger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DEFAULT_TEMP = getResources().getInteger(R.integer.default_room_temperature);
        TEXT_FADE_DURATION = getResources().getInteger(R.integer.text_fade_duration);
        BAR_SHIFT_DURATION = getResources().getInteger(R.integer.bar_shift_duration);

        logger = Logger.getLogger("AWW");

        preferences = this.getSharedPreferences(sharedPreferencesString, Context.MODE_PRIVATE);
        preferencesEditor = preferences.edit();

        bar = findViewById(R.id.progress_temperature);
        barText = findViewById(R.id.progress_text);
        barText.setText(String.format("%.2f", preferences.getFloat(desiredTempKey, DEFAULT_TEMP)));
        recommendText = findViewById(R.id.recommend_text);

        defaultColor = recommendText.getTextColors();

        desiredTemp = findViewById(R.id.desired_temperature);
        occupancy = findViewById(R.id.occupancy);

        desiredTemp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    preferencesEditor.putFloat(desiredTempKey, Float.parseFloat(s.toString()));
                    preferencesEditor.apply();
                } catch(Exception e) {

                }
            }
        });

        desiredTemp.setText(String.format("%.2f", preferences.getFloat(desiredTempKey, DEFAULT_TEMP)));

        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        submit = findViewById(R.id.submit);

        submit.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if(!isValidFloat(desiredTemp.getText().toString())) {
                    fadeInText();
                    setStatus(R.string.status_temperature_invalid, true);
                    return;
                }

                setStatus(R.string.status_network);

                ResponseHandler handler = new ResponseHandler() {
                    @Override
                    public void onComplete() {
                        submit.setEnabled(true);
                        fadeInText();
                    }

                    @Override
                    public void onSuccess() {
                         barText.setText(String.format("%.2f", forceLocal(floatResponse)));
                         recommendText.setText(R.string.recommend_text);
                         animateTemperature(forceF(floatResponse));
                    }

                    @Override
                    public void onError() {
                        setStatus(R.string.status_network_failed, true);
                        animateTemperature(forceF((int)preferences.getFloat(desiredTempKey, DEFAULT_TEMP)));
                        barText.setText(String.format("%.2f", preferences.getFloat(desiredTempKey, DEFAULT_TEMP)));
                    }
                };

                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    String[] permissionsToRequestEvenThoughTheresOnlyOne = {Manifest.permission.ACCESS_FINE_LOCATION};
                    requestPermissions(permissionsToRequestEvenThoughTheresOnlyOne, 0);
                    fadeInText();
                    setStatus(R.string.status_needs_location, true);
                    return;
                }
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                submit.setEnabled(false);
                API.checkTemperature(handler, forceF(preferences.getFloat(desiredTempKey, DEFAULT_TEMP)), (float)location.getLatitude(), (float)location.getLongitude());
            }
        });

        fOrC = findViewById(R.id.fOrC);
        fOrC.setChecked(preferences.getBoolean(fOrCKey, false));

        fOrC.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                preferencesEditor.putBoolean(fOrCKey, checked);
                preferencesEditor.apply();
                calculateTemperatureText(checked);
            }
        });
    }

    private boolean isValidFloat(String s) {
        try {
            Float.parseFloat(s);
        }
        catch(NumberFormatException e) {
            return false;
        }

        return true;
    }

    private float fToC(float f) {
        return (f - 32.0f) * 5.0f / 9.0f;
    }

    private float cToF(float c) {
        return c * 9.0f / 5.0f + 32.0f;
    }

    private float forceF(float value) {
        //Forces the value to fahrenheit
        if(fOrC.isChecked()) {
            return cToF(value);
        }

        return value;
    }

    private float forceLocal(float value) {
        if(fOrC.isChecked()) {
            return fToC(value);
        }

        return value;
    }

    private float convert(float value, boolean checked) {
        if(checked) {
            return fToC(value);
        }
        else {
            return cToF(value);
        }
    }

    private void calculateTemperatureText(boolean checked) {
        //Calculates/recalculates temperature text based on the selected unit for temperature
        float newTemp = convert(preferences.getFloat(desiredTempKey, convert(DEFAULT_TEMP, checked)), checked);
        desiredTemp.setText(String.format("%.2f", newTemp));
        preferencesEditor.putFloat(desiredTempKey, newTemp);

        if(barText.getText().length() > 0) {
            barText.setText(String.format("%.2f", convert(Float.parseFloat((String)barText.getText()), checked)));
        }
    }

    private void fadeIn(View view, long duration) {
        view.animate()
                .alpha(1.0f)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }
                });
    }

    private void fadeInText() {
        fadeIn(recommendText, TEXT_FADE_DURATION);
    }

    private void animateTemperature(float to) {
        TemperatureAnimation animation = new TemperatureAnimation(bar, (float)bar.getProgress(), to);
        animation.setDuration(BAR_SHIFT_DURATION);
        bar.startAnimation(animation);
    }

    private void setStatus(int resid) {
        setStatus(resid, false);
    }

    private void setStatus(int resid, boolean error) {
        recommendText.setText(resid);

        if(error) {
            recommendText.setTextColor(getResources().getColor(R.color.colorError));
        }
        else {
            recommendText.setTextColor(defaultColor);
        }
    }
}