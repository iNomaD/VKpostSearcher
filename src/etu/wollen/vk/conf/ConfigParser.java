package etu.wollen.vk.conf;

import etu.wollen.vk.exceptions.ConfigParseException;
import etu.wollen.vk.transport.HttpClient;
import etu.wollen.vk.models.User;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static etu.wollen.vk.conf.Config.version;

public class ConfigParser {
    private static final long INVALID_USER_ID_CODE = 113;

    public Config parseFileGroups(String filename) throws Exception {
        ArrayList<String> grList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String id_str = br.readLine();
            String date_str = br.readLine();
            String access_token = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.equals("")) {
                    line = line.replaceAll(".*/", "");
                    grList.add(line);
                }
            }

            String[] pts = date_str.split("\\.");
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(pts[2]), Integer.parseInt(pts[1]) - 1, Integer.parseInt(pts[0]));
            Date dateRestr = cal.getTime();

            User find_user = getUserFromStr(id_str, access_token);

            return new Config(find_user, find_user != null ? null : id_str, find_user != null,
                    grList, dateRestr, access_token);
        }
    }

    private User getUserFromStr(String id_str, String access_token) throws Exception{
        String request = "https://api.vk.com/method/users.get?user_ids="+id_str
                +"&v="+version+"&access_token=" + access_token;

        String response = HttpClient.sendGETtimeout(request, 11);
        JSONParser jp = new JSONParser();
        JSONObject jsonresponse = (JSONObject) jp.parse(response);
        JSONArray resp = (JSONArray) jsonresponse.get("response");
        if(resp != null){
            JSONObject user = (JSONObject)resp.get(0);
            long id = (long) user.get("id");
            String first = (String) user.get("first_name");
            String last = (String) user.get("last_name");
            return new User(id, first, last);
        }
        else{
            JSONObject error = (JSONObject) jsonresponse.get("error");
            long error_code = (long) error.get("error_code");
            if(error_code == INVALID_USER_ID_CODE){
                return null;
            }
            throw new ConfigParseException(error.toJSONString());
        }
    }
}
