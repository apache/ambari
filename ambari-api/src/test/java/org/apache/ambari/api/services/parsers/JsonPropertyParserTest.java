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

package org.apache.ambari.api.services.parsers;

import org.apache.ambari.api.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

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


  @Test
  public void testParse() throws Exception {
    RequestBodyParser parser = new JsonPropertyParser();
    Map<PropertyId, String> mapProps = parser.parse(serviceJson);

    Map<PropertyId, Object> mapExpected = new HashMap<PropertyId, Object>();
    mapExpected.put(PropertyHelper.getPropertyId("service_name", "Services"), "HDFS");
    mapExpected.put(PropertyHelper.getPropertyId("display_name", "Services"), "HDFS");
    mapExpected.put(PropertyHelper.getPropertyId("cluster_name", "ServiceInfo"), "tbmetrictest");
    mapExpected.put(PropertyHelper.getPropertyId("attributes", "Services"), "{ \"runnable\": true, \"mustInstall\": true, \"editable\": false, \"noDisplay\": false }");
    mapExpected.put(PropertyHelper.getPropertyId("description", "Services"), "Apache Hadoop Distributed File System");
    mapExpected.put(PropertyHelper.getPropertyId("state", "ServiceInfo"), "STARTED");
    mapExpected.put(PropertyHelper.getPropertyId("propName", "OuterCategory"), "100");
    mapExpected.put(PropertyHelper.getPropertyId("innerPropName", "OuterCategory.nested1.nested2"), "innerPropValue");

    assertEquals(mapExpected, mapProps);
  }
}


