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

package org.apache.hms.common.entity.manifest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.hms.common.util.JAXBUtil;

import com.sun.jersey.api.client.WebResource;

@XmlRootElement
public class ClusterManifest extends Manifest {
  @XmlAttribute
  private String clusterName;
  @XmlElement
  private NodesManifest nodes;
  @XmlElement
  private SoftwareManifest software;
  @XmlElement
  private ConfigManifest config;
  
  public String getClusterName() {
    return this.clusterName;
  }
  
  public NodesManifest getNodes() {
    return this.nodes;
  }
  
  public SoftwareManifest getSoftware() {
    return this.software;
  }
  
  public ConfigManifest getConfig() {
    return this.config;
  }
  
  public void setClusterName(String cluster) {
    this.clusterName = cluster;
  }
  
  public void setNodes(NodesManifest nodes) {
    this.nodes = nodes;
  }
  
  public void setSoftware(SoftwareManifest software) {
    this.software = software;
  }
  
  public void setConfig(ConfigManifest config) {
    this.config = config;
  }
  
  public void load() throws IOException {
    if(nodes!=null && nodes.getUrl()!=null && nodes.getRoles()==null) {
      URL url = nodes.getUrl();
      nodes = fetch(url, NodesManifest.class);
      nodes.setUrl(url);
    }
    if(software!=null && software.getUrl()!=null && software.getRoles()==null) {
      URL url = software.getUrl();
      software = fetch(url, SoftwareManifest.class);
      software.setUrl(url);
    }
    if(config!=null && config.getUrl()!=null && config.getActions()==null) {
      URL url = config.getUrl();
      config = fetch(url, ConfigManifest.class);
      config.setUrl(url);
      config.expand(nodes);
    }
  }
  
  private <T> T fetch(URL url, java.lang.Class<T> c) throws IOException {
    if(url.getProtocol().toLowerCase().equals("file")) {
      FileReader fstream = new FileReader(url.getPath());
      BufferedReader in = new BufferedReader(fstream);
      StringBuilder buffer = new StringBuilder();
      String str;
      while((str = in.readLine()) != null) {
        buffer.append(str);
      }
      return JAXBUtil.read(buffer.toString().getBytes(), c);
    } else {
      com.sun.jersey.api.client.Client wsClient = com.sun.jersey.api.client.Client.create();
      WebResource webResource = wsClient.resource(url.toString());
      return webResource.get(c);
    }
  }
}
