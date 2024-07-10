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
import io.codemc.bot.CodeMCBot;
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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class CmdApplication extends BotCommand{
    
    public CmdApplication(CodeMCBot bot){
        super(bot);
        
        this.name = "application";
        this.help = "Accept or deny applications.";
        
        this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "application");
        
        this.children = new SlashCommand[]{
            new Accept(bot),
            new Deny(bot)
        };
    }
    
    @Override
    public void withModalReply(SlashCommandEvent event){}
    
    @Override
    public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){}
    
    public static void handle(CodeMCBot bot, InteractionHook hook, Guild guild, long messageId, String str, boolean accepted, boolean freestyle){
        TextChannel requestChannel = guild.getTextChannelById(bot.getConfigHandler().getLong("channel", "request_access"));
        if(requestChannel == null){
            CommandUtil.EmbedReply.fromHook(hook).withError("Unable to retrieve `request-access` channel.").send();
            return;
        }
        
        requestChannel.retrieveMessageById(messageId).queue(message -> {
            List<MessageEmbed> embeds = message.getEmbeds();
            if(embeds.isEmpty()){
                CommandUtil.EmbedReply.fromHook(hook).withError("Provided message does not have any embeds.").send();
                return;
            }
            
            MessageEmbed embed = embeds.get(0);
            if(embed.getFooter() == null || embed.getFields().isEmpty()){
                CommandUtil.EmbedReply.fromHook(hook).withError(
                    "Embed does not have a footer or any Embed Fields."
                ).send();
                return;
            }
            
            String userId = embed.getFooter().getText();
            if(userId == null || userId.isEmpty()){
                CommandUtil.EmbedReply.fromHook(hook).withError("Embed does not have a valid footer.").send();
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
                CommandUtil.EmbedReply.fromHook(hook).withError("Embed does not have any valid Fields.").send();
                return;
            }
            
            TextChannel channel = guild.getTextChannelById(accepted
                ? bot.getConfigHandler().getLong("channels", "accepted_requests")
                : bot.getConfigHandler().getLong("channels", "rejected_requests")
            );
            if(channel == null){
                CommandUtil.EmbedReply.fromHook(hook)
                    .withError("Unable to retrieve `" + (accepted ? "accepted" : "rejected") + "-requests` channel.")
                    .send();
                return;
            }
            
            channel.sendMessage(getMessage(bot, userId, userLink, repoLink, str, accepted)).queue(m -> {
                ThreadChannel thread = message.getStartedThread();
                if(thread != null && !thread.isArchived()){
                    thread.getManager().setArchived(true)
                        .reason("Archiving Thread of deleted Request message.")
                        .queue();
                }
                
                message.delete().queue();
                
                Member member = guild.getMemberById(userId);
                
                if(!accepted){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withMessage("Denied Application of " + (member == null ? "Unknown" : member.getUser().getEffectiveName()) + "!")
                        .asSuccess()
                        .send();
                    return;
                }

                String[] split = str.split("/");
                String username = split[4];
                String project = split[6];

                boolean userSuccess = bot.getJenkins().createJenkinsUser(username);
                if (!userSuccess) {
                    CommandUtil.EmbedReply.fromHook(hook)
                            .withError("Failed to create Jenkins user for " + username + "! Manual creation required.")
                            .send();
                    return;
                }

                boolean jobSuccess = bot.getJenkins().createJenkinsJob(username, project, freestyle);
                if (!jobSuccess) {
                    CommandUtil.EmbedReply.fromHook(hook)
                            .withError("Failed to create Jenkins job for " + username + "! Manual creation required.")
                            .send();
                    return;
                }
                
                Role authorRole = guild.getRoleById(bot.getConfigHandler().getLong("author_role"));
                if(authorRole == null){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Unable to retrieve Author Role!")
                        .send();
                    return;
                }
                
                if(member == null){
                    CommandUtil.EmbedReply.fromHook(hook)
                        .withError("Unable to apply Role. Member not found!")
                        .send();
                    return;
                }
                
                guild.addRoleToMember(member, authorRole)
                    .reason("[Access Request] Application accepted.")
                    .queue(
                        v -> CommandUtil.EmbedReply.fromHook(hook)
                            .withMessage("Accepted application of " + member.getUser().getEffectiveName() + "!")
                            .asSuccess()
                            .send(),
                        new ErrorHandler()
                            .handle(
                                ErrorResponse.MISSING_PERMISSIONS,
                                e -> CommandUtil.EmbedReply.fromHook(hook)
                                    .withIssue("I lack the `Manage Roles` permission to apply the role.")
                                    .send()
                            )
                    );
            });
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
        
        private final Pattern projectUrlPattern = Pattern.compile("^https://ci\\.codemc\\.io/job/[a-zA-Z0-9-]+/job/[a-zA-Z0-9-_.]+/?$");
        
        public Accept(CodeMCBot bot){
            super(bot);
            
            this.name = "accept";
            this.help = "Accept an application";
            
            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "application");
            
            this.options = Arrays.asList(
                new OptionData(OptionType.STRING, "id", "The message id of the application.").setRequired(true),
                new OptionData(OptionType.STRING, "project-url", "The URL of the newly made Project. Their username should reflect their GitHub username, not their Discord username.").setRequired(true),
                new OptionData(OptionType.BOOLEAN, "freestyle", "False if built with Maven. If built with something other than Maven (such as Gradle), set to true.").setRequired(true)
            );
        }
        
        @Override
        public void withModalReply(SlashCommandEvent event){}
        
        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){
            long messageId = event.getOption("id", -1L, option -> {
                try{
                    return Long.parseLong(option.getAsString());
                }catch(NumberFormatException ex){
                    return -1L;
                }
            });
            String projectUrl = event.getOption("project-url", null, OptionMapping::getAsString);
            boolean freestyle = event.getOption("freestyle", false, OptionMapping::getAsBoolean);
            
            if(messageId == -1L || projectUrl == null){
                CommandUtil.EmbedReply.fromHook(hook).withError(
                    "Message ID or Project URL were not present!"
                ).send();
                return;
            }
            
            if(!projectUrlPattern.matcher(projectUrl).matches()){
                CommandUtil.EmbedReply.fromHook(hook).withError(
                    "The provided Project URL did not match the pattern `https://ci.codemc.io/job/<user>/job/<project>`!"
                ).send();
                return;
            }
            
            handle(bot, hook, guild, messageId, projectUrl, true, freestyle);
        }
    }
    
    private static class Deny extends BotCommand{
        
        public Deny(CodeMCBot bot){
            super(bot);
            
            this.name = "deny";
            this.help = "Deny an application";
            
            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "application");
            
            this.options = Arrays.asList(
                new OptionData(OptionType.STRING, "id", "The message id of the application.").setRequired(true),
                new OptionData(OptionType.STRING, "reason", "The reason for the denial").setRequired(true)
            );
        }
        
        @Override
        public void withModalReply(SlashCommandEvent event){}
        
        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){
            long messageId = event.getOption("id", -1L, option -> {
                try{
                    return Long.parseLong(option.getAsString());
                }catch(NumberFormatException ex){
                    return -1L;
                }
            });
            String reason = event.getOption("reason", null, OptionMapping::getAsString);
            
            if(messageId == -1L || reason == null){
                CommandUtil.EmbedReply.fromHook(hook).withError(
                    "Message ID or Reason were not present!"
                ).send();
                return;
            }
            
            handle(bot, hook, guild, messageId, reason, false, false);
        }
    }
}
