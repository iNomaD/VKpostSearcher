package etu.wollen.vk.conf;

import etu.wollen.vk.api.UserDownloader;
import etu.wollen.vk.exceptions.ConfigParseException;
import etu.wollen.vk.models.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ConfigParser {

    public Config parseFileGroups(String filename) throws Exception{
        /// TODO extract properties to conf.properties file
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String idStr = br.readLine();
            String dateStr = br.readLine();
            String accessToken = br.readLine();
            String line;
            ArrayList<String> groupList = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (!line.equals("")) {
                    line = line.replaceAll(".*/", "");
                    groupList.add(line);
                }
            }

            String[] pts = dateStr.split("\\.");
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(pts[2]), Integer.parseInt(pts[1]) - 1, Integer.parseInt(pts[0]));
            Date dateRestr = cal.getTime();

            List<User> users = new UserDownloader(accessToken).getUsers(new ArrayList<String>(){{add(idStr);}});
            User userToFind = (users != null && !users.isEmpty()) ? users.get(0) : null;

            return new Config(userToFind, userToFind != null ? null : idStr, userToFind != null,
                    groupList, dateRestr, accessToken);
        }
        catch (Exception e){
            throw new ConfigParseException(e);
        }
    }
}
