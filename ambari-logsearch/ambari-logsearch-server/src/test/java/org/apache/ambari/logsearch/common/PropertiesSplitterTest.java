/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.common;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PropertiesSplitterTest {

  private PropertiesSplitter underTest;

  @Before
  public void setUp() {
    underTest = new PropertiesSplitter();
  }

  @Test
  public void testParseList() {
    // GIVEN
    // WHEN
    List<String> values = underTest.parseList("v1,v2");
    // THEN
    assertTrue(values.contains("v1"));
    assertTrue(values.contains("v2"));
  }

  @Test
  public void testParseListWithEmptyString() {
    // GIVEN
    // WHEN
    List<String> values = underTest.parseList("");
    // THEN
    assertTrue(values.isEmpty());
  }

  @Test
  public void testParseMap() {
    // GIVEN
    // WHEN
    Map<String, String> keyValues = underTest.parseMap("k1:v1,k2:v2");
    // THEN
    assertEquals("v1", keyValues.get("k1"));
    assertEquals("v2", keyValues.get("k2"));
  }

  @Test
  public void testParseMapWithEmptyValue() {
    // GIVEN
    // WHEN
    Map<String, String> keyValues = underTest.parseMap("k1:v1,k2:");
    // THEN
    assertEquals("v1", keyValues.get("k1"));
    assertEquals("", keyValues.get("k2"));
  }

  @Test
  public void testParseMapWithMissingKey() {
    // GIVEN
    // WHEN
    Map<String, String> keyValues = underTest.parseMap("k1:v1,:v2");
    // THEN
    assertEquals("v1", keyValues.get("k1"));
    assertNull(keyValues.get("k2"));
    assertEquals(1, keyValues.size());
  }

  @Test
  public void testParseMapInMap() {
    // GIVEN
    // WHEN
    Map<String, Map<String, String>> keyMapValues = underTest.parseMapInMap("K1#k1:v1,k2:v2;K2#k3:v3,k4:v4");
    // THEN
    Map<String, String> keyValues1 = keyMapValues.get("K1");
    Map<String, String> keyValues2 = keyMapValues.get("K2");
    assertNotNull(keyValues1);
    assertNotNull(keyValues2);
    assertEquals("v1", keyValues1.get("k1"));
    assertEquals("v2", keyValues1.get("k2"));
    assertEquals("v3", keyValues2.get("k3"));
    assertEquals("v4", keyValues2.get("k4"));
  }

  @Test
  public void testParseListInMap() {
    // GIVEN
    // WHEN
    Map<String, List<String>> listInMap = underTest.parseListInMap("K1:v1,v2;K2:v3,v4");
    // THEN
    List<String> valueList1 = listInMap.get("K1");
    List<String> valueList2 = listInMap.get("K2");
    assertNotNull(valueList1);
    assertNotNull(valueList2);
    assertEquals("v1", valueList1.get(0));
    assertEquals("v2", valueList1.get(1));
    assertEquals("v3", valueList2.get(0));
    assertEquals("v4", valueList2.get(1));
  }

}
