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

package io.codemc.bot;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import io.codemc.bot.commands.CmdApplication;
import io.codemc.bot.commands.CmdDisable;
import io.codemc.bot.commands.CmdMsg;
import io.codemc.bot.commands.CmdSubmit;
import io.codemc.bot.listeners.ModalListener;
import io.codemc.bot.menu.ApplicationMenu;
import io.codemc.bot.utils.Constants;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

public class CodeMCBot{
    
    private final Logger LOG = LoggerFactory.getLogger(CodeMCBot.class);
    
    public static void main(String[] args){
        try{
            new CodeMCBot().start(args[0]);
        }catch(LoginException ex){
            new CodeMCBot().LOG.error("Unable to login to Discord!", ex);
        }
    }
    
    private void start(String token) throws LoginException{
        CommandClient commandClient = new CommandClientBuilder()
            .setOwnerId(
                "204232208049766400" // Andre_601#0601
            )
            .setCoOwnerIds(
                "143088571656437760", // sgdc3#0001
                "282975975954710528" // tr7zw#4005
            )
            .setActivity(null)
            .addSlashCommands(
                new CmdApplication(),
                new CmdDisable(),
                new CmdMsg(),
                new CmdSubmit()
            )
            .addContextMenus(
                new ApplicationMenu.Accept(),
                new ApplicationMenu.Deny()
            )
            .forceGuildOnly(Constants.SERVER)
            .build();
        
        JDABuilder.createDefault(token)
            .enableIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
            )
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setActivity(Activity.of(
                Activity.ActivityType.WATCHING,
                "Applications"
            ))
            .addEventListeners(
                commandClient,
                new ModalListener()
            )
            .build();
    }
}
