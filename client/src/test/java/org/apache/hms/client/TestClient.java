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

package org.apache.hms.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.hms.common.entity.action.Action;
import org.apache.hms.common.entity.action.PackageAction;
import org.apache.hms.common.entity.manifest.ClusterManifest;
import org.apache.hms.common.entity.manifest.ConfigManifest;
import org.apache.hms.common.entity.manifest.NodesManifest;
import org.apache.hms.common.entity.manifest.PackageInfo;
import org.apache.hms.common.entity.manifest.Role;
import org.apache.hms.common.entity.manifest.SoftwareManifest;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.common.util.JAXBUtil;
import org.testng.annotations.Test;

public class TestClient {
  @Test
  public void testCreateCluster() {
    try {
      File nodesXmlFile = File.createTempFile("nodes", ".xml");
      nodesXmlFile.deleteOnExit();
      File softwareXmlFile = File.createTempFile("software", ".xml");
      softwareXmlFile.deleteOnExit();
      File configXmlFile = File.createTempFile("config", ".xml");
      configXmlFile.deleteOnExit();

      String nodesXmlPath = nodesXmlFile.getAbsolutePath();
      String softwareXmlPath = softwareXmlFile.getAbsolutePath();
      String configXmlPath = configXmlFile.getAbsolutePath();

      // Setup simulated controller
      // Create node manifest
      NodesManifest n = new NodesManifest();
      List<Role> roles = new ArrayList<Role>();
      Role role = new Role();
      role.setName("namenode");
      String [] hosts = { "localhost" };
      role.setHosts(hosts);
      roles.add(role);
      n.setNodes(roles);
      FileWriter fstream = new FileWriter(nodesXmlPath);
      BufferedWriter out = new BufferedWriter(fstream);
      out.write( new String(JAXBUtil.write(n)).toCharArray());
      out.close();
      fstream.close();
      
      // Create software manifest
      SoftwareManifest sm = new SoftwareManifest();
      sm.setName("hadoop");
      sm.setVersion("0.20.203");
      List<Role> softwareRoles = new ArrayList<Role>();
      Role softwareRole = new Role();
      softwareRole.setName("namenode");
      PackageInfo[] packages = new PackageInfo[1];
      packages[0]= new PackageInfo();
      packages[0].setName("hadoop-0.20.203");
      softwareRole.setPackages(packages);
      softwareRoles.add(softwareRole);
      sm.setRoles(softwareRoles);
      fstream = new FileWriter(softwareXmlPath);
      out = new BufferedWriter(fstream);
      out.write(new String(JAXBUtil.write(sm)).toCharArray());
      out.close();
      fstream.close();
      
      // Create config manifest
      PackageAction installHadoop = new PackageAction();
      installHadoop.setPackages(packages);
      List<Action> actions = new ArrayList<Action>();
      actions.add(installHadoop);
      ConfigManifest cm = new ConfigManifest();
      cm.setActions(actions);
      fstream = new FileWriter(configXmlPath);
      out = new BufferedWriter(fstream);
      out.write(new String(JAXBUtil.write(cm)).toCharArray());
      out.close();
      fstream.close();
      URL softwareUrl = new URL("file://"+softwareXmlPath);
      URL nodeUrl = new URL("file://"+nodesXmlPath);
      URL configUrl = new URL("file://"+configXmlPath);
      
      // Create cluster manifest
      ClusterManifest clusterM = new ClusterManifest();
      NodesManifest nodes = new NodesManifest();
      nodes.setUrl(nodeUrl);
      clusterM.setNodes(nodes);
      SoftwareManifest softwareM = new SoftwareManifest();
      softwareM.setUrl(softwareUrl);      
      clusterM.setSoftware(softwareM);
      ConfigManifest configM = new ConfigManifest();
      configM.setUrl(configUrl);
      clusterM.setConfig(configM);
      
      // Fetch data back from file
      clusterM.load();
      
      // Verify original data and fetched data are the same
      NodesManifest actualNodeManifest = clusterM.getNodes();
      for(Role actualRole: actualNodeManifest.getRoles()) {
        Assert.assertEquals(actualRole.getName(), role.getName());
        for(String host : actualRole.getHosts()) {
          Assert.assertEquals(host, hosts[0]);
        }
      }
      SoftwareManifest actualSoftwareManifest = clusterM.getSoftware();
      for(Role actualRole: actualSoftwareManifest.getRoles()) {
        Assert.assertEquals(actualRole.getName(), role.getName());
        for(PackageInfo pi: actualRole.getPackages()) {
          Assert.assertEquals(pi.getName(), packages[0].getName());
        }
      }
      
      // Send to controller for testing
      Client client = new Client();
      //Assert.assertEquals(0, client.createCluster("foobar", nodeUrl, softwareUrl, configUrl));
    } catch (Exception e) {
      Assert.fail(ExceptionUtil.getStackTrace(e));
    }
  }
}
