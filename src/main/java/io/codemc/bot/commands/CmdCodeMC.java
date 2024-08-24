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

package io.codemc.bot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import io.codemc.api.database.DatabaseAPI;
import io.codemc.api.database.User;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.jenkins.JenkinsJob;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.JavaContinuation;
import io.codemc.bot.utils.APIUtil;
import io.codemc.bot.utils.CommandUtil;
import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CmdCodeMC extends BotCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdCodeMC.class);


    public CmdCodeMC(CodeMCBot bot) {
        super(bot);

        this.name = "codemc";
        this.help = "Commands for the CodeMC Service.";

        this.children = new SlashCommand[]{
                new Jenkins(bot),
                new Nexus(bot),
                new Remove(bot),
                new Validate(bot),
                new Link(bot),
                new Unlink(bot),
                new ChangePassword(bot)
        };
    }

    @Override
    public void withModalReply(SlashCommandEvent event) {
    }

    @Override
    public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
    }

    private static class Jenkins extends BotCommand {

        public Jenkins(CodeMCBot bot) {
            super(bot);

            this.name = "jenkins";
            this.help = "Fetch information about the Jenkins Service.";

            this.options = List.of(
                    new OptionData(OptionType.STRING, "job", "The Jenkins Job Location to fetch. For example, \"CodeMC/API\" gets the API job from the CodeMC User.").setRequired(true)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {
        }

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String job = event.getOption("job", null, OptionMapping::getAsString);
            if (job == null || job.isEmpty()) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Jenkins Job provided!").send();
                return;
            }

            String jenkinsUrl = bot.getConfigHandler().getString("jenkins", "url");
            String username = job.split("/")[0];
            String jobName = job.split("/")[1];

            JenkinsJob info = JenkinsAPI.getJobInfo(username, jobName);
            if (info == null) {
                CommandUtil.EmbedReply.from(hook).error("Failed to fetch Jenkins Job Info!").send();
                return;
            }

            EmbedBuilder embed = CommandUtil.getEmbed()
                    .setTitle(job, info.getUrl())
                    .setAuthor(username, jenkinsUrl + "/job/" + username)
                    .setDescription(info.getDescription())
                    .setTimestamp(Instant.now());

            if (info.getLastBuild() != null)
                embed.addField("Last Build", info.getLastBuild().toString(), false);

            if (info.getLastCompletedBuild() != null)
                embed.addField("Last Complete Build", info.getLastCompletedBuild().toString(), false);

            if (info.getLastFailedBuild() != null)
                embed.addField("Last Failed Build", info.getLastFailedBuild().toString(), false);

            if (info.getLastStableBuild() != null)
                embed.addField("Last Stable Build", info.getLastStableBuild().toString(), false);

            hook.editOriginalEmbeds(embed.build).queue();
        }
    }

    private static class Nexus extends BotCommand {

        public Nexus(CodeMCBot bot) {
            super(bot);

            this.name = "nexus";
            this.help = "Fetch information about the Nexus Service.";

            this.options = List.of(
                    new OptionData(OptionType.STRING, "user", "The user that owns the repository.").setRequired(true)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {
        }

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String user = event.getOption("user", null, OptionMapping::getAsString);

            if (user == null || user.isEmpty()) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Username provided!").send();
                return;
            }

            String nexusUrl = bot.getConfigHandler().getString("nexus", "url");
            String repository = user.toLowerCase();

            CompletableFuture<JsonObject> nexus = new CompletableFuture<>();
            nexus.whenCompleteAsync((info, ex) -> {
                if (ex != null) {
                    CommandUtil.EmbedReply.from(hook).error("Failed to fetch Nexus Repository Info!").send();
                    LOGGER.error("Failed to fetch Nexus Repository Info for '{}'!", user, ex);
                    return;
                }

                if (info == null) {
                    CommandUtil.EmbedReply.from(hook).error("Failed to fetch Nexus Repository Info!").send();
                    return;
                }

                String format = ((JsonPrimitive) info.get("format")).getContent();
                String type = ((JsonPrimitive) info.get("type")).getContent();

                MessageEmbed embed = CommandUtil.getEmbed()
                        .setTitle(user, nexusUrl + "/#browse/browse:" + repository)
                        .setAuthor(user, nexusUrl + "/#browse/browse:" + repository)
                        .setDescription("Information about the Nexus Repository.")
                        .addField("Format", format, true)
                        .addField("Type", type, true)
                        .setTimestamp(Instant.now())
                        .build();

                hook.sendMessageEmbeds(embed).queue();
            });
            NexusAPI.getNexusRepository(repository, JavaContinuation.create(nexus));
        }
    }

    private static class Remove extends BotCommand {

        public Remove(CodeMCBot bot) {
            super(bot);

            this.name = "remove";
            this.help = "Remove a user from a CodeMC Service.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.STRING, "username", "The Jenkins/Nexus username of the user.").setRequired(true),
                    new OptionData(OptionType.USER, "discord", "The discord user that is having their status revoked. Leave blank if the user is no longer in the server.")
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {
        }

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String username = event.getOption("username", null, OptionMapping::getAsString);

            if (username == null || username.isEmpty()) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Jenkins User provided!").send();
                return;
            }

            if (JenkinsAPI.getJenkinsUser(username).isBlank()) {
                CommandUtil.EmbedReply.from(hook).error("The user does not have a Jenkins account!").send();
                return;
            }

            DatabaseAPI.removeUser(username);
            JenkinsAPI.deleteUser(username);

            CompletableFuture<Boolean> deleted = new CompletableFuture<>();
            deleted.whenCompleteAsync((success, ex) -> {
                if (ex != null || (success != null && !success)) {
                    CommandUtil.EmbedReply.from(hook).error("Failed to delete Nexus User!").send();
                    LOGGER.error("Failed to delete Nexus User '{}'!", username, ex);
                }
            });
            NexusAPI.deleteNexus(username, JavaContinuation.create(deleted));
            DatabaseAPI.removeUser(username);

            Member user = event.getOption("discord", null, OptionMapping::getAsMember);
            if (user == null) return;

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

            guild.removeRoleFromMember(user, authorRole)
                    .reason("[Access Request] Author Status revoked by " + member.getUser().getEffectiveName())
                    .queue(
                            v -> CommandUtil.EmbedReply.from(hook)
                                    .success("Revoked Author Status from " + user.getUser().getEffectiveName() + "!")
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

    private static class Validate extends BotCommand{

        public Validate(CodeMCBot bot) {
            super(bot);

            this.name = "validate";
            this.help = "Validates the existence of current API services for specific users.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.STRING, "username", "The target username to validate. If left blank, validates all existing Jenkins users.")
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            AtomicInteger count = new AtomicInteger(0);

            BuildersKt.withContext(Dispatchers.getIO(), (scope, continuation) -> {
                String username = event.getOption("username", null, OptionMapping::getAsString);
                if (username == null) {
                    JenkinsAPI.getAllJenkinsUsers().forEach(user -> validate(scope, user, count::incrementAndGet));
                    CommandUtil.EmbedReply.from(hook).success("Validating all Jenkins Users...").send();
                } else {
                    validate(scope, username, count::incrementAndGet);
                }

                return Unit.INSTANCE;
            }, JavaContinuation.UNIT);

            CommandUtil.EmbedReply.from(hook).success("Successfully validated " + count.get() + " User(s)").send();
        }

        private static void validate(CoroutineScope scope, String username, Runnable callback) {
            BuildersKt.launch(scope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (launch, continuation) -> {
                String password = APIUtil.newPassword();

                String jenkins = JenkinsAPI.getJenkinsUser(username);
                if (jenkins == null || jenkins.isEmpty())
                    JenkinsAPI.createJenkinsUser(username, password);

                CompletableFuture<JsonObject> nexus = new CompletableFuture<>();
                nexus.whenCompleteAsync((info, ex) -> {
                    if (ex != null)
                        LOGGER.error("Failed to validate Nexus Repository for {}!", username, ex);

                    if (info == null || info.isEmpty())
                        APIUtil.createNexus(null, username, password);

                    callback.run();
                });
                NexusAPI.getNexusRepository(username, JavaContinuation.create(nexus));

                return Unit.INSTANCE;
            });
        }
    }

    private static class Link extends BotCommand{

        public Link(CodeMCBot bot) {
            super(bot);

            this.name = "link";
            this.help = "Links a Discord User to a Jenkins/Nexus User. If it currently exists, it will be overridden.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.STRING, "username", "The Jenkins user to validate to.").setRequired(true),
                    new OptionData(OptionType.USER, "discord", "The discord user to link the database to.").setRequired(true)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String username = event.getOption("username", null, OptionMapping::getAsString);
            Member target = event.getOption("discord", null, OptionMapping::getAsMember);

            if (JenkinsAPI.getJenkinsUser(username).isBlank()) {
                CommandUtil.EmbedReply.from(hook).error("The user does not have a Jenkins account!").send();
                return;
            }

            if (target == null) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Discord User provided!").send();
                return;
            }

            Role authorRole = guild.getRoleById(bot.getConfigHandler().getLong("author_role"));
            if (authorRole == null) {
                CommandUtil.EmbedReply.from(hook).error("The Author role is not set up correctly!").send();
                return;
            }

            boolean hasAuthor = target.getRoles().stream().anyMatch(role -> role.getIdLong() == authorRole.getIdLong());
            if (!hasAuthor) {
                CommandUtil.EmbedReply.from(hook).error("The user is not an Author!").send();
                return;
            }

            if (DatabaseAPI.getUser(username) == null)
                DatabaseAPI.addUser(username, target.getIdLong());
            else
                DatabaseAPI.updateUser(username, target.getIdLong());

            CommandUtil.EmbedReply.from(hook).success("Linked Discord User " + target.getUser().getEffectiveName() + " to Jenkins User " + username + "!").send();

        }
    }

    private static class Unlink extends BotCommand {

        public Unlink(CodeMCBot bot) {
            super(bot);

            this.name = "unlink";
            this.help = "Unlinks a discord user from their Jenkins/Nexus account.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.USER, "discord", "The discord user to unlink.").setRequired(true)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            Member target = event.getOption("discord", null, OptionMapping::getAsMember);

            if (target == null) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Discord User provided!").send();
                return;
            }

            String username = DatabaseAPI.getAllUsers().stream()
                    .filter(user -> user.getDiscord() == target.getIdLong())
                    .map(User::getUsername)
                    .findFirst()
                    .orElse(null);

            if (DatabaseAPI.getUser(username) == null) {
                CommandUtil.EmbedReply.from(hook).error("The user is not linked to any Jenkins/Nexus account!").send();
                return;
            }

            DatabaseAPI.removeUser(username);
            CommandUtil.EmbedReply.from(hook).success("Unlinked Discord User " + target.getUser().getEffectiveName() + " from their Jenkins/Nexus account!").send();

        }
    }

    private static class ChangePassword extends BotCommand {

        public ChangePassword(CodeMCBot bot) {
            super(bot);

            this.name = "changepassword";
            this.help = "Regenerates your Nexus Credentials.";
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            Role authorRole = guild.getRoleById(bot.getConfigHandler().getLong("author_role"));
            if (authorRole == null) {
                CommandUtil.EmbedReply.from(hook).error("The Author role is not set up correctly!").send();
                return;
            }

            boolean hasAuthor = member.getRoles().stream().anyMatch(role -> role.getIdLong() == authorRole.getIdLong());
            if (!hasAuthor) {
                CommandUtil.EmbedReply.from(hook).error("Only Authors can regenerate their credentials.").send();
                return;
            }

            String username = DatabaseAPI.getAllUsers().stream()
                    .filter(user -> user.getDiscord() == member.getIdLong())
                    .map(User::getUsername)
                    .findFirst()
                    .orElse(null);

            if (username == null) {
                CommandUtil.EmbedReply.from(hook).error("You are not linked to any Jenkins/Nexus account!").send();
                return;
            }

            String password = APIUtil.newPassword();
            APIUtil.changePassword(hook, username, password);
        }
    }
}
