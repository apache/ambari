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

import java.lang.reflect.Constructor;
import java.util.HashMap;

public class AmbariClient {

    HashMap<String, HashMap<String, String>> commands = new HashMap<String, HashMap<String, String>>();
    
    /*
     * Initialize commands HashMap
     */
    public void InitializeCommandsMap() {
       
        HashMap<String, String> clusterCommands = new HashMap<String, String>();
        clusterCommands.put("create", "ClusterCreate");
        clusterCommands.put("update", "ClusterUpdate");
        clusterCommands.put("delete", "ClusterDelete");
        clusterCommands.put("list", "ClusterList");
        clusterCommands.put("get", "ClusterGet");
        clusterCommands.put("stack", "ClusterStack");
        clusterCommands.put("nodes", "ClusterNodes");
        
        
        HashMap<String, String> stackCommands = new HashMap<String, String>();
        stackCommands.put("list", "StackList");
        stackCommands.put("history", "StackHistory");
        stackCommands.put("add", "StackAdd");
        stackCommands.put("get", "StackGet");
        
        HashMap<String, String> nodeCommands = new HashMap<String, String>();
        nodeCommands.put("list", "NodeList");
        nodeCommands.put("get", "NodeGet");
        
        commands.put("cluster", clusterCommands);
        commands.put("stack", stackCommands);
        commands.put("node", nodeCommands);
        
    }
    
    public static void usage(HashMap<String, HashMap<String, String>> commands) {
        System.out.println("Usage: AmbariClient <CommandCateogry> <CommandName> <CommandOptions>\n");
        System.out.println("To get the help on each command use -help  e.g. \"AmbariClient cluster list -help\"\n");
        for (String category : commands.keySet()) {
            System.out.println("CommandCategory : ["+ category+"] : Commands "+commands.get(category).keySet());
        }    	
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        /*
         * Initialize the commands hash map
         */
        AmbariClient c = new AmbariClient();
        c.InitializeCommandsMap();
        
        /*
         * Validate the arguments
         */
        if (args.length < 2) {
           if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
        	   usage(c.commands);
               System.exit(0);
           }
           if (args[0].equalsIgnoreCase("version")) {
               System.out.println("VERSION 0.1.0");
               System.exit(0);
           }
        }
        
        /*
         * Check if args[0] belongs to command cateogory and args[1] in respective commands
         */
        if (!c.commands.containsKey(args[0])) {
            System.out.println("Invalid command category ["+args[0]+"]");
            System.exit(-1);
        }
        
        if(args.length<2) {
        	usage(c.commands);
        	System.exit(-1);
        }
        
        if (!c.commands.get(args[0]).containsKey(args[1])){
            System.out.println("Invalid command ["+args[1]+"] for category ["+args[0]+"]");
            System.exit(-1);
        }
        
        /*
         * Instantiate appropriate class based on command category and command name
         */
        try {
            Class<?>[] classParm = new Class<?>[] {String[].class};
            Object[] objectParm =  new Object[] {args};
            Class<?> commandClass  = Class.forName("org.apache.ambari.client."+c.commands.get(args[0]).get(args[1]));
            Constructor<?> co = commandClass.getConstructor(classParm);
            Command cmd = (Command)co.newInstance(objectParm);
            cmd.run();
        } catch (Exception e) {
            System.err.println( "Command failed. Reason: <" + e.getMessage() +">\n" );
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
