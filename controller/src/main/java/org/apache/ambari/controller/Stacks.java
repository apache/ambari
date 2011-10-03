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
import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.common.rest.entities.ConfigurationCategory;
import org.apache.ambari.common.rest.entities.Configuration;
import org.apache.ambari.common.rest.entities.PackageRepository;
import org.apache.ambari.common.rest.entities.Role;
import org.apache.ambari.common.rest.entities.Stack;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
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
        
    private Stacks() {
        /*
         * Add stack and default blueprint 
         */
        Stack x = new Stack();
        x.setName("hortonworks-1.0");
        x.setBlueprintLocationURL("http://localhost/~vgogate/ambari/hortonworks-1.0/default-blueprint.json");
        x.setDescription("Hortonworks ambari stack 1.0");
        x.setStackRevision(0);
        ConcurrentHashMap<Integer,Stack> y = new ConcurrentHashMap<Integer,Stack>();
        y.put(x.getStackRevision(), x);
        this.stacks.put(x.getName(), y);
        
        /*
         * Create default blueprint
         */
        Blueprint bp = new Blueprint();
        bp.setName("default");
        bp.setStackName(x.getName());
        bp.setParentName("default");
        bp.setRevision(new Integer(x.getStackRevision()).toString());
        bp.setParentRevision(new Integer(x.getStackRevision()).toString());
       
 
        Component hdfsC = new Component(); hdfsC.setName("hdfs");
        hdfsC.getProperty().add(Blueprints.getInstance().getProperty ("dfs.name.dir", "${HADOOP_NN_DIR}"));
        hdfsC.getProperty().add(Blueprints.getInstance().getProperty ("dfs.data.dir", "${HADOOP_DATA_DIR}"));
        Component mapredC = new Component(); mapredC.setName("hdfs");
        mapredC.getProperty().add(Blueprints.getInstance().getProperty ("mapred.system.dir", "/mapred/mapredsystem"));
        mapredC.getProperty().add(Blueprints.getInstance().getProperty ("mapred.local.dir", "${HADOOP_MAPRED_DIR}"));
        List<Component> compList = new ArrayList();
        compList.add(mapredC);
        compList.add(hdfsC);
        bp.setComponents(compList);
        
        List<PackageRepository> prList = new ArrayList<PackageRepository>();
        PackageRepository pr = new PackageRepository();
        pr.setLocationURL("http://localhost/~vgogate/ambari");
        pr.setType("RPM");  
        bp.setPackageRepositories(prList);
        
        Configuration bpDefaultCfg = new Configuration();
        ConfigurationCategory hdfs_site = new ConfigurationCategory();
        hdfs_site.setName("hdfs-site");
        ConfigurationCategory mapred_site = new ConfigurationCategory();
        mapred_site.setName("mapred-site");  
        hdfs_site.getProperty().add(Blueprints.getInstance().getProperty ("dfs.name.dir", "/tmp/namenode"));
        hdfs_site.getProperty().add(Blueprints.getInstance().getProperty ("dfs.data.dir", "/tmp/datanode"));
        mapred_site.getProperty().add(Blueprints.getInstance().getProperty ("mapred.system.dir", "/mapred/mapredsystem"));
        mapred_site.getProperty().add(Blueprints.getInstance().getProperty ("mapred.local.dir", "/tmp/mapred")); 
        bpDefaultCfg.getCategory().add(mapred_site);
        bpDefaultCfg.getCategory().add(hdfs_site);
        
        bp.setConfiguration(bpDefaultCfg);
        
        List<Role> roleList = new ArrayList<Role>();
        Role hdfs_nn_role = new Role();
        hdfs_nn_role.setName("hdfs-NN");
        hdfs_nn_role.setConfiguration(bpDefaultCfg);
        
        Role mapred_jt_role = new Role();
        mapred_jt_role.setName("mapred-JT");
        mapred_jt_role.setConfiguration(bpDefaultCfg);
        
        Role slaves_role = new Role();
        slaves_role.setName("slaves");
        slaves_role.setConfiguration(bpDefaultCfg);
        
        roleList.add(hdfs_nn_role);
        roleList.add(mapred_jt_role);
        roleList.add(slaves_role);
        
        bp.setRoles(roleList);
        
        /*
         * Add default blueprint to default blueprints list assciated with the stack
         */
        this.default_blueprints.put(x.getName()+":"+x.getStackRevision(), bp);
    }
    
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
            String msg = "Query parameter url must be specified";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        
        /*
         * Convert the string to URL
         * TODO: Map MalformedURLException to appropriate REST exception and response code
         */ 
        URL stackLocationURL;
        try {
            stackLocationURL = new URL(stackLocation);
        } catch (MalformedURLException e) {
            String msg = "MalformedURLException for stack location URL";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
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
                String msg = "Specified stack [Name:"+stack.getName()+", Revision: ["+stack.getStackRevision()+"] is already imported";
                throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
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
        URL blueprintUrl;
        try {
            blueprintUrl = new URL(stack.getBlueprintLocationURL());
            ObjectMapper m = new ObjectMapper();
            InputStream is = blueprintUrl.openStream();
            blueprint = m.readValue(is, Blueprint.class);
            /*
             * Validate default blueprint
             */
            validateDefaultBlueprint(blueprint, stack);
        } catch (WebApplicationException we) {
            throw we;
        } catch (Exception e) {
            this.stacks.get(stack.getName()).remove(stack.getStackRevision());
            if (this.stacks.get(stack.getName()).keySet().isEmpty()) {
                this.stacks.remove(stack.getName());
            }
            throw new WebApplicationException ((new ExceptionResponse(e)).get());
        }
        return blueprint;
    }
   
    /*
     * Validate the default blueprint before importing into stack.
     */
    public void validateDefaultBlueprint(Blueprint blueprint, Stack stack) throws WebApplicationException {
        if (!blueprint.getName().equals("default")) {
            String msg = "Default blueprint associated with stack must be named <default>";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        if (!blueprint.getName().equals(blueprint.getParentName()) ||
            !blueprint.getParentRevision().equals(blueprint.getParentRevision())) {
            String msg = "Parent name & version of default blueprint associated with stack must be same as default blueprint";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        /*
         * Check if stack name and revision matches with blueprint stackname and blueprint revision
         */
        if (!blueprint.getStackName().equals(stack.getName()) ||
            !blueprint.getRevision().equals(stack.getStackRevision())) {
            String msg = "Default blueprint's stanck name and revision does not match with it's associated stack";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
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
            String msg = "Stack ["+stackName+"] revision ["+revision+"] does not exists";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        return sk;
    }
    
    /* 
     * Get default blueprint for given stack 
    */
    public Blueprint getDefaultBlueprint(String stackName, int revision) throws Exception {
        
        Blueprint bp = this.default_blueprints.get(stackName+":"+revision);
        
        if (bp == null) {
            String msg = "Stack ["+stackName+"] revision ["+revision+"] does not exists";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        return bp;
    }
    
    /*
     * Returns stack names
     */
    public JSONArray getStackList() throws Exception {
        List<String> list = new ArrayList<String>();
        list.addAll(this.stacks.keySet());
        return new JSONArray(list);
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
