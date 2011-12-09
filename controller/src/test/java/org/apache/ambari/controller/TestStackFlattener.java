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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ambari.common.rest.entities.Component;
import org.apache.ambari.common.rest.entities.ComponentDefinition;
import org.apache.ambari.common.rest.entities.Configuration;
import org.apache.ambari.common.rest.entities.ConfigurationCategory;
import org.apache.ambari.common.rest.entities.Property;
import org.apache.ambari.common.rest.entities.RepositoryKind;
import org.apache.ambari.common.rest.entities.Role;
import org.apache.ambari.common.rest.entities.Stack;
import org.apache.ambari.components.ComponentPlugin;
import org.apache.ambari.components.ComponentPluginFactory;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;

public class TestStackFlattener {
  
  Stacks stacks;
  Stack parentStack;
  Stack childStack;
  Stack grandchildStack;
  ComponentPluginFactory plugins;
  ComponentPlugin hdfs;
  ComponentPlugin mapreduce;
  ComponentDefinition hdfsDefn;
  ComponentDefinition mapreduceDefn;
  Component parentHdfs;
  Component parentMapreduce;
  StackFlattener flattener;
  
  @BeforeMethod
  public void setup() throws Exception {
    stacks =  mock(Stacks.class);
    parentStack = new Stack();
    parentStack.setName("parent");
    childStack = new Stack();
    childStack.setName("child");
    grandchildStack = new Stack();
    grandchildStack.setName("grandchild");
    childStack.setParentName(parentStack.getName());
    childStack.setParentRevision(0);
    grandchildStack.setParentName(childStack.getName());
    grandchildStack.setParentRevision(0);
    when(stacks.getStack(parentStack.getName(), 0)).thenReturn(parentStack);
    when(stacks.getStack(childStack.getName(), 0)).thenReturn(childStack);    
    when(stacks.getStack(grandchildStack.getName(), 0)).
      thenReturn(grandchildStack);
    plugins = mock(ComponentPluginFactory.class);
    hdfs = mock(ComponentPlugin.class);
    when(hdfs.getActiveRoles()).
      thenReturn(new String[]{"namenode", "datanode"});
    mapreduce = mock(ComponentPlugin.class);
    when(mapreduce.getActiveRoles()).
      thenReturn(new String[]{"jobtracker","tasktracker"});
    hdfsDefn = new ComponentDefinition("hdfs", "org.apache.ambari", "0");
    mapreduceDefn = new ComponentDefinition("mapreduce", "org.apache.ambari", 
                                            "0");
    when(plugins.getPlugin(hdfsDefn)).thenReturn(hdfs);
    when(plugins.getPlugin(mapreduceDefn)).thenReturn(mapreduce);
    parentHdfs = new Component("hdfs", "0.20.205.0", "i386", 
                               "org.apache.ambari",
                               new ComponentDefinition("hdfs", 
                                   "org.apache.ambari", "0"), 
                               new Configuration(), new ArrayList<Role>());
    parentMapreduce = new Component("mapreduce", "0.20.205.0", "i386", 
                                    "org.apache.ambari",
                                    new ComponentDefinition("mapreduce", 
                                                       "org.apache.ambari","0"), 
                                    new Configuration(), new ArrayList<Role>());
    List<Component> compList = new ArrayList<Component>();
    parentStack.setComponents(compList);
    compList.add(parentHdfs);
    compList.add(parentMapreduce);
    flattener = new StackFlattener(stacks, plugins);
  }

  @Test
  public void testRepositoryFlattening() throws Exception {
    parentStack.setPackageRepositories(Arrays.asList
        (new RepositoryKind("kind1", "url1", "url2"),
         new RepositoryKind("kind2", "url3", "url4")));
    childStack.setPackageRepositories(Arrays.asList
        (new RepositoryKind("kind3", "url5")));
    grandchildStack.setPackageRepositories(Arrays.asList
        (new RepositoryKind("kind1", "url7", "url8"),
         new RepositoryKind("kind3", "url9", "url10")));
    grandchildStack.setRevision("123");
    Stack flat = flattener.flattenStack("grandchild", 0);
    List<RepositoryKind> answer = flat.getPackageRepositories();
    assertEquals(new RepositoryKind("kind1", "url7", "url8", "url1", "url2"), 
                 answer.get(0));
    assertEquals(new RepositoryKind("kind2", "url3", "url4"), answer.get(1));
    assertEquals(new RepositoryKind("kind3", "url9", "url10", "url5"), 
                 answer.get(2));
    
    // ensure the name and parent name are what we expect
    assertEquals("grandchild", flat.getName());
    assertEquals("123", flat.getRevision());
    assertEquals(null, flat.getParentName());
    assertEquals(0, flat.getParentRevision());
  }
 
  static void setConfigParam(Configuration conf, String category, String key,
                             String value) {
    for(ConfigurationCategory cat: conf.getCategory()) {
      if (cat.getName().equals(category)) {
        for(Property prop: cat.getProperty()) {
          if (prop.getName().equals(key)) {
            // if we find the right property, update it
            prop.setValue(value);
          }
        }
        // otherwise add a new property
        cat.getProperty().add(new Property(key, value));
      }
    }
    // otherwise, it is a new category
    List<Property> propList = new ArrayList<Property>();
    propList.add(new Property(key,value));
    conf.getCategory().add(new ConfigurationCategory(category, propList));
  }
  
  static String getConfigParam(Configuration conf, String category, 
                               String key) {
    for(ConfigurationCategory cat: conf.getCategory()) {
      if (cat.getName().equals(category)) {
        for(Property prop: cat.getProperty()) {
          if (prop.getName().equals(key)) {
            return prop.getValue();
          }
        }
        return null;
      }
    }
    return null;
  }

  @Test
  public void testConfigFlattening() throws Exception {
    Configuration parentConfiguration = new Configuration();
    parentStack.setConfiguration(parentConfiguration);
    Configuration childConfiguration = new Configuration();
    childStack.setConfiguration(childConfiguration);
    Configuration grandchildConfiguration = new Configuration();
    grandchildStack.setConfiguration(grandchildConfiguration);
    Configuration parentHdfsConfig = parentHdfs.getConfiguration();
    Configuration parentMapredConfig = parentMapreduce.getConfiguration();
    List<Role> hdfsRoles = new ArrayList<Role>();
    Configuration childHdfsConfig = new Configuration();
    childStack.getComponents().add(
        new Component("hdfs", null, null, null, null, 
                      childHdfsConfig, hdfsRoles));
    Configuration nnConf = new Configuration();
    hdfsRoles.add(new Role("namenode", nnConf));
    setConfigParam(parentConfiguration, "ambari", "global", "global-value");
    setConfigParam(parentConfiguration, "cat1", "b", "parent");
    setConfigParam(parentConfiguration, "cat1", "a", "a-value");
    setConfigParam(parentConfiguration, "cat2", "b", "cat2-value");
    setConfigParam(childConfiguration, "cat1", "b", "child");
    setConfigParam(parentHdfsConfig, "cat1", "b", "parent-hdfs");
    setConfigParam(parentHdfsConfig, "cat1", "d", "d-value");
    setConfigParam(parentMapredConfig, "cat1", "b", "parent-mapred");
    setConfigParam(grandchildConfiguration, "cat1", "b", "grandchild");
    setConfigParam(childHdfsConfig, "cat1", "b", "child-hdfs");
    setConfigParam(nnConf, "cat1", "b", "nn");
    setConfigParam(nnConf, "cat1", "c", "nn-c");
    Stack flat = flattener.flattenStack("grandchild", 0);
    Configuration conf = flat.getConfiguration();
    assertEquals("a-value", getConfigParam(conf, "cat1", "a"));
    assertEquals("cat2-value", getConfigParam(conf, "cat2", "b"));
    assertEquals("grandchild", getConfigParam(conf, "cat1", "b"));
    assertEquals("global-value", getConfigParam(conf, "ambari", "global"));
    assertEquals(null, getConfigParam(conf, "cat1", "c"));
    assertEquals(null, getConfigParam(conf, "cat1", "d"));
    Component comp = flat.getComponents().get(0);
    assertEquals("hdfs", comp.getName());
    assertEquals(null, comp.getConfiguration());
    Role role = comp.getRoles().get(0);
    assertEquals("namenode", role.getName());
    conf = role.getConfiguration();
    assertEquals("a-value", getConfigParam(conf, "cat1", "a"));
    assertEquals("cat2-value", getConfigParam(conf, "cat2", "b"));
    assertEquals("grandchild", getConfigParam(conf, "cat1", "b"));
    assertEquals(null, getConfigParam(conf, "ambari", "global"));
    assertEquals("nn-c", getConfigParam(conf, "cat1", "c"));
    assertEquals("d-value", getConfigParam(conf, "cat1", "d"));
    role = comp.getRoles().get(1);
    assertEquals("datanode", role.getName());
    conf = role.getConfiguration();
    assertEquals("a-value", getConfigParam(conf, "cat1", "a"));
    assertEquals("cat2-value", getConfigParam(conf, "cat2", "b"));
    assertEquals("grandchild", getConfigParam(conf, "cat1", "b"));
    assertEquals(null, getConfigParam(conf, "ambari", "global"));
    assertEquals(null, getConfigParam(conf, "cat1", "c"));
    assertEquals("d-value", getConfigParam(conf, "cat1", "d"));
  }
}
