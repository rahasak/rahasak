package com.score.chatz.pojo;

import android.util.Log;

import com.score.senzc.pojos.Senz;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eranga on 8/19/16.
 */
public class SenzStream {
    private boolean isActive;
    private String user;
    private StringBuilder streamBuffer;
    private String image;
    private SENZ_STEAM_TYPE streamType;

    public SenzStream(boolean isActive, String user, StringBuilder buffer) {
        this.isActive = isActive;
        this.user = user;
        this.streamBuffer = buffer;
        this.image = "";
    }

    public void setStreamType(SENZ_STEAM_TYPE type){
        streamType = type;
    }

    public SENZ_STEAM_TYPE getStreamType(){
        return streamType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void putStream(String stream) {

       // this.streamBuffer.append(stream); This doesn't work for some reason ??
        //TODO integarate a string buffer to improve performance
        image += stream;
    }

    public String getStream() {
        return image;
    }

    /**
     * Return a sinle senz of the stream
     * @return
     */
    public String getSenzString(){
        String senzString = getStartOfStream(streamType);
        senzString += getImageFromStream();
        senzString += getEndOfStream(streamType);
        return senzString;
    }

    /**
     * Get only the image part of the stream
     * @return
     */
    private String getImageFromStream(){
        String imageString = "";
        Pattern pattern = getRegexPattern(streamType);
        Matcher matcher = pattern.matcher(image);
        while (matcher.find())
        {
            imageString += matcher.group(1);
            Log.i("STREAM", "PARTS - " + matcher.group(1));
        }
        Log.i("STREAM", "FINAL - " + imageString);
        return imageString;
    }

    /**
     * Return regex pattern based on type, to extract either the chatphoto or profilez photo
     * @param type
     * @return
     */
    private Pattern getRegexPattern(SENZ_STEAM_TYPE type){
        Pattern regex = null;

        switch (type){
            case CHATZPHOTO:
                regex = Pattern.compile("#chatzphoto\\s(.*?)\\s|(^[^D][^A][^T][^A].*?)\\s#time");
                break;
            case PROFILEZPHOTO:
                regex = Pattern.compile("#profilezphoto\\s(.*?)\\s");
                break;
            case CHATZSOUND:
                regex = Pattern.compile("#chatzsound\\s(.*?)\\s");
                break;
        }

        return regex;
    }

    /**
     * Return the start of the stream based on which type of stream
     * @param type
     * @return
     */
    private String getStartOfStream(SENZ_STEAM_TYPE type){
        String startOfStream = null;
        Pattern pattern = null;
        Matcher matcher = null;

        switch (type){
            case CHATZPHOTO:
                pattern = Pattern.compile("(#time\\s[0-9]+\\s)");
                matcher = pattern.matcher(image);
                if (matcher.find())
                {
                    startOfStream = matcher.group(1);
                }
                startOfStream += "#chatzphoto ";
                break;
            case PROFILEZPHOTO:
                pattern = Pattern.compile("(#time\\s[0-9]+\\s)");
                matcher = pattern.matcher(image);
                if (matcher.find())
                {
                    startOfStream = matcher.group(1);
                }
                startOfStream += "#profilezphoto ";
            break;
            case CHATZSOUND:
                pattern = Pattern.compile("(#time\\s[0-9]+\\s)");
                matcher = pattern.matcher(image);
                if (matcher.find())
                {
                    startOfStream = matcher.group(1);
                }
                startOfStream += "#chatzsound ";
                break;
        }

        return "DATA " + startOfStream;
    }

    /**
     * Extract the last part of the stream which is common to all streaming packets
     * @return
     */
    private String getEndOfStream(SENZ_STEAM_TYPE type){
        String lastSection = "";
        Pattern pattern = null;
        Matcher matcher = null;

        switch (type){
            case CHATZPHOTO:
                pattern = Pattern.compile("(\\s@[a-zA-Z0-9]+?\\s\\^[a-zA-Z0-9]+?\\sSIGNATURE)$");
                matcher = pattern.matcher(image);

                if (matcher.find())
                {
                    lastSection = matcher.group(1);
                }
                break;
            case PROFILEZPHOTO:
                pattern = Pattern.compile("(\\s@[a-zA-Z0-9]+?\\s\\^[a-zA-Z0-9]+?\\sSIGNATURE)$");
                matcher = pattern.matcher(image);

                if (matcher.find())
                {
                    lastSection = matcher.group(1);
                }
                break;
            case CHATZSOUND:
                pattern = Pattern.compile("(\\s@[a-zA-Z0-9]+?\\s\\^[a-zA-Z0-9]+?\\sSIGNATURE)$");
                matcher = pattern.matcher(image);

                if (matcher.find())
                {
                    lastSection = matcher.group(1);
                }
                break;
        }



        return lastSection;
    }

    public enum SENZ_STEAM_TYPE{
        CHATZPHOTO, PROFILEZPHOTO, CHATZSOUND
    }
}


