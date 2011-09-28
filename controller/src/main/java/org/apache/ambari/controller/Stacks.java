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
package org.apache.ambari.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.common.rest.entities.Blueprint;
import org.apache.ambari.common.rest.entities.Stack;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Stacks {

    
    /*
     * Stack name -> {revision -> stack} .
     */
    protected ConcurrentHashMap<String, ConcurrentHashMap<Integer,Stack>> stacks = new ConcurrentHashMap<String,ConcurrentHashMap<Integer,Stack>>();
    
    /*
     * Map {stack_name:revision, blueprint}
     */
    protected ConcurrentHashMap<String, Blueprint> default_blueprints = new ConcurrentHashMap<String,Blueprint>();
    
    private static Stacks StacksTypeRef=null;
        
    private Stacks() {}
    
    public static synchronized Stacks getInstance() {
        if(StacksTypeRef == null) {
            StacksTypeRef = new Stacks();
        }
        return StacksTypeRef;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
      
    /* 
     * Import the specific revision of the stack to Ambari  
    */
    public Stack importStackDescription (String stackLocation) throws Exception {
        /*
         * Check for stackLocation not null 
         */
        if (stackLocation == null || stackLocation.equals("")) {
            Exception e = new Exception("Query parameter url must be specified");
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        
        /*
         * Convert the string to URL
         * TODO: Map MalformedURLException to appropriate REST exception and response code
         */ 
        URL stackLocationURL;
        try {
            stackLocationURL = new URL(stackLocation);
        } catch (MalformedURLException e) {
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        
        /*
         * Fetch the stack definition as JSON and create Stack object and 
         * add to stacks list
         */
        ObjectMapper m = new ObjectMapper();
        InputStream is = stackLocationURL.openStream();
        Stack stack = m.readValue(is, Stack.class);
        if (stacks.containsKey(stack.getName())) {
            if (stacks.get(stack.getName()).containsKey(stack.getStackRevision())) {
                Exception e = new Exception(
                      "Specified stack [Name:"+stack.getName()+", Revision: ["+stack.getStackRevision()+"] is already imported");
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            } else {
                stacks.get(stack.getName()).put(stack.getStackRevision(), stack);
                Blueprint blueprint = importDefaultBlueprint(stack);
                this.default_blueprints.put(stack.getName()+":"+stack.getStackRevision(), blueprint);
            }
        } else {
            ConcurrentHashMap<Integer, Stack> x = new ConcurrentHashMap<Integer, Stack>();
            x.put(stack.getStackRevision(), stack);
            this.stacks.put(stack.getName(), x);
            Blueprint blueprint = importDefaultBlueprint(stack);
            this.default_blueprints.put(stack.getName()+":"+stack.getStackRevision(), blueprint);
        }
        return stack;
    } 
    
    /*
     * Import the default blueprint from the URL location
     */
    public Blueprint importDefaultBlueprint (Stack stack) throws Exception {
        Blueprint blueprint;
        try {
            URL blueprintUrl = new URL(stack.getBlueprintLocationURL());
            ObjectMapper m = new ObjectMapper();
            InputStream is = blueprintUrl.openStream();
            blueprint = m.readValue(is, Blueprint.class);
        } catch (Exception e) {
            this.stacks.get(stack.getName()).remove(stack.getStackRevision());
            if (this.stacks.get(stack.getName()).keySet().isEmpty()) {
                this.stacks.remove(stack.getName());
            }
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        return blueprint;
    }
   
    /* 
     * Delete stack 
    */
    public void deleteStack(String stackName, int revision) throws Exception {
        this.stacks.get(stackName).remove(revision);
        if (this.stacks.get(stackName).keySet().isEmpty()) {
            this.stacks.remove(stackName);
        }
    }

    /* 
     * Get StackType from stack list given its name 
    */
    public Stack getStack(String stackName, int revision) throws Exception {

        Stack sk = this.stacks.get(stackName).get(revision);
       
        if (sk == null) {
            Exception e = new Exception ("Stack ["+stackName+"] revision ["+revision+"] does not exists");
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        return sk;
    }
    
    /* 
     * Get default blueprint for given stack 
    */
    public Blueprint getDefaultBlueprint(String stackName, int revision) throws Exception {
        
        Blueprint bp = this.default_blueprints.get(stackName+":"+revision);
        
        if (bp == null) {
            Exception e = new Exception ("Stack ["+stackName+"] revision ["+revision+"] does not exists");
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        return bp;
    }
    
    /*
     * Returns stack names
     */
    public List<String> getStackList() {
        List<String> list = new ArrayList<String>();
        list.addAll(this.stacks.keySet());
        return list;
    }
    
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        ObjectMapper m = new ObjectMapper();
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }
}
