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
package org.apache.ambari.logfeeder.common;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IdGeneratorHelperTest {

  @Test
  public void testGenerateRandomUUID() {
    // GIVEN
    Map<String, Object> fieldKeyMap = new HashMap<>();
    List<String> fields = new ArrayList<>();
    // WHEN
    String uuid1 = IdGeneratorHelper.generateUUID(fieldKeyMap, fields);
    String uuid2 = IdGeneratorHelper.generateUUID(fieldKeyMap, fields);
    // THEN
    assertFalse(uuid1.equals(uuid2));
  }

  @Test
  public void testUUIDFromFields() {
    // GIVEN
    Map<String, Object> fieldKeyMap1 = new HashMap<>();
    fieldKeyMap1.put("one-field", "1");
    Map<String, Object> fieldKeyMap2 = new HashMap<>();
    fieldKeyMap2.put("one-field", "1");
    List<String> fields = new ArrayList<>();
    fields.add("one-field");
    // WHEN
    String uuid1 = IdGeneratorHelper.generateUUID(fieldKeyMap1, fields);
    String uuid2 = IdGeneratorHelper.generateUUID(fieldKeyMap2, fields);
    // THEN
    assertTrue(uuid1.equals(uuid2));
  }

  @Test
  public void testUUIDFromFieldsWithMultipleFields() {
    // GIVEN
    Map<String, Object> fieldKeyMap1 = new HashMap<>();
    fieldKeyMap1.put("one-field", "1");
    fieldKeyMap1.put("two-field", "2");
    Map<String, Object> fieldKeyMap2 = new HashMap<>();
    fieldKeyMap2.put("one-field", "1");
    fieldKeyMap2.put("two-field", "2");
    List<String> fields = new ArrayList<>();
    fields.add("one-field");
    fields.add("two-field");
    // WHEN
    String uuid1 = IdGeneratorHelper.generateUUID(fieldKeyMap1, fields);
    String uuid2 = IdGeneratorHelper.generateUUID(fieldKeyMap2, fields);
    // THEN
    assertTrue(uuid1.equals(uuid2));
  }

  @Test
  public void testUUIDFromFieldsDifferentNumberOfFields() {
    // GIVEN
    Map<String, Object> fieldKeyMap1 = new HashMap<>();
    fieldKeyMap1.put("one-field", "1");
    Map<String, Object> fieldKeyMap2 = new HashMap<>();
    fieldKeyMap2.put("one-field", "1");
    fieldKeyMap2.put("two-field", "2");
    List<String> fields = new ArrayList<>();
    fields.add("one-field");
    fields.add("two-field");
    // WHEN
    String uuid1 = IdGeneratorHelper.generateUUID(fieldKeyMap1, fields);
    String uuid2 = IdGeneratorHelper.generateUUID(fieldKeyMap2, fields);
    // THEN
    assertFalse(uuid1.equals(uuid2));
  }

}
