package etu.wollen.vk.conf;

import etu.wollen.vk.api.UserDownloader;
import etu.wollen.vk.exceptions.ConfigParseException;
import etu.wollen.vk.model.conf.Board;
import etu.wollen.vk.model.conf.User;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ConfigParser {

    public Config parseFileGroups(String filename) throws ConfigParseException {
        /// TODO migrate properties to conf.properties file

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(filename), StandardCharsets.UTF_8))) {

            String searchExpression = br.readLine();
            String dateExpression = br.readLine();
            String accessToken = br.readLine();
            String primaryThreads = br.readLine();
            String secondaryThreads = br.readLine();

            List<String> groupList = new ArrayList<>();
            List<Board> boardList = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && line.contains("vk.com/")) {
                    line = line.replaceAll(".*/", "");

                    String[] topicParts = null;
                    if (line.contains("topic-"))
                        topicParts = line.replace("topic-", "").split("_");

                    if (topicParts != null && topicParts.length == 2 && !topicParts[0].isEmpty() && !topicParts[1].isEmpty()){
                        boardList.add(new Board(Integer.valueOf(topicParts[0]), Integer.valueOf(topicParts[1])));
                    }
                    else {
                        groupList.add(line);
                    }
                }
            }

            String[] pts = dateExpression.split("\\.");
            Calendar cal = Calendar.getInstance();
            //noinspection MagicConstant
            cal.set(Integer.parseInt(pts[2]), Integer.parseInt(pts[1]) - 1, Integer.parseInt(pts[0]));
            Date dateRestriction = cal.getTime();

            int primaryThreadsInt = Integer.parseInt(primaryThreads);
            if (primaryThreadsInt > 0) Config.PRIMARY_THREADS = primaryThreadsInt;
            int secondaryThreadsInt = Integer.parseInt(secondaryThreads);
            if (secondaryThreadsInt > 0) Config.SECONDARY_THREADS = secondaryThreadsInt;

            List<User> users = new UserDownloader(accessToken).getUsers(new ArrayList<String>() {
                { add(searchExpression.replace("https://vk.com/", "")); }
            });
            User userToFind = (users != null && !users.isEmpty()) ? users.get(0) : null;

            return new Config(userToFind, userToFind != null ? null : searchExpression, userToFind != null,
                    groupList, boardList, dateRestriction, accessToken);
        } catch (Exception e) {
            throw new ConfigParseException(e);
        }
    }
}
