/**
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

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.state.ServiceInfo;
import org.junit.Test;
import org.junit.Before;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;


public class AmbariMetaInfoTest {

  private AmbariMetaInfo ambariMetaInfo = null;

  @Before
  public void before() throws Exception {
    ambariMetaInfo = new AmbariMetaInfo();
  }

  /**
   * Method: getSupportedConfigs(String stackName, String version, String serviceName)
   */
  @Test
  public void testGetSupportedConfigs() throws Exception {

    Map<String, Map<String, String>> configsAll = ambariMetaInfo.getSupportedConfigs("HDP", "0.1", "HDFS");
    Set<String> filesKeys = configsAll.keySet();
    for (String file : filesKeys) {
      Map<String, String> configs = configsAll.get(file);
      Set<String> propertyKeys = configs.keySet();
      assertNotNull(propertyKeys);
      assertNotSame(propertyKeys.size(), 0);
    }
  }

  /**
   * Method: getServiceInfo(String stackName, String version, String serviceName)
   */
  @Test
  public void testGetServiceInfo() throws Exception {
    ServiceInfo si = ambariMetaInfo.getServiceInfo("HDP","0.1", "HDFS");
    assertNotNull(si);
  }

  /**
   * Method: getSupportedServices(String stackName, String version)
   */
  @Test
  public void testGetSupportedServices() throws Exception {
    List<ServiceInfo> services = ambariMetaInfo.getSupportedServices("HDP","0.1");
    assertNotNull(services);
    assertNotSame(services.size(), 0);

  }


} 
