package io.codemc.bot;

import dev.coly.jdat.JDAObjects;
import dev.coly.util.Callback;
import io.codemc.bot.config.ConfigHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.InteractionType;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockJDA {

    private static final ConfigHandler CONFIG = MockCodeMCBot.INSTANCE.getConfigHandler();

    public static final Guild GUILD = mockGuild();

    public static final TextChannel REQUEST_CHANNEL = mockChannel("request_access");
    public static final TextChannel ACCEPTED_CHANNEL = mockChannel("accepted_requests");
    public static final TextChannel REJECTED_CHANNEL = mockChannel("rejected_requests");
    public static final List<TextChannel> CHANNELS = List.of(REQUEST_CHANNEL, ACCEPTED_CHANNEL, REJECTED_CHANNEL);

    public static final Role ADMINISTRATOR = mockRole("Administrator", 405917902865170453L, 4);
    public static final Role MAINTAINER = mockRole("Maintainer", 659568973079379971L, 3);
    public static final Role REVIEWER = mockRole("Reviewer", 1233971297185431582L, 2);
    public static final Role AUTHOR = mockRole("Author", CONFIG.getLong("author_role"), 1);
    public static final List<Role> ROLES = List.of(ADMINISTRATOR, MAINTAINER, REVIEWER, AUTHOR);

    public static InteractionHook mockInteractionHook(Member user, MessageChannel channel, InteractionType type) {
        InteractionHook hook = mock(InteractionHook.class);
        when(hook.getJDA()).thenReturn(JDAObjects.getJDA());
        when(hook.getExpirationTimestamp()).thenReturn(0L);

        Interaction interaction = mock(Interaction.class);
        when(interaction.getJDA()).thenReturn(JDAObjects.getJDA());
        when(interaction.getChannel()).thenReturn(channel);
        when(interaction.getChannelIdLong()).thenReturn(channel.getIdLong());
        when(interaction.getMember()).thenReturn(user);
        when(interaction.getUser()).thenReturn(user.getUser());
        when(interaction.getGuild()).thenReturn(GUILD);
        when(interaction.getTypeRaw()).thenReturn(type.getKey());

        when(hook.getInteraction()).thenReturn(interaction);
        return hook;
    }

    public static TextChannel mockChannel(String configName) {
        long id = CONFIG.getLong("channels", configName);
        return (TextChannel) JDAObjects.getMessageChannel(configName.replace('_', '-'), id, Callback.single());
    }

    private static Guild mockGuild() {
        Guild guild = mock(Guild.class);
        long serverId = CONFIG.getLong("server");

        when(guild.getIdLong()).thenReturn(serverId);
        when(guild.getJDA()).thenReturn(JDAObjects.getJDA());

        when(guild.addRoleToMember(any(UserSnowflake.class), any(Role.class))).thenAnswer(inv -> {
            Member member = JDAObjects.getMember(inv.getArgument(0), "0000");
            Role role = inv.getArgument(1);

            member.getRoles().add(role);
            return null;
        });
        when(guild.getRoleById(any(Long.class))).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return ROLES.stream().filter(role -> role.getIdLong() == id).findFirst().orElse(null);
        });
        when(guild.getChannelById(any(), any(Long.class))).thenAnswer(inv -> {
            long id = inv.getArgument(1);
            return CHANNELS.stream().filter(channel -> channel.getIdLong() == id).findFirst().orElse(null);
        });

        return guild;
    }

    public static Member mockMember(String username) {
        Member member = JDAObjects.getMember(username, "0000");

        when(member.getGuild()).thenReturn(GUILD);

        List<Role> roles = new ArrayList<>();
        when(member.getRoles()).thenReturn(roles);

        return member;
    }

    private static Role mockRole(String name, long id, int position) {
        Role role = mock(Role.class);

        when(role.getJDA()).thenReturn(JDAObjects.getJDA());
        when(role.getName()).thenReturn(name);
        when(role.getIdLong()).thenReturn(id);
        when(role.getGuild()).thenReturn(GUILD);
        when(role.getColorRaw()).thenReturn(0);
        when(role.getPosition()).thenReturn(position);
        when(role.getPositionRaw()).thenReturn(0);

        return role;
    }

}
