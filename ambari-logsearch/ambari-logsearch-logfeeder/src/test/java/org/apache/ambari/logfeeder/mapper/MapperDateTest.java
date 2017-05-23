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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.MapDateDescriptorImpl;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapperDateTest {
  private static final Logger LOG = Logger.getLogger(MapperDateTest.class);

  @Test
  public void testMapperDate_epoch() {
    LOG.info("testMapperDate_epoch()");

    MapDateDescriptorImpl mapDateDescriptor = new MapDateDescriptorImpl();
    mapDateDescriptor.setTargetDatePattern("epoch");

    MapperDate mapperDate = new MapperDate();
    assertTrue("Could not initialize!", mapperDate.init(null, "someField", null, mapDateDescriptor));

    Map<String, Object> jsonObj = new HashMap<>();

    Date d = DateUtils.truncate(new Date(), Calendar.SECOND);
    Object mappedValue = mapperDate.apply(jsonObj, Long.toString(d.getTime() / 1000));

    assertEquals("Value wasn't matched properly", d, mappedValue);
    assertEquals("Value wasn't put into jsonObj", d, jsonObj.remove("someField"));
    assertEquals("Value wasn't put into jsonObj", d.getTime(), jsonObj.remove(LogFeederConstants.IN_MEMORY_TIMESTAMP));
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }

  @Test
  public void testMapperDate_pattern() throws Exception {
    LOG.info("testMapperDate_pattern()");

    MapDateDescriptorImpl mapDateDescriptor = new MapDateDescriptorImpl();
    mapDateDescriptor.setTargetDatePattern("yyyy-MM-dd HH:mm:ss.SSS");

    MapperDate mapperDate = new MapperDate();
    assertTrue("Could not initialize!", mapperDate.init(null, "someField", null, mapDateDescriptor));

    Map<String, Object> jsonObj = new HashMap<>();
    String dateString = "2016-04-08 15:55:23.548";
    Object mappedValue = mapperDate.apply(jsonObj, dateString);

    Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(dateString);

    assertEquals("Value wasn't matched properly", d, mappedValue);
    assertEquals("Value wasn't put into jsonObj", d, jsonObj.remove("someField"));
    assertEquals("Value wasn't put into jsonObj", d.getTime(), jsonObj.remove(LogFeederConstants.IN_MEMORY_TIMESTAMP));
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }

  @Test
  public void testMapperDate_noDatePattern() {
    LOG.info("testMapperDate_noDatePattern()");

    MapDateDescriptorImpl mapDateDescriptor = new MapDateDescriptorImpl();

    MapperDate mapperDate = new MapperDate();
    assertFalse("Was not able to initialize!", mapperDate.init(null, "someField", null, mapDateDescriptor));
  }

  @Test
  public void testMapperDate_notParsableDatePattern() {
    LOG.info("testMapperDate_notParsableDatePattern()");

    MapDateDescriptorImpl mapDateDescriptor = new MapDateDescriptorImpl();
    mapDateDescriptor.setTargetDatePattern("not_parsable_content");

    MapperDate mapperDate = new MapperDate();
    assertFalse("Was not able to initialize!", mapperDate.init(null, "someField", null, mapDateDescriptor));
  }

  @Test
  public void testMapperDate_invalidEpochValue() {
    LOG.info("testMapperDate_invalidEpochValue()");

    MapDateDescriptorImpl mapDateDescriptor = new MapDateDescriptorImpl();
    mapDateDescriptor.setTargetDatePattern("epoch");

    MapperDate mapperDate = new MapperDate();
    assertTrue("Could not initialize!", mapperDate.init(null, "someField", null, mapDateDescriptor));

    Map<String, Object> jsonObj = new HashMap<>();
    String invalidValue = "abc";
    Object mappedValue = mapperDate.apply(jsonObj, invalidValue);

    assertEquals("Invalid value wasn't returned as it is", invalidValue, mappedValue);
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }

  @Test
  public void testMapperDate_invalidDateStringValue() {
    LOG.info("testMapperDate_invalidDateStringValue()");

    MapDateDescriptorImpl mapDateDescriptor = new MapDateDescriptorImpl();
    mapDateDescriptor.setTargetDatePattern("yyyy-MM-dd HH:mm:ss.SSS");

    MapperDate mapperDate = new MapperDate();
    assertTrue("Could not initialize!", mapperDate.init(null, "someField", null, mapDateDescriptor));

    Map<String, Object> jsonObj = new HashMap<>();
    String invalidValue = "abc";
    Object mappedValue = mapperDate.apply(jsonObj, invalidValue);

    assertEquals("Invalid value wasn't returned as it is", invalidValue, mappedValue);
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }
}
