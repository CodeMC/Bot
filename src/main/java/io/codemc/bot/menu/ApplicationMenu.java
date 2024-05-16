/*
 * Copyright 2024 CodeMC.io
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

package io.codemc.bot.menu;

import com.jagrosh.jdautilities.command.MessageContextMenu;
import com.jagrosh.jdautilities.command.MessageContextMenuEvent;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class ApplicationMenu{
    private static void handleEvent(CodeMCBot bot, MessageContextMenuEvent event, boolean accepted){
        Guild guild = event.getGuild();
        if(guild == null || guild.getIdLong() != bot.getConfigHandler().getLong("server")){
            event.reply("This Context Menu Action may only work in the CodeMC Server!")
                .setEphemeral(true)
                .queue();
            return;
        }
        
        MessageChannel channel = event.getChannel();
        if(channel == null || channel.getIdLong() != bot.getConfigHandler().getLong("channels", "request_access")){
            event.reply("This Context Menu Action may only work in the request-access channel of the CodeMC Server!")
                .setEphemeral(true)
                .queue();
            return;
        }
        
        Member member = event.getMember();
        if(member == null){
            event.reply("Unable to retrieve Member!").setEphemeral(true).queue();
            return;
        }
        
        if(!CommandUtil.hasRole(member, bot.getConfigHandler().getLongList("allowed_roles", "application"))){
            event.replyEmbeds(CommandUtil.getEmbed().setColor(0xFF0000)
                .setDescription("You do not have permissions to use this Context Menu Action!")
                .build()
            ).queue();
            return;
        }
        
        TextInput input;
        if(accepted){
            input = TextInput.create("text", "Project URL", TextInputStyle.SHORT)
                .setRequired(true)
                .setPlaceholder("https://ci.codemc.io/job/CodeMC/job/CodeMC-Discord-Bot/")
                .build();
        }else{
            input = TextInput.create("text", "Denial Reason", TextInputStyle.PARAGRAPH)
                .setRequired(true)
                .setPlaceholder("The project was denied because ...")
                .build();
        }
        
        Modal modal = Modal.create(
            "application:" + (accepted ? "accepted" : "denied") + ":" + event.getTarget().getId(),
            accepted ? "Accept Application" : "Deny Application"
            )
            .addComponents(ActionRow.of(input))
            .build();
        
        event.replyModal(modal).queue();
    }
    
    public static final class Accept extends MessageContextMenu{
        
        private final CodeMCBot bot;
        
        public Accept(CodeMCBot bot){
            this.bot = bot;
            
            this.name = "Accept this application";
        }
        
        @Override
        protected void execute(MessageContextMenuEvent event){
            handleEvent(bot, event, true);
        }
    }
    
    public static final class Deny extends MessageContextMenu{
        
        private final CodeMCBot bot;
        
        public Deny(CodeMCBot bot){
            this.bot = bot;
            
            this.name = "Deny this application";
        }
        
        @Override
        protected void execute(MessageContextMenuEvent event){
            handleEvent(bot, event, false);
        }
    }
}
