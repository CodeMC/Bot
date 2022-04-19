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

import ch.qos.logback.classic.Logger;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.bot.utils.Constants;
import org.slf4j.LoggerFactory;

public class CmdDisable extends SlashCommand{
    
    private final Logger logger = (Logger)LoggerFactory.getLogger("Shutdown");
    
    public CmdDisable(){
        this.name = "disable";
        this.help = "Disables the bot.";
        
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{
            Constants.ADMINISTRATOR,
            Constants.MODERATOR
        };
    }
    
    @Override
    protected void execute(SlashCommandEvent event){
        event.reply("Disabling bot...").setEphemeral(true).queue(m -> {
            logger.info("Received disable command by {}.", event.getUser().getAsTag());
            logger.info("Disabling bot...");
            System.exit(0);
        });
    }
}
