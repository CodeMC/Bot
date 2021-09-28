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
import net.dv8tion.jda.api.EmbedBuilder;
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
            bot.getCommandUtil().sendError(event, "Either Username, repository or description were null!");
            return;
        }
    
        Guild guild = event.getGuild();
        if(guild == null){
            bot.getCommandUtil().sendError(event, "Unable to retrieve Guild.");
            return;
        }
        
        event.deferReply().setEphemeral(true).queue(hook -> {
            HttpUrl repoUrl = HttpUrl.parse(repository);
            if(repoUrl == null || repoUrl.pathSegments().size() <= 1){
                bot.getCommandUtil().sendError(hook, "The provided Repository URL (`" + repository + "`) was invalid!");
                return;
            }
            
            String finalRepoLink = getLink(repoUrl.pathSegments().get(0) + "/" + repoUrl.pathSegments().get(1), repoUrl.toString());
            
            HttpUrl userUrl = HttpUrl.parse(username);
            String finalUserLink;
            if(userUrl == null){
                finalUserLink = getLink(username, null);
            }else{
                if(userUrl.pathSegments().size() <= 0){
                    bot.getCommandUtil().sendError(hook, "User URL does not have a valid path!");
                    return;
                }
                
                finalUserLink = getLink(userUrl.pathSegments().get(0), userUrl.toString());
            }
    
            MessageEmbed embed = new EmbedBuilder()
                .setColor(0x0172BA)
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
                    description,
                    false
                ).setFooter(
                    event.getUser().getId()
                ).setTimestamp(
                    Instant.now()
                ).build();
            
            event.getUser().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage(getConfirm(embed)))
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
    
    private Message getConfirm(MessageEmbed embed){
        return new MessageBuilder(
            "Please double-check that your provided information is correct and confirm or cancel the submission.\n" +
            "Due to a Discord limitations are line breaks NOT supported for Descriptions.\n" +
            "\n" +
            "> **This request will time out in 1 Minute!**"
        ).setEmbeds(embed)
        .setActionRows(ActionRow.of(
            Button.success("confirm", "Confirm"),
            Button.danger("cancel", "Cancel")
        )).build();
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
                    hook.editOriginal("Submission Cancelled! You can delete this message now.").queue();
                    return;
                }
    
                TextChannel apply = guild.getTextChannelById(Constants.REQUEST_ACCESS);
                if(apply == null){
                    bot.getCommandUtil().sendError(hook, "Unable to retrieve the request-access channel!");
                    return;
                }
                
                apply.sendMessageEmbeds(embed).queue(m -> {
                    message.delete().queue(
                        null,
                        e -> LOG.warn("Could not delete own Message in DMs!")
                    );
                    hook.editOriginal("Submission completed! You can delete this message now.").queue(
                        null,
                        e -> LOG.warn("Unable to edit original command response! Did the user delete it?")
                    );
                    });
            },
            1, TimeUnit.MINUTES,
            () -> {
                message.editMessage("Interaction Timed out!").override(true).queue();
                hook.editOriginal("Interaction Timed out!").queue(
                    null,
                    e -> LOG.warn("Unable to edit original command response! Did the user delete it?")
                );
            }
        );
    }
}
