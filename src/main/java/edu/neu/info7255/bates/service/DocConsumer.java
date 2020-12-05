package edu.neu.info7255.bates.service;

import com.google.gson.JsonObject;
import edu.neu.info7255.bates.utils.ApplicationContextProvider;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static edu.neu.info7255.bates.utils.Constants.*;


public class DocConsumer extends Thread {

    private final StringRedisTemplate template;
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final static HttpClient httpClient = HttpClient.newBuilder().build();
    private final static String baseURL = "http://localhost:9200";

    public DocConsumer() {
        this.template = ApplicationContextProvider.getBean(StringRedisTemplate.class);
    }

    @Override
    public void run() {
        while (true) {
            LOG.info("start running");
            String msg = this.template.opsForList().rightPop(QUEUE_NAME);
            LOG.info(msg);
            if (msg == null) {
                LOG.info("sleep 1s");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            JSONObject msgObj = new JSONObject(msg);
            if (msgObj.getBoolean("isDelete")) {
                // is delete
                this.delete(msgObj.getString("id"));

            } else {
                // put
                JSONObject object = msgObj.getJSONObject("content");
                if (msgObj.has("parentUuid")) {
                    this.put(object, msgObj.getString("parentUuid"));
                } else {
                    this.put(object, null);
                }

            }

        }
    }

    public void put(JSONObject obj, String parentID){
        try {
            String id = obj.getString("objectId");
            LOG.info("start add record to es {} real parent {}", id, parentID);
            obj.put("relation_type", new JSONObject());

            obj.getJSONObject("relation_type").put("name",  obj.getString("objectType"));
            String indexer = String.format("/plan/_doc/%s?routing=1", id);
            if (parentID != null) {
                obj.getJSONObject("relation_type").put("parent",  parentID.split("___")[1]);
                obj.getJSONObject("relation_type").put("name", parentID.split("___")[0] + "___" + obj.getString("objectType"));
            }
            LOG.info("obj {}", obj);
            LOG.info("indexer {}", indexer);
            String url = baseURL + indexer;
            LOG.info("url {}", url);
            HttpRequest request = HttpRequest.newBuilder(new URI(url))
                    .header("Content-Type", "application/json").header("Accept", "*/*")
                    .PUT(HttpRequest.BodyPublishers.ofString(obj.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("response code {}, detail {}", response.statusCode(), response.body());
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

    }

    private void delete(String id) {
        try  {
            String indexer = String.format("/plan/_doc/%s?routing=1", id);
            LOG.info("indexer {}", indexer);
            String url = baseURL + indexer;
            LOG.info("url {}", url);
            HttpRequest request = HttpRequest.newBuilder(new URI(url))
                    .header("Content-Type", "application/json").header("Accept", "*/*")
                    .DELETE()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("response code {}, detail {}", response.statusCode(), response.body());
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

    }
}
