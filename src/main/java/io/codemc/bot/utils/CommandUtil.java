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

package io.codemc.bot.utils;

import ch.qos.logback.classic.Logger;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class CommandUtil{
    
    private static final Logger LOG = (Logger)LoggerFactory.getLogger(CommandUtil.class);

    public static EmbedBuilder getEmbed(){
        return new EmbedBuilder().setColor(0x0172BA);
    }
    
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean hasRole(Member member, List<Long> roleIds){
        if(roleIds.isEmpty())
            return true;
        
        return member.getRoles().stream()
            .filter(role -> roleIds.contains(role.getIdLong()))
            .findFirst()
            .orElse(null) != null;
    }

    public static MessageEmbed embedError(String... lines){
        return EmbedReply.empty().error(lines).build();
    }

    public static MessageEmbed embedSuccess(String... lines){
        return EmbedReply.empty().success(lines).build();
    }

    public static MessageEmbed requestEmbed(String userLink, String repoLink, String submitter, String description) {
        return getEmbed()
            .addField("User/Organisation:", userLink, true)
            .addField("Repository:", repoLink, true)
            .addField("Submitted by:", submitter, true)
            .addField("Description", description, false)
            .setTimestamp(Instant.now())
            .build();
    }
    
    public static class EmbedReply<T> {
        
        private final T type;
        private final EmbedBuilder builder = new EmbedBuilder();
        
        private EmbedReply(T type){
            this.type = type;
        }
        
        public static <T> EmbedReply<T> from(T type){
            return new EmbedReply<>(type);
        }

        public static EmbedReply<?> empty() {
            return new EmbedReply<>(null);
        }
        
        public EmbedReply<T> success(String... lines){
            builder.setDescription(String.join("\n", lines))
                .setColor(0x00FF00);
            return this;
        }
        
        public EmbedReply<T> appendWarning(String... lines){
            builder.addField("Warning:", String.join("\n", lines), false)
                .setColor(0xFFC800);
            return this;
        }
        
        public EmbedReply<T> error(String... lines){
            builder.setDescription(
                "There was an error while trying to handle an action!\n" +
                "If this error persists, report it to the Bot owner!")
                .addField("Error:", String.join("\n", lines), false)
                .setColor(0xFF0000);
            return this;
        }

        public MessageEmbed build() {
            return builder.build();
        }
        
        public void send(){
            if(type == null) return;

            if(type instanceof SlashCommandEvent commandEvent){
                commandEvent.replyEmbeds(build()).setEphemeral(true).queue();
            }else
            if(type instanceof ModalInteractionEvent modalEvent){
                modalEvent.replyEmbeds(build()).setEphemeral(true).queue();
            }else
            if(type instanceof ButtonInteractionEvent buttonEvent){
                buttonEvent.replyEmbeds(build()).setEphemeral(true).queue();
            }else
            if(type instanceof InteractionHook hook){
                hook.editOriginal(EmbedBuilder.ZERO_WIDTH_SPACE).setEmbeds(build()).queue();
            }else{
                LOG.error("Received unknown Type {} for EmbedReply!", type);
            }
        }
    }
}
