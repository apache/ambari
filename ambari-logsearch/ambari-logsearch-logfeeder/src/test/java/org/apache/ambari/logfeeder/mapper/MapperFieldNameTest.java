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

package org.apache.ambari.logfeeder.mapper;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapperFieldNameTest {
  private static final Logger LOG = Logger.getLogger(MapperFieldNameTest.class);

  @Test
  public void testMapperFieldName_replaceField() {
    LOG.info("testMapperFieldName_replaceField()");

    Map<String, Object> mapConfigs = new HashMap<>();
    mapConfigs.put("new_fieldname", "someOtherField");

    MapperFieldName mapperFieldName = new MapperFieldName();
    assertTrue("Could not initialize!", mapperFieldName.init(null, "someField", null, mapConfigs));

    Map<String, Object> jsonObj = new HashMap<>();
    jsonObj.put("someField", "someValue");

    mapperFieldName.apply(jsonObj, "someOtherValue");

    assertFalse("Old field name wasn't removed", jsonObj.containsKey("someField"));
    assertEquals("New field wasn't put", "someOtherValue", jsonObj.remove("someOtherField"));
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }

  @Test
  public void testMapperFieldName_configNotMap() {
    LOG.info("testMapperFieldName_configNotMap()");

    MapperFieldName mapperFieldName = new MapperFieldName();
    assertFalse("Was able to initialize!", mapperFieldName.init(null, "someField", null, ""));
  }

  @Test
  public void testMapperFieldName_noNewFieldName() {
    LOG.info("testMapperFieldName_noNewFieldName()");

    Map<String, Object> mapConfigs = new HashMap<>();

    MapperFieldName mapperFieldName = new MapperFieldName();
    assertFalse("Was able to initialize!", mapperFieldName.init(null, "someField", null, mapConfigs));
  }
}
