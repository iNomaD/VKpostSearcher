package etu.wollen.vk.api;

import etu.wollen.vk.model.conf.User;
import etu.wollen.vk.transport.VkHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.List;

import static etu.wollen.vk.conf.Config.VERSION;
import static etu.wollen.vk.conf.Config.VK_API_URL;

public class UserDownloader {

    private static final long INVALID_USER_ID_CODE = 113;
    private static final long MAX_FRIENDS = 250;

    private static final String REQUEST_USERS_GET = VK_API_URL + "users.get?user_ids=%s&access_token=%s&v=" + VERSION;
    private static final String REQUEST_FRIENDS_GET = VK_API_URL + "friends.get?user_id=%s&count=%s&access_token=%s&v=" + VERSION;

    private String access_token;

    public UserDownloader(String access_token){
        this.access_token = access_token;
    }

    public List<User> getUsers(List<String> ids) throws Exception {
        String request = String.format(REQUEST_USERS_GET, String.join(",", ids), access_token);
        String response = VkHttpClient.getInstance().httpGet(request);
        JSONParser jp = new JSONParser();
        JSONObject jsonResponse = (JSONObject) jp.parse(response);
        JSONArray resp = (JSONArray) jsonResponse.get("response");
        if (resp != null) {
            List<User> users = new ArrayList<>();
            for (Object o : resp) {
                JSONObject user = (JSONObject) o;
                long id = (long) user.get("id");
                String first = (String) user.get("first_name");
                String last = (String) user.get("last_name");
                users.add(new User(id, first, last));
            }
            return users;
        }
        else {
            JSONObject error = (JSONObject) jsonResponse.get("error");
            long error_code = (long) error.get("error_code");
            if (error_code == INVALID_USER_ID_CODE) {
                return null;
            }
            throw new Exception(error.toJSONString());
        }
    }

    public List<User> getFriends(User user) throws Exception{
        String request = String.format(REQUEST_FRIENDS_GET, user.getId(), MAX_FRIENDS, access_token);
        String response = VkHttpClient.getInstance().httpGet(request);
        JSONParser jp = new JSONParser();
        JSONObject jsonresponse = (JSONObject) jp.parse(response);
        JSONObject resp = (JSONObject) jsonresponse.get("response");
        if (resp != null){
            JSONArray items = (JSONArray)resp.get("items");
            List<String> ids = new ArrayList<>();
            for (Object item : items) {
                long id = (long) item;
                ids.add(Long.toString(id));
            }
            return !ids.isEmpty() ? getUsers(ids) : new ArrayList<>();
        }
        else {
            JSONObject error = (JSONObject) jsonresponse.get("error");
            throw new Exception(error.toJSONString());
        }
    }
}
