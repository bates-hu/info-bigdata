package edu.neu.info7255.bates.service;


import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.file.LinkOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

@Service
public class EtagManager {
    private static final Logger LOG =  LoggerFactory.getLogger(RedisService.class);

    @Autowired
    RedisService redisService;

    private final HashMap<String, String> etagMap = new HashMap<>();

    private String genEtag(JSONObject object){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(object.toString().getBytes());
            byte[] digest = md.digest();
            BigInteger number = new BigInteger(1, digest);
            return number.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getEtag(String key) {
        if (!this.etagMap.containsKey(key) && redisService.isKeyExist(key)) {
            updateEtag(key);
        }
        return etagMap.getOrDefault(key, null);
    }

    public boolean compare(String key, String etag){
        return this.getEtag(key).equals(etag);
    }

    public void updateEtag(String key) {
        JSONObject object = redisService.getUtil(key);
        String etagValue = this.genEtag(object);
        etagMap.put(key, etagValue);
        LOG.info("update etag value key {} value {}", key, etagValue);
    }
}
