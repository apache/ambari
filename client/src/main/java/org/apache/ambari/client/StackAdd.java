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
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.common.rest.entities.Stack;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONUnmarshaller;

public class StackAdd extends Command {

    String[] args = null;
    Options options = null;
   
    CommandLine line;
    
    public StackAdd() {
    }
    
    public StackAdd (String [] args) throws Exception {  
        /*
         * Build options for stack add
         */
        this.args = args;
        addOptions();
    }
    
    public void printUsage () {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "ambari stack add", this.options);
    }
    
    public void addOptions () {
             
        Option help = new Option( "help", "Help" );
        
        OptionBuilder.withArgName("name");
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Name of the stack");
        Option name = OptionBuilder.create( "name" );
        
        OptionBuilder.withArgName("location");
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Either URL or local file path where stack in JSON format is available");
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
         * Import stack 
         */
        File f = new File(location);
        ClientResponse response = null;
        if (!f.exists()) {
            try {
                URL urlx = new URL(location);
            } catch (MalformedURLException x) {
                System.out.println("Specified location is either a non-existing file path or a malformed URL");
                System.exit(-1);
            }
            Stack bp = new Stack();
            response = service.path("stacks/"+name)
                    .queryParam("url", location)
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).put(ClientResponse.class, bp);
        } else {
            Stack bp = null;
            if (f.getName().endsWith(".json")) {
                bp = this.readStackFromJSONFile(f);
                response = service.path("stacks/"+name)
                        .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).put(ClientResponse.class, bp);
            } else if (f.getName().endsWith(".xml")) {
                bp = this.readStackFromXMLFile(f);
                response = service.path("stacks/"+name)
                        .accept(MediaType.APPLICATION_XML).type(MediaType.APPLICATION_XML).put(ClientResponse.class, bp);
            } else {
                System.out.println("Specified stack file does not end with .json or .xml");
                System.exit(-1);
            }
            
        }     
        
        if (response.getStatus() != 200) { 
            System.err.println("Stack add command failed. Reason [Code: <"+response.getStatus()+">, Message: <"+response.getHeaders().getFirst("ErrorMessage")+">]");
            System.exit(-1);
        }
        
        Stack bp_return = response.getEntity(Stack.class);
        
        System.out.println("Stack added.\n");
        printStack(bp_return, null);
    }
    
    public Stack readStackFromXMLFile (File f) throws Exception {      
        JAXBContext jc = JAXBContext.newInstance(org.apache.ambari.common.rest.entities.Stack.class);
        Unmarshaller u = jc.createUnmarshaller();
        Stack bp = (Stack)u.unmarshal(f);
        return bp;
    }
    
    public Stack readStackFromJSONFile (File f) throws Exception {   
        JSONJAXBContext jsonContext = 
                new JSONJAXBContext("org.apache.ambari.common.rest.entities");
        InputStream in = new FileInputStream(f.getAbsoluteFile());
        try {
          JSONUnmarshaller um = jsonContext.createJSONUnmarshaller();
          Stack stack = um.unmarshalFromJSON(in, Stack.class);
          return stack;
        } catch (JAXBException je) {
          throw new IOException("Can't parse " + f.getAbsolutePath(), je);
        }
    }
}