package com.example.nikitarykov.justplayer;

import java.util.concurrent.TimeUnit;

/**
 * Created by Nikita Rykov on 20.12.2017.
 */

public class ProgressUtils {

    public static String milliSecondsToDuration(long milliseconds) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds)));
    }

    public static int getProgressPercentage(long currentDuration, long totalDuration) {

        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);

        Double percentage = percentage =(((double)currentSeconds)/totalSeconds)*100;
        return percentage.intValue();
    }

    public static int progressToDuration(int progress, int totalDuration) {
        int currentDuration = 0;
        totalDuration = totalDuration / 1000;
        currentDuration = (int) ((((double)progress) / 100) * totalDuration);
        return currentDuration * 1000;
    }
}
