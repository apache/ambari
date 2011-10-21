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

import org.apache.ambari.common.rest.entities.Blueprint;
import org.apache.ambari.common.rest.entities.BlueprintInformation;
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
            Blueprint bp = (Blueprint) um.unmarshal(in);
            bp.setName(name);
            bp.setRevision(Integer.toString(revision));
            addBlueprint(bp);
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
     * Blueprint name -> {revision -> Blueprint} .
     */
    protected ConcurrentHashMap<String, ConcurrentHashMap<Integer,Blueprint>> blueprints = new ConcurrentHashMap<String,ConcurrentHashMap<Integer,Blueprint>>();
    
    
    /*
     * Get blueprint. If revision = -1 then return latest revision
     */
    public Blueprint getBlueprint(String blueprintName, int revision) throws Exception {
        /*
         * If revision is -1, then return the latest revision
         */  
        Blueprint bp = null;
        if (!this.blueprints.containsKey(blueprintName)) {  
            String msg = "Blueprint ["+blueprintName+"] is not defined";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        if (revision == -1) {
            this.blueprints.get(blueprintName).keySet();
            Integer [] a = new Integer [] {};
            Integer[] keys = this.blueprints.get(blueprintName).keySet().toArray(a);
            Arrays.sort(keys);  
            bp = this.blueprints.get(blueprintName).get(keys[keys.length-1]);
        } else {
            if (!this.blueprints.get(blueprintName).containsKey(revision)) {  
                String msg = "Blueprint ["+blueprintName+"], revision ["+revision+"] does not exist";
                throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
            }
            bp = this.blueprints.get(blueprintName).get(revision);
        }
        return bp;  
    }
     
    /*
     * Add or update the blueprint
     */
    public Blueprint addBlueprint(Blueprint bp) throws IOException {
        
        /*
         * Validate and set the defaults
         */
        validateAndSetBlueprintDefaults(bp);
        
        if (blueprints.containsKey(bp.getName())) {
            if (blueprints.get(bp.getName()).containsKey(new Integer(bp.getRevision()))) {
                String msg = "Specified blueprint [Name:"+bp.getName()+", Revision: ["+bp.getRevision()+"] already imported";
                throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
            } else {
                blueprints.get(bp.getName()).put(new Integer(bp.getRevision()), bp);
            }
        } else {
            ConcurrentHashMap<Integer, Blueprint> x = new ConcurrentHashMap<Integer, Blueprint>();
            x.put(new Integer(bp.getRevision()), bp);
            this.blueprints.put(bp.getName(), x);
        }
        
        return bp;
    }
    
    /*
     * Import the default blueprint from the URL location
     */
    public Blueprint importDefaultBlueprint (String locationURL) throws IOException {
        Blueprint blueprint;
        URL blueprintUrl;
        try {
            blueprintUrl = new URL(locationURL);
            InputStream is = blueprintUrl.openStream();
            
            /* JSON FORMAT READER
            ObjectMapper m = new ObjectMapper();
            blueprint = m.readValue(is, Blueprint.class);
            */
            JAXBContext jc = JAXBContext.newInstance(org.apache.ambari.common.rest.entities.Blueprint.class);
            Unmarshaller u = jc.createUnmarshaller();
            blueprint = (Blueprint)u.unmarshal(is);
            return addBlueprint(blueprint);
        } catch (WebApplicationException we) {
            throw we;
        } catch (Exception e) {
            throw new WebApplicationException ((new ExceptionResponse(e)).get());
        }
    }
   
    /*
     * Validate the blueprint before importing into controller
     */
    public void validateAndSetBlueprintDefaults(Blueprint blueprint) throws IOException {
        
        if (blueprint.getName() == null || blueprint.getName().equals("")) {
            String msg = "Blueprint must be associated with non-empty name";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
        if (blueprint.getRevision() == null || blueprint.getRevision().equals("") ||
            blueprint.getRevision().equalsIgnoreCase("null")) {
            blueprint.setRevision("-1");
        }
        if (blueprint.getParentName() != null && 
            (blueprint.getParentName().equals("") || blueprint.getParentName().equalsIgnoreCase("null"))) {
            blueprint.setParentName(null);
        }
        if (blueprint.getParentRevision() == null || blueprint.getParentRevision().equals("") ||
            blueprint.getParentRevision().equalsIgnoreCase("null")) {
            blueprint.setParentRevision("-1");
        }
        /*
         * Set the creation time 
         */
        blueprint.setCreationTime(new Date());
    }
    
    /*
     *  Get the list of blueprint revisions
     */
    public List<BlueprintInformation> getBlueprintRevisions(String blueprintName) throws Exception {
        List<BlueprintInformation> list = new ArrayList<BlueprintInformation>();
        if (!this.blueprints.containsKey(blueprintName)) {
            String msg = "Blueprint ["+blueprintName+"] does not exist";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
        }
        ConcurrentHashMap<Integer, Blueprint> revisions = this.blueprints.get(blueprintName);
        for (Integer x : revisions.keySet()) {
            // Get the latest blueprint
            Blueprint bp = revisions.get(x);
            BlueprintInformation bpInfo = new BlueprintInformation();
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
    public List<BlueprintInformation> getBlueprintList() throws Exception {
        List<BlueprintInformation> list = new ArrayList<BlueprintInformation>();
        for (String bpName : this.blueprints.keySet()) {
            // Get the latest blueprint
            Blueprint bp = this.getBlueprint(bpName, -1);
            BlueprintInformation bpInfo = new BlueprintInformation();
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
        this.blueprints.get(blueprintName).remove(revision);
        if (this.blueprints.get(blueprintName).keySet().isEmpty()) {
            this.blueprints.remove(blueprintName);
        }    
    }
    
    /*
     * Returns the <key="name-revision", value=""> hash table for cluster referenced blueprints
     */
    public Hashtable<String, String> getClusterReferencedBlueprintsList() throws Exception {
        Hashtable<String, String> clusterBlueprints = new Hashtable<String, String>();
        for (Cluster c : Clusters.getInstance().operational_clusters.values()) {
            String cBPName = c.getLatestClusterDefinition().getBlueprintName();
            String cBPRevision = c.getLatestClusterDefinition().getBlueprintRevision();
            Blueprint bpx = this.getBlueprint(cBPName, Integer.parseInt(cBPRevision));
            clusterBlueprints.put(cBPName+"-"+cBPRevision, "");
            while (bpx.getParentName() != null) {
                if (bpx.getParentRevision() == null) {
                    bpx = this.getBlueprint(bpx.getParentName(), -1);
                } else {
                    bpx = this.getBlueprint(bpx.getParentName(), Integer.parseInt(bpx.getParentRevision()));
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
