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

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.common.rest.entities.StackInformation;
import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.common.rest.entities.Property;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mortbay.log.Log;

public class Blueprints {

    private static Blueprints BlueprintsRef=null;
        
    private Blueprints() {
      try {
        JAXBContext jaxbContext = 
            JAXBContext.newInstance("org.apache.ambari.common.rest.entities");
        loadDummyBlueprint(jaxbContext, "hadoop-security", 0);
        loadDummyBlueprint(jaxbContext, "cluster123", 0);
        loadDummyBlueprint(jaxbContext, "cluster124", 0);
      } catch (JAXBException e) {
        throw new RuntimeException("Can't create jaxb context", e);
      }
    }
    
    public void loadDummyBlueprint (JAXBContext jaxbContext,
                                    String name, int revision) {
        try {
            Unmarshaller um = jaxbContext.createUnmarshaller();
            String resourceName =
                "org/apache/ambari/stacks/" + name + "-" + revision + ".xml";
            InputStream in = 
                ClassLoader.getSystemResourceAsStream(resourceName);
            Stack bp = (Stack) um.unmarshal(in);
            bp.setName(name);
            bp.setRevision(Integer.toString(revision));
            addStack(bp);
        } catch (IOException e) {
            Log.warn("Problem loading blueprint " + name + " rev " + revision,
                     e);
        } catch (JAXBException e) {
          Log.warn("Problem loading blueprint " + name + " rev " + revision,
              e);
        }
    }
    
    public static synchronized Blueprints getInstance() {
        if(BlueprintsRef == null) {
            BlueprintsRef = new Blueprints();
        }
        return BlueprintsRef;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
      
    /*
     * Stack name -> {revision -> Stack} .
     */
    protected ConcurrentHashMap<String, ConcurrentHashMap<Integer,Stack>> stacks = new ConcurrentHashMap<String,ConcurrentHashMap<Integer,Stack>>();
    
    
    /*
     * Get blueprint. If revision = -1 then return latest revision
     */
    public Stack getStack(String blueprintName, int revision) throws Exception {
        /*
         * If revision is -1, then return the latest revision
         */  
        Stack bp = null;
        if (!this.stacks.containsKey(blueprintName)) {  
            String msg = "Stack ["+blueprintName+"] is not defined";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        if (revision == -1) {
            this.stacks.get(blueprintName).keySet();
            Integer [] a = new Integer [] {};
            Integer[] keys = this.stacks.get(blueprintName).keySet().toArray(a);
            Arrays.sort(keys);  
            bp = this.stacks.get(blueprintName).get(keys[keys.length-1]);
        } else {
            if (!this.stacks.get(blueprintName).containsKey(revision)) {  
                String msg = "Stack ["+blueprintName+"], revision ["+revision+"] does not exist";
                throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
            }
            bp = this.stacks.get(blueprintName).get(revision);
        }
        return bp;  
    }
     
    /*
     * Add or update the blueprint
     */
    public Stack addStack(Stack bp) throws IOException {
        
        /*
         * Validate and set the defaults
         */
        validateAndSetStackDefaults(bp);
        
        if (stacks.containsKey(bp.getName())) {
            if (stacks.get(bp.getName()).containsKey(new Integer(bp.getRevision()))) {
                String msg = "Specified blueprint [Name:"+bp.getName()+", Revision: ["+bp.getRevision()+"] already imported";
                throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
            } else {
                stacks.get(bp.getName()).put(new Integer(bp.getRevision()), bp);
            }
        } else {
            ConcurrentHashMap<Integer, Stack> x = new ConcurrentHashMap<Integer, Stack>();
            x.put(new Integer(bp.getRevision()), bp);
            this.stacks.put(bp.getName(), x);
        }
        
        return bp;
    }
    
    /*
     * Import the default blueprint from the URL location
     */
    public Stack importDefaultStack (String locationURL) throws IOException {
        Stack stack;
        URL blueprintUrl;
        try {
            blueprintUrl = new URL(locationURL);
            InputStream is = blueprintUrl.openStream();
            
            /* JSON FORMAT READER
            ObjectMapper m = new ObjectMapper();
            blueprint = m.readValue(is, Stack.class);
            */
            JAXBContext jc = JAXBContext.newInstance(org.apache.ambari.common.rest.entities.Stack.class);
            Unmarshaller u = jc.createUnmarshaller();
            stack = (Stack)u.unmarshal(is);
            return addStack(stack);
        } catch (WebApplicationException we) {
            throw we;
        } catch (Exception e) {
            throw new WebApplicationException ((new ExceptionResponse(e)).get());
        }
    }
   
    /*
     * Validate the blueprint before importing into controller
     */
    public void validateAndSetStackDefaults(Stack stack) throws IOException {
        
        if (stack.getName() == null || stack.getName().equals("")) {
            String msg = "Stack must be associated with non-empty name";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        if (stack.getRevision() == null || stack.getRevision().equals("") ||
            stack.getRevision().equalsIgnoreCase("null")) {
            stack.setRevision("-1");
        }
        if (stack.getParentName() != null && 
            (stack.getParentName().equals("") || stack.getParentName().equalsIgnoreCase("null"))) {
            stack.setParentName(null);
        }
        if (stack.getParentRevision() == null || stack.getParentRevision().equals("") ||
            stack.getParentRevision().equalsIgnoreCase("null")) {
            stack.setParentRevision("-1");
        }
        /*
         * Set the creation time 
         */
        stack.setCreationTime(new Date());
    }
    
    /*
     *  Get the list of blueprint revisions
     */
    public List<StackInformation> getBlueprintRevisions(String blueprintName) throws Exception {
        List<StackInformation> list = new ArrayList<StackInformation>();
        if (!this.stacks.containsKey(blueprintName)) {
            String msg = "Stack ["+blueprintName+"] does not exist";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        ConcurrentHashMap<Integer, Stack> revisions = this.stacks.get(blueprintName);
        for (Integer x : revisions.keySet()) {
            // Get the latest blueprint
            Stack bp = revisions.get(x);
            StackInformation bpInfo = new StackInformation();
            // TODO: get the creation time from blueprint
            bpInfo.setCreationTime(bp.getCreationTime());
            bpInfo.setName(bp.getName());
            bpInfo.setRevision(bp.getRevision());
            bpInfo.setParentName(bp.getParentName());
            bpInfo.setParentRevision(bp.getParentRevision());
            List<String> componentNameVersions = new ArrayList<String>();
            for (Component com : bp.getComponents()) {
                String comNameVersion = ""+com.getName()+"-"+com.getVersion();
                componentNameVersions.add(comNameVersion);
            }
            bpInfo.setComponent(componentNameVersions);
            list.add(bpInfo);
        }
        return list;
    }
    
    /*
     * Return list of blueprint names
     */
    public List<StackInformation> getBlueprintList() throws Exception {
        List<StackInformation> list = new ArrayList<StackInformation>();
        for (String bpName : this.stacks.keySet()) {
            // Get the latest blueprint
            Stack bp = this.getStack(bpName, -1);
            StackInformation bpInfo = new StackInformation();
            // TODO: get the creation and update times from blueprint
            bpInfo.setCreationTime(bp.getCreationTime());
            bpInfo.setName(bp.getName());
            bpInfo.setRevision(bp.getRevision());
            bpInfo.setParentName(bp.getParentName());
            bpInfo.setParentRevision(bp.getParentRevision());
            List<String> componentNameVersions = new ArrayList<String>();
            for (Component com : bp.getComponents()) {
                String comNameVersion = ""+com.getName()+"-"+com.getVersion();
                componentNameVersions.add(comNameVersion);
            }
            bpInfo.setComponent(componentNameVersions);
            list.add(bpInfo);
        }
        return list;
    }
    
    /*
     * Delete the specified version of blueprint
     * TODO: Check if blueprint is associated with any stack... 
     */
    public void deleteBlueprint(String blueprintName, int revision) throws Exception {
        
        /*
         * Check if the specified blueprint revision is used in any cluster definition
         */
        Hashtable<String, String> clusterReferencedBPList = getClusterReferencedBlueprintsList();
        if (clusterReferencedBPList.containsKey(blueprintName+"-"+revision)) {
            String msg = "One or more clusters are associated with the specified blueprint";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_ACCEPTABLE)).get());
        }
        
        /*
         * If no cluster is associated then remove the blueprint
         */
        this.stacks.get(blueprintName).remove(revision);
        if (this.stacks.get(blueprintName).keySet().isEmpty()) {
            this.stacks.remove(blueprintName);
        }    
    }
    
    /*
     * Returns the <key="name-revision", value=""> hash table for cluster referenced stacks
     */
    public Hashtable<String, String> getClusterReferencedBlueprintsList() throws Exception {
        Hashtable<String, String> clusterBlueprints = new Hashtable<String, String>();
        for (Cluster c : Clusters.getInstance().operational_clusters.values()) {
            String cBPName = c.getLatestClusterDefinition().getBlueprintName();
            String cBPRevision = c.getLatestClusterDefinition().getBlueprintRevision();
            Stack bpx = this.getStack(cBPName, Integer.parseInt(cBPRevision));
            clusterBlueprints.put(cBPName+"-"+cBPRevision, "");
            while (bpx.getParentName() != null) {
                if (bpx.getParentRevision() == null) {
                    bpx = this.getStack(bpx.getParentName(), -1);
                } else {
                    bpx = this.getStack(bpx.getParentName(), Integer.parseInt(bpx.getParentRevision()));
                }
                clusterBlueprints.put(bpx.getName()+"-"+bpx.getRevision(), "");
            }
        }
        return clusterBlueprints;
    }
   
    /*
     * UTIL methods
     */
    public Property getProperty(String key, String value) {
        Property p = new Property();
        p.setName(key);
        p.setValue(value);
        return p;
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
