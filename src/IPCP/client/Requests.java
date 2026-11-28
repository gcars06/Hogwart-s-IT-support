package IPCP.client;

import java.io.IOException;
import java.time.LocalDate;
import org.json.JSONObject;

/**
 * Sends health-data requests to the backend using the IPCP client.
 *
 * <p>Daily reads return the backend's nutrition, fitness, sleep, and goals
 * fields. A daily write follows the backend's current PUT contract and stores
 * one goal alongside the health measurements.
 */
public final class Requests {
    private static final byte IPCP_VERSION = 1;

    private Requests() {}

    /**
     * Retrieves one user's health data and goals for a date.
     *
     * @param userId user identifier
     * @param entryDate date of the entry
     * @return backend response body, including a {@code goals} array
     * @throws IOException if IPCP cannot connect or the backend rejects the request
     */
    public static JSONObject getData(int userId, LocalDate entryDate) throws IOException {
        JSONObject body = new JSONObject()
                .put("entryDate", requireDate(entryDate));
        return send(userId, IPCPRequest.Type.GET, body).body;
    }

    /**
     * Stores a user's daily health measurements and one goal.
     *
     * <p>{@code nutritionData} must contain {@code calories}, {@code protein},
     * {@code carbs}, and {@code fat}; {@code fitnessData} must contain
     * {@code steps}, {@code lapTimeSeconds}, and {@code mileTimeSeconds}.
     *
     * @param userId user identifier
     * @param entryDate date of the entry
     * @param nutritionData nutrition measurements
     * @param fitnessData fitness measurements
     * @param hoursSlept hours slept
     * @param goal goal text to store
     * @throws IOException if IPCP cannot connect or the backend rejects the request
     */
    public static void saveData(
            int userId,
            LocalDate entryDate,
            JSONObject nutritionData,
            JSONObject fitnessData,
            double hoursSlept,
            String goal) throws IOException {
        if (nutritionData == null || fitnessData == null) {
            throw new IllegalArgumentException("Nutrition and fitness data are required.");
        }
        requireNumericFields(nutritionData, "calories", "protein", "carbs", "fat");
        requireNumericFields(fitnessData, "steps", "lapTimeSeconds", "mileTimeSeconds");
        if (!Double.isFinite(hoursSlept)) {
            throw new IllegalArgumentException("Hours slept must be a finite number.");
        }
        if (goal == null) {
            throw new IllegalArgumentException("Goal text is required.");
        }

        JSONObject body = new JSONObject()
                .put("entryDate", requireDate(entryDate))
                .put("nutritionData", nutritionData)
                .put("fitnessData", fitnessData)
                .put("hoursSlept", hoursSlept)
                .put("goal", goal);
        send(userId, IPCPRequest.Type.PUT, body);
    }

    /** Converts a non-null date to the ISO format expected by the backend. */
    private static String requireDate(LocalDate entryDate) {
        if (entryDate == null) {
            throw new IllegalArgumentException("Entry date is required.");
        }
        return entryDate.toString();
    }

    /** Rejects missing, non-numeric, and non-finite fields before sending a packet. */
    private static void requireNumericFields(JSONObject values, String... fields) {
        for (String field : fields) {
            if (!(values.opt(field) instanceof Number number)
                    || !Double.isFinite(number.doubleValue())) {
                throw new IllegalArgumentException(
                        "Expected a finite numeric value for '" + field + "'.");
            }
        }
    }

    /** Opens IPCP if needed, sends the packet, and converts backend errors to IOExceptions. */
    private static IPCPResponse send(
            int userId, IPCPRequest.Type type, JSONObject body) throws IOException {
        if (!IPCP.init()) {
            throw new IOException("Could not initialize IPCP; make sure the backend is running.");
        }

        IPCPResponse response = IPCP.send(
                new IPCPRequest(IPCP_VERSION, type, userId, body));
        if (response.status != IPCPResponse.StatusCode.OK
                && response.status != IPCPResponse.StatusCode.OK_NO_CONENT) {
            throw new IOException(response.body.optString(
                    "message", "Backend request failed: " + response.status));
        }
        return response;
    }
}
