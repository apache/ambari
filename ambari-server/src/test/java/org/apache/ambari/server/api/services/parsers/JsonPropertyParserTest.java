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

package org.apache.ambari.server.api.services.parsers;

import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for JsonPropertyParser.
 */
public class JsonPropertyParserTest {

  String serviceJson = "{\"Services\" : {" +
      "    \"display_name\" : \"HDFS\"," +
      "    \"description\" : \"Apache Hadoop Distributed File System\"," +
      "    \"attributes\" : \"{ \\\"runnable\\\": true, \\\"mustInstall\\\": true, \\\"editable\\\": false, \\\"noDisplay\\\": false }\"," +
      "    \"service_name\" : \"HDFS\"" +
      "  }," +
      "  \"ServiceInfo\" : {" +
      "    \"cluster_name\" : \"tbmetrictest\"," +
      "    \"state\" : \"STARTED\"" +
      "  }," +
      "\"OuterCategory\" : { \"propName\" : 100, \"nested1\" : { \"nested2\" : { \"innerPropName\" : \"innerPropValue\" } } } }";


  String clustersJson = "[ {" +
      "\"Clusters\" : {\n" +
      "    \"cluster_name\" : \"unitTestCluster1\"" +
      "} }," +
      "{" +
      "\"Clusters\" : {\n" +
      "    \"cluster_name\" : \"unitTestCluster2\"," +
      "    \"property1\" : \"prop1Value\"" +
      "} }," +
      "{" +
      "\"Clusters\" : {\n" +
      "    \"cluster_name\" : \"unitTestCluster3\"," +
      "    \"Category\" : { \"property2\" : \"prop2Value\"}" +
      "} } ]";


  @Test
  public void testParse() throws Exception {
    RequestBodyParser parser = new JsonPropertyParser();
    Set<Map<String, Object>> setProps = parser.parse(serviceJson);

    assertEquals(1, setProps.size());

    Map<String, Object> mapExpected = new HashMap<String, Object>();
    mapExpected.put(PropertyHelper.getPropertyId("Services", "service_name"), "HDFS");
    mapExpected.put(PropertyHelper.getPropertyId("Services", "display_name"), "HDFS");
    mapExpected.put(PropertyHelper.getPropertyId("ServiceInfo", "cluster_name"), "tbmetrictest");
    mapExpected.put(PropertyHelper.getPropertyId("Services", "attributes"), "{ \"runnable\": true, \"mustInstall\": true, \"editable\": false, \"noDisplay\": false }");
    mapExpected.put(PropertyHelper.getPropertyId("Services", "description"), "Apache Hadoop Distributed File System");
    mapExpected.put(PropertyHelper.getPropertyId("ServiceInfo", "state"), "STARTED");
    mapExpected.put(PropertyHelper.getPropertyId("OuterCategory", "propName"), "100");
    mapExpected.put(PropertyHelper.getPropertyId("OuterCategory.nested1.nested2", "innerPropName"), "innerPropValue");

    assertEquals(mapExpected, setProps.iterator().next());
  }

  @Test
  public void testParse_NullBody() {
    RequestBodyParser parser = new JsonPropertyParser();
    Set<Map<String, Object>> setProps = parser.parse(null);
    assertNotNull(setProps);
    assertEquals(0, setProps.size());
  }

  @Test
  public void testParse_EmptyBody() {
    RequestBodyParser parser = new JsonPropertyParser();
    Set<Map<String, Object>> setProps = parser.parse("");
    assertNotNull(setProps);
    assertEquals(0, setProps.size());
  }

  @Test
  public void testParse_Array() {
    RequestBodyParser parser = new JsonPropertyParser();
    Set<Map<String, Object>> setProps = parser.parse(clustersJson);
    assertEquals(3, setProps.size());

    boolean cluster1Matches = false;
    boolean cluster2Matches = false;
    boolean cluster3Matches = false;

    Map<String, String> mapCluster1 = new HashMap<String, String>();
    mapCluster1.put(PropertyHelper.getPropertyId("Clusters", "cluster_name"), "unitTestCluster1");

    Map<String, String> mapCluster2 = new HashMap<String, String>();
    mapCluster2.put(PropertyHelper.getPropertyId("Clusters", "cluster_name"), "unitTestCluster2");
    mapCluster2.put(PropertyHelper.getPropertyId("Clusters", "property1"), "prop1Value");


    Map<String, String> mapCluster3 = new HashMap<String, String>();
    mapCluster3.put(PropertyHelper.getPropertyId("Clusters", "cluster_name"), "unitTestCluster3");
    mapCluster3.put(PropertyHelper.getPropertyId("Clusters.Category", "property2"), "prop2Value");


    for (Map<String, Object> mapProps : setProps) {
      if (mapProps.equals(mapCluster1)) {
        cluster1Matches = true;
      } else if (mapProps.equals(mapCluster2)) {
        cluster2Matches = true;
      } else if (mapProps.equals(mapCluster3)) {
        cluster3Matches = true;
      }
    }

    assertTrue(cluster1Matches);
    assertTrue(cluster2Matches);
    assertTrue(cluster3Matches);
  }
}


