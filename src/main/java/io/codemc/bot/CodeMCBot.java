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

package io.codemc.bot;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import io.codemc.api.CodeMCAPI;
import io.codemc.api.database.DBConfig;
import io.codemc.api.jenkins.JenkinsAPI;
import io.codemc.api.jenkins.JenkinsConfig;
import io.codemc.api.nexus.NexusAPI;
import io.codemc.api.nexus.NexusConfig;
import io.codemc.bot.commands.*;
import io.codemc.bot.config.ConfigHandler;
import io.codemc.bot.listeners.ButtonListener;
import io.codemc.bot.listeners.ModalListener;
import io.codemc.bot.utils.APIUtil;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.List;

public class CodeMCBot{
    
    private final Logger logger = LoggerFactory.getLogger(CodeMCBot.class);
    private final ConfigHandler configHandler = new ConfigHandler();
    
    public static void main(String[] args){
        try{
            new CodeMCBot().start();
        }catch(LoginException ex){
            new CodeMCBot().logger.error("Unable to login to Discord!", ex);
        }
    }
    
    private void start() throws LoginException{
        if(!configHandler.loadConfig()){
            logger.warn("Unable to load config.json! See previous logs for any errors.");
            System.exit(1);
            return;
        }
        
        String token = configHandler.getString("bot_token");
        if(token == null || token.isEmpty()){
            logger.warn("Received invalid Bot Token!");
            System.exit(1);
            return;
        }
        
        long owner = configHandler.getLong("users", "owner");
        if(owner == -1L){
            logger.warn("Unable to retrieve Owner ID. This value is required!");
            System.exit(1);
            return;
        }
        
        long guildId = configHandler.getLong("server");
        if(guildId == -1L){
            logger.warn("Unable to retrieve Server ID. This value is required!");
            System.exit(1);
            return;
        }
        
        CommandClientBuilder clientBuilder = new CommandClientBuilder().setActivity(null).forceGuildOnly(guildId);
        
        clientBuilder.setOwnerId(owner);
        
        List<Long> coOwners = configHandler.getLongList("users", "co_owners");
        
        if(coOwners != null && !coOwners.isEmpty()){
            logger.info("Adding {} Co-Owner(s) to the bot.", coOwners.size());
            // Annoying, but setCoOwnerIds has no overload with a Collection<Long>...
            long[] coOwnerIds = new long[coOwners.size()];
            for(int i = 0; i < coOwnerIds.length; i++){
                coOwnerIds[i] = coOwners.get(i);
            }
            
            clientBuilder.setCoOwnerIds(coOwnerIds);
        }

        logger.info("Initializing API...");
        JenkinsConfig jenkins = new JenkinsConfig(
                configHandler.getString("jenkins", "url"),
                configHandler.getString("jenkins", "username"),
                configHandler.getString("jenkins", "token")
        );
        NexusConfig nexus = new NexusConfig(
                configHandler.getString("nexus", "url"),
                configHandler.getString("nexus", "username"),
                configHandler.getString("nexus", "password")
        );

        String dbService = configHandler.getString("database", "service");
        String dbHost = configHandler.getString("database", "host");
        int dbPort = configHandler.getInt("database", "port");
        DBConfig db = new DBConfig(
                "jdbc:" + dbService + "://" + dbHost + ":" + dbPort + "/" + configHandler.getString("database", "database"),
                configHandler.getString("database", "username"),
                configHandler.getString("database", "password")
        );

        CodeMCAPI.initialize(jenkins, nexus, db);
        logger.info("Connected to the database at {}:{}", dbHost, dbPort);

        boolean jenkinsPing = JenkinsAPI.ping();
        if (!jenkinsPing) {
            logger.error("Failed to connect to Jenkins at {}!", jenkins.getUrl());
            System.exit(1);
            return;
        }
        logger.info("Connected to Jenkins at {}", jenkins.getUrl());

        boolean nexusPing = NexusAPI.ping();
        if (!nexusPing) {
            logger.error("Failed to connect to Nexus at {}!", nexus.getUrl());
            System.exit(1);
            return;
        }
        logger.info("Connected to Nexus at {}", nexus.getUrl());

        APIUtil.GITHUB_API_TOKEN = configHandler.getString("github");
        if (APIUtil.GITHUB_API_TOKEN.isEmpty())
            logger.warn("GitHub API Token is empty! This may cause issues with GitHub API requests.");
        else
            logger.info("GitHub API Token set");
        
        logger.info("Adding commands...");
        clientBuilder.addSlashCommands(
            new CmdApplication(this),
            new CmdDisable(this),
            new CmdMsg(this),
            new CmdReload(this),
            new CmdSubmit(this),
            new CmdCodeMC(this)
        );
        
        logger.info("Starting bot...");
        JDABuilder.createDefault(token)
            .enableIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
            )
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setActivity(Activity.of(
                Activity.ActivityType.WATCHING,
                "Applications"
            ))
            .addEventListeners(
                clientBuilder.build(),
                new ButtonListener(this),
                new ModalListener(this)
            )
            .build();
    }
    
    public ConfigHandler getConfigHandler(){
        return configHandler;
    }
}
