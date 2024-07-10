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

package io.codemc.bot.jenkins;

import com.cdancy.jenkins.rest.JenkinsClient;
import com.cdancy.jenkins.rest.domain.common.RequestStatus;
import com.cdancy.jenkins.rest.domain.system.SystemInfo;
import io.codemc.bot.CodeMCBot;
import io.codemc.bot.config.ConfigHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class JenkinsAPI {

    private final CodeMCBot bot;
    private final JenkinsClient client;

    public JenkinsAPI(CodeMCBot bot){
        this.bot = bot;

        ConfigHandler config = bot.getConfigHandler();
        String url = config.getString("jenkins", "url");
        String username = config.getString("jenkins", "username");
        String token = config.getString("jenkins", "token");
        this.client = JenkinsClient.builder()
                .endPoint(url)
                .credentials(username + ":" + token)
                .build();
    }

    public boolean ping(){
        SystemInfo info = client.api().systemApi().systemInfo();
        return info != null;
    }

    // Templates

    private String template(String path){
        try(InputStream stream = CodeMCBot.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            try (InputStreamReader isr = new InputStreamReader(stream)) {
                BufferedReader reader = new BufferedReader(isr);
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException e) {
                bot.getLogger().error("Error reading {}", path, e);
                return null;
            }
        } catch (IOException e) {
            bot.getLogger().error("Error finding {}", path, e);
            return null;
        }
    }

    private String jenkinsUserTemplate(){
        return template("/template-user-config.xml");
    }

    private String jenkinsMavenJob(){
        return template("/template-job-maven.xml");
    }

    private String jenkinsFreestyleJob(){
        return template("/template-job-freestyle.xml");
    }

    // Functions

    private String createJenkinsConfig(String username){
        String template = jenkinsUserTemplate();
        if (template == null) return null;

        return template.replace("{USERNAME}", username);
    }

    public boolean createJenkinsUser(String username){
        String config = createJenkinsConfig(username);
        if (config == null) return false;

        RequestStatus status = client.api().jobsApi().create("/", username, config);
        return status.value();
    }

    public boolean createJenkinsJob(String username, String jobName, boolean isFreestyle){
        String template = isFreestyle ? jenkinsFreestyleJob() : jenkinsMavenJob();
        if (template == null) return false;

        // Jenkins will automatically add job to the URL
        RequestStatus status = client.api().jobsApi().create(username, jobName, template);
        return status.value();
    }

}
