package com.unifurniture.mobile.util;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundManager {

    public static final int SOUND_SUCCESS = 1;
    public static final int SOUND_ERROR = 2;

    public static void playSound(Context context, int soundType) {
        try {
            int resourceId = 0;
            switch (soundType) {
                case SOUND_SUCCESS:
                    resourceId = context.getResources().getIdentifier("success", "raw", context.getPackageName());
                    break;
                case SOUND_ERROR:
                    resourceId = context.getResources().getIdentifier("error", "raw", context.getPackageName());
                    break;
            }

            if (resourceId != 0) {
                MediaPlayer mediaPlayer = MediaPlayer.create(context, resourceId);
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
