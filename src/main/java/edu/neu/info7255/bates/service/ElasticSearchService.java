package edu.neu.info7255.bates.service;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ReactiveListCommands;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.*;

@Service
public class ElasticSearchService {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final static HttpClient httpClient = HttpClient.newBuilder().build();
    private final static String baseURL = "http://localhost:9200";

    public void put(JSONObject obj, String parentID) throws Exception {

    }

    public void delete(String id) throws Exception {
        LOG.info("start add record to es {}", id);
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
    }

}
