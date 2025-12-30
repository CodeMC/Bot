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
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.modals.Modal;

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
        TextInput user = TextInput.create("user", TextInputStyle.SHORT)
            .setPlaceholder("CodeMC")
            .setRequired(true)
            .build();
        Label userLabel = Label.of("GitHub Username", user);

        TextInput repo = TextInput.create("repo", TextInputStyle.SHORT)
            .setPlaceholder("Bot")
            .setRequired(true)
            .build();
        Label repoLabel = Label.of("Repository Name", repo);

        TextInput repoLink = TextInput.create("repoLink",  TextInputStyle.SHORT)
            .setPlaceholder("https://git.example.com/CodeMC/Bot")
            .setRequired(false)
            .build();
        Label repoLinkLabel = Label.of("Repository Link (Leave blank if on GitHub)", repoLink);

        TextInput description = TextInput.create("description", TextInputStyle.PARAGRAPH)
            .setPlaceholder("Discord Bot for the CodeMC Server.")
            .setRequired(true)
            .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
            .build();
        Label descriptionLabel = Label.of("Project Description", description);
        
        Modal modal = Modal.create("submit", "Join Request")
            .addComponents(userLabel, repoLabel, repoLinkLabel, descriptionLabel)
            .build();
        
        event.replyModal(modal).queue();
    }
}
