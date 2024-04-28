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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModalListener extends ListenerAdapter{
    
    private final Pattern userLinkPattern = Pattern.compile("^https://github\\.com/(?<user>[a-zA-Z0-9-]+)/?$");
    private final Pattern repoLinkPattern = Pattern.compile("^https://github\\.com/(?<user>[a-zA-Z0-9-]+)/(?<repo>[a-zA-Z0-9-_.]+)/?$");
    
    private final Logger logger = LoggerFactory.getLogger(ModalListener.class);
    
    private final CodeMCBot bot;
    
    public ModalListener(CodeMCBot bot){
        this.bot = bot;
    }
    
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event){
        if(!event.isFromGuild())
            return;
        
        Guild guild = event.getGuild();
        if(guild == null || guild.getIdLong() != bot.getConfigHandler().getLong("server")){
            CommandUtil.EmbedReply.fromModalEvent(event)
                .withError("Unable to retrieve CodeMC Server!")
                .send();
            return;
        }
        
        String[] args = event.getModalId().split(":");
        
        switch(args[0]){
            case "submit" -> event.deferReply(true).queue(hook -> {                 
                String userLink = value(event, "userlink");
                String repoLink = value(event, "repolink");
                String description = value(event, "description");
                
                if(userLink == null || userLink.isEmpty() || repoLink == null || repoLink.isEmpty() || description == null || description.isEmpty()){
                    CommandUtil.EmbedReply.fromHook(hook).withError(
                        "User Link, Repository Link or Description was not present!"
                    ).send();
                    return;
                }
                
                Matcher userMatcher = userLinkPattern.matcher(userLink);
                Matcher repoMatcher = repoLinkPattern.matcher(repoLink);
                
                if(!userMatcher.matches() || !repoMatcher.matches()){
                    CommandUtil.EmbedReply.fromHook(hook).withError(
                        "The provided User or Repository link does not match a valid GitHub URL.",
                        "Make sure the patterns are `https://github.com/<user>` and `https://github.com/<user>/<repository>` respectively."
                    ).send();
                    return;
                }
                
                String username = String.format("[`%s`](%s)", userMatcher.group("user"), userLink);
                String repo = String.format("[`%s/%s`](%s)", repoMatcher.group("user"), repoMatcher.group("repo"), repoLink);
                String submitter = String.format("`%s` (%s)", event.getUser().getEffectiveName(), event.getUser().getAsMention());
                
                TextChannel requestChannel = guild.getTextChannelById(bot.getConfigHandler().getLong("channels", "request_access"));
                if(requestChannel == null){
                    CommandUtil.EmbedReply.fromHook(hook).withError(
                        "Unable to retrieve `request-access` channel!"
                    ).send();                     
                    return;
                }
                
                MessageEmbed embed = CommandUtil.getEmbed()
                    .addField("User/Organisation:", username, true)
                    .addField("Repository:", repo, true)
                    .addField("Submitted by:", submitter, true)
                    .addField("Description", description, false)
                    .setFooter(event.getUser().getId())
                    .setTimestamp(Instant.now())
                    .build();
                
                requestChannel.sendMessageEmbeds(embed).queue(
                    message -> {
                        CommandUtil.EmbedReply.fromHook(hook).withMessage(
                            "[Request sent!](" + message.getJumpUrl() + ")"
                        ).asSuccess().send();
                        
                        RestAction.allOf(
                            message.createThreadChannel("Access Request - " + event.getUser().getName()),
                            message.addReaction(Emoji.fromCustom("like", 935126958193405962L, false)),
                            message.addReaction(Emoji.fromCustom("dislike", 935126958235344927L, false))
                        ).queue();
                        
                        logger.info("[Access Request] User {} requested access to the CI.", event.getUser().getEffectiveName());
                    },
                    e -> CommandUtil.EmbedReply.fromHook(hook).withError(
                        "Error while submitting request!",
                        "Reported Error: " + e.getMessage()
                    ).send()
                );
            });
            
            case "message" -> event.deferReply(true).queue(hook -> {
                if(args.length < 4){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Invalid Modal data. Expected `>=4` but received `" + args.length + "`!")
                        .send();
                    return;
                }
                
                TextChannel channel = guild.getTextChannelById(args[2]);
                if(channel == null){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Received invalid Text Channel.")
                        .send();
                    return;
                }
                
                String text = value(event, "message");
                if(text == null || text.isEmpty()){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Received invalid Message to sent/edit.")
                        .send();
                    return;
                }
                
                if(!channel.canTalk()){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("I lack the permission to see and/or write in " + channel.getAsMention() + ".")
                        .send();
                    return;
                }
                
                boolean asEmbed = Boolean.parseBoolean(args[3]);
                
                if(args[1].equals("post")){
                    if(asEmbed){
                        channel.sendMessageEmbeds(CommandUtil.getEmbed().setDescription(text).build()).queue(
                            message -> sendConfirmation(hook, message, false),
                            e -> CommandUtil.EmbedReply.fromHook(hook)
                                .withError("Unable to sent message. Reason: " + e.getMessage())
                                .send()
                        );
                    }else{
                        channel.sendMessage(text).queue(
                            message -> sendConfirmation(hook, message, false),
                            e -> CommandUtil.EmbedReply.fromHook(hook)
                                .withError("Unable to sent message. Reason: " + e.getMessage())
                                .send()
                        );
                    }
                }else if(args[1].equals("edit")){
                    if(args.length == 4){
                        CommandUtil.EmbedReply.fromHook(hook)
                            .withError("Received invalid Modal data. Expected `>4` but got `=4`")
                            .send();
                        return;
                    }
                    
                    long messageId;
                    try{
                        messageId = Long.parseLong(args[4]);
                    }catch(NumberFormatException ex){
                        messageId = -1L;
                    }
                    
                    if(messageId == -1L){
                        CommandUtil.EmbedReply.fromHook(hook)
                            .withError("Received invalid message ID `" + args[4] + "`.")
                            .send();
                        return;
                    }
                    
                    channel.retrieveMessageById(messageId).queue(
                        message -> {
                            if(asEmbed){
                                message.editMessageEmbeds(CommandUtil.getEmbed().setDescription(text).build()).setReplace(true).queue(
                                    m -> sendConfirmation(hook, m, true),
                                    e -> CommandUtil.EmbedReply.fromHook(hook)
                                        .withError("Unable to edit message. Reason: " + e.getMessage())
                                        .send()
                                );
                            }else{
                                message.editMessage(text).setReplace(true).queue(
                                    m -> sendConfirmation(hook, m, true),
                                    e -> CommandUtil.EmbedReply.fromHook(hook)
                                        .withError("Unable to edit message. Reason: " + e.getMessage())
                                        .send()
                                );
                            }
                        }
                    );
                }else{
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Received Unknown Message type: `" + args[1] + "`.")
                        .send();
                }
            });
                
            case "application" -> event.deferReply(true).queue(hook -> {
                if(args.length < 3){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Invalid Modal data. Expected `=3` but received `" + args.length + "`!")
                        .send();
                    return;
                }
                
                if(!args[1].equals("accepted") && !args[1].equals("denied")){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Received unknown Application type. Expected `accepted` or `denied` but received `" + args[1] + "`.")
                        .send();
                    return;
                }
                
                long messageId;
                try{
                    messageId = Long.parseLong(args[2]);
                }catch(NumberFormatException ex){
                    messageId = -1L;
                }
                
                if(messageId == -1L){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Received Invalid Message ID. Expected number but got `" + args[2] + "` instead!")
                        .send();
                    return;
                }
                
                boolean accepted = args[1].equals("accepted");
                
                String text = value(event, "text");
                if(text == null || text.isEmpty()){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Received invalid " + (accepted ? "Project URL" : "Reason") + ". Text was empty/null.")
                        .send();
                    return;
                }
                
                CmdApplication.handle(bot, hook, guild, messageId, text, accepted);
            });
            
            default -> CommandUtil.EmbedReply.fromModalEvent(event)
                .withError("Received Modal with unknown ID `" + event.getModalId() + "`.")
                .send();
        }
    }
    
    private void sendConfirmation(InteractionHook hook, Message message, boolean edit){
        CommandUtil.EmbedReply.fromHook(hook)
            .withMessage(String.format("[%s](%s)", edit ? "Message edited!" : "Message sent!", message.getJumpUrl()))
            .asSuccess()
            .send();
        
        logger.info("[Message] User {} {} a Message as the Bot.", hook.getInteraction().getUser().getEffectiveName(), edit ? "edited" : "sent");
    }
    
    private String value(ModalInteractionEvent event, String id){
        ModalMapping value = event.getValue(id);
        if(value == null)
            return null;
        
        return value.getAsString();
    }
}
