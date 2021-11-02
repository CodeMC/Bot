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
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.Constants;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import okhttp3.HttpUrl;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CmdSubmit extends SlashCommand{
    
    private final Logger LOG = (Logger)LoggerFactory.getLogger(CmdSubmit.class);
    
    private final CodeMCBot bot;
    
    public CmdSubmit(CodeMCBot bot){
        this.bot = bot;
        
        this.name = "submit";
        this.help = "Send a Access Request for the CodeMC CI.";
        
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
        String username = bot.getCommandUtil().getString(event, "user");
        String repository = bot.getCommandUtil().getString(event, "repository");
        String description = bot.getCommandUtil().getString(event, "description");
        
        if(username == null || repository == null || description == null){
            bot.getCommandUtil().sendError(event, "The provided username/link, repository-url and/or description were null!");
            return;
        }
    
        Guild guild = event.getGuild();
        if(guild == null){
            bot.getCommandUtil().sendError(event, "Unable to retrieve Server.", "Make sure to execute this in a Server!");
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
                bot.getCommandUtil().sendError(
                    hook,
                    "The provided Repository URL (`" + repository + "`) was invalid!",
                    "Make sure it follows the pattern `https://domain.tld/:user/:repository`"
                );
                return;
            }
            
            String finalRepoLink = getLink(repoUrl.pathSegments().get(0) + "/" + repoUrl.pathSegments().get(1), repoUrl.toString());
            
            HttpUrl userUrl = HttpUrl.parse(username);
            String finalUserLink;
            if(userUrl == null){
                finalUserLink = getLink(username, null);
            }else{
                if(userUrl.pathSegments().size() <= 0){
                    bot.getCommandUtil().sendError(
                        hook,
                        "User URL does not have a valid format!\n" +
                        "Make sure it follows the pattern `https://domain.tld/:user`"
                    );
                    return;
                }
                
                finalUserLink = getLink(userUrl.pathSegments().get(0), userUrl.toString());
            }
    
            MessageEmbed embed = bot.getCommandUtil().getEmbed()
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
                    "`" + event.getUser().getAsTag() + "`",
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
                        e -> bot.getCommandUtil().sendError(hook, "Can't send a Private Message!")
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
                .append("*Your description was longer than " + MessageEmbed.VALUE_MAX_LENGTH + " characters and has been truncated!*");
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
        bot.getEventWaiter().waitForEvent(
            ButtonClickEvent.class,
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
                        ).override(true).queue(
                            null,
                            e -> LOG.warn("Unable to edit own message in User DMs.")
                        )
                    );
                    return;
                }
    
                TextChannel apply = guild.getTextChannelById(Constants.REQUEST_ACCESS);
                if(apply == null){
                    bot.getCommandUtil().sendError(hook, "Unable to retrieve the request-access channel!");
                    
                    message.editMessage(
                        "There was an issue while processing your request.\n" +
                        "Please check the command-response of the bot for further information!"
                    ).override(true).queue(
                        null,
                        e -> LOG.warn("Unable to edit own message in User DMs.")
                    );
                    return;
                }
                
                apply.sendMessageEmbeds(embed).queue(m -> {
                    message.editMessage(
                        "Submission completed! You can close the DMs now."
                    ).queue(
                        null,
                        e -> LOG.warn("Unable to edit own message in User DMs.")
                    );
                    
                    hook.editOriginal("Submission completed! You can delete this message now.").queue(
                        null,
                        e -> LOG.warn("Unable to edit original command response! Did the user delete it?")
                    );
                });
            },
            1, TimeUnit.MINUTES,
            () -> {
                message.editMessage("Interaction Timed out!").override(true).queue(
                    null,
                    e -> LOG.warn("Unable to edit own message in User DMs.")
                );
                
                hook.editOriginal("Interaction Timed out!").queue(
                    null,
                    e -> LOG.warn("Unable to edit original command response! Did the user delete it?")
                );
            }
        );
    }
}
