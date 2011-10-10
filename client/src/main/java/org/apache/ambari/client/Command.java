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

import java.util.Hashtable;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public abstract class Command {
    
    protected String baseURLString = "http://localhost:4080/rest";
    
    //Hashtable<String, String> commandOptions = new Hashtable<String, String>();
    
    //Options options = null;
    
    public Command() {
        
        /* boolean, name, description, required, argname,    
        Option wait = "boolean, false, wait, Optionally wait for cluster to reach desired state";
        String dry_run = "boolean, false dry_run, Dry run";
        String help = "boolean, false, help, help";
        String name = "false, true, name, cluster_name, true, Name of the cluster to be created";
        
        
        
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
        */
    }
    
    public abstract void run () throws Exception;
}
