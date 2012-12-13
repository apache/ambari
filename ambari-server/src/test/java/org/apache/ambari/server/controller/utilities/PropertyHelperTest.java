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
package org.apache.ambari.server.controller.utilities;

//import org.apache.ambari.server.controller.spi.Resource;
//import org.codehaus.jackson.map.ObjectMapper;
//import org.codehaus.jackson.type.TypeReference;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import java.util.Collections;
//import java.util.Map;
//import java.util.TreeMap;

/**
 *
 */
public class PropertyHelperTest {

//  private static final String JMX_PROPERTIES_FILE = "jmx_properties.json";
//  private static final String GANGLIA_PROPERTIES_FILE = "ganglia_properties.json";
//
//  @Ignore
//  @Test
//  public void testGetGPropertyIds() throws Exception {
//    ObjectMapper mapper = new ObjectMapper();
//
//    Map<Resource.Type, Map<String, Map<String, PropertyHelper.Metric>>> gangliaMetrics =
//        mapper.readValue(ClassLoader.getSystemResourceAsStream(GANGLIA_PROPERTIES_FILE),
//            new TypeReference<Map<Resource.Type, Map<String, Map<String, PropertyHelper.Metric>>>>() {});
//
//    Map<Resource.Type, Map<String, Map<String, PropertyHelper.Metric>>> jmxMetrics =
//        mapper.readValue(ClassLoader.getSystemResourceAsStream(JMX_PROPERTIES_FILE),
//            new TypeReference<Map<Resource.Type, Map<String, Map<String, PropertyHelper.Metric>>>>() {});
//
//    System.out.println("{\n");
//
//    for (Resource.Type type : Resource.Type.values()) {
//
//      Map<String, Map<String, PropertyHelper.Metric>> gMap = gangliaMetrics.get(type);
//
//      if (gMap == null) {
//        continue;
//      }
//
//      System.out.println("  \"" + type + "\":{\n");
//
//      Map<String, Map<String, PropertyHelper.Metric>> jMap = jmxMetrics.get(type);
//
//
//      for (Map.Entry<String, Map<String, PropertyHelper.Metric>> entry: gMap.entrySet()) {
//        String componentName = entry.getKey();
//
//        System.out.println("    \"" + componentName + "\":{\n");
//
//
//          Map<String, PropertyHelper.Metric> gMetricMap = entry.getValue();
//        Map<String, PropertyHelper.Metric> jMetricMap = jMap == null ?
//            Collections.<String, PropertyHelper.Metric>emptyMap() : jMap.get(componentName);
//
//        Map<String, PropertyHelper.Metric> newGMetricMap = new TreeMap<String, PropertyHelper.Metric>();
//
//        for (Map.Entry<String, PropertyHelper.Metric> metricEntry : gMetricMap.entrySet()) {
//
//          String metricName = metricEntry.getKey();
//          PropertyHelper.Metric gMetric = metricEntry.getValue();
//          PropertyHelper.Metric jMetric = jMetricMap == null ? null : jMetricMap.get(metricName);
//
//          boolean jmxPointInTime = jMetric != null && jMetric.isPointInTime();
//
//          newGMetricMap.put(metricName, new PropertyHelper.Metric(gMetric.getMetric(),
//              jmxPointInTime ? false : gMetric.isPointInTime(),
//              gMetric.isTemporal()));
//        }
//
//        for (Map.Entry<String, PropertyHelper.Metric> metricEntry : newGMetricMap.entrySet()) {
//          String metricName = metricEntry.getKey();
//          PropertyHelper.Metric gMetric = metricEntry.getValue();
//
//
//          System.out.println("      \"" + metricName + "\":{\n" +
//              "        \"metric\" : \"" + gMetric.getMetric() + "\",\n" +
//              "        \"pointInTime\" : " + gMetric.isPointInTime() + ",\n" +
//              "        \"temporal\" : " + gMetric.isTemporal() + "\n" +
//              "      },");
//
//        }
//
//        System.out.println("    },\n");
//      }
//      System.out.println("  },\n");
//    }
//    System.out.println("},\n");
//  }
//
//  @Ignore
//  @Test
//  public void testGetJPropertyIds() throws Exception {
//    ObjectMapper mapper = new ObjectMapper();
//
//
//    Map<Resource.Type, Map<String, Map<String, PropertyHelper.Metric>>> jmxMetrics =
//        mapper.readValue(ClassLoader.getSystemResourceAsStream(JMX_PROPERTIES_FILE),
//            new TypeReference<Map<Resource.Type, Map<String, Map<String, PropertyHelper.Metric>>>>() {});
//
//    System.out.println("{\n");
//
//    for (Resource.Type type : Resource.Type.values()) {
//
//      Map<String, Map<String, PropertyHelper.Metric>> jMap = jmxMetrics.get(type);
//
//      if (jMap == null) {
//        continue;
//      }
//
//      System.out.println("  \"" + type + "\":{\n");
//
//
//      for (Map.Entry<String, Map<String, PropertyHelper.Metric>> entry: jMap.entrySet()) {
//        String componentName = entry.getKey();
//
//        System.out.println("    \"" + componentName + "\":{\n");
//
//
//        Map<String, PropertyHelper.Metric> jMetricMap = entry.getValue();
//
//        Map<String, PropertyHelper.Metric> newJMetricMap = new TreeMap<String, PropertyHelper.Metric>();
//
//        for (Map.Entry<String, PropertyHelper.Metric> metricEntry : jMetricMap.entrySet()) {
//
//          String metricName = metricEntry.getKey();
//          PropertyHelper.Metric jMetric = metricEntry.getValue();
//
//
//          newJMetricMap.put(metricName, new PropertyHelper.Metric(jMetric.getMetric(),
//              jMetric.isPointInTime(),
//              jMetric.isTemporal()));
//        }
//
//        for (Map.Entry<String, PropertyHelper.Metric> metricEntry : newJMetricMap.entrySet()) {
//          String metricName = metricEntry.getKey();
//          PropertyHelper.Metric jMetric = metricEntry.getValue();
//
//
//          System.out.println("      \"" + metricName + "\":{\n" +
//              "        \"metric\" : \"" + jMetric.getMetric() + "\",\n" +
//              "        \"pointInTime\" : " + jMetric.isPointInTime() + ",\n" +
//              "        \"temporal\" : " + jMetric.isTemporal() + "\n" +
//              "      },");
//
//        }
//
//        System.out.println("    },\n");
//      }
//      System.out.println("  },\n");
//    }
//    System.out.println("},\n");
//  }
}

