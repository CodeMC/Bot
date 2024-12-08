package io.codemc.bot.utils;

import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.InteractionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestAPIUtil {

    @BeforeAll
    public static void ping() {
        MockCodeMCBot.INSTANCE.validateConfig();

        assertTrue(NexusAPI.ping());
        assertTrue(JenkinsAPI.ping());
    }

    @Test
    @DisplayName("Test APIUtil#newPassword")
    public void testCreatePassword() {
        String p1 = APIUtil.newPassword();
        assertEquals(APIUtil.PASSWORD_SIZE, p1.length());

        String p2 = APIUtil.newPassword();
        assertEquals(APIUtil.PASSWORD_SIZE, p2.length());
        assertNotEquals(p1, p2);
    }

    @Test
    @DisplayName("Test APIUtil#isGroup")
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
    @DisplayName("Test APIUtil#createNexus")
    public void testNexus() {
        String user1 = "TestNexus1";
        Member u1 = MockJDA.mockMember(user1);
        String p1 = APIUtil.newPassword();
        InteractionHook h1 = MockJDA.mockInteractionHook(u1, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        assertTrue(APIUtil.createNexus(h1, user1, p1));
        assertTrue(NexusAPI.exists(user1));
        assertNotNull(NexusAPI.getNexusRepository(user1));

        assertTrue(NexusAPI.deleteNexus(user1));
        assertFalse(NexusAPI.exists(user1));
        assertNull(NexusAPI.getNexusRepository(user1));

        String user2 = "TestNexus2";
        Member u2 = MockJDA.mockMember(user2);
        String p2 = APIUtil.newPassword();
        InteractionHook h2 = MockJDA.mockInteractionHook(u2, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        assertTrue(APIUtil.createNexus(h2, user2, p2));
        assertTrue(NexusAPI.exists(user2));
        assertNotNull(NexusAPI.getNexusRepository(user2));

        assertTrue(NexusAPI.deleteNexus(user2));
        assertFalse(NexusAPI.exists(user2));
        assertNull(NexusAPI.getNexusRepository(user2));
    }

    @Test
    @DisplayName("Test APIUtil#createJenkinsJob")
    public void testJenkins() {
        String user1 = "TestJenkins1";
        String j1 = "Job";
        Member u1 = MockJDA.mockMember(user1);
        String p1 = APIUtil.newPassword();
        InteractionHook h1 = MockJDA.mockInteractionHook(u1, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        assertTrue(APIUtil.createJenkinsJob(h1, user1, p1, j1, "https://github.com/gmitch215/SocketMC", false));
        assertTrue(JenkinsAPI.existsUser(user1));
        assertNotNull(JenkinsAPI.getJobInfo(user1, j1));

        assertTrue(JenkinsAPI.deleteJob(user1, j1));
        assertNull(JenkinsAPI.getJobInfo(user1, j1));
        assertTrue(JenkinsAPI.deleteUser(user1));
        assertFalse(JenkinsAPI.existsUser(user1));

        String user2 = "TestJenkins2";
        String j2 = "Job";
        Member u2 = MockJDA.mockMember(user2);
        String p2 = APIUtil.newPassword();
        InteractionHook h2 = MockJDA.mockInteractionHook(u2, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        assertTrue(APIUtil.createJenkinsJob(h2, user2, p2, j2, "https://github.com/CodeMC/Bot", false));
        assertTrue(JenkinsAPI.existsUser(user2));
        assertNotNull(JenkinsAPI.getJobInfo(user2, j2));

        assertTrue(JenkinsAPI.deleteJob(user2, j2));
        assertNull(JenkinsAPI.getJobInfo(user2, j2));
        assertTrue(JenkinsAPI.deleteUser(user2));
        assertFalse(JenkinsAPI.existsUser(user2));

        String user3 = "TestJenkins3";
        String j3 = "Job";
        Member u3 = MockJDA.mockMember(user3);
        String p3 = APIUtil.newPassword();
        InteractionHook h3 = MockJDA.mockInteractionHook(u3, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        JenkinsAPI.createJenkinsUser(user3, p3, false);
        APIUtil.createJenkinsJob(h3, user3, p3, j3, "https://github.com/CodeMC/Bot", false);

        MockJDA.assertEmbeds(
            List.of(CommandUtil.embedError("Jenkins User for " + user3 + " already exists!")), 
            MockJDA.getEmbeds(h3.getIdLong()),
            true
        );

        assertTrue(JenkinsAPI.deleteUser(user3));
    }

    @Test
    @DisplayName("Test APIUtil#changePassword")
    public void testChangePassword() {
        String u1 = "TestChangePassword1";
        String j1 = "Job";
        Member user1 = MockJDA.mockMember(u1);
        InteractionHook h1 = MockJDA.mockInteractionHook(user1, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        String old1 = APIUtil.newPassword();
        assertTrue(APIUtil.createNexus(h1, u1, old1));
        assertTrue(APIUtil.createJenkinsJob(h1, u1, old1, j1, "https://github.com/CodeMC/API", false));

        String new1 = APIUtil.newPassword();
        assertTrue(APIUtil.changePassword(h1, u1, new1));

        assertTrue(JenkinsAPI.deleteJob(u1, j1));
        assertTrue(JenkinsAPI.deleteUser(u1));
        assertTrue(NexusAPI.deleteNexus(u1));

        String u2 = "TestChangePassword2";
        Member user2 = MockJDA.mockMember(u2);
        InteractionHook h2 = MockJDA.mockInteractionHook(user2, MockJDA.REQUEST_CHANNEL, InteractionType.MODAL_SUBMIT);

        String old2 = APIUtil.newPassword();
        assertTrue(APIUtil.createNexus(h2, u2, old2));

        String new2 = APIUtil.newPassword();
        APIUtil.changePassword(h2, u2, new2);

        MockJDA.assertEmbeds(
            List.of(CommandUtil.embedError("Failed to change Jenkins Password for " + u2 + "!")), 
            MockJDA.getEmbeds(h2.getIdLong()), 
            false
        );

        assertTrue(NexusAPI.deleteNexus(u2));
    }

}
