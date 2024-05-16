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
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.Arrays;

public class CmdMsg extends BotCommand{
    
    public CmdMsg(CodeMCBot bot){
        super(bot);
        
        this.name = "msg";
        this.help = "Sends a message in a specified channel or edits one.";
        
        this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "msg");
        
        this.children = new SlashCommand[]{
            new Post(bot),
            new Edit(bot)
        };
    }
    
    @Override
    public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){}
    
    @Override
    public void withModalReply(SlashCommandEvent event){}
    
    private static class Post extends BotCommand{
        
        public Post(CodeMCBot bot){
            super(bot);
            
            this.name = "send";
            this.help = "Sends a message as the Bot.";
            
            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "msg");
            this.hasModalReply = true;
            
            this.options = Arrays.asList(
                new OptionData(OptionType.CHANNEL, "channel", "The channel to sent the message in.")
                    .setRequired(true)
                    .setChannelTypes(ChannelType.TEXT),
                new OptionData(OptionType.BOOLEAN, "embed", "Whether to sent the message as an embed or not.")
            );
        }
        
        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){
            
        }
        
        @Override
        public void withModalReply(SlashCommandEvent event){
            TextChannel channel = event.getOption("channel", null, option -> option.getAsChannel().asTextChannel());
            boolean asEmbed = event.getOption("embed", false, OptionMapping::getAsBoolean);
            
            if(channel == null){
                CommandUtil.EmbedReply.fromCommandEvent(event)
                    .withError("Received invalid Channel input.")
                    .send();
                return;
            }
            
            TextInput input = TextInput.create("message", "Message", TextInputStyle.PARAGRAPH)
                .setMaxLength(asEmbed ? MessageEmbed.DESCRIPTION_MAX_LENGTH : Message.MAX_CONTENT_LENGTH)
                .setRequired(true)
                .build();
            
            Modal modal = Modal.create("message:post:" + channel.getId() + ":" + asEmbed, "Send Message")
                .addComponents(ActionRow.of(input))
                .build();
            
            event.replyModal(modal).queue();
        }
    }
    
    private static class Edit extends BotCommand{
        
        public Edit(CodeMCBot bot){
            super(bot);
            
            this.name = "edit";
            this.help = "Edit an existing message of the bot.";
            
            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "msg");
            this.hasModalReply = true;
            
            this.options = Arrays.asList(
                new OptionData(OptionType.CHANNEL, "channel", "The channel to edit the message in.")
                    .setRequired(true)
                    .setChannelTypes(ChannelType.TEXT),
                new OptionData(OptionType.STRING, "id", "The ID of the message to edit.")
                    .setRequired(true),
                new OptionData(OptionType.BOOLEAN, "embed", "Whether to sent the message as an embed or not.")
            );
        }
        
        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){}
        
        @Override
        public void withModalReply(SlashCommandEvent event){
            TextChannel channel = event.getOption("channel", null, option -> option.getAsChannel().asTextChannel());
            long messageId = event.getOption("id", -1L, option -> {
                try{
                    return Long.parseLong(option.getAsString());
                }catch(NumberFormatException ex){
                    return -1L;
                }
            });
            boolean asEmbed = event.getOption("embed", false, OptionMapping::getAsBoolean);
            
            if(channel == null || messageId == -1L){
                CommandUtil.EmbedReply.fromCommandEvent(event)
                    .withError("Received invalid Channel or Message ID.")
                    .send();
                return;
            }
            
            TextInput input = TextInput.create("message", "Message", TextInputStyle.PARAGRAPH)
                .setMaxLength(asEmbed ? 4000 : Message.MAX_CONTENT_LENGTH)
                .setRequired(true)
                .build();
            
            Modal modal = Modal.create("message:edit:" + channel.getId() + ":" + asEmbed + ":" + messageId, "Send Message")
                .addComponents(ActionRow.of(input))
                .build();
            
            event.replyModal(modal).queue();
        }
    }
}
