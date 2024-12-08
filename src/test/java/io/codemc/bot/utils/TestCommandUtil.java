package io.codemc.bot.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codemc.bot.MockCodeMCBot;
import io.codemc.bot.MockJDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.InteractionType;

public class TestCommandUtil {

    private static final long AUTHOR_ROLE = MockCodeMCBot.INSTANCE.getConfigHandler().getLong("author_role");
    private static final long ADMIN_ROLE = 405917902865170453L;
    private static final long MAINTAINER_ROLE = 659568973079379971L;
    
    @Test
    @DisplayName("Test CommandUtil#hasRole")
    public void testHasRole() {
        Member m1 = MockJDA.mockMember("gmitch215");
        
        assertFalse(CommandUtil.hasRole(m1, List.of(AUTHOR_ROLE)));

        MockJDA.GUILD.addRoleToMember(m1, MockJDA.AUTHOR);

        assertTrue(CommandUtil.hasRole(m1, List.of(AUTHOR_ROLE)));

        Member m2 = MockJDA.mockMember("sgdc3");

        assertFalse(CommandUtil.hasRole(m2, List.of(ADMIN_ROLE)));
        assertFalse(CommandUtil.hasRole(m2, List.of(MAINTAINER_ROLE)));

        MockJDA.GUILD.addRoleToMember(m2, MockJDA.ADMINISTRATOR);
        MockJDA.GUILD.addRoleToMember(m2, MockJDA.MAINTAINER);

        assertTrue(CommandUtil.hasRole(m2, List.of(ADMIN_ROLE, MAINTAINER_ROLE)));
    }

    @Test
    @DisplayName("Test CommandUtil.EmbedReply#build")
    public void testEmbedReply() {
        CommandUtil.EmbedReply<?> r1 = CommandUtil.EmbedReply.empty();
        MessageEmbed m1 = r1.success("Success!").build();

        MockJDA.assertEmbed(m1, CommandUtil.embedSuccess("Success!"), true);

        CommandUtil.EmbedReply<?> r2 = CommandUtil.EmbedReply.empty();
        MessageEmbed m2 = r2.error("Error!").build();

        MockJDA.assertEmbed(m2, CommandUtil.embedError("Error!"), true);
    }

    @Test
    @DisplayName("Test CommandUtil.EmbedReply#send")
    public void testEmbedSend() {
        Member member = MockJDA.mockMember("gmitch215");
        InteractionHook h1 = MockJDA.mockInteractionHook(member, MockJDA.REQUEST_CHANNEL, InteractionType.COMMAND);
        CommandUtil.EmbedReply<?> r1 = CommandUtil.EmbedReply.from(h1);
        r1.success("Success!").send();
        MockJDA.assertEmbeds(
            List.of(CommandUtil.embedSuccess("Success!")),
            MockJDA.getEmbeds(h1.getIdLong()),
            true    
        );

        CommandUtil.EmbedReply<?> r2 = CommandUtil.EmbedReply.empty();
        r2.error("Error!").send();
    }

}
