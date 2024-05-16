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

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.ArrayList;
import java.util.List;

public abstract class BotCommand extends SlashCommand{
    
    protected List<Long> allowedRoles = new ArrayList<>();
    protected boolean hasModalReply = false;
    
    public final CodeMCBot bot;
    
    public BotCommand(CodeMCBot bot){
        this.bot = bot;
    }
    
    @Override
    public void execute(SlashCommandEvent event){
        Guild guild = event.getGuild();
        if(guild == null){
            CommandUtil.EmbedReply.fromCommandEvent(event)
                .withError("Command can only be executed in a Server!")
                .send();
            return;
        }
        
        if(guild.getIdLong() != bot.getConfigHandler().getLong("server")){
            CommandUtil.EmbedReply.fromCommandEvent(event)
                .withError("Unable to find CodeMC Server!")
                .send();
            return;
        }
        
        Member member = event.getMember();
        if(member == null){
            CommandUtil.EmbedReply.fromCommandEvent(event)
                .withError("Unable to retrieve Member from Event!")
                .send();
            return;
        }
        
        if(!CommandUtil.hasRole(member, allowedRoles)){
            CommandUtil.EmbedReply.fromCommandEvent(event)
                .withError("You lack the permissions required to use this command!")
                .send();
            return;
        }
        
        if(hasModalReply){
            withModalReply(event);
        }else{
            event.deferReply(true).queue(hook -> withHookReply(hook, event, guild, member));
        }
    }
    
    public abstract void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member);
    
    public abstract void withModalReply(SlashCommandEvent event);
}
