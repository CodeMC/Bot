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

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.api.database.DatabaseAPI;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.utils.APIUtil;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdApplication extends BotCommand{

    public static final Pattern GITHUB_URL_PATTERN = Pattern.compile("^https://github\\.com/([a-zA-Z0-9-]+)/([a-zA-Z0-9-_.]+?)(?:\\.git)?(?:/.*)?$");

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdApplication.class);

    public CmdApplication(CodeMCBot bot){
        super(bot);
        
        this.name = "application";
        this.help = "Accept or deny applications.";
        
        this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "application");
        
        this.children = new SlashCommand[]{
            new Accept(bot),
            new Deny(bot)
        };
    }
    
    @Override
    public void withModalReply(SlashCommandEvent event){}
    
    @Override
    public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){}
    
    public static void handle(CodeMCBot bot, InteractionHook hook, Guild guild, long messageId, String str, boolean accepted){
        TextChannel requestChannel = guild.getTextChannelById(bot.getConfigHandler().getLong("channel", "request_access"));
        if(requestChannel == null){
            CommandUtil.EmbedReply.from(hook).error("Unable to retrieve `request-access` channel.").send();
            return;
        }
        
        requestChannel.retrieveMessageById(messageId).queue(message -> {
            List<MessageEmbed> embeds = message.getEmbeds();
            if(embeds.isEmpty()){
                CommandUtil.EmbedReply.from(hook).error("Provided message does not have any embeds.").send();
                return;
            }
            
            MessageEmbed embed = embeds.get(0);
            if(embed.getFooter() == null || embed.getFields().isEmpty()){
                CommandUtil.EmbedReply.from(hook).error("Embed does not have a footer or any Embed Fields.").send();
                return;
            }
            
            String userId = embed.getFooter().getText();
            if(userId == null || userId.isEmpty()){
                CommandUtil.EmbedReply.from(hook).error("Embed does not have a valid footer.").send();
                return;
            }
            
            String userLink = null;
            String repoLink = null;
            for(MessageEmbed.Field field : embed.getFields()){
                if(field.getName() == null || field.getValue() == null)
                    continue;
                
                if(field.getName().equalsIgnoreCase("user/organisation:")){
                    userLink = field.getValue();
                }else
                if(field.getName().equalsIgnoreCase("repository:")){
                    repoLink = field.getValue();
                }
            }
            
            if(userLink == null || repoLink == null){
                CommandUtil.EmbedReply.from(hook).error("Embed does not have any valid Fields.").send();
                return;
            }
            
            TextChannel channel = guild.getTextChannelById(accepted
                ? bot.getConfigHandler().getLong("channels", "accepted_requests")
                : bot.getConfigHandler().getLong("channels", "rejected_requests")
            );
            if(channel == null){
                CommandUtil.EmbedReply.from(hook)
                    .error("Unable to retrieve `" + (accepted ? "accepted" : "rejected") + "-requests` channel.")
                    .send();
                return;
            }

            Matcher matcher = GITHUB_URL_PATTERN.matcher(repoLink);
            if (!matcher.matches()) {
                CommandUtil.EmbedReply.from(hook)
                        .error("The user/organisation or repository name is invalid!")
                        .send();
                return;
            }

            String username = matcher.group(1);
            String project = matcher.group(2);
            String jenkinsUrl = bot.getConfigHandler().getString("jenkins", "url") + "/job/" + username + "/job/" + project + "/";
            Member member = guild.getMemberById(userId);

            channel.sendMessage(getMessage(bot, userId, userLink, repoLink, str == null ? jenkinsUrl : str, accepted)).queue(m -> {
                ThreadChannel thread = message.getStartedThread();
                if(thread != null && !thread.isArchived()){
                    thread.getManager().setArchived(true)
                        .reason("Archiving Thread of deleted Request message.")
                        .queue();
                }
                
                message.delete().queue();
                
                if(!accepted){
                    CommandUtil.EmbedReply.from(hook)
                        .success("Denied Application of " + (member == null ? "Unknown" : member.getUser().getEffectiveName()) + "!")
                        .send();
                    return;
                }
                
                Role authorRole = guild.getRoleById(bot.getConfigHandler().getLong("author_role"));
                if(authorRole == null){
                    CommandUtil.EmbedReply.from(hook)
                        .error("Unable to retrieve Author Role!")
                        .send();
                    return;
                }
                
                if(member == null){
                    CommandUtil.EmbedReply.from(hook)
                        .error("Unable to apply Role. Member not found!")
                        .send();
                    return;
                }
                
                guild.addRoleToMember(member, authorRole)
                    .reason("[Access Request] Application accepted.")
                    .queue(
                        v -> CommandUtil.EmbedReply.from(hook)
                            .success("Accepted application of " + member.getUser().getEffectiveName() + "!")
                            .send(),
                        new ErrorHandler()
                            .handle(
                                ErrorResponse.MISSING_PERMISSIONS,
                                e -> CommandUtil.EmbedReply.from(hook)
                                    .appendWarning("I lack the `Manage Roles` permission to apply the role.")
                                    .send()
                            )
                    );
            });

            String password = APIUtil.newPassword();
            APIUtil.createNexus(hook, username, password);
            APIUtil.createJenkinsJob(hook, username, password, project, repoLink);
            DatabaseAPI.addUser(username, member.getIdLong());
        });
    }
    
    private static MessageCreateData getMessage(CodeMCBot bot, String userId, String userLink, String repoLink, String str, boolean accepted){
        String msg = String.join("\n", bot.getConfigHandler().getStringList("messages", (accepted ? "accepted" : "denied"))); 
        
        MessageEmbed embed = new EmbedBuilder()
            .setColor(accepted ? 0x00FF00 : 0xFF0000)
            .setDescription(msg)
            .addField("User/Organisation:", userLink, true)
            .addField("Repository:", repoLink, true)
            .addField(accepted ? "New Project:" : "Reason:", str, false)
            .build();
        
        return new MessageCreateBuilder()
            .addContent("<@" + userId + ">")
            .setEmbeds(embed)
            .build();
    }
    
    private static class Accept extends BotCommand{

        public Accept(CodeMCBot bot){
            super(bot);
            
            this.name = "accept";
            this.help = "Accept an application";
            
            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "application");
            
            this.options = List.of(
                    new OptionData(OptionType.STRING, "id", "The message id of the application.").setRequired(true)
            );
        }
        
        @Override
        public void withModalReply(SlashCommandEvent event){}
        
        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){
            long messageId = event.getOption("id", -1L, OptionMapping::getAsLong);

            if(messageId == -1L){
                CommandUtil.EmbedReply.from(hook).error("Message ID was not present!").send();
                return;
            }
            
            handle(bot, hook, guild, messageId, null, true);
        }
    }
    
    private static class Deny extends BotCommand{
        
        public Deny(CodeMCBot bot){
            super(bot);
            
            this.name = "deny";
            this.help = "Deny an application";
            
            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "application");
            
            this.options = List.of(
                    new OptionData(OptionType.STRING, "id", "The message id of the application.").setRequired(true),
                    new OptionData(OptionType.STRING, "reason", "The reason for the denial").setRequired(true)
            );
        }
        
        @Override
        public void withModalReply(SlashCommandEvent event){}
        
        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){
            long messageId = event.getOption("id", -1L, OptionMapping::getAsLong);
            String reason = event.getOption("reason", null, OptionMapping::getAsString);
            
            if(messageId == -1L || reason == null){
                CommandUtil.EmbedReply.from(hook).error("Message ID or Reason were not present!").send();
                return;
            }
            
            handle(bot, hook, guild, messageId, reason, false);
        }
    }
}
