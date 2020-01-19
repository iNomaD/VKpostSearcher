package etu.wollen.vk.api;

import etu.wollen.vk.exceptions.ApiError;
import etu.wollen.vk.transport.VkHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static etu.wollen.vk.conf.Config.VERSION;
import static etu.wollen.vk.conf.Config.VK_API_URL;

public class RequestAgent {
    private static final int GET_POSTS_COUNT = 100;
    private static final String REQUEST_WALL_GET = VK_API_URL + "wall.get?owner_id=%s&offset=%s&count=" + GET_POSTS_COUNT + "&access_token=%s&v=" + VERSION;

    private final Set<String> validTokens = ConcurrentHashMap.newKeySet();

    public RequestAgent(List<String> accessTokens) {

    }

    public JSONObject getPosts(long wallId, long offset) throws ApiError, IOException, InterruptedException, ParseException {
        String request = String.format(REQUEST_WALL_GET, wallId, offset, accessToken);
        return handleRequest(request);
    }

    private void pickAccessToken() {

    }

    private JSONObject handleRequest(String request) throws ApiError, IOException, InterruptedException, ParseException {
        String response = VkHttpClient.getInstance().httpGet(request);
        JSONParser jp = new JSONParser();
        JSONObject jsonResponse = (JSONObject) jp.parse(response);
        Optional<ApiError> apiError = handleApiErrors(jsonResponse);
        if (apiError.isPresent()) {
            throw apiError.get();
        }
        return (JSONObject) jsonResponse.get("response");
    }

    private Optional<ApiError> handleApiErrors(JSONObject jsonResponse) {
        JSONObject error = (JSONObject) jsonResponse.get("error");
        if (error != null) {
            Object errorCode = error.get("error_code");
            Object errorMsg = error.get("error_msg");
            return Optional.of(new ApiError(errorMsg + " (" + errorCode + ")"));
        }
        return Optional.empty();
    }
}
