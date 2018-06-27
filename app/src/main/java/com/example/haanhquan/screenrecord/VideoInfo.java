package com.example.haanhquan.screenrecord;


public class VideoInfo {
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private static final int FRAME_RATE = 15;
    private static final int IFRAME = 10;
    private static final int BITRATE = 2000000;

    public static int getDefaultWidth(){
        return DISPLAY_WIDTH;
    }
    public static int getDefaultHeight(){
        return DISPLAY_HEIGHT;
    }
    public static int getDefaultFrameRate(){
        return FRAME_RATE;
    }
    public static int getDefaultIframe(){
        return IFRAME;
    }
    public static int getDefaultBitrate(){
        return BITRATE;
    }
}
