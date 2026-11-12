package start;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Normalization baselines from the top-level discovery.Q20 fields in baseline.json. */
record NormalizationBaselines(double baseline56, double baseline57) {
    static NormalizationBaselines read(Path path) throws IOException {
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json.startsWith("\uFEFF")) json = json.substring(1);
            JSONTokener tokens = new JSONTokener(json);
            JSONObject values = new JSONObject(tokens);
            if (tokens.nextClean() != 0) throw new JSONException("Unexpected content after JSON object");
            return new NormalizationBaselines(positiveNumber(values, "a56_CARMEN"),
                    positiveNumber(values, "a57_SN055525"));
        } catch (IOException | JSONException | IllegalArgumentException e) {
            throw new IOException("Failed to read baselines from " + path.toAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    private static double positiveNumber(JSONObject values, String key) {
        Object value = values.opt(key);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue()) || number.doubleValue() <= 0) {
            throw new IllegalArgumentException("Expected a finite positive JSON number for " + key);
        }
        return number.doubleValue();
    }
}
