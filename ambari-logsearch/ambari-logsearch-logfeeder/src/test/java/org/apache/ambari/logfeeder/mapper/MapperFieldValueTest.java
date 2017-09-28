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

import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.MapFieldValueDescriptorImpl;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapperFieldValueTest {
  private static final Logger LOG = Logger.getLogger(MapperFieldValueTest.class);

  @Test
  public void testMapperFieldValue_replaceValue() {
    LOG.info("testMapperFieldValue_replaceValue()");

    MapFieldValueDescriptorImpl mapFieldValueDescriptor = new MapFieldValueDescriptorImpl();
    mapFieldValueDescriptor.setPreValue("someValue");
    mapFieldValueDescriptor.setPostValue("someOtherValue");

    MapperFieldValue mapperFieldValue = new MapperFieldValue();
    assertTrue("Could not initialize!", mapperFieldValue.init(null, "someField", null, mapFieldValueDescriptor));

    Map<String, Object> jsonObj = new HashMap<>();

    Object mappedValue = mapperFieldValue.apply(jsonObj, "someValue");

    assertEquals("Value wasn't mapped", "someOtherValue", mappedValue);
    assertEquals("New field wasn't put into jsonObj", "someOtherValue", jsonObj.remove("someField"));
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }

  @Test
  public void testMapperFieldValue_noPostValue() {
    LOG.info("testMapperFieldValue_noPostValue()");

    MapFieldValueDescriptorImpl mapFieldValueDescriptor = new MapFieldValueDescriptorImpl();

    MapperFieldValue mapperFieldValue = new MapperFieldValue();
    assertFalse("Was not able to initialize!", mapperFieldValue.init(null, "someField", null, mapFieldValueDescriptor));
  }

  @Test
  public void testMapperFieldValue_noPreValueFound() {
    LOG.info("testMapperFieldValue_noPreValueFound()");

    MapFieldValueDescriptorImpl mapFieldValueDescriptor = new MapFieldValueDescriptorImpl();
    mapFieldValueDescriptor.setPreValue("someValue");
    mapFieldValueDescriptor.setPostValue("someOtherValue");

    MapperFieldValue mapperFieldValue = new MapperFieldValue();
    assertTrue("Could not initialize!", mapperFieldValue.init(null, "someField", null, mapFieldValueDescriptor));

    Map<String, Object> jsonObj = new HashMap<>();

    Object mappedValue = mapperFieldValue.apply(jsonObj, "yetAnotherValue");

    assertEquals("Value was mapped", "yetAnotherValue", mappedValue);
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }
}
