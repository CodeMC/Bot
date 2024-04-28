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

package io.codemc.bot.commands;

import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class CmdReload extends BotCommand{
    
    private final CodeMCBot bot;
    
    public CmdReload(CodeMCBot bot){
        this.bot = bot;
        
        this.name = "reload";
        this.help = "Reloads the configuration.";
        
        this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "reload");
    }
    
    @Override
    public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){
        boolean success = bot.getConfigHandler().reloadConfig();
        
        if(success){
            CommandUtil.EmbedReply.fromHook(hook)
                .withMessage("Reload success!")
                .asSuccess()
                .send();
        }else{
            CommandUtil.EmbedReply.fromHook(hook)
                .withError("There was an issue while reloading the configuration! Check console.")
                .send();
        }
    }
    
    @Override
    public void withModalReply(SlashCommandEvent event){}
}
