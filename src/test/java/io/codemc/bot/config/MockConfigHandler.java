package io.codemc.bot.config;

import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MockConfigHandler extends ConfigHandler {

    private static final Map<String, String> TEST_CONFIG = Stream.of(
            "bot_token test_token",
            "server 405915656039694336",
            "channels.request_access 1233971297185431582",
            "channels.accepted_requests 784119059138478080",
            "channels.rejected_requests 800423355551449098",
            "author_role 405918641859723294",
            "allowed_roles.applications.accept 405917902865170453,659568973079379971,1233971297185431582",
            "allowed_roles.applications.deny 405917902865170453,659568973079379971,1233971297185431582",
            "allowed_roles.commands.application 405917902865170453,659568973079379971,1233971297185431582",
            "allowed_roles.commands.codemc 405917902865170453,659568973079379971",
            "allowed_roles.commands.disable 405917902865170453",
            "allowed_roles.commands.msg 405917902865170453",
            "allowed_roles.commands.reload 405917902865170453",
            "users.owner 204232208049766400",
            "users.co_owners 143088571656437760,282975975954710528",
            "messages.accepted Accepted",
            "messages.denied Rejected",
            // Configuration
            "jenkins.url http://localhost:8080",
            "jenkins.username admin",
            "jenkins.token 00000000000000000000000000000000",

            "nexus.url http://localhost:8081",
            "nexus.username admin",
            "nexus.password password",

            "database.service mariadb",
            "database.host localhost",
            "database.port 3306",
            "database.database test",
            "database.username admin",
            "database.password password"
    ).map(s -> s.split("\\s", 1)).collect(Collectors.toMap(s -> s[0], s -> s[1]));

    private final Logger logger = LoggerFactory.getLogger(MockConfigHandler.class);


    @Override
    public boolean loadConfig() {
        return true;
    }

    @Override
    public boolean reloadConfig() {
        return true;
    }

    @Override
    public String getString(Object... path) {
        String[] args = Arrays.stream(path).map(Object::toString).toArray(String[]::new);
        String key = String.join(".", args);
        if (TEST_CONFIG.containsKey(key)) {
            return TEST_CONFIG.get(key);
        }
        logger.warn(() -> "Missing test config value for key: " + key);
        return null;
    }

    @Override
    public long getLong(Object... path) {
        String value = getString(path);
        if (value == null) {
            return -1L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            logger.warn(() -> "Invalid long value for key: " + String.join(".", Arrays.stream(path).map(Object::toString).toArray(String[]::new)));
            return -1L;
        }
    }

    @Override
    public int getInt(Object... path) {
        String value = getString(path);
        if (value == null) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            logger.warn(() -> "Invalid int value for key: " + String.join(".", Arrays.stream(path).map(Object::toString).toArray(String[]::new)));
            return -1;
        }
    }

    @Override
    public List<String> getStringList(Object... path) {
        String value = getString(path);
        if (value == null) {
            return null;
        }
        return Arrays.asList(value.split(","));
    }

    @Override
    public List<Long> getLongList(Object... path) {
        String value = getString(path);
        if (value == null) {
            return null;
        }
        try {
            return Arrays.stream(value.split(",")).map(Long::parseLong).toList();
        } catch (NumberFormatException ex) {
            logger.warn(() -> "Invalid long list value for key: " + String.join(".", Arrays.stream(path).map(Object::toString).toArray(String[]::new)));
            return null;
        }
    }



}
