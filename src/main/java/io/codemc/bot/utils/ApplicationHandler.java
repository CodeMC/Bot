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

package io.codemc.bot.utils;

import io.codemc.bot.CodeMCBot;
import io.codemc.api.database.DatabaseAPI;
import io.codemc.api.database.Request;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationHandler{

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationHandler.class);
    
    public static void handle(CodeMCBot bot, InteractionHook hook, Guild guild, long messageId, String str, boolean accepted){
        TextChannel requestChannel = guild.getTextChannelById(bot.getConfigHandler().getLong("channels", "request_access"));
        if(requestChannel == null){
            CommandUtil.EmbedReply.from(hook).error("Unable to retrieve `request-access` channel.").send();
            return;
        }
        
        hook.editOriginal(
            "[1/5] Handling Join Request...\n" +
            "- [1/1] Retrieving Request..."
        ).queue();
        
        requestChannel.retrieveMessageById(messageId).queue(message -> {
            Request req = DatabaseAPI.getRequest(messageId);
            if(req == null){
                // try to parse from message
                if (message.getEmbeds().isEmpty()) {
                    CommandUtil.EmbedReply.from(hook).error("Request not found in Database and Message has no embeds to parse from.").send();
                    return;
                }

                MessageEmbed embed = message.getEmbeds().get(0);
                req = fromEmbed(message.getIdLong(), embed);

                if (req == null) {
                    CommandUtil.EmbedReply.from(hook).error("Request not found in Database and data could not be parsed from embed.").send();
                    return;
                }
            }

            hook.editOriginal(
                """
                [2/5] Handling Join Request...
                - [<:like:935126958193405962>] Request retrieved!
                - [1/2] Validating Request...
                """
            ).queue();
            
            long userId = req.getUserId();
            if(userId <= 0){
                CommandUtil.EmbedReply.from(hook).error("Request does not have a valid user.").send();
                return;
            }

            hook.editOriginalFormat(
                """
                [2/5] Handling Join Request...
                - [<:like:935126958193405962>] Request retrieved!
                - [2/2] Validating Request...
                    - Found User ID `%d`.
                    - Find and validate User and Repository link...
                """, userId
            ).queue();

            String username = req.getGithubName();
            String repoName = req.getRepoName();
            
            if(username.isEmpty() || repoName.isEmpty()){
                CommandUtil.EmbedReply.from(hook).error("Database Request is missing values.").send();
                return;
            }

            String userLink = "https://github.com/" + username;
            String repoLink = userLink + "/" + repoName;
            
            hook.editOriginalFormat(
                """
                [3/5] Handling Join Request...
                - [<:like:935126958193405962>] Request retrieved!
                    - Found User ID `%s`.
                    - User and Repository found and validated!
                - [1/1] Finding `%s-requests` channel...
                """, userId, (accepted ? "accepted" : "rejected")
            ).queue();
            
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
            
            hook.editOriginalFormat(
                """
                [4/5] Handling Join Request...
                - [<:like:935126958193405962>] Request retrieved!
                    - Found User ID `%d`.
                    - User and Repository Link found and validated!
                - [<:like:935126958193405962>] `%s-requests` channel found!
                - [1/2] Removing Join Request...
                    - Archive Thread...
                """, userId, (accepted ? "accepted" : "rejected")
            ).queue();
            
            String jenkinsUrl = bot.getConfigHandler().getString("jenkins", "url") + "/job/" + username + "/job/" + repoName + "/";
            Member member = guild.getMemberById(userId);
            
            if(accepted){
                String password = APIUtil.newPassword();
                boolean jenkinsSuccess = APIUtil.createJenkinsJob(hook, username, password, repoName, repoLink, true);
                boolean nexusSuccess = APIUtil.createNexus(hook, username, password);
                
                if(!jenkinsSuccess || !nexusSuccess)
                    return;
                
                if(member == null){
                    LOGGER.warn("Member with ID {} not found!", userId);
                }else{
                    if(DatabaseAPI.getUser(username) == null)
                        DatabaseAPI.addUser(username, member.getIdLong());
                }
            }

            channel.sendMessage(getMessage(bot, String.valueOf(userId), userLink, repoLink, str == null ? jenkinsUrl : str, hook.getInteraction().getUser(), accepted)).queue(m -> {
                ThreadChannel thread = message.getStartedThread();
                if(thread != null && !thread.isArchived()){
                    thread.getManager().setArchived(true)
                        .reason("Archiving Thread of deleted Request message.")
                        .queue();
                }
                
                hook.editOriginalFormat(
                    """
                    [4/5] Handling Join Request...
                    - [<:like:935126958193405962>] Request retrieved!
                        - Found User ID `%d`.
                        - User and Repository found and validated!
                    - [<:like:935126958193405962>] `%s-requests` channel found!
                    - [2/2] Removing Join Request...
                        - Thread archived!
                        - Delete Request Message...
                    """, userId, (accepted ? "accepted" : "rejected")
                ).queue();
                
                message.delete().queue();
                
                hook.editOriginalFormat(
                    """
                    [5/5] Handling Join Request...
                    - [<:like:935126958193405962>] Request retrieved!
                        - Found User ID `%d`.
                        - User and Repository Link found and validated!
                    - [<:like:935126958193405962>] `%s-requests` channel found!
                    - [<:like:935126958193405962>] Join Request removed!
                        - Thread archived!
                        - Request Message deleted!
                    - %s
                    """, userId, (accepted ? "accepted" : "rejected"), (accepted ? "[1/2] Giving User role...\n  - Finding Author role..." : "[1/1] Finishing...")
                ).queue();
                
                if(!accepted){
                    hook.editOriginalFormat(
                        """
                        [<:like:935126958193405962>] Handling of Join Request complete!
                        - [<:like:935126958193405962>] Request retrieved!
                            - Found User ID `%d`.
                            - User and Repository found and validated!
                        - [<:like:935126958193405962>] `rejected-requests` channel found!
                        - [<:like:935126958193405962>] Join Request removed!
                            - Thread archived!
                            - Request Message deleted!
                        - [<:like:935126958193405962>] Finished rejecting join request of %s!
                        """, userId, (member == null ? "*Unknown*" : member.getUser().getEffectiveName())
                    ).queue();
                    return;
                }
                
                Role authorRole = guild.getRoleById(bot.getConfigHandler().getLong("author_role"));
                if(authorRole == null){
                    CommandUtil.EmbedReply.from(hook).error("Unable to retrieve Author Role!").send();
                    return;
                }
                
                hook.editOriginalFormat(
                    """
                    [5/5] Handling Join Request...
                    - [<:like:935126958193405962>] Request retrieved!
                        - Found User ID `%d`.
                        - User and Repository found and validated!
                    - [<:like:935126958193405962>] `accepted-requests` channel found!
                    - [<:like:935126958193405962>] Join Request removed!
                        - Thread archived!
                        - Request Message deleted!
                    - [2/2] Giving User role...
                        - Found Author Role!
                        - Applying role to user...
                    """, userId
                ).queue();
                
                if(member == null){
                    CommandUtil.EmbedReply.from(hook).error("Unable to apply Role. Member not found!").send();
                    return;
                }
                
                guild.addRoleToMember(member, authorRole)
                    .reason("[Join Request] Application accepted.")
                    .queue(v -> hook.editOriginalFormat(
                        """
                        [5/5] Handling Join Request...
                        - [<:like:935126958193405962>] Request retrieved!
                            - Found User ID `%d`.
                            - User and Repository Link found and validated!
                        - [<:like:935126958193405962>] `accepted-requests` channel found!
                        - [<:like:935126958193405962>] Join Request removed!
                            - Thread archived!
                            - Request Message deleted!
                        - [<:like:935126958193405962>] Gave User Role!
                            - Found Author Role!
                            - Applied Author Role to User!
                        
                        **Successfully accepted Join Request of user %s!**
                        """, userId, member.getUser().getEffectiveName()
                    ).queue(), 
                        new ErrorHandler()
                            .handle(
                                ErrorResponse.MISSING_PERMISSIONS,
                                e -> CommandUtil.EmbedReply.from(hook)
                                    .error("I lack the `Manage Roles` permission to apply the role.")
                                    .send()
                            )
                    );
            });
        }, e -> {
            CommandUtil.EmbedReply.from(hook)
                .error(
                    "Unable to retrieve Message. Encountered error:",
                    "`" + e.getMessage() + "`"
                ).send();
            
            LOGGER.warn("Encountered an Exception while retrieving a message!", e);
        });
    }
    
    @VisibleForTesting
    static MessageCreateData getMessage(CodeMCBot bot, String userId, String userLink, String repoLink, String str, User reviewer, boolean accepted){
        String msg = String.join("\n", bot.getConfigHandler().getStringList("messages", (accepted ? "accepted" : "denied")));
        
        MessageEmbed embed = new EmbedBuilder()
            .setColor(accepted ? 0x00FF00 : 0xFF0000)
            .setDescription(msg)
            .addField("User/Organisation:", userLink, true)
            .addField("Repository:", repoLink, true)
            .addField("Reviewer:", reviewer.getAsMention(), true)
            .addField(accepted ? "New Project:" : "Reason:", str, false)
            .build();
        
        return new MessageCreateBuilder()
            .addContent("<@" + userId + ">")
            .setEmbeds(embed)
            .build();
    }

    private static final String GITHUB_PREFIX = "https://github.com/";

    @VisibleForTesting
    static Request fromEmbed(long messageId, MessageEmbed embed) {
        if(embed == null) return null;

        String userLink = null;
        String repoLink = null;
        Long userId = null;

        for(MessageEmbed.Field field : embed.getFields()) {
            switch(field.getName()) {
                case CommandUtil.USER_ORGANISATION -> userLink = field.getValue();
                case CommandUtil.REPOSITORY -> repoLink = field.getValue();
                case CommandUtil.SUBMITTED_BY -> {
                    String submitter = field.getValue();
                    String[] parts = submitter.split("\\s+");
                    if(parts.length == 2) {
                        String mention = parts[1];
                        String idStr;
                        if(mention.startsWith("(<@") && mention.endsWith(">)")) {
                            idStr = mention.substring(3, mention.length() - 2);
                        } else if (mention.startsWith("<@") && mention.endsWith(">")) {
                            idStr = mention.substring(2, mention.length() - 1);
                        } else {
                            LOGGER.warn("Unexpected mention format in submitter field: {}", submitter);
                            break;
                        }

                        try {
                            userId = Long.parseLong(idStr);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Failed to parse user ID from mention: {}", mention, e);
                        }
                    }
                }
            }
        }

        if(userLink == null || repoLink == null || userId == null) {
            LOGGER.warn("Embed is missing required fields: userLink={}, repoLink={}, userId={}", userLink, repoLink, userId);
            return null;
        }

        String githubName = null;
        String repoName = null;
        try {
            if(userLink.startsWith("[") && userLink.contains("](") && userLink.endsWith(")")) {
                int start = userLink.indexOf("](") + 2;
                int end = userLink.length() - 1;
                String url = userLink.substring(start, end);
                if (url.startsWith(GITHUB_PREFIX)) {
                    githubName = url.substring(GITHUB_PREFIX.length());
                } else {
                    LOGGER.warn("User link does not start with expected prefix: {}", userLink);
                }
            } else {
                LOGGER.warn("User link is not in expected markdown format: {}", userLink);
            }

            if(repoLink.startsWith("[") && repoLink.contains("](") && repoLink.endsWith(")")) {
                int start = repoLink.indexOf("](") + 2;
                int end = repoLink.length() - 1;
                String url = repoLink.substring(start, end);
                if (url.startsWith(GITHUB_PREFIX) && url.contains("/")) {
                    String path = url.substring(GITHUB_PREFIX.length());
                    int slashIndex = path.indexOf("/");
                    githubName = path.substring(0, slashIndex);
                    repoName = path.substring(slashIndex + 1);
                } else {
                    LOGGER.warn("Repository link does not start with expected prefix or is malformed: {}", repoLink);
                }
            } else {
                LOGGER.warn("Repository link is not in expected markdown format: {}", repoLink);
            }
        } catch(Exception e) {
            LOGGER.warn("Failed to parse GitHub name or repository name from links: userLink={}, repoLink={}", userLink, repoLink, e);
        }

        if(githubName == null || repoName == null) {
            LOGGER.warn("Failed to extract githubName or repoName: githubName={}, repoName={}", githubName, repoName);
            return null;
        }

        return new Request(messageId, userId, githubName, repoName);
    }
}
