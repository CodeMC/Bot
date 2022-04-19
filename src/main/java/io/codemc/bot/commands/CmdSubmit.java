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
import io.codemc.bot.utils.CommandUtil;
import io.codemc.bot.utils.Constants;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import okhttp3.HttpUrl;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static io.codemc.bot.CodeMCBot.eventWaiter;

public class CmdSubmit extends SlashCommand{
    
    private final Logger logger = (Logger)LoggerFactory.getLogger("Application Manager");
    
    public CmdSubmit(){
        this.name = "submit";
        this.help = "Make a Join application for CodeMC to review.";
        
        this.options = Arrays.asList(
            new OptionData(
                OptionType.STRING,
                "user",
                "Username or Link to profile."
            ).setRequired(true),
            new OptionData(
                OptionType.STRING,
                "repository",
                "Link to the Repository"
            ).setRequired(true),
            new OptionData(
                OptionType.STRING,
                "description",
                "A brief description of why you want to be added. NEWLINES NOT SUPPORTED!"
            ).setRequired(true)
        );
    }
    
    @Override
    protected void execute(SlashCommandEvent event){
        String username = CommandUtil.getString(event, "user");
        String repository = CommandUtil.getString(event, "repository");
        String description = CommandUtil.getString(event, "description");
        
        if(username == null || repository == null || description == null){
            CommandUtil.EmbedReply.fromEvent(event)
                .withError("Either the username/-link, repository URL or description returned null.")
                .send();
            return;
        }
    
        Guild guild = event.getGuild();
        if(guild == null){
            CommandUtil.EmbedReply.fromEvent(event)
                .withError(
                    "Could not get CodeMC Server.",
                    "Make sure you use this command in it."
                )
                .send();
            return;
        }
        
        boolean cutDescription = false;
        if(description.length() > MessageEmbed.VALUE_MAX_LENGTH){
            description = description.substring(0, MessageEmbed.VALUE_MAX_LENGTH - 10) + "...";
            cutDescription = true;
        }
        
        final String desc = description;
        final boolean cutDesc = cutDescription;
        event.deferReply().setEphemeral(true).queue(hook -> {
            HttpUrl repoUrl = HttpUrl.parse(repository);
            if(repoUrl == null || repoUrl.pathSegments().size() <= 1){
                CommandUtil.EmbedReply.fromHook(hook)
                    .withError(
                        "The provided repository (" + repository + ") is not a valid URL.",
                        "Make sure it follows the format `https://domain.tld/:user/:repository`"
                    )
                    .send();
                return;
            }
            
            String finalRepoLink = getLink(repoUrl.pathSegments().get(0) + "/" + repoUrl.pathSegments().get(1), repoUrl.toString());
            
            HttpUrl userUrl = HttpUrl.parse(username);
            String finalUserLink;
            if(userUrl == null){
                finalUserLink = getLink(username, null);
            }else{
                if(userUrl.pathSegments().size() <= 0){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError(
                            "The provided User URL (" + username + ") is not a valid URL.",
                            "Make sure it follows he format `https://domain.tld/:user`"
                        )
                        .send();
                    return;
                }
                
                finalUserLink = getLink(userUrl.pathSegments().get(0), userUrl.toString());
            }
    
            MessageEmbed embed = CommandUtil.getEmbed()
                .addField(
                    "User/Organisation:",
                    finalUserLink,
                    true
                ).addField(
                    "Repository:",
                    finalRepoLink,
                    true
                ).addField(
                    "Submited by:",
                    "`" + event.getUser().getAsTag() + "` (" + event.getUser().getAsMention() + ")",
                    true
                ).addField(
                    "Description",
                    desc,
                    false
                ).setFooter(
                    event.getUser().getId()
                ).setTimestamp(
                    Instant.now()
                ).build();
            
            event.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessage(getConfirm(embed, cutDesc)))
                .queue(
                    message -> handleButtons(message, event.getUser(), guild, embed, hook),
                    e -> CommandUtil.EmbedReply.fromHook(hook)
                        .withError(
                            "Cannot send a private message to you.",
                            "Make sure that you accept Private messages from this server."
                        )
                        .send()
                );
        });
    }
    
    private String getLink(String name, String url){
        return String.format(
            "[`%s`](%s)",
            name,
            url == null ? "https://github.com/" + name : url
        );
    }
    
    private Message getConfirm(MessageEmbed embed, boolean cutDescription){
        MessageBuilder builder = new MessageBuilder();
        
        builder.append("Below is the request you want to submit.")
            .append("\n")
            .append("Make sure it is correct. If it is, click the `Confirm` button, otherwise click `Cancel` instead.");
        
        if(cutDescription){
            builder.append("\n")
                .append("\n")
                .append("*Your description was longer than ")
                .append(MessageEmbed.VALUE_MAX_LENGTH)
                .append(" characters and has been truncated!*");
        }
        
        builder.append("\n")
            .append("\n")
            .append("> **This request will time out in 2 Minutes!**")
            .setEmbeds(embed)
            .setActionRows(ActionRow.of(
                Button.success("confirm", "Confirm"),
                Button.danger("cancel", "Cancel")
            ));
        
        return builder.build();
    }
    
    private void handleButtons(Message message, User user, Guild guild, MessageEmbed embed, InteractionHook hook){
        eventWaiter.waitForEvent(
            ButtonInteractionEvent.class,
            event -> {
                if(event.getUser().isBot())
                    return false;
                
                if(!event.getComponentId().equals("confirm") && !event.getComponentId().equals("cancel"))
                    return false;
                
                if(!event.isAcknowledged())
                    event.deferEdit().queue();
                
                if(!event.getUser().equals(user))
                    return false;
                
                return event.getMessageId().equals(message.getId());
            },
            event -> {
                if(event.getComponentId().equals("cancel")){
                    hook.editOriginal("Submission Cancelled! You can delete this message now.").queue(
                        m -> message.editMessage(
                            "Submission cancelled! You can close the DMs now."
                        ).setActionRows(Collections.emptyList())
                        .queue(
                            null,
                            e -> logger.warn("Unable to edit own message in User DMs.")
                        )
                    );
                    return;
                }
    
                TextChannel apply = guild.getTextChannelById(Constants.REQUEST_ACCESS);
                if(apply == null){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError(
                            "Cannot get the #request-access channel.",
                            "Please report this to the CodeMC staff!"
                        )
                        .send();
                    
                    message.editMessage(
                        "There was an issue while processing your request.\n" +
                        "Please check the original command-response in the server for more details."
                    ).setActionRows(Collections.emptyList())
                    .queue(
                        null,
                        e -> logger.warn("Unable to edit own message in User DMs.")
                    );
                    return;
                }
                
                apply.sendMessageEmbeds(embed).queue(m -> {
                    m.addReaction("like:935126958193405962").queue();
                    m.addReaction("dislike:935126958235344927").queue();
                    
                    m.createThreadChannel("Access Request - " + user.getName()).queue();
                    
                    logger.info("{} submited a new Application.", user.getAsTag());
                    
                    message.editMessage(
                        "Submission completed! You can close these DMs now."
                    ).setActionRows(Collections.emptyList())
                    .queue(
                        null,
                        e -> logger.warn("Unable to edit own message in User DMs.")
                    );
                    
                    hook.editOriginal("Submission completed! You can delete this message now.").queue(
                        null,
                        e -> logger.warn("Unable to edit original command response! Did the user delete it?")
                    );
                });
            },
            1, TimeUnit.MINUTES,
            () -> {
                message.editMessage("Interaction Timed out!")
                    .setActionRows(Collections.emptyList())
                    .queue(
                        null, 
                        e -> logger.warn("Unable to edit own message in User DMs.")
                    );
                
                hook.editOriginal("Interaction Timed out!")
                    .queue(
                        null, 
                        e -> logger.warn("Unable to edit original command response! Did the user delete it?")
                    );
            }
        );
    }
}
