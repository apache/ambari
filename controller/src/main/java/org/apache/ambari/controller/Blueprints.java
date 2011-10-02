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
import org.apache.ambari.common.rest.entities.Cluster;
import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.common.rest.entities.ConfigPropertiesCategory;
import org.apache.ambari.common.rest.entities.Configuration;
import org.apache.ambari.common.rest.entities.PackageRepository;
import org.apache.ambari.common.rest.entities.Property;
import org.apache.ambari.common.rest.entities.Role;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.resource.statemachine.ClusterState;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Blueprints {

    private static Blueprints BlueprintsRef=null;
        
    private Blueprints() {
      
        Blueprint bp = new Blueprint();
        bp.setName("MyClusterBlueprint");
        bp.setStackName("hortonworks-3.0");
        bp.setParentName("MySiteBlueprint");
        bp.setRevision("0");
        bp.setParentRevision("0");
 
        Component hdfsC = new Component(); hdfsC.setName("hdfs");
        hdfsC.getProperty().add(getProperty ("dfs.name.dir", "${HADOOP_NN_DIR}"));
        hdfsC.getProperty().add(getProperty ("dfs.data.dir", "${HADOOP_DATA_DIR}"));
        Component mapredC = new Component(); mapredC.setName("hdfs");
        mapredC.getProperty().add(getProperty ("mapred.system.dir", "/mapred/mapredsystem"));
        mapredC.getProperty().add(getProperty ("mapred.local.dir", "${HADOOP_MAPRED_DIR}"));
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
        ConfigPropertiesCategory hdfs_site = new ConfigPropertiesCategory();
        hdfs_site.setName("hdfs-site");
        ConfigPropertiesCategory mapred_site = new ConfigPropertiesCategory();
        mapred_site.setName("mapred-site");  
        hdfs_site.getProperty().add(getProperty ("dfs.name.dir", "/tmp/namenode"));
        hdfs_site.getProperty().add(getProperty ("dfs.data.dir", "/tmp/datanode"));
        mapred_site.getProperty().add(getProperty ("mapred.system.dir", "/mapred/mapredsystem"));
        mapred_site.getProperty().add(getProperty ("mapred.local.dir", "/tmp/mapred")); 
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
        
        ConcurrentHashMap<Integer, Blueprint> x = new ConcurrentHashMap<Integer, Blueprint>();
        x.put(new Integer(bp.getRevision()), bp);
        this.blueprints.put(bp.getName(), x);
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
     * Get blueprint
     */
    public Blueprint getBlueprint(String blueprintName, int revision) throws Exception {
        Blueprint bp = this.blueprints.get(blueprintName).get(revision);
        
        if (bp == null) {
            Exception e = new Exception ("Stack ["+blueprintName+"] revision ["+revision+"] does not exists");
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        return bp;  
    }
     
    /*
     * Add or update the blueprint
     */
    public void addBlueprint(Blueprint bp) throws Exception {
        
        /* 
         * Validate the name is not a reserved keyword and it does not exists already
         * TODO: Check the specified parent blueprint already exits? may be not! 
         */
        if (bp.getName().equals("default")) {
            String msg = "Blueprint can not have name default, a reserved keyword for default blueprints associated w/ Hadoop stack";
            throw new WebApplicationException((new ExceptionResponse(msg, Response.Status.NOT_ACCEPTABLE)).get());
        }
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
    }
    
    /*
     * Return list of blueprint names
     */
    /*
     * Returns stack names
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
            String bpName = c.getClusterDefinition().getBlueprintName();
            String bpRevision = c.getClusterDefinition().getBlueprintRevision();
            
            // TODO: May be don't consider ATTIC clusters
            if (c.getClusterState().getState().equals(ClusterState.ATTIC)) {
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
         * If no cluster is associated then remvove the blueprint
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
}
