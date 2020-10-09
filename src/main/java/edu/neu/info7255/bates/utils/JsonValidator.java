package edu.neu.info7255.bates.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.InputStream;


public class JsonValidator {
    private final static Schema schema = loadSchema();

    public static boolean validateJson(JSONObject object) {
        schema.validate(object);
        return true;
    }

    private static Schema loadSchema() {
        InputStream inputStream = JsonValidator.class.getResourceAsStream("/PlanSchema.json");
        System.out.println(inputStream.toString());
        JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
        Schema schema = SchemaLoader.load(rawSchema);
        return schema;
    }
}