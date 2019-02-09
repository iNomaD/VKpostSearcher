package etu.wollen.vk.utils;

public class CommandLineUtils {

    private CommandLineUtils() {}

    public static boolean getFlag(String[] args, String flag) {
        if (args.length > 0){
            for (String arg : args){
                if (arg.equals(flag)) return true;
            }
        }
        return false;
    }

    public static String getParameter(String[] args, String parameterName) {
        if (args.length > 0){
            for (String arg : args){
                if (arg.contains(parameterName)) {
                    return arg.substring(arg.indexOf(parameterName) + parameterName.length());
                }
            }
        }
        return null;
    }
}
