package edu.neu.info7255.bates.utils;

import org.json.JSONObject;

public class ExceptionResponse {
    public static JSONObject response(String msg) {
        JSONObject obj = new JSONObject();
        obj.put("message", msg);
        return obj;
    }
}
