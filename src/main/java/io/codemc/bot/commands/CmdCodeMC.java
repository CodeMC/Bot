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
import io.codemc.bot.utils.APIUtil;
import io.codemc.bot.utils.CommandUtil;
import kotlinx.serialization.json.JsonArray;
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

import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CmdCodeMC extends BotCommand {

    static final Logger LOGGER = LoggerFactory.getLogger(CmdCodeMC.class);

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
                new ChangePassword(bot),
                new CreateUser(bot),
                new DeleteUser(bot),
                new MyStatus(bot),
                new Help(bot)
        };
    }

    @Override
    public void withModalReply(SlashCommandEvent event) {
    }

    @Override
    public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
    }

    @VisibleForTesting
    static class Jenkins extends BotCommand {

        public Jenkins(CodeMCBot bot) {
            super(bot);

            this.name = "jenkins";
            this.help = "Fetch information about the Jenkins Service.";

            this.options = List.of(
                    new OptionData(OptionType.STRING, "job", "The Jenkins Job Location to fetch. I.e. \"CodeMC/API\".").setRequired(true)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {
        }

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String job = event.getOption("job", null, OptionMapping::getAsString);
            if (job == null || !job.contains("/")) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Jenkins Job provided!").send();
                return;
            }

            MessageEmbed embed = createInfoEmbed(job);
            if (embed == null) {
                CommandUtil.EmbedReply.from(hook).error("Failed to fetch Jenkins Job Info!").send();
                return;
            }

            hook.editOriginalEmbeds(embed).queue();
        }

        @VisibleForTesting
        MessageEmbed createInfoEmbed(String job) {
            String jenkinsUrl = bot.getConfigHandler().getString("jenkins", "url");
            String username = job.split("/")[0];
            String jobName = job.split("/")[1];

            JenkinsJob info = JenkinsAPI.getJobInfo(username, jobName);
            if (info == null) return null;

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
            
            return embed.build();
        }
    }

    @VisibleForTesting
    static class Nexus extends BotCommand {

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

            MessageEmbed embed = createInfoEmbed(user);
            if (embed == null) {
                CommandUtil.EmbedReply.from(hook).error("Failed to fetch Nexus Repository Info!").send();
                return;
            }

            hook.sendMessageEmbeds(embed).queue();
        }

        @VisibleForTesting
        MessageEmbed createInfoEmbed(String user) {
            String nexusUrl = bot.getConfigHandler().getString("nexus", "url");
            String repository = user.toLowerCase();

            JsonObject info = NexusAPI.getNexusRepository(repository);
            if (info == null) return null;

            String format = ((JsonPrimitive) info.get("format")).getContent();
            String type = ((JsonPrimitive) info.get("type")).getContent();

            return CommandUtil.getEmbed()
                    .setTitle(user, nexusUrl + "/#browse/browse:" + repository)
                    .setDescription("Information about the Nexus Repository.")
                    .addField("Format", format, true)
                    .addField("Type", type, true)
                    .setTimestamp(Instant.now())
                    .build();
        }
    }

    @VisibleForTesting
    static class Remove extends BotCommand {

        public Remove(CodeMCBot bot) {
            super(bot);

            this.name = "remove";
            this.help = "Remove a user from a CodeMC Service.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.STRING, "username", "The Jenkins/Nexus username of the user.").setRequired(true)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {
        }

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String username = event.getOption("username", null, OptionMapping::getAsString);

            if (checkUsername(hook, username)) return;

            User dbUser = DatabaseAPI.getUser(username);

            DatabaseAPI.removeUser(username);
            JenkinsAPI.deleteUser(username);
            NexusAPI.deleteNexus(username);

            if (dbUser == null) {
                CommandUtil.EmbedReply.from(hook).success("Successfully removed " + username + " from the CodeMC Services!").send();
                return;
            }

            long id = dbUser.getDiscord();
            Member user = guild.getMemberById(id);
            if (user == null) {
                CommandUtil.EmbedReply.from(hook).success("Successfully removed " + username + " from the CodeMC Services!").send();
                return;
            }

            Role authorRole = guild.getRoleById(bot.getConfigHandler().getLong("author_role"));
            if (authorRole == null) {
                CommandUtil.EmbedReply.from(hook).error("The Author role is not set up correctly!").send();
                return;
            }

            boolean hasAuthor = user.getRoles().stream()
                    .anyMatch(role -> role.getIdLong() == authorRole.getIdLong());

            if (!hasAuthor) {
                CommandUtil.EmbedReply.from(hook).error("User was deleted, but is not an Author!").send();
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

        private static boolean checkUsername(InteractionHook hook, String username) {
            if (username == null || username.isEmpty()) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Jenkins User provided!").send();
                return true;
            }

            if (!JenkinsAPI.existsUser(username)) {
                CommandUtil.EmbedReply.from(hook).error("The user does not have a Jenkins account!").send();
                return true;
            }
            
            return false;
        }
    }

    @VisibleForTesting
    static class Validate extends BotCommand{

        public Validate(CodeMCBot bot) {
            super(bot);

            this.name = "validate";
            this.help = "Validates the existence of current API services for specific users.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.STRING, "username", "Target Username to validate. When left blank, validates all existing Jenkins Users.")
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            AtomicInteger count = new AtomicInteger(0);
            AtomicBoolean success = new AtomicBoolean(true);

            String username = event.getOption("username", null, OptionMapping::getAsString);
            if (username == null) {
                CommandUtil.EmbedReply.from(hook)
                        .success("Validating all Jenkins Users...")
                        .send();

                JenkinsAPI.getAllJenkinsUsers().forEach(user -> {
                    if (success.get())
                        success.set(validate(null, user, count));
                });
            } else {
                success.set(validate(hook, username, count));
                if (!success.get()) return;
            }

            if (success.get())
                CommandUtil.EmbedReply.from(hook)
                        .success("Successfully validated " + count.get() + " User(s)")
                        .send();
            else
                CommandUtil.EmbedReply.from(hook)
                        .error("Failed to validate user(s)!")
                        .send();
        }

        private boolean validate(InteractionHook hook, String username, AtomicInteger count) {
            boolean success = true;

            String password = APIUtil.newPassword();
            String jenkins = JenkinsAPI.getJenkinsUser(username);

            boolean noJenkins = jenkins == null || jenkins.isEmpty();
            if (noJenkins)
                success &= JenkinsAPI.createJenkinsUser(username, password, APIUtil.isGroup(username));

            JenkinsAPI.checkUserConfig(username);
            JenkinsAPI.checkCredentials(username, password);

            JsonObject info = NexusAPI.getNexusRepository(username);
            if (info == null || info.isEmpty()) {
                success &= APIUtil.createNexus(hook, username, password);

                if (!noJenkins)
                    success &= JenkinsAPI.changeJenkinsPassword(username, password);
            } else {
                success &= NexusAPI.validatePrivileges(username);
            }

            count.incrementAndGet();
            return success;
        }
    }

    @VisibleForTesting
    static class MyStatus extends BotCommand {

        public MyStatus(CodeMCBot bot) {
            super(bot);

            this.name = "my-status";
            this.help = "Checks the status of your CodeMC Jenkins and Nexus accounts.";
            this.aliases = new String[]{"mystatus"};

            List<Long> roles = new ArrayList<>();
            roles.add(bot.getConfigHandler().getLong("author_role"));
            roles.addAll(bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc"));

            this.allowedRoles = roles;
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {
        }

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String username = DatabaseAPI.getAllUsers().stream()
                    .filter(user -> user.getDiscord() == member.getIdLong())
                    .map(User::getUsername)
                    .findFirst()
                    .orElse(null);

            if (username == null) {
                CommandUtil.EmbedReply.from(hook).error("You are not linked to any Jenkins/Nexus accounts!").send();
                return;
            }

            MessageEmbed embed = build(username);
            hook.sendMessageEmbeds(embed)
                    .setEphemeral(true)
                    .queue();
        }

        @VisibleForTesting
        @NotNull
        MessageEmbed build(String username) {
            try {
                int totalUsers = JenkinsAPI.getAllJenkinsUsers().size();
                int totalRepositories = NexusAPI.getRepositories().size();

                String jenkinsUrl = bot.getConfigHandler().getString("jenkins", "url");
                String nexusUrl = bot.getConfigHandler().getString("nexus", "url");


                EmbedBuilder embed = CommandUtil.getEmbed()
                        .setTitle("CodeMC Account Status for " + username)
                        .setDescription("Total Jenkins Users: " + totalUsers + "\nTotal Nexus Repositories: " + totalRepositories)
                        .setTimestamp(Instant.now());

                boolean jenkinsExists = JenkinsAPI.existsUser(username);
                if (jenkinsExists) {
                    String config = JenkinsAPI.getJenkinsUser(username);
                    embed.addField("Jenkins Account", jenkinsUrl + "/job/" + username, true);

                    if (config.contains("com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty")) {
                        embed.addField("Jenkins Permissions", "Contains Authorization Matrix", true);
                    } else {
                        embed.addField("Jenkins Permissions", MarkdownUtil.underline("Missing Authorization Matrix"), true);
                    }

                    if (config.contains("<id>nexus-repository</id>")) {
                        embed.addField("Jenkins Nexus Credentials", "Nexus Repository Credentials Added", true);
                    } else {
                        embed.addField("Jenkins Nexus Credentials", MarkdownUtil.underline("Nexus Repository Not Linked"), true);
                    }
                } else {
                    embed.addField("Jenkins Account", MarkdownUtil.underline("Does Not Exist"), true);
                }

                JsonObject nexusInfo = NexusAPI.getNexusRepository(username);
                boolean nexusExists = nexusInfo != null && !nexusInfo.isEmpty();

                if (nexusExists) {
                    embed.addField("Nexus Repository", nexusUrl + "/#browse/browse:" + username.toLowerCase(), true);

                    JsonObject user = NexusAPI.getNexusUser(username.toLowerCase());
                    String userId = ((JsonPrimitive) user.get("userId")).getContent();
                    String roles = ((JsonArray) user.get("roles"))
                            .stream()
                            .map(role -> ((JsonPrimitive) role).getContent())
                            .collect(Collectors.joining("\n- ", "- ", ""));

                    embed.addField("Nexus User ID", userId, true);
                    embed.addField("Nexus Roles", roles, false);

                    JsonObject role = NexusAPI.getNexusRole(username.toLowerCase());
                    String privileges = ((JsonArray) role.get("privileges"))
                            .stream()
                            .map(priv -> ((JsonPrimitive) priv).getContent())
                            .collect(Collectors.joining("\n- ", "- ", ""));

                    embed.addField("Nexus Privileges", privileges, false);
                } else embed.addField("Nexus Repository", MarkdownUtil.underline("Does Not Exist"), true);

                return embed.build();
            } catch (Exception e) {
                LOGGER.error("Failed to build MyStatus embed for user '{}'", username, e);
                return CommandUtil.getEmbed()
                        .setTitle("CodeMC Account Status for " + username)
                        .setDescription("An error occurred while fetching your account status.")
                        .setTimestamp(Instant.now())
                        .build();
            }
        }
    }

    @VisibleForTesting
    static class Help extends BotCommand {

        public Help(CodeMCBot bot) {
            super(bot);

            this.name = "help";
            this.help = "Displays help information for CodeMC commands.";
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {
        }

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            List<Role> roles = member.getRoles();
            boolean isAuthor = roles.stream()
                    .anyMatch(role -> role.getIdLong() == bot.getConfigHandler().getLong("author_role"));

            List<Long> reviewerRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "application");
            boolean isReviewer = roles.stream()
                    .anyMatch(role -> reviewerRoles.contains(role.getIdLong()));

            List<Long> adminRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");
            boolean isAdmin = roles.stream()
                    .anyMatch(role -> adminRoles.contains(role.getIdLong()));

            EmbedBuilder embed = CommandUtil.getEmbed()
                    .setTitle("CodeMC Command Help")
                    .setDescription("Here is a list of available commands:");

            userHelp(embed);

            if (isReviewer) {
                reviewerHelp(embed);
            }

            if (isAuthor) {
                authorHelp(embed);
            }

            if (isAdmin) {
                adminHelp(embed);
            }

            embed.setTimestamp(Instant.now());

            hook.sendMessageEmbeds(embed.build())
                    .setEphemeral(true)
                    .queue();
        }

        @VisibleForTesting
        void userHelp(EmbedBuilder builder) {
            Map<String, String> commands = Map.of(
                    "/codemc help", "Displays this help message.",
                    "/codemc jenkins", "Fetch information about a Jenkins Author.",
                    "/codemc nexus", "Fetch information about a Nexus Repository."
            );

            for (Map.Entry<String, String> entry : commands.entrySet()) {
                builder.addField(entry.getKey(), entry.getValue(), false);
            }

            builder.setFooter("User View");
        }

        @VisibleForTesting
        void authorHelp(EmbedBuilder builder) {
            builder.addBlankField(false);

            Map<String, String> commands = Map.of(
                    "/codemc my-status", "Check the status of your CodeMC Jenkins and Nexus accounts. This will usually report any issues with your accounts, and will help administrators with diagnosing issues.",
                    "/codemc change-password", "Regenerates your Nexus Credentials. Useful if you suspect they have been compromised, or if they are not working."
            );

            for (Map.Entry<String, String> entry : commands.entrySet()) {
                builder.addField(entry.getKey(), entry.getValue(), false);
            }

            builder.setFooter("Author View");
        }

        @VisibleForTesting
        void reviewerHelp(EmbedBuilder builder) {
            builder.addBlankField(false);

            Map<String, String> commands = Map.of(
                    "/application accept", "Accepts a user's application for Author status.",
                    "/application deny", "Denies a user's application for Author status."
            );

            for (Map.Entry<String, String> entry : commands.entrySet()) {
                builder.addField(entry.getKey(), entry.getValue(), false);
            }

            builder.setFooter("Reviewer View");
        }

        @VisibleForTesting
        void adminHelp(EmbedBuilder builder) {
            builder.addBlankField(false);

            Map<String, String> commands = Map.of(
                    "/codemc validate", "Performs a validation check on all or a specific user's existing jenkins credentials. Useful for regenerating credentials for a user.",
                    "/codemc remove", "Removes a user from the CodeMC Services, and revokes their Author role.",
                    "/codemc link", "Links a Discord User to a Jenkins/Nexus User. If it currently exists, it will be overridden.",
                    "/codemc unlink", "Unlinks a discord user from their Jenkins/Nexus account.",
                    "/codemc createuser", "Creates a new user in the Jenkins/Nexus services. Useful if user configuration is corrupted and recreation is necesssary. You can specify a user to link it to.",
                    "/codemc deluser", "Deletes a user in the Jenkins/Nexus services. Useful if user configuration is corrupted and recreation is necesssary. Does not affect discord roles."
            );

            for (Map.Entry<String, String> entry : commands.entrySet()) {
                builder.addField(entry.getKey(), entry.getValue(), false);
            }

            builder.setFooter("Administrator View");
        }
    }

    @VisibleForTesting
    static class Link extends BotCommand{

        public Link(CodeMCBot bot) {
            super(bot);

            this.name = "link";
            this.help = "Links a Discord User to a Jenkins/Nexus User. If it currently exists, it will be overridden.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.STRING, "username", "The Jenkins user to validate to.").setRequired(true),
                    new OptionData(OptionType.USER, "discord", "The discord user to link the account to.").setRequired(true)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String username = event.getOption("username", null, OptionMapping::getAsString);
            Member target = event.getOption("discord", null, OptionMapping::getAsMember);

            if (Remove.checkUsername(hook, username)) return;

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

    @VisibleForTesting
    static class Unlink extends BotCommand {

        public Unlink(CodeMCBot bot) {
            super(bot);

            this.name = "unlink";
            this.help = "Unlinks a discord user from their Jenkins/Nexus account.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.USER, "discord", "The discord user to unlink.").setRequired(true),
                    new OptionData(OptionType.STRING, "username", "The Jenkins user to unlink from. Use if the discord user is linked to multiple accounts.").setRequired(false)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            Member target = event.getOption("discord", null, OptionMapping::getAsMember);
            String userTarget = event.getOption("username", null, OptionMapping::getAsString);

            if (target == null) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Discord User provided!").send();
                return;
            }

            String username = DatabaseAPI.getAllUsers().stream()
                    .filter(user -> user.getDiscord() == target.getIdLong())
                    .map(User::getUsername)
                    .filter(user -> userTarget == null || user.equals(userTarget))
                    .findFirst()
                    .orElse(null);
            
            if (username == null) {
                if (userTarget == null)
                    CommandUtil.EmbedReply.from(hook).error("The user is not linked to any Jenkins/Nexus account!").send();
                else
                    CommandUtil.EmbedReply.from(hook).error("The user is not linked to the specified Jenkins/Nexus account!").send();
                return;
            }

            DatabaseAPI.removeUser(username);
            CommandUtil.EmbedReply.from(hook).success("Unlinked Discord User " + target.getUser().getEffectiveName() + " from their Jenkins/Nexus account!").send();

        }
    }

    @VisibleForTesting
    static class ChangePassword extends BotCommand {

        public ChangePassword(CodeMCBot bot) {
            super(bot);

            this.name = "change-password";
            this.help = "Regenerates your Nexus Credentials.";

            this.options = List.of(
                new OptionData(OptionType.STRING, "username", "The name of the account to regenerate, if you have multiple. Defaults to the first one found.").setRequired(false)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            Role authorRole = guild.getRoleById(bot.getConfigHandler().getLong("author_role"));
            String target = event.getOption("username", null, OptionMapping::getAsString);

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
                    .filter(user -> target == null || user.equals(target))
                    .findFirst()
                    .orElse(null);

            if (username == null) {
                if (target == null)
                    CommandUtil.EmbedReply.from(hook).error("You are not linked to any Jenkins/Nexus accounts!").send();
                else
                    CommandUtil.EmbedReply.from(hook).error("You are not linked to the specified Jenkins/Nexus account!").send();
                return;
            }

            if (!JenkinsAPI.existsUser(username)) {
                CommandUtil.EmbedReply.from(hook).error("You do not have a Jenkins account!").send();
                return;
            }

            String password = APIUtil.newPassword();
            boolean success = APIUtil.changePassword(hook, username, password);
            if (!success) {
                CommandUtil.EmbedReply.from(hook).error("Failed to regenerate your Nexus Credentials!").send();
                return;
            }

            CommandUtil.EmbedReply.from(hook)
                    .success("Successfully changed your password!")
                    .send();
        }
    }

    @VisibleForTesting
    static class CreateUser extends BotCommand{

        public CreateUser(CodeMCBot bot) {
            super(bot);

            this.name = "createuser";
            this.help = "Creates a new user in the Jenkins/Nexus services.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.STRING, "username", "The name of Jenkins user to create.").setRequired(true),
                    new OptionData(OptionType.USER, "discord", "The discord user to link the account to.").setRequired(true)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String username = event.getOption("username", null, OptionMapping::getAsString);
            Member target = event.getOption("discord", null, OptionMapping::getAsMember);

            if (username == null || username.isEmpty()) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Username provided!").send();
                return;
            }

            if (JenkinsAPI.existsUser(username)) {
                CommandUtil.EmbedReply.from(hook).error("A user with that username already exists.").send();
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
                guild.addRoleToMember(target, authorRole)
                        .reason("[Access Request] Author Status granted by " + member.getUser().getEffectiveName())
                        .queue(
                                v -> CommandUtil.EmbedReply.from(hook)
                                        .success("Granted Author Status to " + target.getUser().getEffectiveName() + "!")
                                        .send(),
                                new ErrorHandler()
                                        .handle(
                                                ErrorResponse.MISSING_PERMISSIONS,
                                                e -> CommandUtil.EmbedReply.from(hook)
                                                        .appendWarning("I lack the `Manage Roles` permission to add the role.")
                                                        .send()
                                        )
                        );
            }

            String password = APIUtil.newPassword();
            DatabaseAPI.addUser(username, target.getIdLong());
            JenkinsAPI.createJenkinsUser(username, password, APIUtil.isGroup(username));
            APIUtil.createNexus(hook, username, password);

            CommandUtil.EmbedReply.from(hook).success("Successfully created user " + username + " and linked it to " + target.getUser().getEffectiveName() + "!").send();
            LOGGER.info("Created user '{}' in the Jenkins/Nexus services.", username);
        }
    }

    @VisibleForTesting
    static class DeleteUser extends BotCommand{

        public DeleteUser(CodeMCBot bot) {
            super(bot);

            this.name = "deluser";
            this.help = "Deletes a user in the Jenkins/Nexus services. Does not affect discord roles.";

            this.allowedRoles = bot.getConfigHandler().getLongList("allowed_roles", "commands", "codemc");

            this.options = List.of(
                    new OptionData(OptionType.STRING, "username", "The name of Jenkins user to delete.").setRequired(true)
            );
        }

        @Override
        public void withModalReply(SlashCommandEvent event) {}

        @Override
        public void withHookReply(InteractionHook hook, SlashCommandEvent event, Guild guild, Member member) {
            String username = event.getOption("username", null, OptionMapping::getAsString);

            if (username == null || username.isEmpty()) {
                CommandUtil.EmbedReply.from(hook).error("Invalid Username provided!").send();
                return;
            }

            if (!JenkinsAPI.existsUser(username)) {
                CommandUtil.EmbedReply.from(hook).error("The user does not exist!").send();
                return;
            }

            DatabaseAPI.removeUser(username);
            JenkinsAPI.deleteUser(username);
            NexusAPI.deleteNexus(username);

            CommandUtil.EmbedReply.from(hook).success("Successfully deleted user " + username + "!").send();
            LOGGER.info("Deleted user '{}' from the Jenkins/Nexus services.", username);
        }
    }
}
