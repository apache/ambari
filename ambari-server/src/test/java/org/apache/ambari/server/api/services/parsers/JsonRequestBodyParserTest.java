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

import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for JsonPropertyParser.
 */
public class JsonRequestBodyParserTest {

  String serviceJson = "{\"Services\" : {" +
      "    \"display_name\" : \"HDFS\"," +
      "    \"description\" : \"Apache Hadoop Distributed File System\","+
      "    \"service_name\" : \"HDFS\"" +
      "  }," +
      "  \"ServiceInfo\" : {" +
      "    \"cluster_name\" : \"tbmetrictest\"," +
      "    \"state\" : \"STARTED\"" +
      "  }," +
      "\"OuterCategory\" : { \"propName\" : 100, \"nested1\" : { \"nested2\" : { \"innerPropName\" : \"innerPropValue\" } } }," +
      "\"topLevelProp\" : \"value\" }";


  String arrayJson = "[ {" +
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

  String arrayJson2 = "{" +
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
      "} }";

  String queryPostJson = "{ \"services\" : [ {" +
      "\"ServiceInfo\" : {\n" +
      "    \"service_name\" : \"unitTestService1\"" +
      "} }," +
      "{" +
      "\"ServiceInfo\" : {\n" +
      "    \"service_name\" : \"unitTestService2\"," +
      "    \"property1\" : \"prop1Value\"" +
      "} }," +
      "{" +
      "\"ServiceInfo\" : {\n" +
      "    \"service_name\" : \"unitTestService3\"," +
      "    \"Category\" : { \"property2\" : \"prop2Value\"}" +
      "} } ] }";

  String queryPostMultipleSubResourcesJson = "{ \"foo\" : [ {" +
      "\"ServiceInfo\" : {\n" +
      "    \"service_name\" : \"unitTestService1\"" +
      "} }" +
      "]," +
      " \"bar\" : [" +
      "{" +
      "\"ServiceInfo\" : {\n" +
      "    \"service_name\" : \"unitTestService2\"," +
      "    \"Category\" : { \"property2\" : \"prop2Value\"}" +
      "} } ] }";


  String malformedJson = "{ \"Category\" : { \"foo\" : \"bar\"}";


  @Test
  public void testParse() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    RequestBody body = parser.parse(serviceJson);

    Set<NamedPropertySet> setProps = body.getPropertySets();
    assertEquals(1, setProps.size());

    Map<String, Object> mapExpected = new HashMap<String, Object>();
    mapExpected.put(PropertyHelper.getPropertyId("Services", "service_name"), "HDFS");
    mapExpected.put(PropertyHelper.getPropertyId("Services", "display_name"), "HDFS");
    mapExpected.put(PropertyHelper.getPropertyId("ServiceInfo", "cluster_name"), "tbmetrictest");
    mapExpected.put(PropertyHelper.getPropertyId("Services", "description"), "Apache Hadoop Distributed File System");
    mapExpected.put(PropertyHelper.getPropertyId("ServiceInfo", "state"), "STARTED");
    mapExpected.put(PropertyHelper.getPropertyId("OuterCategory", "propName"), "100");
    mapExpected.put(PropertyHelper.getPropertyId("OuterCategory/nested1/nested2", "innerPropName"), "innerPropValue");
    mapExpected.put(PropertyHelper.getPropertyId(null, "topLevelProp"), "value");

    assertEquals(mapExpected, setProps.iterator().next().getProperties());

    //assert body is correct by checking that properties match
    String b = body.getBody();
    body = parser.parse(b);
    Set<NamedPropertySet> setProps2 = body.getPropertySets();
    assertEquals(mapExpected, setProps2.iterator().next().getProperties());
  }

  @Test
  public void testParse_NullBody() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    RequestBody body = parser.parse(null);

    assertNotNull(body.getPropertySets());
    assertEquals(0, body.getPropertySets().size());
    assertNull(body.getQueryString());
    assertNull(body.getPartialResponseFields());
    assertNull(body.getBody());
  }

  @Test
  public void testParse_EmptyBody() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    RequestBody body = parser.parse("");

    assertNotNull(body.getPropertySets());
    assertEquals(0, body.getPropertySets().size());
    assertNull(body.getQueryString());
    assertNull(body.getPartialResponseFields());
    assertNull(body.getBody());
  }

  @Test
  public void testParse_Array() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    RequestBody body = parser.parse(arrayJson);

    Set<NamedPropertySet> setProps = body.getPropertySets();

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
    mapCluster3.put(PropertyHelper.getPropertyId("Clusters/Category", "property2"), "prop2Value");


    for (NamedPropertySet propertySet : setProps) {
      assertEquals("", propertySet.getName());
      Map<String, Object> mapProps = propertySet.getProperties();
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

    //assert body is correct by checking that properties match
    String b = body.getBody();

    body = parser.parse(b);

    Set<NamedPropertySet> setProps2 = body.getPropertySets();
    assertEquals(3, setProps2.size());
    assertEquals(setProps, setProps2);
  }

  @Test
  public void testParse___Array_NoArrayBrackets() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    RequestBody body = parser.parse(arrayJson2);

    Set<NamedPropertySet> setProps = body.getPropertySets();

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
    mapCluster3.put(PropertyHelper.getPropertyId("Clusters/Category", "property2"), "prop2Value");


    for (NamedPropertySet propertySet : setProps) {
      Map<String, Object> mapProps = propertySet.getProperties();
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

    //assert body is correct by checking that properties match
    String b = body.getBody();
    body = parser.parse(b);

    Set<NamedPropertySet> setProps2 = body.getPropertySets();
    assertEquals(3, setProps2.size());
    assertEquals(setProps, setProps2);
  }

  @Test
  public void testParse_QueryInBody() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    String queryBody = "{ \"RequestInfo\" : { \"query\" : \"foo=bar\" }, \"Body\":" + serviceJson + "}";
    RequestBody body = parser.parse(queryBody);


    Set<NamedPropertySet> setProps = body.getPropertySets();
    assertEquals(1, setProps.size());

    Map<String, Object> mapExpected = new HashMap<String, Object>();
    mapExpected.put(PropertyHelper.getPropertyId("Services", "service_name"), "HDFS");
    mapExpected.put(PropertyHelper.getPropertyId("Services", "display_name"), "HDFS");
    mapExpected.put(PropertyHelper.getPropertyId("ServiceInfo", "cluster_name"), "tbmetrictest");
    mapExpected.put(PropertyHelper.getPropertyId("Services", "description"), "Apache Hadoop Distributed File System");
    mapExpected.put(PropertyHelper.getPropertyId("ServiceInfo", "state"), "STARTED");
    mapExpected.put(PropertyHelper.getPropertyId("OuterCategory", "propName"), "100");
    mapExpected.put(PropertyHelper.getPropertyId("OuterCategory/nested1/nested2", "innerPropName"), "innerPropValue");
    mapExpected.put(PropertyHelper.getPropertyId(null, "topLevelProp"), "value");

    assertEquals(mapExpected, setProps.iterator().next().getProperties());
    assertEquals("foo=bar", body.getQueryString());

    //assert body is correct by checking that properties match
    String b = body.getBody();
    body = parser.parse(b);

    Set<NamedPropertySet> setProps2 = body.getPropertySets();
    assertEquals(mapExpected, setProps2.iterator().next().getProperties());
  }

  @Test
  public void testParse_QueryPost() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    RequestBody body = parser.parse(queryPostJson);


    Set<NamedPropertySet> setProperties = body.getPropertySets();

    assertEquals(3, setProperties.size());
    boolean contains1 = false;
    boolean contains2 = false;
    boolean contains3 = false;

    for (NamedPropertySet ps : setProperties) {
      assertEquals("services", ps.getName());
      Map<String, Object> mapProps = ps.getProperties();
      String serviceName = (String) mapProps.get("ServiceInfo/service_name");
      if (serviceName.equals("unitTestService1")) {
        assertEquals(1, mapProps.size());
        contains1 = true;
      } else if (serviceName.equals("unitTestService2")) {
        assertEquals("prop1Value", mapProps.get("ServiceInfo/property1"));
        assertEquals(2, mapProps.size());
        contains2 = true;
      } else if (serviceName.equals("unitTestService3")) {
        assertEquals("prop2Value", mapProps.get("ServiceInfo/Category/property2"));
        assertEquals(2, mapProps.size());
        contains3 = true;
      } else {
        fail("Unexpected service name");
      }
    }
    assertTrue(contains1);
    assertTrue(contains2);
    assertTrue(contains3);

    //assert body is correct by checking that properties match
    String b = body.getBody();
    body = parser.parse(b);

    Set<NamedPropertySet> setProps2 = body.getPropertySets();
    assertEquals(3, setProps2.size());
    assertEquals(setProperties, setProps2);
  }

  @Test
  public void testParse___QueryPost_multipleSubResTypes() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    RequestBody body = parser.parse(queryPostMultipleSubResourcesJson);


    Set<NamedPropertySet> setProperties = body.getPropertySets();

    assertEquals(2, setProperties.size());
    boolean contains1 = false;
    boolean contains2 = false;

    for (NamedPropertySet ps : setProperties) {
      Map<String, Object> mapProps = ps.getProperties();
      String serviceName = (String) mapProps.get("ServiceInfo/service_name");
      if (serviceName.equals("unitTestService1")) {
        assertEquals("foo", ps.getName());
        assertEquals(1, mapProps.size());
        contains1 = true;
      } else if (serviceName.equals("unitTestService2")) {
        assertEquals("bar", ps.getName());
        assertEquals("prop2Value", mapProps.get("ServiceInfo/Category/property2"));
        assertEquals(2, mapProps.size());
        contains2 = true;
      } else {
        fail("Unexpected service name");
      }
    }
    assertTrue(contains1);
    assertTrue(contains2);

    //assert body is correct by checking that properties match
    String b = body.getBody();
    body = parser.parse(b);

    Set<NamedPropertySet> setProps2 = body.getPropertySets();
    assertEquals(2, setProps2.size());
    assertEquals(setProperties, setProps2);
  }

  @Test
  public void testParse___QueryPost_QueryInBody() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    String queryBody = "{ \"RequestInfo\" : { \"query\" : \"foo=bar\" }, \"Body\":" + queryPostJson + "}";
    RequestBody body = parser.parse(queryBody);


    Set<NamedPropertySet> setProperties = body.getPropertySets();

    assertEquals("foo=bar", body.getQueryString());
    assertEquals(3, setProperties.size());
    boolean contains1 = false;
    boolean contains2 = false;
    boolean contains3 = false;

    for (NamedPropertySet ps : setProperties) {
      assertEquals("services", ps.getName());
      Map<String, Object> mapProps = ps.getProperties();
      String serviceName = (String) mapProps.get("ServiceInfo/service_name");
      if (serviceName.equals("unitTestService1")) {
        assertEquals(1, mapProps.size());
        contains1 = true;
      } else if (serviceName.equals("unitTestService2")) {
        assertEquals("prop1Value", mapProps.get("ServiceInfo/property1"));
        assertEquals(2, mapProps.size());
        contains2 = true;
      } else if (serviceName.equals("unitTestService3")) {
        assertEquals("prop2Value", mapProps.get("ServiceInfo/Category/property2"));
        assertEquals(2, mapProps.size());
        contains3 = true;
      } else {
        fail("Unexpected service name");
      }
    }
    assertTrue(contains1);
    assertTrue(contains2);
    assertTrue(contains3);

    //assert body is correct by checking that properties match
    String b = body.getBody();
    assertEquals("{\"services\":[{\"ServiceInfo\":{" +
        "\"service_name\":\"unitTestService1\"}},{\"ServiceInfo\":{" +
        "\"service_name\":\"unitTestService2\",\"property1\":\"prop1Value\"}},{\"ServiceInfo\":{" +
        "\"service_name\":\"unitTestService3\",\"Category\":{\"property2\":\"prop2Value\"}}}]}", b);

    body = parser.parse(b);

    Set<NamedPropertySet> setProps2 = body.getPropertySets();
    assertEquals(3, setProps2.size());
    assertEquals(setProperties, setProps2);
  }

  @Test
  public void testParse_QueryOnlyInBody() throws BodyParseException {
    RequestBodyParser parser = new JsonRequestBodyParser();
    String queryBody = "{ \"RequestInfo\" : { \"query\" : \"foo=bar\" }}";
    RequestBody body = parser.parse(queryBody);

    assertEquals("foo=bar", body.getQueryString());
    assertNull(body.getBody());
  }

  @Test
  public void testParse_malformedBody() {
    RequestBodyParser parser = new JsonRequestBodyParser();

    try {
      parser.parse(malformedJson);
      fail("Expected exception due to malformed body");
    } catch (BodyParseException e) {
      // expected case
    }
  }
}


