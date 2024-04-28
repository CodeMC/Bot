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

package io.codemc.bot.config;

import io.codemc.bot.CodeMCBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class ConfigHandler{
    
    private final Logger logger = LoggerFactory.getLogger(ConfigHandler.class);
    private final File file = new File("./config.json");
    
    private ConfigurationNode node = null;
    
    public ConfigHandler(){}
    
    public boolean loadConfig(){
        logger.info("Loading config.json...");
        
        if(!file.exists()){
            try(InputStream stream = CodeMCBot.class.getResourceAsStream("/config.json")){
                if(stream == null){
                    logger.warn("Unable to create config.json! InputStream was null.");
                    return false;
                }
                
                Files.copy(stream, file.toPath());
                logger.info("Successfully created config.json!");
            }catch(IOException ex){
                logger.warn("Encountered IOException while creating config.json!", ex);
                return false;
            }
        }
        
        return reloadConfig();
    }
    
    public boolean reloadConfig(){
        GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
            .file(file)
            .build();
        
        try{
            return (node = loader.load()) != null;
        }catch(IOException ex){
            logger.warn("Encountered IOException while loading Configuration!", ex);
            return false;
        }
    }
    
    public String getString(Object... path){
        return node.node(path).getString("");
    }
    
    public long getLong(Object... path){
        return node.node(path).getLong(-1L);
    }
    
    public List<Long> getLongList(Object... path){
        try{
            return node.node(path).getList(Long.class);
        }catch(SerializationException ex){
            return Collections.emptyList();
        }
    }
}
