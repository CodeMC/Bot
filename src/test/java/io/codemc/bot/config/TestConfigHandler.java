package io.codemc.bot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.codemc.bot.MockCodeMCBot;

public class TestConfigHandler {

    private final ConfigHandler handler = MockCodeMCBot.INSTANCE.getConfigHandler();

    @Test
    public void testGetString() {
        assertEquals(handler.getString("bot_token"), "TOKEN");
        assertEquals(handler.getString("github"), "token");

        assertEquals(handler.getString("jenkins", "username"), "admin");
        assertEquals(handler.getString("jenkins", "url"), "https://ci.codemc.io/");

        assertEquals(handler.getString("database", "database"), "test");
        assertEquals(handler.getString("database", "host"), "localhost");
        assertEquals(handler.getString("database", "username"), "admin");

        assertNotEquals(handler.getString("nexus", "password"), "unset");
    }

    @Test
    public void testGetLong() {
        assertEquals(handler.getLong("server"), 405915656039694336L);
        assertEquals(handler.getLong("author_role"), 405918641859723294L);

        assertEquals(handler.getLong("uesrs", "owner"), 204232208049766400L);
        assertEquals(handler.getLong("channels", "request_access"), 1233971297185431582L);
        assertEquals(handler.getLong("channels", "accepted_requests"), 784119059138478080L);
        assertEquals(handler.getLong("channels", "rejected_requests"), 800423355551449098L);
    }

    @Test
    public void testGetStringList() {
        assertEquals(handler.getStringList("messages", "accepted").size(), 4);
        assertEquals(handler.getStringList("messages", "accepted").get(0), "Your request has been **accepted**!");

        assertEquals(handler.getStringList("messages", "denied").size(), 4);
        assertEquals(handler.getStringList("messages", "denied").get(0), "Your request has been **rejected**!");
    }

    @Test
    public void testGetLongList() {
        assertEquals(handler.getLongList("allowed_roles", "applications", "accept").size(), 3);
        assertEquals(handler.getLongList("allowed_roles", "applications", "accept").get(0), 405917902865170453L);

        assertEquals(handler.getLongList("allowed_roles", "applications", "deny").size(), 3);
        assertEquals(handler.getLongList("allowed_roles", "applications", "deny").get(0), 405917902865170453L);
    }

    @Test
    public void testGetInt() {
        assertEquals(handler.getInt("database", "port"), 3306);
    }

}
