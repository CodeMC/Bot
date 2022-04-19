/*
 * Copyright 2021 CodeMC.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.codemc.bot.commands;

import ch.qos.logback.classic.Logger;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.bot.utils.CommandUtil;
import io.codemc.bot.utils.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static io.codemc.bot.CodeMCBot.eventWaiter;

public class CmdMsg extends SlashCommand{
    
    private static final Logger logger = (Logger)LoggerFactory.getLogger("Message Handler");
    
    public CmdMsg(){
        this.name = "msg";
        this.help = "Sends a message in a specified channel or edits one.";
        
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{
            Constants.ADMINISTRATOR,
            Constants.MODERATOR
        };
        
        this.children = new SlashCommand[]{
            new Send(),
            new Edit()
        };
    }
    
    @Override
    protected void execute(SlashCommandEvent event){}
    
    // Method to handle the command types and reduce duplicate code.
    private static void handleCommand(SlashCommandEvent event, boolean isEdit){
        boolean isEmbed = CommandUtil.getBoolean(event, "embed", false);
        
        TextChannel currentChannel = event.getTextChannel();
        TextChannel targetChannel = CommandUtil.getChannel(event, "channel");
        
        String messageId = CommandUtil.getString(event, "message-id");
        
        if(targetChannel == null){
            CommandUtil.EmbedReply.fromEvent(event)
                .withError("The provided TextChannel was null.")
                .send();
            return;
        }
        
        if(isEdit && (messageId == null)){
            CommandUtil.EmbedReply.fromEvent(event)
                .withError("The provided Message ID was null.")
                .send();
            return;
        }
    
        Guild guild = targetChannel.getGuild();
        if(CommandUtil.lackPerms(event, guild, targetChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY))
            return;
        
        if(isEmbed && CommandUtil.lackPerms(event, guild, targetChannel, Permission.MESSAGE_EMBED_LINKS))
            return;
        
        event.reply(
            "Please provide the message to use for the message.\n" +
            "\n" +
            "> **The request will time out in 5 minutes!**"
        ).queue(hook -> handleResponse(hook, targetChannel, isEmbed, currentChannel.getId(), event.getUser().getId(), messageId));
    }
    
    private static void handleResponse(InteractionHook hook, TextChannel targetChannel, boolean isEmbed, String currentChannelId,
                                       String userId, String messageId){
        eventWaiter.waitForEvent(
            MessageReceivedEvent.class,
            event -> {
                if(!event.isFromGuild())
                    return false;
                
                User user = event.getAuthor();
                if(!user.getId().equals(userId) || user.isBot())
                    return false;
                
                Message message = event.getMessage();
                if(message.getType() != MessageType.DEFAULT)
                    return false;
                
                return event.getChannel().getId().equals(currentChannelId);
            },
            event -> {
                String message = event.getMessage().getContentRaw();
                User user = event.getAuthor();
                
                MessageBuilder builder = new MessageBuilder();
                if(isEmbed){
                    builder.append(EmbedBuilder.ZERO_WIDTH_SPACE)
                        .setEmbeds(
                            CommandUtil.getEmbed()
                                .setDescription(message)
                                .build()
                        );
                }else{
                    builder.append(message);
                }
                
                if(messageId != null){
                    targetChannel.retrieveMessageById(messageId)
                        .flatMap(msg -> msg.editMessage(builder.build()).override(true))
                        .queue(
                            m -> {
                                CommandUtil.EmbedReply.fromHook(hook)
                                    .withMessage("Successfully [edited message](" + m.getJumpUrl() + ")!")
                                    .asSuccess()
                                    .send();
                                event.getMessage().addReaction("✅").queue();
                                
                                logger.info("{} edited a message with ID {}", user.getAsTag(), messageId);
                            },
                            e -> {
                                CommandUtil.EmbedReply.fromHook(hook)
                                    .withIssue(
                                        "Cannot edit provided Message. Is it even one of my own?",
                                        "",
                                        "Reason: `" + e.getMessage() + "`"
                                    )
                                    .send();
                                event.getMessage().addReaction("❌").queue();
                            }
                        );
                }else{
                    targetChannel.sendMessage(builder.build()).queue(
                        m -> {
                            CommandUtil.EmbedReply.fromHook(hook)
                                .withMessage("Successfully [send new Message](" + m.getJumpUrl() + ")!")
                                .asSuccess()
                                .send();
                            event.getMessage().addReaction("✅").queue();
                            
                            logger.info("{} send a message through the bot in {}.", user.getAsTag(), event.getTextChannel().getName());
                        },
                        e -> {
                            CommandUtil.EmbedReply.fromHook(hook)
                                .withIssue(
                                    "Cannot send new message to target channel.",
                                    "",
                                    "Reason: `" + e.getMessage() + "`"
                                )
                                .send();
                            event.getMessage().addReaction("❌").queue();
                        }
                    );
                }
            },
            5, TimeUnit.MINUTES,
            () -> hook.editOriginal("You took too long and the timer ran out!").queue()
        );
    }
    
    private static class Send extends SlashCommand{
        
        public Send(){
            this.name = "send";
            this.help = "Sends a message or embed to a specific channel";
            
            this.defaultEnabled = false;
            this.enabledRoles = new String[]{
                Constants.ADMINISTRATOR,
                Constants.MODERATOR
            };
            
            this.options = Arrays.asList(
                new OptionData(OptionType.BOOLEAN, "embed", "Should the message be send in an embed?")
                    .setRequired(true),
                new OptionData(OptionType.CHANNEL, "channel", "The channel to send the message in.")
                    .setRequired(true)
                    .setChannelTypes(ChannelType.TEXT)
            );
        }
        
        @Override
        protected void execute(SlashCommandEvent event){
            handleCommand(event, false);
        }
    }
    
    private static class Edit extends SlashCommand{
        
        public Edit(){
            this.name = "edit";
            this.help = "Edit an existing message of the bot.";
            
            this.defaultEnabled = false;
            this.enabledRoles = new String[]{
                Constants.ADMINISTRATOR,
                Constants.MODERATOR
            };
            
            this.options = Arrays.asList(
                new OptionData(OptionType.BOOLEAN, "embed", "Should the message be send in an embed?")
                    .setRequired(true),
                new OptionData(OptionType.CHANNEL, "channel", "The channel to send the message in.")
                    .setRequired(true)
                    .setChannelTypes(ChannelType.TEXT),
                new OptionData(OptionType.STRING, "message-id", "The ID of the message to edit.")
                    .setRequired(true)
            );
        }
        
        @Override
        protected void execute(SlashCommandEvent event){
            handleCommand(event, true);
        }
    }
}
