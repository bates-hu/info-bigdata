package edu.neu.info7255.bates.service;

import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import static edu.neu.info7255.bates.utils.Constants.*;


@Service
public class RedisService {
    private static final Logger LOG =  LoggerFactory.getLogger(RedisService.class);
    private static final String QUEUE_NAME = "INDEX_QUEUE";

//    @Autowired
//    private ElasticSearchService elasticSearchService;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    public void indexEnqueue(boolean isDelete, JSONObject content, String id, String parentUuid) {
        JSONObject msg = new JSONObject();
        msg.put("isDelete", isDelete);
        msg.put("content", content);
        msg.put("id", id);
        msg.put("parentUuid", parentUuid);

        redisTemplate.opsForList().leftPush(QUEUE_NAME, msg.toString());
    }

    public String indexDequeue() {
        return redisTemplate.opsForList().rightPop(QUEUE_NAME);
    }

    public boolean deleteUtil(String uuid) {
        try {
            // recursively deleting all embedded json objects
            LOG.info("delete {}", uuid);
            Set<String> keys = redisTemplate.keys(uuid+ SEP +"*");
            this.indexEnqueue(true, null, uuid.split("___")[1], null);
            for(String key : keys) {
//                LOG.info("inner delete {}", key);
                if (!(key.contains("___inv_") )) {
                    Set<String> jsonKeySet = redisTemplate.opsForSet().members(key);
                    for (String embd_uuid : jsonKeySet) {
                        deleteUtil(embd_uuid);
                    }
                }
                redisTemplate.delete(key);
            }

            // deleting simple fields
            redisTemplate.delete(uuid);
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean put(JSONObject jsonObject) {
        String uuid = jsonObject.getString("objectType") + SEP + jsonObject.getString("objectId");
//        LOG.info("delete {}", uuid);
        boolean ret = deleteUtil(uuid);
//        LOG.info("keys number after delete{}, ret {}", redisTemplate.keys("*").size(), ret);
        ret = insertUtil(jsonObject);
//        LOG.info("insert ret {}", ret);
        return ret;
    }

    public boolean patch(JSONObject jsonObject) {
        return patch(jsonObject, null);
    }

    public boolean patch(JSONObject jsonObject, String parentId) {
        try {
            String uuid = jsonObject.getString("objectType") + SEP + jsonObject.getString("objectId");
            Map<String,String> simpleMap = new HashMap<String,String>();
            for(Object key : jsonObject.keySet()) {
                String attributeKey = String.valueOf(key);
                Object attributeVal = jsonObject.get(String.valueOf(key));
                String edge = attributeKey;

                if(attributeVal instanceof JSONObject) {

                    JSONObject embdObject = (JSONObject) attributeVal;
                    String setKey = uuid + SEP + edge;
                    String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
                    redisTemplate.opsForSet().add(setKey, embd_uuid);
                    patch(embdObject, uuid);

                } else if (attributeVal instanceof JSONArray) {

                    JSONArray jsonArray = (JSONArray) attributeVal;
                    Iterator<Object> jsonIterator = jsonArray.iterator();
                    String setKey = uuid + SEP + edge;

                    while(jsonIterator.hasNext()) {
                        JSONObject embdObject = (JSONObject) jsonIterator.next();
                        String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
                        redisTemplate.opsForSet().add(setKey, embd_uuid);
                        patch(embdObject, uuid);
                    }

                } else {
                    simpleMap.put(attributeKey, String.valueOf(attributeVal));
                    redisTemplate.opsForHash().put(uuid, attributeKey, String.valueOf(attributeVal));
                }
            }
            LOG.info("hash set key {}  map {}", uuid, simpleMap);
            redisTemplate.opsForHash().putAll(uuid, simpleMap);
            this.indexEnqueue(false, new JSONObject(simpleMap), null, parentId);
            return true;

        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean insertUtil(JSONObject jsonObject) {
        return insertUtil(jsonObject, null);
    }


    public boolean insertUtil(JSONObject jsonObject, String parentId) {
        try {
            String uuid = jsonObject.get("objectType") + SEP + jsonObject.getString("objectId");
            Map<String,String> simpleMap = new HashMap<String,String>();

            for (Object key : jsonObject.keySet()) {
                String attributeKey = String.valueOf(key);
                Object attributeVal = jsonObject.get(String.valueOf(key));
                String edge = attributeKey;
                if (attributeVal instanceof JSONObject) {

                    JSONObject embdObject = (JSONObject) attributeVal;
                    String setKey = uuid + SEP + edge;
                    String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");

//                    LOG.info("sadd key {} value {}", setKey, embd_uuid);
                    redisTemplate.opsForSet().add(setKey, embd_uuid);

//                    LOG.info("set key {} value {}", embd_uuid + SEP + "inv_" + edge, uuid);
                    redisTemplate.opsForValue().set(embd_uuid + SEP + "inv_" + edge, uuid);

                    insertUtil(embdObject, uuid);

                } else if (attributeVal instanceof JSONArray) {

                    JSONArray jsonArray = (JSONArray) attributeVal;
                    Iterator<Object> jsonIterator = jsonArray.iterator();
                    String setKey = uuid + SEP  + edge;

                    while (jsonIterator.hasNext()) {
                        JSONObject embdObject = (JSONObject) jsonIterator.next();
                        String embd_uuid = embdObject.get("objectType") + SEP  + embdObject.getString("objectId");

                        redisTemplate.opsForSet().add(setKey, embd_uuid);
//                        LOG.info("sadd key {} value {}", setKey, embd_uuid);
                        redisTemplate.opsForValue().set(embd_uuid + SEP + "inv_" + edge, uuid);
//                        LOG.info("set key {} value {}", embd_uuid + SEP + "inv_" + edge, uuid);
                        insertUtil(embdObject, uuid);
                    }

                } else {
                    simpleMap.put(attributeKey, String.valueOf(attributeVal));
//                    redisTemplate.opsForHash().put(uuid, attributeKey, String.valueOf(attributeVal));
//                    LOG.info("hash set key {} key {} value {}", uuid, attributeKey, attributeVal);
                }
            }
            LOG.info("hash set key {}  map {}", uuid, simpleMap);
            redisTemplate.opsForHash().putAll(uuid, simpleMap);
            this.indexEnqueue(false, new JSONObject(simpleMap), null, parentId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public JSONObject getUtil(String uuid) {
        try {
            JSONObject o = new JSONObject();

//            LOG.info("key {}", uuid);
            Set<String> keys = redisTemplate.keys(uuid + SEP + "*");
//            LOG.info("keys {}", keys);
            keys.removeIf(key -> key.contains("___inv"));
            // object members
            for (String key : keys) {
                Set<String> jsonKeySet = redisTemplate.opsForSet().members(key);
//                LOG.info("jsonKeySet {}", jsonKeySet);
                String newKey = key.substring(key.lastIndexOf(SEP) + SEPL);
                if (jsonKeySet.size() > 1) {
                    JSONArray ja = new JSONArray();
                    Iterator<String> jsonKeySetIterator = jsonKeySet.iterator();
                    while (jsonKeySetIterator.hasNext()) {
                        String nextKey = jsonKeySetIterator.next();
                        ja.put(getUtil(nextKey));
                    }
                    o.put(newKey, ja);
                } else {

                    Iterator<String> jsonKeySetIterator = jsonKeySet.iterator();
                    JSONObject embdObject = null;
                    while (jsonKeySetIterator.hasNext()) {
                        String nextKey = jsonKeySetIterator.next();
                        embdObject = getUtil(nextKey);

                    }
                    o.put(newKey, embdObject);

                }

            }

            // simple members
            Map<String, String> simpleMap = redisTemplate.<String, String>opsForHash().entries(uuid);
//            LOG.info("next key {}", uuid);
//            LOG.info("embdObject {}", simpleMap);
            for (String simpleKey : simpleMap.keySet()) {
                o.put(simpleKey, simpleMap.get(simpleKey));
            }
            return o;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isKeyExist(final String key) {
        return redisTemplate.hasKey(key) == Boolean.TRUE;
    }
}
