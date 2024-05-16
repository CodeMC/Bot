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

import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.bot.CodeMCBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class CmdSubmit extends BotCommand{
    
    public CmdSubmit(CodeMCBot bot){
        super(bot);
        
        this.name = "submit";
        this.help = "Submit a request to join the CodeMC CI with a project.";
        
        this.hasModalReply = true;
    }
    
    @Override
    public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){}
    
    @Override
    public void withModalReply(SlashCommandEvent event){
        TextInput userLink = TextInput.create("userlink", "User Link", TextInputStyle.SHORT)
            .setPlaceholder("https://github.com/CodeMC")
            .setRequired(true)
            .build();
        TextInput repoLink = TextInput.create("repolink", "Repository Link", TextInputStyle.SHORT)
            .setPlaceholder("https://github.com/CodeMC/Bot")
            .setRequired(true)
            .build();
        TextInput description = TextInput.create("description", "Description", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Discord Bot for the CodeMC Server.")
            .setRequired(true)
            .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
            .build();
        
        Modal modal = Modal.create("submit", "Join Request")
            .addComponents(
                ActionRow.of(userLink),
                ActionRow.of(repoLink),
                ActionRow.of(description)
            )
            .build();
        
        event.replyModal(modal).queue();
    }
}
