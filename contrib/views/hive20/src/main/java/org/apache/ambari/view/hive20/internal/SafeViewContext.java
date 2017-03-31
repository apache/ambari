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

package org.apache.ambari.view.hive20.internal;

import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.HttpImpersonator;
import org.apache.ambari.view.ImpersonatorSetting;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.SecurityException;
import org.apache.ambari.view.URLConnectionProvider;
import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewController;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.cluster.Cluster;

import java.util.Collection;
import java.util.Map;

/**
 * Wrapper to ViewContext. This delegates all the method calls to wrapped ViewContext object excepting for
 * #getUsername() and #getLoggedinUser(). At the creation time, the username and loggedinuser are store
 * in instance variable. This was done to bypass the ThreadLocal variables implicitly used in actual viewContext.
 * So, object of this class should be used in the ActorSystem.
 */
public class SafeViewContext implements ViewContext {
  private final ViewContext viewContext;
  private final String username;
  private final String loggedinUser;

  public SafeViewContext(ViewContext viewContext) {
    this.viewContext = viewContext;
    username = viewContext.getUsername();
    loggedinUser = viewContext.getLoggedinUser();
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getLoggedinUser() {
    return loggedinUser;
  }

  @Override
  public void hasPermission(String userName, String permissionName) throws SecurityException {
    viewContext.hasPermission(userName, permissionName);
  }

  @Override
  public String getViewName() {
    return viewContext.getViewName();
  }

  @Override
  public ViewDefinition getViewDefinition() {
    return viewContext.getViewDefinition();
  }

  @Override
  public String getInstanceName() {
    return viewContext.getInstanceName();
  }

  @Override
  public ViewInstanceDefinition getViewInstanceDefinition() {
    return viewContext.getViewInstanceDefinition();
  }

  @Override
  public Map<String, String> getProperties() {
    return viewContext.getProperties();
  }

  @Override
  public void putInstanceData(String key, String value) {
    viewContext.putInstanceData(key, value);
  }

  @Override
  public String getInstanceData(String key) {
    return viewContext.getInstanceData(key);
  }

  @Override
  public Map<String, String> getInstanceData() {
    return viewContext.getInstanceData();
  }

  @Override
  public void removeInstanceData(String key) {
    viewContext.removeInstanceData(key);
  }

  @Override
  public String getAmbariProperty(String key) {
    return viewContext.getAmbariProperty(key);
  }

  @Override
  public ResourceProvider<?> getResourceProvider(String type) {
    return viewContext.getResourceProvider(type);
  }

  @Override
  public URLStreamProvider getURLStreamProvider() {
    return viewContext.getURLStreamProvider();
  }

  @Override
  public URLConnectionProvider getURLConnectionProvider() {
    return viewContext.getURLConnectionProvider();
  }

  @Override
  public AmbariStreamProvider getAmbariStreamProvider() {
    return viewContext.getAmbariStreamProvider();
  }

  @Override
  public AmbariStreamProvider getAmbariClusterStreamProvider() {
    return viewContext.getAmbariClusterStreamProvider();
  }

  @Override
  public DataStore getDataStore() {
    return viewContext.getDataStore();
  }

  @Override
  public Collection<ViewDefinition> getViewDefinitions() {
    return viewContext.getViewDefinitions();
  }

  @Override
  public Collection<ViewInstanceDefinition> getViewInstanceDefinitions() {
    return viewContext.getViewInstanceDefinitions();
  }

  @Override
  public ViewController getController() {
    return viewContext.getController();
  }

  @Override
  public HttpImpersonator getHttpImpersonator() {
    return viewContext.getHttpImpersonator();
  }

  @Override
  public ImpersonatorSetting getImpersonatorSetting() {
    return viewContext.getImpersonatorSetting();
  }

  @Override
  public Cluster getCluster() {
    return viewContext.getCluster();
  }
}
