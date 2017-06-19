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

import org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl.MapAnonymizeDescriptorImpl;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapperAnonymizeTest {
  private static final Logger LOG = Logger.getLogger(MapperAnonymizeTest.class);

  @Test
  public void testMapperAnonymize_anonymize() {
    LOG.info("testMapperAnonymize_anonymize()");

    MapAnonymizeDescriptorImpl mapAnonymizeDescriptorImpl = new MapAnonymizeDescriptorImpl();
    mapAnonymizeDescriptorImpl.setPattern("secret <hide> / <hide> is here");

    MapperAnonymize mapperAnonymize = new MapperAnonymize();
    assertTrue("Could not initialize!", mapperAnonymize.init(null, "someField", null, mapAnonymizeDescriptorImpl));

    Map<String, Object> jsonObj = new HashMap<>();
    mapperAnonymize.apply(jsonObj, "something else secret SECRET1 / SECRET2 is here something else 2");

    assertEquals("Field wasnt anonymized", "something else secret ******* / ******* is here something else 2", jsonObj.remove("someField"));
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }

  @Test
  public void testMapperAnonymize_anonymize2() {
    LOG.info("testMapperAnonymize_anonymize2()");

    MapAnonymizeDescriptorImpl mapAnonymizeDescriptorImpl = new MapAnonymizeDescriptorImpl();
    mapAnonymizeDescriptorImpl.setPattern("<hide> / <hide> is the secret");
    mapAnonymizeDescriptorImpl.setHideChar('X');

    MapperAnonymize mapperAnonymize = new MapperAnonymize();
    assertTrue("Could not initialize!", mapperAnonymize.init(null, "someField", null, mapAnonymizeDescriptorImpl));

    Map<String, Object> jsonObj = new HashMap<>();
    mapperAnonymize.apply(jsonObj, "something else SECRET1 / SECRET2 is the secret something else 2");

    assertEquals("Field wasnt anonymized", "something else XXXXXXX / XXXXXXX is the secret something else 2", jsonObj.remove("someField"));
    assertTrue("jsonObj is not empty", jsonObj.isEmpty());
  }

  @Test
  public void testMapperAnonymize_noPattern() {
    LOG.info("testMapperAnonymize_noPattern()");

    MapAnonymizeDescriptorImpl mapAnonymizeDescriptorImpl = new MapAnonymizeDescriptorImpl();

    MapperAnonymize mapperAnonymize = new MapperAnonymize();
    assertFalse("Was not able to initialize!", mapperAnonymize.init(null, "someField", null, mapAnonymizeDescriptorImpl));
  }
}
