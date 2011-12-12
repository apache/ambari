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
package org.apache.ambari.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.common.rest.entities.ClusterDefinition;
import org.apache.ambari.common.rest.entities.ClusterState;
import org.apache.ambari.common.rest.entities.Stack;

import com.google.inject.Singleton;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONUnmarshaller;

/**
 * A data store that uses in-memory maps and some preset values for testing.
 */
@Singleton
class StaticDataStore implements DataStore {

  private Map<String, List<ClusterDefinition>> clusters = 
      new TreeMap<String, List<ClusterDefinition>>();

  private Map<String, List<Stack>> stacks =
      new TreeMap<String, List<Stack>>();
  
  private Map<String, ClusterState> clusterStates =
      new TreeMap<String, ClusterState>();

  private static final JAXBContext jaxbContext;
  private static final JSONJAXBContext jsonContext;
  static {
    try {
      jaxbContext = JAXBContext.
          newInstance("org.apache.ambari.common.rest.entities");
      jsonContext = 
          new JSONJAXBContext("org.apache.ambari.common.rest.entities");
    } catch (JAXBException e) {
      throw new RuntimeException("Can't create jaxb context", e);
    }
  }

  StaticDataStore() throws IOException {
    addStackFile("org/apache/ambari/stacks/hadoop-security-0.xml", 
                 "hadoop-security");
    addStackFile("org/apache/ambari/stacks/cluster123-0.xml", "cluster123");
    addStackFile("org/apache/ambari/stacks/cluster124-0.xml", "cluster124");
    addStackJsonFile("org/apache/ambari/stacks/puppet1-0.json", "puppet1");
    addClusterFile("org/apache/ambari/clusters/cluster123.xml", "cluster123");
  }

  private void addStackFile(String filename, 
                            String stackName) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(filename);
    if (in == null) {
      throw new IllegalArgumentException("Can't find resource for " + filename);
    }
    try {
      Unmarshaller um = jaxbContext.createUnmarshaller();
      Stack stack = (Stack) um.unmarshal(in);
      storeStack(stackName, stack);
    } catch (JAXBException je) {
      throw new IOException("Can't parse " + filename, je);
    }
  }

  private void addStackJsonFile(String filename, 
                                String stackName) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(filename);
    if (in == null) {
      throw new IllegalArgumentException("Can't find resource for " + filename);
    }
    try {
      JSONUnmarshaller um = jsonContext.createJSONUnmarshaller();
      Stack stack = um.unmarshalFromJSON(in, Stack.class);
      storeStack(stackName, stack);
    } catch (JAXBException je) {
      throw new IOException("Can't parse " + filename, je);
    }
  }

  private void addClusterFile(String filename,
                              String clusterName) throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream(filename);
    if (in == null) {
      throw new IllegalArgumentException("Can't find resource for " + filename);
    }
    try {
      Unmarshaller um = jaxbContext.createUnmarshaller();
      ClusterDefinition cluster = (ClusterDefinition) um.unmarshal(in);
      cluster.setName(clusterName);
      storeClusterDefinition(cluster);
    } catch (JAXBException je) {
      throw new IOException("Can't parse " + filename, je);
    }    
  }

  @Override
  public void close() throws IOException {
    // PASS
  }

  @Override
  public boolean clusterExists(String clusterName) throws IOException {
    return clusters.containsKey(clusterName);
  }

  @Override
  public int retrieveLatestClusterRevisionNumber(String clusterName)
      throws IOException {
    return clusters.get(clusterName).size()-1;
  }

  @Override
  public void storeClusterState(String clusterName, 
                                ClusterState clsState) throws IOException {
    clusterStates.put(clusterName, clsState);
  }

  @Override
  public ClusterState retrieveClusterState(String clusterName)
      throws IOException {
    return clusterStates.get(clusterName);
  }

  @Override
  public int storeClusterDefinition(ClusterDefinition clusterDef
                                    ) throws IOException {
    String name = clusterDef.getName();
    List<ClusterDefinition> list = clusters.get(name);
    if (list == null) {
      list = new ArrayList<ClusterDefinition>();
      clusters.put(name, list);
    }
    list.add(clusterDef);
    return list.size() - 1;
  }

  @Override
  public ClusterDefinition retrieveClusterDefinition(String clusterName,
      int revision) throws IOException {
    return clusters.get(clusterName).get(revision);
  }

  @Override
  public List<String> retrieveClusterList() throws IOException {
    return new ArrayList<String>(clusters.keySet());
  }

  @Override
  public void deleteCluster(String clusterName) throws IOException {
    clusters.remove(clusterName);
  }

  @Override
  public int storeStack(String stackName, Stack stack) throws IOException {
    List<Stack> list = stacks.get(stackName);
    if (list == null) {
      list = new ArrayList<Stack>();
      stacks.put(stackName, list);
    }
    int index = list.size();
    stack.setRevision(Integer.toString(index));
    list.add(stack);
    return index;
  }

  @Override
  public Stack retrieveStack(String stackName, 
                             int revision) throws IOException {
    List<Stack> history = stacks.get(stackName);
    if (revision == -1) {
      revision = history.size() - 1;
    }
    return history.get(revision);
  }

  @Override
  public List<String> retrieveStackList() throws IOException {
    return new ArrayList<String>(stacks.keySet());
  }

  @Override
  public int retrieveLatestStackRevisionNumber(String stackName
                                               ) throws IOException {
    return stacks.get(stackName).size() - 1;
  }

  @Override
  public void deleteStack(String stackName) throws IOException {
    stacks.remove(stackName);
  }

  @Override
  public boolean stackExists(String stackName) throws IOException {
    return stacks.containsKey(stackName);
  }

}
