package io.codemc.bot.utils;

import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.MockJDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.InteractionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAPIUtil {

    @BeforeAll
    public static void ping() {
        assertTrue(NexusAPI.ping());
        assertTrue(JenkinsAPI.ping());
    }

    @Test
    public void testCreatePassword() {
        String p1 = APIUtil.newPassword();
        assertEquals(APIUtil.PASSWORD_SIZE, p1.length());

        String p2 = APIUtil.newPassword();
        assertEquals(APIUtil.PASSWORD_SIZE, p2.length());
        assertNotEquals(p1, p2);
    }

    @Test
    public void testIsGroup() {
        // Tests if the username is a GitHub Organization
        assertTrue(APIUtil.isGroup("CodeMC"));
        assertTrue(APIUtil.isGroup("Team-Inceptus"));

        assertFalse(APIUtil.isGroup("gmitch215"));
        assertFalse(APIUtil.isGroup("sgdc3"));
        assertFalse(APIUtil.isGroup("Andre601"));

        assertFalse(APIUtil.isGroup("_"));
        assertFalse(APIUtil.isGroup("-1"));
    }

    @Test
    public void testNexus() {
        String user1 = "gmitch215";
        Member u1 = MockJDA.mockMember(user1);
        String p1 = APIUtil.newPassword();
        InteractionHook h1 = MockJDA.mockInteractionHook(u1, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        assertTrue(APIUtil.createNexus(h1, user1, p1));
        assertNotNull(NexusAPI.getNexusUser(user1));
        assertNotNull(NexusAPI.getNexusRepository(user1));

        assertTrue(NexusAPI.deleteNexus(user1));
        assertNull(NexusAPI.getNexusUser(user1));
        assertNull(NexusAPI.getNexusRepository(user1));

        String user2 = "CodeMC";
        Member u2 = MockJDA.mockMember(user2);
        String p2 = APIUtil.newPassword();
        InteractionHook h2 = MockJDA.mockInteractionHook(u2, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        assertTrue(APIUtil.createNexus(h2, user2, p2));
        assertNotNull(NexusAPI.getNexusUser(user2));
        assertNotNull(NexusAPI.getNexusRepository(user2));

        assertTrue(NexusAPI.deleteNexus(user2));
        assertNull(NexusAPI.getNexusUser(user2));
        assertNull(NexusAPI.getNexusRepository(user2));
    }

    @Test
    public void testJenkins() {
        String user1 = "gmitch215";
        String j1 = "SocketMC";
        Member u1 = MockJDA.mockMember(user1);
        String p1 = APIUtil.newPassword();
        InteractionHook h1 = MockJDA.mockInteractionHook(u1, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        assertTrue(APIUtil.createJenkinsJob(h1, user1, p1, j1, "https://github.com/gmitch215/SocketMC"));
        assertNotNull(JenkinsAPI.getJenkinsUser(user1));
        assertNotNull(JenkinsAPI.getJobInfo(user1, j1));

        assertTrue(JenkinsAPI.deleteJob(user1, j1));
        assertNull(JenkinsAPI.getJobInfo(user1, j1));
        assertTrue(JenkinsAPI.deleteUser(user1));
        assertNull(JenkinsAPI.getJenkinsUser(user1));

        String user2 = "CodeMC";
        String j2 = "Bot";
        Member u2 = MockJDA.mockMember(user2);
        String p2 = APIUtil.newPassword();
        InteractionHook h2 = MockJDA.mockInteractionHook(u2, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        assertTrue(APIUtil.createJenkinsJob(h2, user2, p2, j2, "https://github.com/CodeMC/Bot"));
        assertNotNull(JenkinsAPI.getJenkinsUser(user2));
        assertNotNull(JenkinsAPI.getJobInfo(user2, j2));

        assertTrue(JenkinsAPI.deleteJob(user2, j2));
        assertNull(JenkinsAPI.getJobInfo(user2, j2));
        assertTrue(JenkinsAPI.deleteUser(user2));
        assertNull(JenkinsAPI.getJenkinsUser(user2));
    }

    @Test
    public void testChangePassword() {
        String username = "CodeMC";
        String job = "API";
        Member user = MockJDA.mockMember(username);
        InteractionHook hook = MockJDA.mockInteractionHook(user, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        String oldPassword = APIUtil.newPassword();
        assertTrue(APIUtil.createNexus(hook, username, oldPassword));
        assertTrue(APIUtil.createJenkinsJob(hook, username, oldPassword, job, "https://github.com/CodeMC/API"));

        String newPassword = APIUtil.newPassword();
        assertTrue(APIUtil.changePassword(hook, username, newPassword));
    }

}
