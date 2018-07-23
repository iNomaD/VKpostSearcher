package etu.wollen.vk.api;

import etu.wollen.vk.models.User;
import etu.wollen.vk.transport.HttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.List;

import static etu.wollen.vk.conf.Config.version;

public class UserDownloader {

    private static final long INVALID_USER_ID_CODE = 113;
    private static final long MAX_FRIENDS = 250;

    private String access_token;

    public UserDownloader(String access_token){
        this.access_token = access_token;
    }

    public List<User> getUsers(List<String> ids) throws Exception{
        StringBuilder ids_request = new StringBuilder();
        for(String id: ids) ids_request.append(id).append(",");

        String request = "https://api.vk.com/method/users.get?user_ids="+ids_request
                +"&v="+version+"&access_token=" + access_token;

        String response = HttpClient.sendGETtimeout(request, 11);
        JSONParser jp = new JSONParser();
        JSONObject jsonresponse = (JSONObject) jp.parse(response);
        JSONArray resp = (JSONArray) jsonresponse.get("response");
        if(resp != null){
            List<User> users = new ArrayList<>();
            for(int i=0; i<resp.size(); ++i){
                JSONObject user = (JSONObject)resp.get(i);
                long id = (long) user.get("id");
                String first = (String) user.get("first_name");
                String last = (String) user.get("last_name");
                users.add(new User(id, first, last));
            }
            return users;
        }
        else{
            JSONObject error = (JSONObject) jsonresponse.get("error");
            long error_code = (long) error.get("error_code");
            if(error_code == INVALID_USER_ID_CODE){
                return null;
            }
            throw new Exception(error.toJSONString());
        }
    }

    public List<User> getFriends(User user) throws Exception{
        String request = "https://api.vk.com/method/friends.get?user_id="+user.getId()
                + "&count=" + MAX_FRIENDS + "&v="+version+"&access_token=" + access_token;

        String response = HttpClient.sendGETtimeout(request, 11);
        JSONParser jp = new JSONParser();
        JSONObject jsonresponse = (JSONObject) jp.parse(response);
        JSONObject resp = (JSONObject) jsonresponse.get("response");
        if(resp != null){
            JSONArray items = (JSONArray)resp.get("items");
            List<String> ids = new ArrayList<>();
            for(int i=0; i<items.size(); ++i){
                long id = (long) items.get(i);
                ids.add(Long.toString(id));
            }
            return !ids.isEmpty() ? getUsers(ids) : new ArrayList<>();
        }
        else{
            JSONObject error = (JSONObject) jsonresponse.get("error");
            throw new Exception(error.toJSONString());
        }
    }
}
