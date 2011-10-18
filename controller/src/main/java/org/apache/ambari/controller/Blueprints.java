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
import org.apache.ambari.common.rest.entities.BlueprintInformation;
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
        ambari.getProperty().add(getProperty ("AMBARI_INSTALL_DIR", "/var/ambari"));
        ambari.getProperty().add(getProperty ("AMBARI_HADOOP_JAVA_HOME", "/home/hms/apps/jdk1.6.0_27"));
        ambari.getProperty().add(getProperty ("AMBARI_HADOOP_NN_DIR","/grid/2/hadoop/var/hdfs/name")); 
        ambari.getProperty().add(getProperty ("AMBARI_DATA_DIRS", "/grid/*"));
        ambari.getProperty().add(getProperty ("AMBARI_HADOOP_SECURITY", "false"));
        
        ambari.getProperty().add(getProperty ("AMBARI_HADOOP_DN_ADDR", "DEFAULT"));
        ambari.getProperty().add(getProperty ("AMBARI_HADOOP_DN_HTTP_ADDR", "DEFAULT"));
        ambari.getProperty().add(getProperty ("AMBARI_HADOOP_NN_HOST", "DEFAULT"));
        
        

        
        ConfigurationCategory core_site = new ConfigurationCategory();
        core_site.setName("core-site");
        core_site.getProperty().add(getProperty ("local.realm","${KERBEROS_REALM}"));
        core_site.getProperty().add(getProperty ("fs.default.name","hdfs://${HADOOP_NN_HOST}:8020"));
        core_site.getProperty().add(getProperty ("fs.trash.interval","360"));
        core_site.getProperty().add(getProperty ("hadoop.security.auth_to_local","RULE:[2:$1@$0]([jt]t@.*${KERBEROS_REALM})s/.*/${HADOOP_MR_USER}/ RULE:[2:$1@$0](hm@.*${KERBEROS_REALM})s/.*/${HADOOP_HDFS_USER}/ RULE:[2:$1@$0](rs@.*${KERBEROS_REALM})s/.*/${HADOOP_HDFS_USER}/ RULE:[2:$1@$0]([nd]n@.*${KERBEROS_REALM})s/.*/${HADOOP_HDFS_USER}/ RULE:[2:$1@$0](mapred@.*${KERBEROS_REALM})s/.*/${HADOOP_MR_USER}/ RULE:[2:$1@$0](hdfs@.*${KERBEROS_REALM})s/.*/${HADOOP_HDFS_USER}/ RULE:[2:$1@$0](mapredqa@.*${KERBEROS_REALM})s/.*/${HADOOP_MR_USER}/ RULE:[2:$1@$0](hdfsqa@.*${KERBEROS_REALM})s/.*/${HADOOP_HDFS_USER}/ DEFAULT"));
        core_site.getProperty().add(getProperty ("hadoop.security.authentication","${SECURITY_TYPE}"));
        core_site.getProperty().add(getProperty ("hadoop.security.authorization","${SECURITY}"));
        core_site.getProperty().add(getProperty ("hadoop.security.groups.cache.secs","14400"));
        core_site.getProperty().add(getProperty ("hadoop.kerberos.kinit.command","${KINIT}"));
        core_site.getProperty().add(getProperty ("hadoop.http.filter.initializers","org.apache.hadoop.http.lib.StaticUserWebFilter"));
        
        ConfigurationCategory hdfs_site = new ConfigurationCategory();
        hdfs_site.setName("hdfs-site");
        hdfs_site.getProperty().add(getProperty ("dfs.name.dir","${HADOOP_NN_DIR}"));
        hdfs_site.getProperty().add(getProperty ("dfs.data.dir","${HADOOP_DN_DIR}"));
        hdfs_site.getProperty().add(getProperty ("dfs.safemode.threshold.pct","1.0f"));
        hdfs_site.getProperty().add(getProperty ("dfs.datanode.address","${HADOOP_DN_ADDR}"));
        hdfs_site.getProperty().add(getProperty ("dfs.datanode.http.address","${HADOOP_DN_HTTP_ADDR}"));
        hdfs_site.getProperty().add(getProperty ("dfs.http.address","${HADOOP_NN_HOST}:50070"));
        hdfs_site.getProperty().add(getProperty ("dfs.umaskmode","077"));
        hdfs_site.getProperty().add(getProperty ("dfs.block.access.token.enable","${SECURITY}"));
        hdfs_site.getProperty().add(getProperty ("dfs.namenode.kerberos.principal","nn/_HOST@${local.realm}"));
        hdfs_site.getProperty().add(getProperty ("dfs.secondary.namenode.kerberos.principal","nn/_HOST@${local.realm}"));
        hdfs_site.getProperty().add(getProperty ("dfs.namenode.kerberos.https.principal","host/_HOST@${local.realm}"));
        hdfs_site.getProperty().add(getProperty ("dfs.secondary.namenode.kerberos.https.principal","host/_HOST@${local.realm}"));
        hdfs_site.getProperty().add(getProperty ("dfs.secondary.https.port","50490"));
        hdfs_site.getProperty().add(getProperty ("dfs.datanode.kerberos.principal","dn/_HOST@${local.realm}"));
        hdfs_site.getProperty().add(getProperty ("dfs.namenode.keytab.file","/etc/security/keytabs/nn.service.keytab"));
        hdfs_site.getProperty().add(getProperty ("dfs.secondary.namenode.keytab.file","/etc/security/keytabs/nn.service.keytab"));
        hdfs_site.getProperty().add(getProperty ("dfs.datanode.keytab.file","/etc/security/keytabs/dn.service.keytab"));
        hdfs_site.getProperty().add(getProperty ("dfs.https.port","50470"));
        hdfs_site.getProperty().add(getProperty ("dfs.https.address","${HADOOP_NN_HOST}:50470"));
        hdfs_site.getProperty().add(getProperty ("dfs.datanode.data.dir.perm","700"));
        hdfs_site.getProperty().add(getProperty ("dfs.cluster.administrators","${HADOOP_HDFS_USER}"));
        hdfs_site.getProperty().add(getProperty ("dfs.permissions.superusergroup","${HADOOP_GROUP}"));
        hdfs_site.getProperty().add(getProperty ("dfs.namenode.http-address","${HADOOP_NN_HOST}:50070"));
        hdfs_site.getProperty().add(getProperty ("dfs.namenode.https-address","${HADOOP_NN_HOST}:50470"));
        hdfs_site.getProperty().add(getProperty ("dfs.secondary.http.address","${HADOOP_SNN_HOST}:50090"));
        hdfs_site.getProperty().add(getProperty ("dfs.hosts","${HADOOP_CONF_DIR}/dfs.include"));
        hdfs_site.getProperty().add(getProperty ("dfs.hosts.exclude","${HADOOP_CONF_DIR}/dfs.exclude"));
        
        ConfigurationCategory hadoop_env = new ConfigurationCategory();
        hadoop_env.setName("hadoop-env");
        hadoop_env.getProperty().add(getProperty ("JAVA_HOME","${JAVA_HOME}"));
        hadoop_env.getProperty().add(getProperty ("HADOOP_CONF_DIR","${HADOOP_CONF_DIR:-\"/etc/hadoop\"}"));
        hadoop_env.getProperty().add(getProperty ("HADOOP_OPTS","\"-Djava.net.preferIPv4Stack=true $HADOOP_OPTS\""));
        hadoop_env.getProperty().add(getProperty ("HADOOP_NAMENODE_OPTS","\"-Dsecurity.audit.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_NAMENODE_OPTS}\""));
        hadoop_env.getProperty().add(getProperty ("HADOOP_JOBTRACKER_OPTS","\"-Dsecurity.audit.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dmapred.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\""));
        hadoop_env.getProperty().add(getProperty ("HADOOP_TASKTRACKER_OPTS","\"-Dsecurity.audit.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\""));
        hadoop_env.getProperty().add(getProperty ("HADOOP_DATANODE_OPTS","\"-Dsecurity.audit.logger=ERROR,DRFAS ${HADOOP_DATANODE_OPTS}\""));
        hadoop_env.getProperty().add(getProperty ("HADOOP_SECONDARYNAMENODE_OPTS","\"-Dsecurity.audit.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_SECONDARYNAMENODE_OPTS}\""));
        hadoop_env.getProperty().add(getProperty ("HADOOP_CLIENT_OPTS","\"-Xmx128m ${HADOOP_CLIENT_OPTS}\""));
        hadoop_env.getProperty().add(getProperty ("HADOOP_SECURE_DN_USER","${HADOOP_SECURE_DN_USER}"));
        hadoop_env.getProperty().add(getProperty ("HADOOP_LOG_DIR","${HADOOP_LOG_DIR}/$USER"));
        hadoop_env.getProperty().add(getProperty ("HADOOP_SECURE_DN_LOG_DIR","${HADOOP_LOG_DIR}/${HADOOP_HDFS_USER}"));
        hadoop_env.getProperty().add(getProperty ("HADOOP_PID_DIR","${HADOOP_PID_DIR}"));
        hadoop_env.getProperty().add(getProperty ("HADOOP_SECURE_DN_PID_DIR","${HADOOP_PID_DIR}"));
        hadoop_env.getProperty().add(getProperty ("HADOOP_IDENT_STRING","$USER"));

        ConfigurationCategory hadoop_metrics2 = new ConfigurationCategory();
        hadoop_metrics2.setName("hadoop_metrics2.properties");
        hadoop_metrics2.getProperty().add(getProperty ("*.period","60"));
        
        bpDefaultCfg.getCategory().add(core_site);
        bpDefaultCfg.getCategory().add(hdfs_site);
        bpDefaultCfg.getCategory().add(hadoop_env);
        bpDefaultCfg.getCategory().add(hadoop_metrics2);
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
        //hdfs_nn_role.setConfiguration(bpDefaultCfg);
        
        Role hdfs_dn_role = new Role();
        hdfs_dn_role.setName("datanode");
        //hdfs_dn_role.setConfiguration(bpDefaultCfg);
        
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
                String msg = "Blueprint ["+blueprintName+"], revision ["+revision+"] does not exists";
                throw new WebApplicationException ((new ExceptionResponse(msg, Response.Status.NOT_FOUND)).get());
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
    public List<BlueprintInformation> getBlueprintList() throws Exception {
        List<BlueprintInformation> list = new ArrayList<BlueprintInformation>();
        for (String bpName : this.blueprints.keySet()) {
            // Get the latest blueprint
            Blueprint bp = this.getBlueprint(bpName, -1);
            BlueprintInformation bpInfo = new BlueprintInformation();
            // TODO: get the creation and update times from blueprint
            bpInfo.setLastUpdateTime(null);
            bpInfo.setCreationTime(null);
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
