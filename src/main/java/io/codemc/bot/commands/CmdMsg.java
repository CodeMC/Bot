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
import io.codemc.bot.utils.Constants;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Arrays;

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
        TextChannel channel = bot.getCommandUtil().getChannel(event, "channel");
        String message = bot.getCommandUtil().getString(event, "message");
        
        String messageId = bot.getCommandUtil().getString(event, "message-id");
        
        if(channel == null || message == null){
            bot.getCommandUtil().sendError(event, "The provided channel or message were null!");
            return;
        }
        
        if(isEdit && (messageId == null)){
            bot.getCommandUtil().sendError(event, "The provided message id was null!");
            return;
        }
    
        Guild guild = channel.getGuild();
        if(!guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE)){
            bot.getCommandUtil().sendError(event, "I lack the `Send Message` Permission for the selected channel!");
            return;
        }
        
        if(!guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_HISTORY)){
            bot.getCommandUtil().sendError(event, "I lack the `View Channel History` Permission for the selected channel!");
            return;
        }
    
        MessageBuilder builder = new MessageBuilder();
        if(isEmbed){
            if(!guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)){
                bot.getCommandUtil().sendError(event, "I lack the `Embed Links` Permission for the selected channel!");
                return;
            }
            
            builder.setEmbeds(
                bot.getCommandUtil().getEmbed()
                    .setDescription(message.replace("{n}", "\n"))
                    .build()
            );
        }else{
            builder.append(message.replace("{n}", "\n"));
        }
        
        if(isEdit){
            channel.retrieveMessageById(messageId)
                .flatMap(msg -> msg.editMessage(builder.build()).override(true))
                .queue(
                    m -> event.reply("Successfully edited [message](" + m.getJumpUrl() + ")!").queue(),
                    e -> event.reply("Unable to edit existing message! Is it even mine?").setEphemeral(true).queue()
                );
        }else{
            channel.sendMessage(builder.build()).queue(
                m -> event.reply("Successfully send [new message](" + m.getJumpUrl() + ")!").queue(),
                e -> event.reply("Unable to send message in selected channel!").setEphemeral(true).queue()
            );
        }
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
                    .setChannelTypes(ChannelType.TEXT),
                new OptionData(OptionType.STRING, "message", "The message to send. Use {n} for new lines.")
                    .setRequired(true)
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
                new OptionData(OptionType.STRING, "message-id", "The ID of the message to edit.").setRequired(true),
                new OptionData(OptionType.STRING, "message", "The message to send. Use {n} for new lines.")
                    .setRequired(true)
            );
        }
        
        @Override
        protected void execute(SlashCommandEvent event){
            handleCommand(event, true, bot);
        }
    }
}
