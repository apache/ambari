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

import org.apache.ambari.common.rest.entities.ClusterDefinition;
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
    
    String clusterName = null;
    String description = null;
    String blueprint = null;
    String blueprint_revision = "-1";
    String goalState = null;
    String activeServices = null;
    String nodeRangeExpressions = null;
    
    Properties roleToNodeExpressions = null;
    List<RoleToNodes> roleToNodeMap = null;
    Boolean wait = false;
    Boolean dry_run = false;
    
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
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(  "Blueprint revision, if not specified latest revision is used" );
        Option blueprint_revision = OptionBuilder.create( "revision" );
        
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
        options.addOption(blueprint_revision);
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
            CommandLine line = parser.parse(this.options, this.args );
            
            if (line.hasOption("help")) {
                printUsage();
                System.exit(0);
            }
            
            this.clusterName=line.getOptionValue("name");
            this.blueprint=line.getOptionValue("blueprint");
            this.nodeRangeExpressions=line.getOptionValue("nodes");
            
            if (line.hasOption("revision")){
                this.blueprint_revision=line.getOptionValue("revision");
                System.out.println("Blueprint Revision = "+this.blueprint_revision);
            }
            if (line.hasOption("desc")){
                this.description=line.getOptionValue("desc");
                System.out.println("DESCRIPTION = "+this.description);
            }
            if (line.hasOption("role")){
                this.roleToNodeExpressions = line.getOptionProperties("role");
                /* 
                System.out.println ("RoleToNodesMap");
                for (String roleName : this.roleToNodeExpressions.stringPropertyNames()) {
                    System.out.println ("    <"+roleName+">:<"+ this.roleToNodeExpressions.getProperty(roleName)+">");
                }
                */
            }
            if (line.hasOption("goalstate")){
                this.goalState=line.getOptionValue("goalstate");
                System.out.println("Goalstate = "+this.goalState);
            }
            if (line.hasOption("services")){
                this.activeServices=line.getOptionValue("services");
                System.out.println("Active Services = "+this.activeServices);
            }
            if (line.hasOption("wait")) {
                this.wait = true;
            }
            if (line.hasOption("dry_run")) {
                this.dry_run = true;
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
    List<RoleToNodes> getRoleToNodesMap (Properties roleToNodeExpressions) {
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
      String[] arr = services.split(",");
      List<String> result = new ArrayList<String>(arr.length);
      Collections.addAll(result, arr);
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
        clsDef.setName(this.clusterName);
        clsDef.setBlueprintName(this.blueprint);
        clsDef.setNodes(this.nodeRangeExpressions);
        
        clsDef.setGoalState(this.goalState);
        clsDef.setBlueprintRevision(this.blueprint_revision);
        clsDef.setActiveServices(splitServices(this.activeServices));
        clsDef.setDescription(this.description);
        clsDef.setRoleToNodesMap(getRoleToNodesMap(this.roleToNodeExpressions));
        
        ClientResponse response = service.path("clusters").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(ClientResponse.class, clsDef);
        if (response.getStatus() != 200) { 
            System.err.println("Cluster create command failed. Reason [Code: <"+response.getStatus()+">, Message: <"+response.getHeaders().getFirst("ErrorMessage")+">]");
            System.exit(-1);
        }
        
        ClusterDefinition def = response.getEntity(ClusterDefinition.class);
        System.out.println("CLUSTER NAME ["+def.getName()+"]");
        System.out.println("CLUSTER NAME ["+def.getDescription()+"]");
    }
}
