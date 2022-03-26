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

import com.jagrosh.jdautilities.command.SlashCommand;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.CommandUtil;
import io.codemc.bot.utils.Constants;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class CmdMsg extends SlashCommand{
    
    public CmdMsg(CodeMCBot bot){
        this.name = "msg";
        this.help = "Sends a message in a specified channel or edits one.";
        
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{
            Constants.ADMINISTRATOR,
            Constants.MODERATOR
        };
        
        this.children = new SlashCommand[]{
            new Send(bot),
            new Edit(bot)
        };
    }
    
    @Override
    protected void execute(SlashCommandEvent event){}
    
    // Method to handle the command types and reduce duplicate code.
    private static void handleCommand(SlashCommandEvent event, boolean isEdit, CodeMCBot bot){
        boolean isEmbed = bot.getCommandUtil().getBoolean(event, "embed", false);
        
        TextChannel currentChannel = event.getTextChannel();
        TextChannel targetChannel = bot.getCommandUtil().getChannel(event, "channel");
        
        String messageId = bot.getCommandUtil().getString(event, "message-id");
        
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
        if(bot.getCommandUtil().lackPerms(event, guild, targetChannel, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY))
            return;
        
        if(isEmbed && bot.getCommandUtil().lackPerms(event, guild, targetChannel, Permission.MESSAGE_EMBED_LINKS))
            return;
        
        event.reply(
            "Please provide the message to use for the message.\n" +
            "\n" +
            "> **The request will time out in 5 minutes!**"
        ).queue(hook -> handleResponse(hook, targetChannel, isEmbed, currentChannel.getId(), event.getUser().getId(), messageId, bot));
    }
    
    private static void handleResponse(InteractionHook hook, TextChannel targetChannel, boolean isEmbed, String currentChannelId,
                                       String userId, String messageId, CodeMCBot bot){
        bot.getEventWaiter().waitForEvent(
            GuildMessageReceivedEvent.class,
            event -> {
    
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
                
                MessageBuilder builder = new MessageBuilder();
                if(isEmbed){
                    builder.setEmbeds(
                        bot.getCommandUtil().getEmbed()
                            .setDescription(message)
                            .build()
                    );
                }else{
                    builder.append(message);
                }
                
                if(messageId != null){
                    targetChannel.retrieveMessageById(messageId)
                        .flatMap(msg -> msg.editMessage(builder.build()).setEmbeds(Collections.emptyList()))
                        .queue(
                            m -> CommandUtil.EmbedReply.fromHook(hook)
                                .withMessage("Successfully [edited message](" + m.getJumpUrl() + ")!")
                                .asSuccess()
                                .send(),
                            e -> CommandUtil.EmbedReply.fromHook(hook)
                                .withError(
                                    "Unable to edit message. Was it even from me?",
                                    "",
                                    "Error response: " + e.getMessage()
                                )
                                .send()
                        );
                }else{
                    targetChannel.sendMessage(builder.build()).queue(
                        m -> hook.editOriginal("Successfully send [new message](<" + m.getJumpUrl() + ">)!").queue(),
                        e -> hook.editOriginal("Unable to send new message in target channel!").queue()
                    );
                }
            },
            5, TimeUnit.MINUTES,
            () -> hook.editOriginal("You took too long and the timer ran out!").queue()
        );
    }
    
    private static class Send extends SlashCommand{
        
        private final CodeMCBot bot;
        
        public Send(CodeMCBot bot){
            this.bot = bot;
            
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
            handleCommand(event, false, bot);
        }
    }
    
    private static class Edit extends SlashCommand{
        
        private final CodeMCBot bot;
        
        public Edit(CodeMCBot bot){
            this.bot = bot;
            
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
            handleCommand(event, true, bot);
        }
    }
}
