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

package org.apache.ambari.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.junit.jupiter.api.Test;

public class CommonsBeanUtilsSecurityTest {

  @Test
  public void testEnumDeclaringClassAccessIsSuppressed() throws Exception {
    BeanUtilsBean beanUtils = new BeanUtilsBean();
    ServiceConfiguration configuration = new ServiceConfiguration();
    configuration.setState(ServiceState.STARTED);

    Map<String, Object> enumProperties = beanUtils.getPropertyUtils().describe(ServiceState.STARTED);
    assertFalse(enumProperties.containsKey("class"));
    assertFalse(enumProperties.containsKey("declaringClass"));
    assertThrows(NoSuchMethodException.class,
        () -> beanUtils.getProperty(configuration, "state.declaringClass.classLoader"));
  }

  @Test
  public void testNormalBeanPropertiesAreCopied() throws Exception {
    ServiceConfiguration source = new ServiceConfiguration();
    source.setClusterName("production");
    source.setServiceName("HDFS");
    source.setState(ServiceState.STARTED);
    ServiceConfiguration destination = new ServiceConfiguration();

    BeanUtilsBean.getInstance().copyProperties(destination, source);

    assertEquals(source.getClusterName(), destination.getClusterName());
    assertEquals(source.getServiceName(), destination.getServiceName());
    assertEquals(source.getState(), destination.getState());
  }

  public enum ServiceState {
    STARTED
  }

  public static class ServiceConfiguration {
    private String clusterName;
    private String serviceName;
    private ServiceState state;

    public String getClusterName() {
      return clusterName;
    }

    public void setClusterName(String clusterName) {
      this.clusterName = clusterName;
    }

    public String getServiceName() {
      return serviceName;
    }

    public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
    }

    public ServiceState getState() {
      return state;
    }

    public void setState(ServiceState state) {
      this.state = state;
    }
  }
}
