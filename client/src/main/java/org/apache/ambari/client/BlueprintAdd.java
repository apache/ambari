/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.common.rest.entities.Blueprint;
import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterInformation;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class BlueprintAdd extends Command {

    String[] args = null;
    Options options = null;
   
    CommandLine line;
    
    public BlueprintAdd() {
    }
    
    public BlueprintAdd (String [] args) throws Exception {  
        /*
         * Build options for blueprint add
         */
        this.args = args;
        addOptions();
    }
    
    public void printUsage () {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "ambari blueprint add", this.options);
    }
    
    public void addOptions () {
             
        Option help = new Option( "help", "Help" );
        
        OptionBuilder.withArgName("name");
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Name of the blueprint");
        Option name = OptionBuilder.create( "name" );
        
        OptionBuilder.withArgName("location");
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Either URL or local file path where blueprint in XML format is available");
        Option location = OptionBuilder.create( "location" );
        
        this.options = new Options();
        options.addOption(location);
        options.addOption(name);
        options.addOption(help);
    }
    
    public void parseCommandLine() {
     
        // create the parser
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            line = parser.parse(this.options, this.args );
            
            if (line.hasOption("help")) {
                printUsage();
                System.exit(0);
            }
        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Command parsing failed. Reason: <" + exp.getMessage()+">\n" );
            printUsage();
            System.exit(-1);
        } 
    }
    
    private static URI getBaseURI() {
        return UriBuilder.fromUri(
                "http://localhost:4080/rest/").build();
    }
    
    
    public void run() throws Exception {
        /* 
         * Parse the command line to get the command line arguments
         */
        parseCommandLine();
        
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        WebResource service = client.resource(getBaseURI());
        String location = line.getOptionValue("location");
        String name = line.getOptionValue("name");
        
        /*
         * Import blueprint 
         */
        File f = new File(location);
        ClientResponse response = null;
        if (!f.exists()) {
            try {
                URL urlx = new URL(location);
            } catch (MalformedURLException x) {
                System.out.println("Specified location is either a file path that does not exist or a malformed URL");
                System.exit(-1);
            }
            Blueprint bp = new Blueprint();
            response = service.path("blueprints/"+name)
                    .queryParam("url", location)
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_XML).put(ClientResponse.class, bp);
        } else {
            Blueprint bp = this.readBlueprintFromXMLFile(f);
            response = service.path("blueprints/"+name)
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_XML).put(ClientResponse.class, bp);
        }     
        
        if (response.getStatus() != 200) { 
            System.err.println("Blueprint add command failed. Reason [Code: <"+response.getStatus()+">, Message: <"+response.getHeaders().getFirst("ErrorMessage")+">]");
            System.exit(-1);
        }
        
        Blueprint bp_return = response.getEntity(Blueprint.class);
        
        System.out.println("Blueprint added.\n");
        printBlueprint(bp_return, null);
    }
    
    public Blueprint readBlueprintFromXMLFile (File f) throws Exception {      
        JAXBContext jc = JAXBContext.newInstance(org.apache.ambari.common.rest.entities.Blueprint.class);
        Unmarshaller u = jc.createUnmarshaller();
        Blueprint bp = (Blueprint)u.unmarshal(f);
        return bp;
    }
    
    public Blueprint readBlueprintFromJSONFile (File f) throws Exception {      
        FileInputStream fis = new FileInputStream(f);
        ObjectMapper m = new ObjectMapper();
        Blueprint blueprint = m.readValue(fis, Blueprint.class);
        return blueprint;
    }
    
    
}

