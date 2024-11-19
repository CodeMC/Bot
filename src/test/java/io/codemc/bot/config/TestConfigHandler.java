package io.codemc.bot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import io.codemc.bot.MockCodeMCBot;

public class TestConfigHandler {

    private final ConfigHandler handler = MockCodeMCBot.INSTANCE.getConfigHandler();

    @Test
    public void testGetString() {
        assertEquals("TOKEN", handler.getString("bot_token"));

        assertEquals("admin", handler.getString("jenkins", "username"));
        assertEquals("http://localhost:8080", handler.getString("jenkins", "url"));

        assertEquals("test", handler.getString("database", "database"));
        assertEquals("localhost", handler.getString("database", "host"));
        assertEquals("admin", handler.getString("database", "username"));

        assertNotEquals("unset", handler.getString("nexus", "password"));
    }

    @Test
    public void testGetLong() {
        assertEquals(405915656039694336L, handler.getLong("server"));
        assertEquals(405918641859723294L, handler.getLong("author_role"));

        assertEquals(204232208049766400L, handler.getLong("users", "owner"));
        assertEquals(1233971297185431582L, handler.getLong("channels", "request_access"));
        assertEquals(784119059138478080L, handler.getLong("channels", "accepted_requests"));
        assertEquals(800423355551449098L, handler.getLong("channels", "rejected_requests"));
    }

    @Test
    public void testGetStringList() {
        assertEquals(4, handler.getStringList("messages", "accepted").size());
        assertEquals("Your request has been **accepted**!", handler.getStringList("messages", "accepted").get(0));

        assertEquals(4, handler.getStringList("messages", "denied").size());
        assertEquals("Your request has been **rejected**!", handler.getStringList("messages", "denied").get(0));
    }

    @Test
    public void testGetLongList() {
        assertEquals(3, handler.getLongList("allowed_roles", "applications", "accept").size());
        assertEquals(405917902865170453L, handler.getLongList("allowed_roles", "applications", "accept").get(0));

        assertEquals(3, handler.getLongList("allowed_roles", "applications", "deny").size());
        assertEquals(405917902865170453L, handler.getLongList("allowed_roles", "applications", "deny").get(0));
    }

    @Test
    public void testGetInt() {
        assertEquals(3306, handler.getInt("database", "port"));
    }

}
