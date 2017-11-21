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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.topology;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public class TopologyTemplateFactoryTest {

  public static final String CLUSTER_TEMPLATE =
    getResource("blueprintv2/cluster_template_v2.json");
  public static final String CLUSTER_TEMPLATE_INVALID =
    getResource("blueprintv2/cluster_template_v2_invalid_hostgroup.json");


  @Test
  public void testProvisionClusterTemplate() throws Exception {
    TopologyTemplateFactory factory = new TopologyTemplateFactory();
    TopologyTemplate template = factory.convertFromJson(CLUSTER_TEMPLATE);
    verifyClusterTemplate(template);
  }

  @Test(expected = IllegalStateException.class)
  public void testProvisionClusterTemplateInvalidTemplate() throws Exception {
    TopologyTemplateFactory factory = new TopologyTemplateFactory();
    TopologyTemplate template = factory.convertFromJson(CLUSTER_TEMPLATE_INVALID);
  }


  private void verifyClusterTemplate(TopologyTemplate template) {
    TopologyTemplate.Service zk1 = template.getServiceById(ServiceId.of("ZK1", "CORE_SG"));
    assertNotNull(zk1);
    Map<String, Map<String, String>> expectedZkProperties = ImmutableMap.of(
      "zoo.cfg", ImmutableMap.of("dataDir", "/zookeeper1"));
    assertEquals(expectedZkProperties, zk1.getConfiguration().getProperties());

    TopologyTemplate.Service hdfs = template.getServiceById(ServiceId.of("HDFS", "CORE_SG"));
    Map<String, Map<String, String>> expectedHdfsProperties = ImmutableMap.of(
      "hdfs-site", ImmutableMap.of("property-name", "property-value"));
    assertNotNull(hdfs);
    assertEquals(expectedHdfsProperties, hdfs.getConfiguration().getProperties());

    TopologyTemplate.HostGroup hostGroup1 = template.getHostGroupByName("host-group-1");
    assertNotNull(hostGroup1);
    assertEquals(2, hostGroup1.getHosts().size());
    assertEquals(0, hostGroup1.getHostCount());
    assertEquals(ImmutableSet.of("host.domain.com", "host2.domain.com"),
      hostGroup1.getHosts().stream().map(host -> host.getFqdn()).collect(toSet()));
    hostGroup1.getHosts().forEach(host -> assertEquals("/dc1/rack1", host.getRackInfo()));

    TopologyTemplate.HostGroup hostGroup2 = template.getHostGroupByName("host-group-2");
    assertNotNull(hostGroup2);
    assertEquals(0, hostGroup2.getHosts().size());
    assertEquals(2, hostGroup2.getHostCount());
    assertEquals("Hosts/os_type=centos6&Hosts/cpu_count=2", hostGroup2.getHostPredicate());
  }


  private static String getResource(String fileName) {
    try {
      return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
