/*
 * Copyright 2022 CodeMC.io
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

package io.codemc.bot.events;

import ch.qos.logback.classic.Logger;
import io.codemc.bot.utils.Constants;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

public class MessageEvents extends ListenerAdapter{
    
    private final Logger logger = (Logger)LoggerFactory.getLogger("ThreadChannel Manager");
    
    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event){
        if(!event.isFromGuild())
            return;
        
        if(!event.getGuild().getId().equals(Constants.SERVER))
            return;
        
        TextChannel tc = event.getTextChannel();
        if(!tc.getId().equals(Constants.REQUEST_ACCESS))
            return;
        
        String id = event.getMessageId();
        ThreadChannel thread = tc.getThreadChannels().stream()
            .filter(t -> !t.isArchived())
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(null);
        
        if(thread == null)
            return;
        
        thread.getManager().setArchived(true)
            .reason("Archive Thread channel of deleted Access request message.").queue(
                v -> {
                    logger.info("Archiving Thread channel from a deleted message.");
                    logger.info(" |- Thread: {}", thread.getName());
                    logger.info(" |- Message ID: {}", id);
                },
                e -> {
                    logger.warn("Cannot archive Thread channel from a deleted message.");
                    logger.warn(" |- Thread: {}", thread.getName());
                    logger.warn(" |- Message ID: {}", id);
                    logger.warn(" |- Reason: {}", e.getMessage());
                });
    }
}
