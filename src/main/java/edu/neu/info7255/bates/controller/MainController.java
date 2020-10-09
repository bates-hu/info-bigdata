package edu.neu.info7255.bates.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.neu.info7255.bates.service.RedisService;
import edu.neu.info7255.bates.utils.Constants;
import edu.neu.info7255.bates.utils.ExceptionResponse;
import edu.neu.info7255.bates.utils.IDUtil;
import edu.neu.info7255.bates.utils.JsonValidator;
import io.netty.handler.codec.json.JsonObjectDecoder;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.ShallowEtagHeaderFilter;



import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.text.ParseException;
import java.util.Date;
@RestController
public class MainController {
    // to read json instance from redis or cache
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private RedisService redisService;

    @GetMapping(value = "/{objectType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> get(
            @PathVariable(name="objectType", required=true) String objectType,
            @PathVariable(name="objectId", required=true) String objectId,
            @RequestHeader HttpHeaders requestHeaders) {
        LOG.info("Getting object with ID {}.", objectId);

        String key = objectType + Constants.SEP + objectId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String content = redisService.get(key);
        if (StringUtils.isEmpty(content)) {
            return ResponseEntity.noContent().build();
        }

        return new ResponseEntity<String>(redisService.get(key), headers, HttpStatus.OK);
    }

    @PostMapping(value = "/{objectType}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> create(
            @PathVariable(name="objectType", required=true) String objectType,
            @RequestBody(required=true) String body,
            @RequestHeader HttpHeaders requestHeaders) {
        LOG.info("create object with body {}.", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject node = null;
        try {
            node = new JSONObject(body);
        } catch (JSONException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            JsonValidator.validateJson(node);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ExceptionResponse.response(e.getMessage()).toString());
        }
        String key = IDUtil.getID(node);
        if (redisService.isKeyExist(key)) {
            return ResponseEntity.badRequest().body(ExceptionResponse.response("Entity already exist").toString());
        }

        redisService.set(IDUtil.getID(node), node.toString());

        return new ResponseEntity<String>(node.toString(), HttpStatus.CREATED);
    }

    @DeleteMapping(value = "/{objectType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> delete(
            @PathVariable(name="objectType", required=true) String objectType,
            @PathVariable(name="objectId", required=true) String objectId,
            @RequestHeader HttpHeaders requestHeaders) {
        LOG.info("Getting object with ID {}.", objectId);

        String key = objectType + Constants.SEP + objectId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (!redisService.isKeyExist(key)) {
            return ResponseEntity.noContent().build();
        }

        boolean result = redisService.delete(key);
        if (!result) {
            return ResponseEntity.badRequest().body(ExceptionResponse.response("delete fail").toString());
        }

        return ResponseEntity.noContent().build();
    }
}
