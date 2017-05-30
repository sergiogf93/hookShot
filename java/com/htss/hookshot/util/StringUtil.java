package com.htss.hookshot.util;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sergio on 30/05/2017.
 */
public class StringUtil {

    public static int sizeOfString(String string, int fontSize){
        int size = 0;
        String[] words = string.split("  ");

        for(String word : words){
            size += word.length()*fontSize/2; //Every two characters is one fontSize
        }
        size += (words.length - 1)*fontSize; //Spaces

        return size;
    }

}
