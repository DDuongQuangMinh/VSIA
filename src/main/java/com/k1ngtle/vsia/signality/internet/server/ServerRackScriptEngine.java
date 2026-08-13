package com.k1ngtle.vsia.signality.internet.server;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public final class ServerRackScriptEngine {
    private static final int MAX_COMMANDS = 64;
    private ServerRackScriptEngine() {}

    public static String execute(ServerRackBlockEntity rack, ServerLevel level, String source) {
        List<String> commands = split(source);
        if (commands.size() > MAX_COMMANDS) return "ERROR: A script may contain at most 64 commands.";
        StringBuilder output = new StringBuilder();
        int line = 0;
        for (String command : commands) {
            line++;
            if (command.isBlank() || command.startsWith("#")) continue;
            String result = rack.executeScriptCommand(command.trim(), level);
            output.append("[").append(line).append("] ").append(command.trim()).append('\n');
            output.append(result).append('\n');
            if (result.startsWith("ERROR:")) break;
        }
        return output.length() == 0 ? "No commands to execute." : output.toString();
    }

    private static List<String> split(String source) {
        List<String> commands = new ArrayList<>();
        for (String line : source.replace('\r', '\n').split("[\\n;]+")) commands.add(line.trim());
        return commands;
    }
}
