package edu.utdallas.tonym.awwclient;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

//Created thanks to a helpful answer here https://stackoverflow.com/questions/8035682/animate-progressbar-update-in-android

public class TemperatureAnimation extends Animation {
    private ProgressBar bar;
    private float start;
    private float stop;

    public TemperatureAnimation(ProgressBar bar, float start, float stop) {
        super();
        this.bar = bar;
        this.start = start;
        this.stop = stop;
    }

    @Override
    protected void applyTransformation(float time, Transformation transformation) {
        super.applyTransformation(time, transformation);

        bar.setProgress((int)(start + (stop - start) * time));
    }
}
