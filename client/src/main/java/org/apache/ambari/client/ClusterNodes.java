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

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterInformation;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.RoleToNodes;
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
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class ClusterNodes extends Command {

    String[] args = null;
    Options options = null;
    
    String urlPath = "/clusters";
    URL resourceURL = null;
    CommandLine line;
    
    public ClusterNodes() {
    }
    
    public ClusterNodes (String [] args) throws Exception {  
        /*
         * Build options for cluster create
         */
        this.args = args;
        addOptions();
        this.resourceURL = new URL (""+this.baseURLString+this.urlPath);
    }
    
    public void printUsage () {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "ambari cluster create", this.options);
    }
    
    public void addOptions () {
             
        Option help = new Option( "help", "Help" );
        
        OptionBuilder.withArgName("cluster_name");
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Name of the cluster to be created");
        Option name = OptionBuilder.create( "name" );
        
        OptionBuilder.withArgName("role_name");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Role name to get list of nodes associated with specified role");
        Option role = OptionBuilder.create( "role");
        
        OptionBuilder.withArgName( "[true/false]" );
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(  "Node state alive as true or false" );
        Option alive = OptionBuilder.create( "alive" );
  
        this.options = new Options();

        options.addOption( name );
        options.addOption( role );   
        options.addOption( alive );
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
        String clusterName = line.getOptionValue("name");
        String role = ""; 
        String alive = "";
        if (line.getOptionValue("alive") != null) { alive = line.getOptionValue("alive"); }
        if (line.getOptionValue("role") != null) { role = line.getOptionValue("role"); }
        
        
        /*
         * Get Cluster node list
         */
        ClientResponse response = service.path("clusters/"+clusterName+"/nodes")
                      .queryParam("alive", alive)
                      .queryParam("role", role)
                      .accept(MediaType.APPLICATION_JSON)
                      .type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        if (response.getStatus() == 204) {
            System.out.println ("No nodes are associated.");
            System.exit(0);
        }
        if (response.getStatus() != 200) { 
            System.err.println("Cluster nodes command failed. Reason [Code: <"+response.getStatus()+">, Message: <"+response.getHeaders().getFirst("ErrorMessage")+">]");
            System.exit(-1);
        }
        
        /* 
         * Retrieve the node list from response
         */
        List<Node> nodes = response.getEntity(new GenericType<List<Node>>(){});
        
        System.out.println("List of cluster nodes: \n");
        for (Node node : nodes ) {
            printNodeInformation(node);
            System.out.println("\n");
        }
    }
}
