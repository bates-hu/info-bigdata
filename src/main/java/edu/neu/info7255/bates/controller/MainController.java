package edu.neu.info7255.bates.controller;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import edu.neu.info7255.bates.service.EtagManager;
import edu.neu.info7255.bates.service.RedisService;
import edu.neu.info7255.bates.utils.Constants;
import edu.neu.info7255.bates.utils.ExceptionResponse;
import edu.neu.info7255.bates.utils.IDUtil;
import edu.neu.info7255.bates.utils.JsonValidator;
import org.apache.tomcat.util.bcel.classfile.ArrayElementValue;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.text.ParseException;
import java.util.Date;


@RestController
public class MainController {
    // to read json instance from redis or cache
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private RSAKey rsaPublicJWK;

    @Autowired
    private RedisService redisService;

    @Autowired
    private EtagManager etagManager;

    @GetMapping(value = "/{objectType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> get(
            @PathVariable(name="objectType", required=true) String objectType,
            @PathVariable(name="objectId", required=true) String objectId,
            @RequestHeader HttpHeaders requestHeaders) {
        if (!ifAuthorized(requestHeaders)) {
            return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
        }
        String etag = requestHeaders.getFirst("If-None-Match");

        String key = objectType + Constants.SEP + objectId;
        if (!redisService.isKeyExist(key)) {
            return ResponseEntity.noContent().build();
        }
        if (etag != null && etagManager.compare(key, etag)) {
            LOG.info("NOT MODIFIED");
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        JSONObject node = redisService.getUtil(key);


        if (node == null || node.length() == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().eTag(etagManager.getEtag(key)).body(node.toString());
    }

    @PostMapping(value = "/{objectType}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> create(
            @PathVariable(name="objectType", required=true) String objectType,
            @RequestBody(required=true) String body,
            @RequestHeader HttpHeaders requestHeaders) {
        JSONObject node = null;

        if (!ifAuthorized(requestHeaders)) {
            return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
        }
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
        redisService.insertUtil(node);
        etagManager.updateEtag(key);

        return ResponseEntity.status(HttpStatus.CREATED).eTag(etagManager.getEtag(key)).build();
    }

    @PatchMapping(value = "/{objectType}/{objectId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> patch(
            @PathVariable(name="objectType", required=true) String objectType,
            @PathVariable(name="objectId", required=true) String objectId,
            @RequestBody(required=true) String body,
            @RequestHeader HttpHeaders requestHeaders) {
        JSONObject node = null;

        if (!ifAuthorized(requestHeaders)) {
            return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
        }
        String etag = requestHeaders.getFirst("If-Match");
        LOG.info("etag value {}", etag);
        if (etag == null) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).build();
        }
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
        if (!redisService.isKeyExist(key)) {
            return ResponseEntity.badRequest().body(ExceptionResponse.response("Entity does not exist").toString());
        }

        if (!etagManager.compare(key, etag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }

        redisService.patch(node);
        etagManager.updateEtag(key);

        return ResponseEntity.ok().eTag(etagManager.getEtag(key)).build();
    }


    @PutMapping(value = "/{objectType}/{objectId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> put(
            @PathVariable(name="objectType", required=true) String objectType,
            @PathVariable(name="objectId", required=true) String objectId,
            @RequestBody(required=true) String body,
            @RequestHeader HttpHeaders requestHeaders) {
        JSONObject node = null;

        if (!ifAuthorized(requestHeaders)) {
            return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
        }
        String etag = requestHeaders.getFirst("If-Match");
        if (etag == null) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).build();
        }
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
        if (!etagManager.compare(key, etag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }
        redisService.put(node);
        etagManager.updateEtag(key);

        return ResponseEntity.ok().eTag(etagManager.getEtag(key)).build();
    }

    @DeleteMapping(value = "/{objectType}/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> delete(
            @PathVariable(name="objectType", required=true) String objectType,
            @PathVariable(name="objectId", required=true) String objectId,
            @RequestHeader HttpHeaders requestHeaders) {
        if (!ifAuthorized(requestHeaders)) {
            return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
        }
        String key = objectType + Constants.SEP + objectId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (!redisService.isKeyExist(key)) {
            return ResponseEntity.noContent().build();
        }

        boolean result = redisService.deleteUtil(key);
        if (!result) {
            return ResponseEntity.badRequest().body(ExceptionResponse.response("delete fail").toString());
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getToken() throws JOSEException {
        // RSA signatures require a public and private RSA key pair, the public key
        // must be made known to the JWS recipient in order to verify the signatures
        RSAKey rsaJWK = new RSAKeyGenerator(2048).keyID("bates").generate();
        rsaPublicJWK = rsaJWK.toPublicJWK();
        // verifier = new RSASSAVerifier(rsaPublicJWK);

        // Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(rsaJWK);

        // Prepare JWT with claims set
        int expireTime = 30000; // seconds

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .expirationTime(new Date(new Date().getTime() + expireTime * 1000)) // milliseconds
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
                claimsSet);

        // Compute the RSA signature
        signedJWT.sign(signer);

        // To serialize to compact form, produces something like
        // eyJhbGciOiJSUzI1NiJ9.SW4gUlNBIHdlIHRydXN0IQ.IRMQENi4nJyp4er2L
        // mZq3ivwoAjqa1uUkSBKFIX7ATndFF5ivnt-m8uApHO4kfIFOrW7w2Ezmlg3Qd
        // maXlS9DhN0nUk_hGI3amEjkKd0BWYCB8vfUbUv0XGjQip78AI4z1PrFRNidm7
        // -jPDm5Iq0SZnjKjCNS5Q15fokXZc8u0A
        String token = signedJWT.serialize();

        String res = "{\"status\": \"Successful\",\"token\": \"" + token + "\"}";
        return new ResponseEntity<String>(res, HttpStatus.OK);

    }


    private boolean ifAuthorized(HttpHeaders requestHeaders) {
        try {
            String authHeader =  requestHeaders.getFirst("Authorization");
            if (authHeader == null) {
                return false;
            }
            String[] tokenArray = authHeader.split(" ");
            if (tokenArray.length < 2) {
                return false;
            }
            String token = tokenArray[1];
            LOG.info("token get {}", token);
            // On the consumer side, parse the JWS and verify its RSA signature
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new RSASSAVerifier(rsaPublicJWK);
            // Retrieve / verify the JWT claims according to the app requirements
            if (!signedJWT.verify(verifier)) {
                return false;
            }
            JWTClaimsSet claimset = signedJWT.getJWTClaimsSet();
            Date exp = claimset.getExpirationTime();

            return new Date().before(exp);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
            return false;
        }
    }

}
