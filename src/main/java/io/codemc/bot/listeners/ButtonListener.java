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

package io.codemc.bot.listeners;

import io.codemc.bot.CodeMCBot;
import io.codemc.bot.commands.CmdApplication;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ButtonListener extends ListenerAdapter{
    
    private final CodeMCBot bot;
    
    public ButtonListener(CodeMCBot bot){
        this.bot = bot;
    }
    
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event){
        Guild guild = event.getGuild();
        if(!event.isFromGuild() || guild == null){
            CommandUtil.EmbedReply.from(event).error("Buttons only work on the CodeMC Server!").send();
            return;
        }
        
        if(event.getButton().getId() == null){
            event.deferReply().queue();
            return;
        }
        
        List<Long> acceptApplicationRoles = bot.getConfigHandler().getLongList("allowed_roles", "applications", "accept");
        List<Long> denyApplicationRoles = bot.getConfigHandler().getLongList("allowed_roles", "applications", "deny");
        
        if(acceptApplicationRoles.isEmpty() || denyApplicationRoles.isEmpty()){
            CommandUtil.EmbedReply.from(event).error("No roles for accepting or denying applications set!").send();
            return;
        }
        
        Member member = event.getMember();
        if(member == null){
            CommandUtil.EmbedReply.from(event).error("Cannot get Member from Server!").send();
            return;
        }
        
        String[] values = event.getButton().getId().split(":");
        if(values.length < 4 || !values[0].equals("application")){
            CommandUtil.EmbedReply.from(event).error("Received non-application button event!").send();
            return;
        }
        
        if(!values[1].equals("accept") && !values[1].equals("deny")){
            CommandUtil.EmbedReply.from(event).error(
                "Received unknown Button Application type.",
                "Expected `accept` or `deny` but got " + values[1] + "."
            ).send();
            return;
        }
        
        List<Long> roleIds = member.getRoles().stream()
            .map(Role::getIdLong)
            .toList();
        
        if(values[1].equals("accept")){
            if(lacksRole(roleIds, acceptApplicationRoles)){
                CommandUtil.EmbedReply.from(event).error("You lack permissions to perform this action.").send();
                return;
            }
            
            event.deferReply(true).queue(
                // TODO: Add project URL here (Maybe move application handling from CmdApplication)
                hook -> CmdApplication.handle(bot, hook, guild, event.getMessageIdLong(), "", true)
            );
        }else{
            if(lacksRole(roleIds, denyApplicationRoles)){
                CommandUtil.EmbedReply.from(event).error("You lack permissions to perform this action.").send();
                return;
            }
            
            event.deferReply(true).queue(
                // TODO: Add project URL here (Maybe move application handling from CmdApplication)
                hook -> CmdApplication.handle(bot, hook, guild, event.getMessageIdLong(), "", false)
            );
        }
    }
    
    private boolean lacksRole(List<Long> roleIds, List<Long> allowedRoleIds){
        if(roleIds.isEmpty())
            return true;
        
        return roleIds.stream().anyMatch(allowedRoleIds::contains);
    }
}
