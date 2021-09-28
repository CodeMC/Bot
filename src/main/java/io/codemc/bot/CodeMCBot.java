package io.codemc.bot;

import ch.qos.logback.classic.Logger;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import io.codemc.bot.commands.CmdApplication;
import io.codemc.bot.commands.CmdDisable;
import io.codemc.bot.commands.CmdSubmit;
import io.codemc.bot.utils.CommandUtil;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

public class CodeMCBot{
    
    private final Logger LOG = (Logger)LoggerFactory.getLogger(CodeMCBot.class);
    
    private final CommandUtil commandUtil = new CommandUtil();
    private final EventWaiter eventWaiter = new EventWaiter();
    
    public static void main(String[] args){
        try{
            new CodeMCBot().start(args[0]);
        }catch(LoginException ex){
            new CodeMCBot().LOG.error("Unable to login to Discord!", ex);
        }
    }
    
    private void start(String token) throws LoginException{
        CommandClient commandClient = new CommandClientBuilder()
            .setOwnerId(
                "204232208049766400" // Andre_601#0601
            )
            .setCoOwnerIds(
                "143088571656437760", // sgdc3#0001
                "282975975954710528", // tr7zw#4005
                "249717577796812801"  // Xephi#2537
            )
            .setActivity(Activity.of(Activity.ActivityType.WATCHING, "Access applications"))
            .addSlashCommands(
                new CmdApplication(this),
                new CmdDisable(),
                new CmdSubmit(this)
            ).forceGuildOnly("405915656039694336")
            .build();
        
        JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .setActivity(Activity.of(
                Activity.ActivityType.WATCHING,
                "Applications"
            )).addEventListeners(
                commandClient,
                eventWaiter
            ).build();
    }
    
    public CommandUtil getCommandUtil(){
        return commandUtil;
    }
    
    public EventWaiter getEventWaiter(){
        return eventWaiter;
    }
}
