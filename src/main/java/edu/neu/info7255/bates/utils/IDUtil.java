package edu.neu.info7255.bates.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONObject;

public class IDUtil {
    public static String getID(JSONObject obj){
        return obj.getString("objectType") + Constants.SEP + obj.getString("objectId");
    }
}
