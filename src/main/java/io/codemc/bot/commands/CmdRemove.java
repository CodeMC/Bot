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

import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.JavaContinuation;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CmdRemove extends BotCommand{

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdRemove.class);

    public CmdRemove(CodeMCBot bot){
        super(bot);

        this.name = "remove";
        this.help = "Revoke a user's Author status.";

        this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "service");

        this.options = List.of(
                new OptionData(OptionType.USER, "user", "The user that is having their status revoked.").setRequired(true),
                new OptionData(OptionType.STRING, "username", "The Jenkins/Nexus username of the user. If blank, defaults to their server nickname.").setRequired(false)
        );
    }

    @Override
    public void withModalReply(SlashCommandEvent event){}

    @Override
    public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member){
        Member user = event.getOption("user", OptionMapping::getAsMember);

        if (user == null) {
            CommandUtil.EmbedReply.from(hook).error("Invalid User was provided!").send();
            return;
        }

        String username = event.getOption("username", user.getNickname(), OptionMapping::getAsString);
        Role authorRole = guild.getRoleById(bot.getConfigHandler().getLong("author_role"));

        if (authorRole == null) {
            CommandUtil.EmbedReply.from(hook).error("The Author role is not set up correctly!").send();
            return;
        }

        boolean hasAuthor = user.getRoles().stream()
                .anyMatch(role -> role.getIdLong() == authorRole.getIdLong());

        if (!hasAuthor) {
            CommandUtil.EmbedReply.from(hook).error("The user is not an Author!").send();
            return;
        }

        if (JenkinsAPI.getJenkinsUser(username).isBlank()) {
            CommandUtil.EmbedReply.from(hook).error("The user does not have a Jenkins account!").send();
            return;
        }

        JenkinsAPI.deleteUser(username);

        CompletableFuture<Boolean> deleted = new CompletableFuture<>();
        deleted.handleAsync((success, ex) -> {
            if (ex != null) {
                CommandUtil.EmbedReply.from(hook).error("Failed to delete Nexus User!").send();
                LOGGER.error("Failed to delete Nexus User '{}'!", username, ex);
                return false;
            }

            return true;
        });
        NexusAPI.deleteNexus(username, JavaContinuation.create(deleted));

        guild.removeRoleFromMember(user, authorRole)
                .reason("[Access Request] Author Status revoked by " + member.getUser().getAsTag())
                .queue(
                        v -> CommandUtil.EmbedReply.from(hook)
                                .success("Revoked Author Status from " + member.getUser().getEffectiveName() + "!")
                                .send(),
                        new ErrorHandler()
                                .handle(
                                        ErrorResponse.MISSING_PERMISSIONS,
                                        e -> CommandUtil.EmbedReply.from(hook)
                                                .appendWarning("I lack the `Manage Roles` permission to remove the role.")
                                                .send()
                                )
                );
    }
}
