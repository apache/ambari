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
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.ambari.common.rest.entities.Node;
import org.apache.ambari.common.rest.entities.NodeRole;
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

public class NodeList extends Command {

    String[] args = null;
    Options options = null;
   
    CommandLine line;
    
    public NodeList() {
    }
    
    public NodeList (String [] args) throws Exception {  
        /*
         * Build options for node list
         */
        this.args = args;
        addOptions();
    }
    
    public void printUsage () {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "ambari node list", this.options);
    }
    
    public void addOptions () {
             
        Option help = new Option( "help", "Help" );
        Option verbose = new Option( "verbose", "Verbose mode" );
        
        OptionBuilder.withArgName("true/false");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "State of the node indicating if node is allocated to some cluster. If not specified, implies both allocated and free nodes");
        Option allocated = OptionBuilder.create( "allocated" );
        
        OptionBuilder.withArgName("true/false");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "State of the node to be listed. If not specified, implies both alive and dead nodes");
        Option alive = OptionBuilder.create( "alive" );
        
        this.options = new Options();
        options.addOption( verbose );   
        options.addOption(help);
        options.addOption(allocated);
        options.addOption(alive);
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
        boolean verbose = line.hasOption("verbose");
        String allocated = "";
        if (line.hasOption("allocated")) {
            allocated = line.getOptionValue("allocated");
        }
        String alive = "";
        if (line.hasOption("alive")) {
            alive = line.getOptionValue("alive");
        }
        
        /*
         * list nodes
         */
        ClientResponse response;
        response = service.path("nodes")
                   .queryParam("alive", alive)
                   .queryParam("allocated", allocated)
                   .accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        if (response.getStatus() == 204) {
            System.exit(0);
        }
        
        if (response.getStatus() != 200) { 
            System.err.println("node list command failed. Reason [Code: <"+response.getStatus()+">, Message: <"+response.getHeaders().getFirst("ErrorMessage")+">]");
            System.exit(-1);
        }
        
        /* 
         * Retrieve the cluster Information from the response
         */
        List<Node> nodes = response.getEntity(new GenericType<List<Node>>(){});
        
        if (!verbose) {
            System.out.println("[NAME]\t[LAST HEARTBEAT TIME]\t[ASSOCIATED_ROLES]\t[ACTIVE_ROLES]\t[CLUSTER_ID]\n");
            for (Node node : nodes ) {
                String clusterID = "";
                if (node.getNodeState().getClusterName() != null) clusterID = node.getNodeState().getClusterName();
                System.out.println("["+node.getName()+"]\t"+
                                   "["+node.getNodeState().getLastHeartbeatTime()+"]\t"+
                                   "["+node.getNodeState().getNodeRoleNames("")+"]\t"+
                                   "["+node.getNodeState().getNodeRoleNames(NodeRole.NODE_SERVER_STATE_UP)+"]\t"+
                                   "["+clusterID+"]\n");
            }
        } else {
            System.out.println("Node List:\n");
            for (Node node : nodes ) {
              printNodeInformation(node);
              System.out.println("\n");
            }
        }
    }
}