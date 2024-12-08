package io.codemc.bot;

import dev.coly.jdat.JDAObjects;
import dev.coly.util.Callback;
import io.codemc.bot.commands.BotCommand;
import io.codemc.bot.commands.TestCommandListener;
import io.codemc.bot.config.ConfigHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;

import com.jagrosh.jdautilities.command.SlashCommandEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class MockJDA {

    private static final ConfigHandler CONFIG = MockCodeMCBot.INSTANCE.getConfigHandler();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Map<Long, String> messages = new HashMap<>();
    private static final Map<Long, MessageEmbed[]> embeds = new HashMap<>();
    private static final Map<Long, Member> members = new HashMap<>();
    private static final Map<Long, String> latestMessages = new HashMap<>();
    private static final Map<Long, MessageEmbed[]> latestEmbeds = new HashMap<>();

    public static final JDA JDA = JDAObjects.getJDA();
    public static final Member SELF = mockMember("Bot");
    public static final Guild GUILD = mockGuild();
    public static Modal CURRENT_MODAL = null;

    public static final TextChannel GENERAL = mockChannel("general"); // Not found, let it be -1
    public static final TextChannel REQUEST_CHANNEL = mockChannel("request_access");
    public static final TextChannel ACCEPTED_CHANNEL = mockChannel("accepted_requests");
    public static final TextChannel REJECTED_CHANNEL = mockChannel("rejected_requests");
    public static final List<TextChannel> CHANNELS = List.of(GENERAL, REQUEST_CHANNEL, ACCEPTED_CHANNEL, REJECTED_CHANNEL);

    public static final Role ADMINISTRATOR = mockRole("Administrator", 405917902865170453L, 4);
    public static final Role MAINTAINER = mockRole("Maintainer", 659568973079379971L, 3);
    public static final Role REVIEWER = mockRole("Reviewer", 1233971297185431582L, 2);
    public static final Role AUTHOR = mockRole("Author", CONFIG.getLong("author_role"), 1);
    public static final List<Role> ROLES = List.of(ADMINISTRATOR, MAINTAINER, REVIEWER, AUTHOR);

    public static String getMessage(long id) {
        return messages.get(id);
    }

    public static List<MessageEmbed> getEmbeds(long id) {
        return Arrays.asList(embeds.get(id));
    }

    public static String getLatestMessage(MessageChannel channel) {
        return latestMessages.get(channel.getIdLong());
    }

    public static List<MessageEmbed> getLatestEmbeds(MessageChannel channel) {
        return Arrays.asList(latestEmbeds.get(channel.getIdLong()));
    }

    public static InteractionHook mockInteractionHook(Member user, MessageChannel channel, InteractionType type) {
        return mockInteractionHook(user, channel, type, RANDOM.nextLong(0, Long.MAX_VALUE));
    }

    public static InteractionHook mockInteractionHook(Member user, MessageChannel channel, InteractionType type, long id) {
        return mockInteractionHook(mockInteraction(user, channel, type, id));
    }

    public static InteractionHook mockInteractionHook(Interaction interaction) {
        InteractionHook hook = mock(InteractionHook.class);
        when(hook.getJDA()).thenReturn(JDA);
        when(hook.getExpirationTimestamp()).thenReturn(0L);
        when(hook.getIdLong()).thenAnswer(inv -> interaction.getIdLong());

        when(hook.getInteraction()).thenReturn(interaction);

        MessageChannel channel = interaction.getMessageChannel();

        when(hook.editOriginal(anyString())).thenAnswer(inv -> {
            String content = inv.getArgument(0);
            messages.put(hook.getIdLong(), content);
            return mockWebhookReply(WebhookMessageEditAction.class, hook, mockMessage(content, channel, hook.getIdLong()));
        });
        when(hook.editOriginalFormat(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String content = String.format(inv.getArgument(0), (Object[]) inv.getRawArguments()[1]);
            messages.put(hook.getIdLong(), content);
            return mockWebhookReply(WebhookMessageEditAction.class, hook, mockMessage(content, channel, hook.getIdLong()));
        });

        when(hook.editOriginalEmbeds(any(MessageEmbed[].class))).thenAnswer(inv -> {
            Object obj = inv.getArgument(0);
            if (obj instanceof MessageEmbed[] allEmbeds)
                embeds.put(hook.getIdLong(), allEmbeds);
            else
                embeds.put(hook.getIdLong(), new MessageEmbed[] { (MessageEmbed) obj });
            
            return mockWebhookReply(WebhookMessageEditAction.class, hook, mockMessage(null, Arrays.asList(embeds.get(hook.getIdLong())), channel, hook.getIdLong()));
        });

        when(hook.sendMessageEmbeds(any(), any(MessageEmbed[].class))).thenAnswer(inv -> {
            MessageEmbed first = inv.getArgument(0);

            List<MessageEmbed> embeds = new ArrayList<>();
            embeds.add(first);
            if (inv.getArguments().length > 1) {
                Object obj = inv.getArgument(1);

                if (obj instanceof MessageEmbed[] allEmbeds)
                    embeds.addAll(Arrays.asList(allEmbeds));
                else
                    embeds.add((MessageEmbed) obj);
            }
            
            return mockWebhookReply(WebhookMessageCreateAction.class, hook, mockMessage(null, embeds, channel, hook.getIdLong()));
        });
        
        return hook;
    }

    public static Interaction mockInteraction(Member user, MessageChannel channel, InteractionType type, long id) {
        Interaction interaction = mock(Interaction.class);

        when(interaction.getJDA()).thenReturn(JDA);
        when(interaction.getChannel()).thenReturn(channel);
        when(interaction.getMessageChannel()).thenReturn(channel);

        when(interaction.getMember()).thenReturn(user);
        when(interaction.getUser()).thenAnswer(inv -> user.getUser());
        when(interaction.getGuild()).thenReturn(GUILD);
        when(interaction.getTypeRaw()).thenReturn(type.getKey());
        when(interaction.getIdLong()).thenReturn(id);

        return interaction;
    }

    private static MessageChannelUnion mockUnion(MessageChannel channel) {
        MessageChannelUnion union = mock(MessageChannelUnion.class);
        when(union.asTextChannel()).thenAnswer(inv -> channel);
        when(union.getJDA()).thenReturn(JDA);
        
        return union;
    }

    public static TextChannel mockChannel(String configName) {
        long id = CONFIG.getLong("channels", configName);
        TextChannel channel = (TextChannel) JDAObjects.getMessageChannel(configName.replace('_', '-'), id, Callback.single());

        when(channel.getGuild()).thenReturn(GUILD);
        when(channel.getJDA()).thenReturn(JDA);
        when(channel.canTalk()).thenReturn(true);

        when(channel.retrieveMessageById(anyLong())).thenAnswer(inv -> {
            long messageId = inv.getArgument(0);
            String content = messages.get(messageId);
            Message message = mockMessage(
                content, Arrays.asList(embeds.getOrDefault(messageId, new MessageEmbed[0])), channel, messageId
            );

            return mockAction(message);
        });
        when(channel.sendMessage(any(CharSequence.class))).thenAnswer(inv -> {
            String content = inv.getArgument(0);
            return mockReply(MessageCreateAction.class, mockMessage(content, channel));
        });
        when(channel.sendMessage(any(MessageCreateData.class))).thenAnswer(inv -> {
            MessageCreateData data = inv.getArgument(0, MessageCreateData.class);
            String content = data.getContent();
            List<MessageEmbed> embeds = data.getEmbeds();

            return mockReply(MessageCreateAction.class, mockMessage(content, embeds, channel));
        });
        when(channel.sendMessageFormat(anyString(), any(Object[].class))).thenAnswer(inv -> {
            String content = String.format(inv.getArgument(0), (Object[]) inv.getRawArguments()[1]);
            return mockReply(MessageCreateAction.class, mockMessage(content, channel));
        });
        when(channel.sendMessageEmbeds(any(), any(MessageEmbed[].class))).thenAnswer(inv -> {
            MessageEmbed first = inv.getArgument(0);

            List<MessageEmbed> embeds = new ArrayList<>();
            embeds.add(first);
            if (inv.getArguments().length > 1) {
                Object obj = inv.getArgument(1);

                if (obj instanceof MessageEmbed[] allEmbeds)
                    embeds.addAll(Arrays.asList(allEmbeds));
                else
                    embeds.add((MessageEmbed) obj);
            }
            
            return mockReply(MessageCreateAction.class, mockMessage(null, embeds, channel));
        });

        return channel;
    }

    public static Message mockMessage(String content, MessageChannel channel) {
        return mockMessage(content, channel, RANDOM.nextLong(0, Long.MAX_VALUE));
    }

    public static Message mockMessage(String content, MessageChannel channel, long id) {
        Message message = JDAObjects.getMessage(content, channel);
        messages.put(message.getIdLong(), content);
        latestMessages.put(channel.getIdLong(), content);
        latestEmbeds.remove(channel.getIdLong());

        when(message.getContentRaw()).thenAnswer(inv -> messages.get(id));
        when(message.getJumpUrl()).thenReturn("<Jump URL>");
        when(message.getIdLong()).thenReturn(id);
        when(message.getId()).thenReturn(Long.toString(id));
        when(message.getGuild()).thenReturn(GUILD);
        when(message.getMember()).thenReturn(SELF);
        when(message.getChannel()).thenAnswer(inv -> mockUnion(channel));

        when(message.getStartedThread()).thenReturn(null);
        when(message.delete()).thenAnswer(inv -> {
            messages.remove(id);
            MockJDA.embeds.remove(id);
            return mockAuditLog();
        });

        when(message.editMessage(anyString())).thenAnswer(inv -> {
            messages.put(id, inv.getArgument(0));
            return mockReply(MessageEditAction.class, message);
        });
        when(message.editMessageEmbeds(any(MessageEmbed[].class))).thenAnswer(inv -> {
            Object obj = inv.getArgument(0);
            if (obj instanceof MessageEmbed[] allEmbeds)
                embeds.put(id, allEmbeds);
            else
                embeds.put(id, new MessageEmbed[] { (MessageEmbed) obj });
            
            return mockReply(MessageEditAction.class, message);
        });
        when(message.editMessageEmbeds(anyCollection())).thenAnswer(inv -> {
            Collection<MessageEmbed> allEmbeds = inv.getArgument(0);
            embeds.put(id, allEmbeds.toArray(new MessageEmbed[0]));
            return mockReply(MessageEditAction.class, message);
        });

        when(message.createThreadChannel(anyString())).thenAnswer(inv -> mock(ThreadChannelAction.class));
        when(message.addReaction(any())).thenAnswer(inv -> mockAction(null));
        
        return message;
    }

    public static Message mockMessage(String content, List<MessageEmbed> embeds, MessageChannel channel) {
        return mockMessage(content, embeds, channel, RANDOM.nextLong(0, Long.MAX_VALUE));
    }

    public static Message mockMessage(String content, List<MessageEmbed> embeds, MessageChannel channel, long id) {
        Message message = mockMessage(content, channel, id);
        MockJDA.embeds.put(message.getIdLong(), embeds.toArray(new MessageEmbed[0]));
        latestEmbeds.put(channel.getIdLong(), embeds.toArray(new MessageEmbed[0]));

        when(message.getEmbeds()).thenAnswer(inv -> Arrays.asList(MockJDA.embeds.get(message.getIdLong())));

        return message;
    }

    private static Guild mockGuild() {
        Guild guild = mock(Guild.class);
        long serverId = CONFIG.getLong("server");

        when(guild.getIdLong()).thenReturn(serverId);
        when(guild.getJDA()).thenReturn(JDA);
        when(guild.getTextChannels()).thenReturn(CHANNELS);
        when(guild.getRoles()).thenReturn(ROLES);
        when(guild.getSelfMember()).thenReturn(SELF);

        when(guild.addRoleToMember(any(UserSnowflake.class), any(Role.class))).thenAnswer(inv -> {
            Member member = inv.getArgument(0);
            Role role = inv.getArgument(1);

            member.getRoles().add(role);
            return mockAuditLog();
        });
        when(guild.removeRoleFromMember(any(UserSnowflake.class), any(Role.class))).thenAnswer(inv -> {
            Member member = inv.getArgument(0);
            Role role = inv.getArgument(1);

            member.getRoles().remove(role);
            return mockAuditLog();
        });
        when(guild.getRoleById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return ROLES.stream().filter(role -> role.getIdLong() == id).findFirst().orElse(null);
        });
        when(guild.getChannelById(any(), anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(1);
            return CHANNELS.stream().filter(channel -> channel.getIdLong() == id).findFirst().orElse(null);
        });
        when(guild.getTextChannelById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return CHANNELS.stream().filter(channel -> channel.getIdLong() == id).findFirst().orElse(null);
        });
        when(guild.getTextChannelById(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return CHANNELS.stream().filter(channel -> channel.getId().equals(id)).findFirst().orElse(null);
        });
        when(guild.getMemberById(anyString())).thenAnswer(inv -> {
            long id = Long.parseLong(inv.getArgument(0));
            return members.get(id);
        });
        when(guild.getMemberById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return members.get(id);
        });

        return guild;
    }

    public static Member mockMember(String username) {
        Member member = JDAObjects.getMember(username, "0000");

        long id = RANDOM.nextLong(0, Long.MAX_VALUE);
        members.put(id, member);

        when(member.getJDA()).thenReturn(JDA);
        when(member.getGuild()).thenReturn(GUILD);
        when(member.getId()).thenReturn(Long.toString(id));
        when(member.getIdLong()).thenReturn(id);
        when(member.getAsMention()).thenReturn("<@" + id + ">");

        List<Role> roles = new ArrayList<>();
        when(member.getRoles()).thenReturn(roles);
        when(member.getUser().getEffectiveName()).thenReturn(username);
        when(member.getUser().getIdLong()).thenReturn(id);
        when(member.getUser().getAsMention()).thenReturn("<@" + id + ">");

        return member;
    }

    private static Role mockRole(String name, long id, int position) {
        Role role = mock(Role.class);

        when(role.getJDA()).thenReturn(JDA);
        when(role.getName()).thenReturn(name);
        when(role.getIdLong()).thenReturn(id);
        when(role.getGuild()).thenReturn(GUILD);
        when(role.getColorRaw()).thenReturn(0);
        when(role.getPosition()).thenReturn(position);
        when(role.getPositionRaw()).thenReturn(0);

        return role;
    }

    public static void assertEmbeds(long id, List<MessageEmbed> expectedOutputs, boolean ignoreTimestamp) {
        assertEmbeds(expectedOutputs, Arrays.asList(embeds.getOrDefault(id, new MessageEmbed[0])), ignoreTimestamp);
    }
    

    public static void assertEmbeds(List<MessageEmbed> expectedOutputs, List<MessageEmbed> embeds, boolean ignoreTimestamp) {
        assertEquals(expectedOutputs.size(), embeds.size(), "Number of embeds");

        int i = 0;
        for (MessageEmbed embed : embeds) {
            MessageEmbed expectedOutput = expectedOutputs.get(i);
            assertEmbed(embed, expectedOutput, ignoreTimestamp);
            i++;
        }
    }

    public static void assertEmbed(MessageEmbed embed, MessageEmbed expectedOutput, boolean ignoreTimestamp) {
        Assertions.assertEquals(expectedOutput.getTitle(), embed.getTitle());
        Assertions.assertEquals(expectedOutput.getColor(), embed.getColor());
        Assertions.assertEquals(expectedOutput.getDescription(), embed.getDescription());
        Assertions.assertEquals(expectedOutput.getUrl(), embed.getUrl());
        if (expectedOutput.getAuthor() != null)
            Assertions.assertEquals(expectedOutput.getAuthor().getName(), Objects.requireNonNull(embed.getAuthor()).getName());
        
        if (expectedOutput.getFooter() != null) {
            Assertions.assertEquals(expectedOutput.getFooter().getText(), Objects.requireNonNull(embed.getFooter()).getText());
            Assertions.assertEquals(expectedOutput.getFooter().getIconUrl(), Objects.requireNonNull(embed.getFooter()).getIconUrl());
        }

        if (expectedOutput.getImage() != null)
            Assertions.assertEquals(expectedOutput.getImage().getUrl(), Objects.requireNonNull(embed.getImage()).getUrl());
        
        if (expectedOutput.getThumbnail() != null)
            Assertions.assertEquals(expectedOutput.getThumbnail().getUrl(), Objects.requireNonNull(embed.getThumbnail()).getUrl());
        
        int i = 0;
        for (MessageEmbed.Field field : embed.getFields()) {
            try {
                Assertions.assertEquals(expectedOutput.getFields().get(i).getName(), field.getName());
                Assertions.assertEquals(expectedOutput.getFields().get(i).getValue(), field.getValue());
                Assertions.assertEquals(expectedOutput.getFields().get(i).isInline(), field.isInline());
            } catch (IndexOutOfBoundsException e) {
                Assertions.fail("Too many fields in embed: " + field.getName() + " - '" + field.getValue() + "'");
            }
            i++;
        }

        if (!ignoreTimestamp)
            Assertions.assertEquals(expectedOutput.getTimestamp(), embed.getTimestamp());
    }

    public static long assertSlashCommandEvent(TestCommandListener listener, Map<String, Object> options, MessageEmbed... outputs) {
        BotCommand command = listener.getCommand();
        SlashCommandEvent event = mockSlashCommandEvent(GENERAL, command, options);
        return assertSlashCommandEvent(event, listener, outputs);
    }

    public static long assertSlashCommandEvent(SlashCommandEvent event, TestCommandListener listener, MessageEmbed... outputs) {
        listener.onEvent(event);

        if (outputs != null)
            assertEmbeds(event.getIdLong(), Arrays.asList(outputs), true);

        return event.getIdLong();
    }

    public static SlashCommandEvent mockSlashCommandEvent(MessageChannel channel, BotCommand command, Map<String, Object> options) {
        SlashCommandEvent event = mock(SlashCommandEvent.class);
        long id = RANDOM.nextLong(0, Long.MAX_VALUE);

        when(event.getName()).thenAnswer(invocation -> command.getName());
        when(event.getSubcommandName()).thenAnswer(invocation -> command.getName());
        when(event.getSubcommandGroup()).thenAnswer(invocation -> command.getSubcommandGroup());
        when(event.getChannel()).thenAnswer(invocation -> channel);
        when(event.getGuild()).thenAnswer(invocation -> GUILD);
        when(event.getIdLong()).thenReturn(id);

        Member user = mockMember("User");
        GUILD.addRoleToMember(user, AUTHOR);
        GUILD.addRoleToMember(user, REVIEWER);
        GUILD.addRoleToMember(user, MAINTAINER);
        GUILD.addRoleToMember(user, ADMINISTRATOR);

        when(event.getMember()).thenReturn(user);
        when(event.getUser()).thenAnswer(inv -> user.getUser());
        when(event.getOptions()).thenAnswer(invocation -> options);

        when(event.getOption(anyString())).thenAnswer(invocation -> {
            if (options == null) return null;

            OptionMapping mapping = mock(OptionMapping.class);
            Object option = options.get(invocation.getArgument(0));

            when(mapping.getAsAttachment()).thenReturn((Attachment) option);
            when(mapping.getAsString()).thenReturn((String) option);
            when(mapping.getAsBoolean()).thenReturn((Boolean) option);
            when(mapping.getAsLong()).thenReturn((Long) option);
            when(mapping.getAsInt()).thenReturn((Integer) option);
            when(mapping.getAsDouble()).thenReturn((Double) option);
            when(mapping.getAsMentionable()).thenReturn((IMentionable) option);
            when(mapping.getAsRole()).thenReturn((Role) option);
            when(mapping.getAsUser()).thenReturn((User) option);
            when(mapping.getAsMember()).thenReturn((Member) option);
            when(mapping.getAsChannel()).thenReturn((GuildChannelUnion) option);

            return mapping;
        });
        when(event.getOption(anyString(), any(), any())).thenAnswer(inv -> {
            if (options == null) return null;

            String key = inv.getArgument(0);
            Object def = inv.getArgument(1);

            return options.getOrDefault(key, def);
        });
        when(event.getOption(anyString(), isA(Object.class), any())).thenAnswer(inv -> {
            if (options == null) return null;

            String key = inv.getArgument(0);
            Object def = inv.getArgument(1);

            return options.getOrDefault(key, def);
        });

        mockReplyCallbacks(event, channel, id);
        when(event.replyModal(any(Modal.class))).thenAnswer(inv -> {
            CURRENT_MODAL = inv.getArgument(0);
            return mockModalReply(user, channel);
        });

        return event;
    }

    public static long assertButtonInteractionEvent(ListenerAdapter listener, Message message, Button button, MessageEmbed... outputs) {
        ButtonInteractionEvent event = mockButtonInteractionEvent(message, button);
        return assertButtonInteractionEvent(listener, event, outputs);
    }

    public static long assertButtonInteractionEvent(ListenerAdapter listener, ButtonInteractionEvent event, MessageEmbed... outputs) {
        listener.onButtonInteraction(event);

        if (outputs != null)
            assertEmbeds(event.getIdLong(), Arrays.asList(outputs), true);

        return event.getIdLong();
    }

    public static ButtonInteractionEvent mockButtonInteractionEvent(Message message, Button button) {
        ButtonInteractionEvent event = mock(ButtonInteractionEvent.class);
        TextChannel channel = message.getChannel().asTextChannel();
        long id = RANDOM.nextLong(0, Long.MAX_VALUE);

        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(GUILD);
        when(event.getMessageChannel()).thenReturn(channel);
        when(event.getButton()).thenReturn(button);
        when(event.isFromGuild()).thenReturn(true);

        Member user = mockMember("User");
        GUILD.addRoleToMember(user, AUTHOR);
        GUILD.addRoleToMember(user, REVIEWER);
        GUILD.addRoleToMember(user, MAINTAINER);
        GUILD.addRoleToMember(user, ADMINISTRATOR);

        when(event.getMember()).thenReturn(user);
        when(event.getUser()).thenAnswer(inv -> user.getUser());
        when(event.getIdLong()).thenReturn(id);

        when(event.getMessage()).thenReturn(message);
        when(event.getMessageId()).thenAnswer(inv -> message.getId());
        when(event.getMessageIdLong()).thenAnswer(inv -> message.getIdLong());

        mockReplyCallbacks(event, channel, id);
        when(event.replyModal(any(Modal.class))).thenAnswer(inv -> {
            CURRENT_MODAL = inv.getArgument(0);
            return mockModalReply(user, channel);
        });

        return event;
    }

    public static long assertModalInteractionEvent(ListenerAdapter listener, Modal modal, MessageChannel channel, Map<String, String> options, MessageEmbed... outputs) {
        ModalInteractionEvent event = mockModalInteractionEvent(modal, channel, options);
        return assertModalInteractionEvent(listener, event, outputs);
    }

    public static long assertModalInteractionEvent(ListenerAdapter listener, ModalInteractionEvent event, MessageEmbed... outputs) {
        listener.onModalInteraction(event);

        if (outputs != null)
            assertEmbeds(event.getIdLong(), Arrays.asList(outputs), true);

        return event.getIdLong();
    }

    public static ModalInteractionEvent mockModalInteractionEvent(Modal modal, MessageChannel channel, Map<String, String> options) {
        ModalInteractionEvent event = mock(ModalInteractionEvent.class);
        long id = RANDOM.nextLong(0, Long.MAX_VALUE);

        when(event.getJDA()).thenReturn(JDA);
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(GUILD);
        when(event.getModalId()).thenAnswer(inv -> modal.getId());
        when(event.getChannel()).thenAnswer(inv -> mockUnion(channel));
        when(event.getMessageChannel()).thenReturn(channel);

        when(event.getValue(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            if (!options.containsKey(key)) return null;

            ModalMapping mapping = mock(ModalMapping.class);
            when(mapping.getId()).thenReturn(key);
            when(mapping.getAsString()).thenReturn(options.get(key));

            return mapping;
        });

        Member user = mockMember("User");
        GUILD.addRoleToMember(user, AUTHOR);
        GUILD.addRoleToMember(user, REVIEWER);
        GUILD.addRoleToMember(user, MAINTAINER);
        GUILD.addRoleToMember(user, ADMINISTRATOR);

        when(event.getMember()).thenReturn(user);
        when(event.getUser()).thenAnswer(inv -> user.getUser());
        when(event.getIdLong()).thenReturn(id);

        mockReplyCallbacks(event, channel, id);
        return event;
    }

    public static Modal mockModal(String id, String title) {
        Modal modal = mock(Modal.class);
        when(modal.getId()).thenReturn(id);
        when(modal.getTitle()).thenReturn(title);

        return modal;
    }

    private static void mockReplyCallbacks(IReplyCallback event, MessageChannel channel, long id) {
        when(event.reply(anyString())).thenAnswer(invocation -> 
            mockReply(mockMessage(invocation.getArgument(0), channel, id)));
        when(event.reply(any(MessageCreateData.class))).thenAnswer(invocation ->
            mockReply(mockMessage(invocation.getArgument(0, MessageCreateData.class).getContent(), channel, id)));
        when(event.replyEmbeds(anyList())).thenAnswer(invocation ->
            mockReply(mockMessage(null, invocation.getArgument(0), channel, id)));
        when(event.replyEmbeds(any(MessageEmbed.class), any(MessageEmbed[].class))).thenAnswer(invocation -> {
            List<MessageEmbed> embeds = invocation.getArguments().length == 1 ? new ArrayList<>() : Arrays.asList(invocation.getArgument(1));
            embeds.add(invocation.getArgument(0));
            return mockReply(mockMessage(null, embeds, channel, id));
        });

        when(event.deferReply()).thenAnswer(inv -> mockReply(mockMessage(null, channel, id)));
        when(event.deferReply(anyBoolean())).thenAnswer(inv -> mockReply(mockMessage(null, channel, id)));
    }

    private static ReplyCallbackAction mockReply(Message message) {
        ReplyCallbackAction action = mock(ReplyCallbackAction.class);

        messages.put(message.getIdLong(), message.getContentRaw());
        embeds.put(message.getIdLong(), message.getEmbeds().toArray(new MessageEmbed[0]));
        
        when(action.getEmbeds()).thenAnswer(inv -> embeds.get(message.getIdLong()));

        when(action.setEmbeds(anyCollection())).thenAnswer(inv -> {
            Collection<MessageEmbed> embed = inv.getArgument(0);
            embeds.put(message.getIdLong(), embed.toArray(new MessageEmbed[0]));
            return action;
        });

        when(action.addEmbeds(anyCollection())).thenAnswer(inv -> {
            List<MessageEmbed> total = new ArrayList<>();
            total.addAll(Arrays.asList(embeds.get(message.getIdLong())));
            total.addAll(inv.getArgument(0));
            embeds.put(message.getIdLong(), total.toArray(new MessageEmbed[0]));
            return action;
        });

        doAnswer(inv -> {
            Consumer<InteractionHook> hookConsumer = inv.getArgument(0);
            hookConsumer.accept(mockInteractionHook(message.getMember(), message.getChannel(), InteractionType.COMMAND, message.getIdLong()));
            return null;
        }).when(action).queue(any());

        when(action.setEphemeral(anyBoolean())).thenAnswer(invocation -> {
            when(message.isEphemeral()).thenReturn(true);
            return action;
        });

        return action;
    }

    private static ModalCallbackAction mockModalReply(Member user, MessageChannel channel) {
        ModalCallbackAction action = mock(ModalCallbackAction.class);

        doAnswer(inv -> {
            Consumer<InteractionHook> hookConsumer = inv.getArgument(0);
            hookConsumer.accept(mockInteractionHook(user, channel, InteractionType.MODAL_SUBMIT));
            return null;
        }).when(action).queue(any());

        return action;
    }

    private static <T extends MessageRequest<?> & RestAction<?>> T mockWebhookReply(Class<T> clazz, InteractionHook hook, Message message) {
        T action = mock(clazz);

        doAnswer(inv -> {
            Consumer<InteractionHook> hookConsumer = inv.getArgument(0);
            hookConsumer.accept(hook);
            return null;
        }).when(action).queue(any());

        when(action.getEmbeds()).thenAnswer(inv -> embeds.get(message.getIdLong()));

        when(action.setEmbeds(anyCollection())).thenAnswer(inv -> {
            Collection<MessageEmbed> embed = inv.getArgument(0);
            embeds.put(message.getIdLong(), embed.toArray(new MessageEmbed[0]));
            return action;
        });

        when(action.setEmbeds(any(MessageEmbed[].class))).thenAnswer(inv -> {
            Object obj = inv.getArgument(0);

            if (obj instanceof MessageEmbed[] allEmbeds)
                embeds.put(message.getIdLong(), allEmbeds);
            else
                embeds.put(message.getIdLong(), new MessageEmbed[] { (MessageEmbed) obj });
            return action;
        });

        return action;
    }

    private static <T extends MessageRequest<?> & RestAction<?>> T mockReply(Class<T> clazz, Message message) {
        T action = mock(clazz);

        doAnswer(inv -> {
            Consumer<Message> messageConsumer = inv.getArgument(0);
            messageConsumer.accept(message);
            return null;
        }).when(action).queue(any());

        doAnswer(inv -> {
            Consumer<Message> messageConsumer = inv.getArgument(0);
            Consumer<Throwable> errorConsumer = inv.getArgument(1);

            try {
                messageConsumer.accept(message);
            } catch (Exception e) {
                errorConsumer.accept(e);
            }
            return null;
        }).when(action).queue(any(), any());

        when(action.getEmbeds()).thenAnswer(inv -> message.getEmbeds());
        when(action.setEmbeds(anyCollection())).thenAnswer(inv -> {
            Collection<MessageEmbed> embed = inv.getArgument(0);
            embeds.put(message.getIdLong(), embed.toArray(new MessageEmbed[0]));
            return action;
        });

        when(action.setActionRow(any(ItemComponent[].class))).thenAnswer(inv -> action);
        when(action.setActionRow(anyCollection())).thenAnswer(inv -> action);

        if (action instanceof MessageEditAction edit) {
            when(edit.setReplace(anyBoolean())).thenReturn(edit);
        }

        return action;
    }

    private static <T> RestAction<T> mockAction(T object) {
        RestAction<T> action = mock(RestAction.class);
        when(action.complete()).thenReturn(object);

        doAnswer(inv -> {
            Consumer<T> consumer = inv.getArgument(0);
            consumer.accept(object);
            return null;
        }).when(action).queue(any());

        doAnswer(inv -> {
            Consumer<T> consumer = inv.getArgument(0);
            Consumer<Throwable> error = inv.getArgument(1);

            try {
                consumer.accept(object);
            } catch (Exception e) {
                error.accept(e);
            }
            return null;
        }).when(action).queue(any(), any());

        return action;
    }

    private static AuditableRestAction<Void> mockAuditLog() {
        AuditableRestAction<Void> action = mock(AuditableRestAction.class);
        
        doAnswer(inv -> {
            Consumer<Void> consumer = inv.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(action).queue(any());

        doAnswer(inv -> {
            Consumer<Void> consumer = inv.getArgument(0);
            Consumer<Throwable> error = inv.getArgument(1);

            try {
                consumer.accept(null);
            } catch (Exception e) {
                error.accept(e);
            }
            return null;
        }).when(action).queue(any(), any());

        when(action.reason(any())).thenReturn(action);

        return action;
    }

}
