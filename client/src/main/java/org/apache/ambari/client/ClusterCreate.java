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
import org.apache.ambari.common.rest.entities.ClusterState;
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
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class ClusterCreate extends Command {

    String[] args = null;
    Options options = null;
    
    String urlPath = "/clusters";
    URL resourceURL = null;
    CommandLine line;
    String dry_run = "false";
    
    Properties roleToNodeExpressions = null;
    List<RoleToNodes> roleToNodeList = null;
    
    public ClusterCreate (String [] args) throws Exception {  
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
             
        Option wait = new Option( "wait", "Optionally wait for cluster to reach desired state" );
        Option dry_run = new Option( "dry_run", "Dry run" );
        Option help = new Option( "help", "Help" );
        
        OptionBuilder.withArgName("cluster_name");
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Name of the cluster to be created");
        Option name = OptionBuilder.create( "name" );
        
        OptionBuilder.withArgName("blueprint_name");
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription( "Name of the cluster blueprint");
        Option blueprint = OptionBuilder.create( "blueprint" );
        
        OptionBuilder.withArgName( "\"node_exp1; node_exp2; ...\"" );
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(  "List of node range expressions separated by semicolon (;) and contained in double quotes (\"\")" );
        Option nodes = OptionBuilder.create( "nodes" );
        
        OptionBuilder.withArgName( "blueprint_revision" );
        OptionBuilder.isRequired();
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(  "Blueprint revision, if not specified latest revision is used" );
        Option revision = OptionBuilder.create( "revision" );
        
        OptionBuilder.withArgName( "description" );
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(  "Description to be associated with cluster" );
        Option desc = OptionBuilder.create( "desc" );
        
        OptionBuilder.withArgName( "goalstate" );
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(  "Desired goal state of the cluster" );
        Option goalstate = OptionBuilder.create( "goalstate" );
        
        OptionBuilder.withArgName( "\"component-1; component-2; ...\"" );
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(  "List of components to be active in the cluster. Components are seperated by semicolon \";\"" );
        Option services = OptionBuilder.create( "services" );
        
        OptionBuilder.withArgName( "rolename=\"node_exp1; node_exp2; ... \"" );
        OptionBuilder.hasArgs(2);
        OptionBuilder.withValueSeparator();
        OptionBuilder.withDescription( "Provide node range expressions for a given rolename separated by semicolon (;) and contained in double quotes (\"\")" );
        Option role = OptionBuilder.create( "role" );

        this.options = new Options();
        options.addOption( wait );   
        options.addOption(dry_run);
        options.addOption( name );
        options.addOption( blueprint );   
        options.addOption(revision);
        options.addOption( desc );
        options.addOption( role );
        options.addOption( goalstate );
        options.addOption( nodes );
        options.addOption( services );
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
            
            if (line.hasOption("dry_run")) {
                dry_run = "true";
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
    
    public static 
    List<RoleToNodes> getRoleToNodesList (Properties roleToNodeExpressions) {
        if (roleToNodeExpressions == null) { return null; };
        
        List<RoleToNodes> roleToNodesMap = new ArrayList<RoleToNodes>();
        for (String roleName : roleToNodeExpressions.stringPropertyNames()) {
            RoleToNodes e = new RoleToNodes();
            e.setRoleName(roleName);
            e.setNodes(roleToNodeExpressions.getProperty(roleName));
            roleToNodesMap.add(e);
        }
        return roleToNodesMap;
    }
    
    private List<String> splitServices(String services) {
      if (services == null) { return null; }
      String[] arr = services.split(",");
      List<String> result = new ArrayList<String>(arr.length);
      for (String x: arr) {
          result.add(x.trim());
      }
      return result;
    }
    
    public void run() throws Exception {
        /* 
         * Parse the command line to get the command line arguments
         */
        parseCommandLine();
        
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        WebResource service = client.resource(getBaseURI());
        
        // Create Cluster Definition
        ClusterDefinition clsDef = new ClusterDefinition();
        clsDef.setName(line.getOptionValue("name"));
        clsDef.setBlueprintName(line.getOptionValue("blueprint"));
        clsDef.setNodes(line.getOptionValue("nodes"));
        
        clsDef.setGoalState(line.getOptionValue("goalstate"));
        clsDef.setBlueprintRevision(line.getOptionValue("revision"));
        clsDef.setActiveServices(splitServices(line.getOptionValue("services")));
        clsDef.setDescription(line.getOptionValue("desc"));
        clsDef.setRoleToNodesMap(getRoleToNodesList(line.getOptionProperties("role")));
        
        /*
         * Create cluster
         */
        ClientResponse response = service.path("clusters").queryParam("dry_run", dry_run).accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(ClientResponse.class, clsDef);
        if (response.getStatus() != 200) { 
            System.err.println("Cluster create command failed. Reason [Code: <"+response.getStatus()+">, Message: <"+response.getHeaders().getFirst("ErrorMessage")+">]");
            System.exit(-1);
        }
        
        /* 
         * Retrieve the cluster definition from the response
         */
        ClusterDefinition def = response.getEntity(ClusterDefinition.class);
        
        /*
         * If dry_run print the clsuter defn and return
         */
        if (line.hasOption("dry_run")) {
            System.out.println("Cluster: ["+def.getName()+"] created. Mode: dry_run.\n");
            printClusterDefinition(def);
            return;
        }
        
        /*
         * If no wait, then print the cluster definition and return
         */
        if (!line.hasOption("wait")) {
           System.out.println("Cluster: ["+def.getName()+"] created.\n");
           printClusterDefinition(def);
           return; 
        }
        
        /*
         * If wait option is specified then wait for cluster state to reach the desired state 
         */
        ClusterState clusterState;
        for (;;) {
            response = service.path("clusters/"+def.getName()+"/state").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            if (response.getStatus() != 200) { 
                System.err.println("Failed to get the cluster state. Reason [Code: <"+response.getStatus()+">, Message: <"+response.getHeaders().getFirst("ErrorMessage")+">]");
                System.exit(-1);
            }
            
            clusterState = response.getEntity(ClusterState.class);
            if (clusterState.getState().equals(def.getGoalState())) {
                break;
            }
            System.out.println("Waiting for cluster ["+def.getName()+"] to get to desired goalstate of ["+def.getGoalState()+"]");
            Thread.sleep(15 * 60000);
        }  
        
        System.out.println("Cluster: ["+def.getName()+"] created. Cluster state: ["+clusterState.getState()+"]\n");
        printClusterDefinition(def);
    }
}
