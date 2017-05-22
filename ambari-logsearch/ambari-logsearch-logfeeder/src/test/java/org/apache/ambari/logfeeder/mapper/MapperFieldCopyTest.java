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

import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.MapFieldCopyDescriptorImpl;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapperFieldCopyTest {
  private static final Logger LOG = Logger.getLogger(MapperFieldCopyTest.class);

  @Test
  public void testMapperFieldCopy_copyField() {
    LOG.info("testMapperFieldCopy_copyField()");

    MapFieldCopyDescriptorImpl mapFieldCopyDescriptor = new MapFieldCopyDescriptorImpl();
    mapFieldCopyDescriptor.setCopyName("someOtherField");

    MapperFieldCopy mapperFieldCopy = new MapperFieldCopy();
    assertTrue("Could not initialize!", mapperFieldCopy.init(null, "someField", null, mapFieldCopyDescriptor));

    Map<String, Object> jsonObj = new HashMap<>();
    jsonObj.put("someField", "someValue");

    mapperFieldCopy.apply(jsonObj, "someValue");

    assertEquals("Old field name wasn't removed", "someValue", jsonObj.remove("someField"));
    assertEquals("New field wasn't put", "someValue", jsonObj.remove("someOtherField"));
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }

  @Test
  public void testMapperFieldCopy_noNewFieldName() {
    LOG.info("testMapperFieldCopy_noNewFieldName()");

    MapFieldCopyDescriptorImpl mapFieldCopyDescriptor = new MapFieldCopyDescriptorImpl();

    MapperFieldCopy mapperFieldCopy = new MapperFieldCopy();
    assertFalse("Was not able to initialize!", mapperFieldCopy.init(null, "someField", null, mapFieldCopyDescriptor));
  }
}
