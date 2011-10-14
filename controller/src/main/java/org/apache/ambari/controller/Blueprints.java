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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.common.rest.entities.Blueprint;
import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.common.rest.entities.ComponentDefinition;
import org.apache.ambari.common.rest.entities.Configuration;
import org.apache.ambari.common.rest.entities.ConfigurationCategory;
import org.apache.ambari.common.rest.entities.RepositoryKind;
import org.apache.ambari.common.rest.entities.Property;
import org.apache.ambari.common.rest.entities.Role;
import org.apache.ambari.resource.statemachine.ClusterStateFSM;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Blueprints {

    private static Blueprints BlueprintsRef=null;
        
    private Blueprints() {
        this.createDummyBlueprint("MyDummyBlueprint", "0", "MyDummySiteBlueprint", "0");
    }
    
    public void createDummyBlueprint (String name, String revision, String siteName, String siteVersion) {
        Blueprint bp = new Blueprint();
        
        bp.setName(name);
        bp.setParentName(siteName);
        bp.setRevision(revision);
        bp.setParentRevision(siteVersion);
        
        /*
         * Repository URLs
         */
        List<RepositoryKind> prList = new ArrayList<RepositoryKind>();
        RepositoryKind hdfsRepo = new RepositoryKind();
        hdfsRepo.setKind("TAR");
        List<String> repoURLs = new ArrayList<String>();
        repoURLs.add("http://www.apache.org/dist/hadoop/common/");   
        hdfsRepo.setUrls(repoURLs);
        prList.add(hdfsRepo);
        bp.setPackageRepositories(prList);
        
        /*
         * Global Configuration
         */
        Configuration bpDefaultCfg = new Configuration();
        
        ConfigurationCategory ambari = new ConfigurationCategory();
        ambari.setName("ambari");
        ambari.getProperty().add(getProperty ("ambari.install.dir", "/var/ambari"));
        ambari.getProperty().add(getProperty ("ambari.data.dir.pattern", "/grid/*"));
        
        ConfigurationCategory core_site = new ConfigurationCategory();
        core_site.setName("core-site");
        core_site.getProperty().add(getProperty ("dfs.name.dir", "/tmp/namenode"));
        
        ConfigurationCategory hdfs_site = new ConfigurationCategory();
        hdfs_site.setName("hdfs-site");
        hdfs_site.getProperty().add(getProperty ("dfs.name.dir", "/tmp/namenode"));
        hdfs_site.getProperty().add(getProperty ("dfs.data.dir", "/tmp/datanode"));
        
        bpDefaultCfg.getCategory().add(core_site);
        bpDefaultCfg.getCategory().add(hdfs_site);
        bpDefaultCfg.getCategory().add(ambari);
        
        bp.setConfiguration(bpDefaultCfg);
        
        /*
         * Define and add common component
         */
        List<Component> compList = new ArrayList<Component>();
        
        Component commonC = new Component(); 
        commonC.setName("common");
        commonC.setArchitecture("x86_64");
        commonC.setVersion("0.20.205.0");
        commonC.setProvider("org.apache.hadoop");
        ComponentDefinition commonCD = new ComponentDefinition(); 
        commonCD.setGroup("org.apache.ambari");
        commonCD.setDefinition("hadoop-common-0.1.0.acd");
        commonCD.setVersion("0.1.0");
        commonC.setDefinition(commonCD);
        
        compList.add(commonC);
        
        /*
         * Define and add hdfs component
         */
        Component hdfsC = new Component(); 
        hdfsC.setName("hdfs");
        hdfsC.setArchitecture("x86_64");
        hdfsC.setVersion("0.20.205.0");
        hdfsC.setProvider("org.apache.hadoop");
        ComponentDefinition hdfsCD = new ComponentDefinition(); 
        hdfsCD.setGroup("org.apache.ambari");
        hdfsCD.setDefinition("hadoop-hdfs-0.1.0.acd");
        hdfsCD.setVersion("0.1.0");
        hdfsC.setDefinition(hdfsCD);
        /*
         * Set the list of roles to hdfsC
         */
        List<Role> hdfsRoleList = new ArrayList<Role>();
        Role hdfs_nn_role = new Role();
        hdfs_nn_role.setName("namenode");
        hdfs_nn_role.setConfiguration(bpDefaultCfg);
        
        Role hdfs_dn_role = new Role();
        hdfs_dn_role.setName("datanode");
        hdfs_dn_role.setConfiguration(bpDefaultCfg);
        
        hdfsRoleList.add(hdfs_nn_role);
        hdfsRoleList.add(hdfs_dn_role);
        hdfsC.setRoles(hdfsRoleList);
       
        compList.add(hdfsC);  
        bp.setComponents(compList);
     
      
        try {
            addBlueprint (bp);
        }catch (Exception e) {
            e.printStackTrace();
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
        if (revision == -1) {
            this.blueprints.get(blueprintName).keySet();
            Integer [] a = new Integer [] {};
            Integer[] keys = this.blueprints.get(blueprintName).keySet().toArray(a);
            Arrays.sort(keys);  
            bp = this.blueprints.get(blueprintName).get(keys[keys.length-1]);
        } else {
            if (!this.blueprints.containsKey(blueprintName) || !this.blueprints.get(blueprintName).containsKey(revision)) {  
                String msg = "Blueprint ["+blueprintName+"], revision ["+revision+"] does not exists";
                throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
            }
            bp = this.blueprints.get(blueprintName).get(revision);
        }
        return bp;  
    }
     
    /*
     * Add or update the blueprint
     */
    public Blueprint addBlueprint(Blueprint bp) throws Exception {
        
        /*
         * Validate the blueprint
         */
        validateDefaultBlueprint(bp);
        
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
    public Blueprint importDefaultBlueprint (String locationURL) throws Exception {
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
     * Validate the default blueprint before importing into stack.
     */
    public void validateDefaultBlueprint(Blueprint blueprint) throws WebApplicationException {
        
        if (blueprint.getName() == null || blueprint.getName().equals("")) {
            String msg = "Blueprint must be associated with non-empty name";
            throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.BAD_REQUEST)).get());
        }
    }
    
    /*
     * Return list of blueprint names
     */
    public JSONArray getBlueprintList() throws Exception {
        List<String> list = new ArrayList<String>();
        list.addAll(this.blueprints.keySet());
        return new JSONArray(list);
    }
    
    /*
     * Delete the specified version of blueprint
     * TODO: Check if blueprint is associated with any stack... 
     */
    public void deleteBlueprint(String blueprintName, int revision) throws Exception {
        
        /*
         * Check if the specified blueprint revision is used in any cluster definition
         * except in ATTIC clusters.
         */
        Blueprint bp = this.blueprints.get(blueprintName).get(new Integer(revision));
        for (Cluster c : Clusters.getInstance().operational_clusters.values()) {
            String bpName = c.getLatestClusterDefinition().getBlueprintName();
            String bpRevision = c.getLatestClusterDefinition().getBlueprintRevision();
            
            // TODO: May be don't consider ATTIC clusters
            if (c.getClusterState().getState().equals(ClusterStateFSM.ATTIC)) {
                continue;
            }
            Blueprint bpx = Blueprints.getInstance().blueprints.get(bpName).get(new Integer(bpRevision));
            if (bpx.getName().equals(bp.getName()) && bpx.getRevision().equals(bp.getRevision()) ||
                bpx.getParentName().equals(bp.getParentName()) && bpx.getParentRevision().equals(bp.getParentRevision())) {
                String msg = "One or more clusters are associated with the specified blueprint";
                throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_ACCEPTABLE)).get());
            }
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
