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

package org.apache.ambari.view.hive;

import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.validation.Validator;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class PropertyValidatorTest {

  @Test
  public void testValidatePropertyWebHDFSCom() throws Exception {
    PropertyValidator validator = new PropertyValidator();
    ViewInstanceDefinition definition = getViewInstanceDefinition();

    definition.getPropertyMap().put(PropertyValidator.WEBHDFS_URL, "hdfs://hostname.com:8020");

    assertTrue(validator.validateProperty(PropertyValidator.WEBHDFS_URL,
        definition, Validator.ValidationContext.PRE_CREATE).isValid());

    definition.getPropertyMap().put(PropertyValidator.WEBHDFS_URL, "webhdfs://hostname.com:50070");

    assertTrue(validator.validateProperty(PropertyValidator.WEBHDFS_URL,
        definition, Validator.ValidationContext.PRE_CREATE).isValid());

    definition.getPropertyMap().put(PropertyValidator.WEBHDFS_URL, "http://hostname.com:50070");

    assertFalse(validator.validateProperty(PropertyValidator.WEBHDFS_URL,
        definition, Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyWebHDFSInternal() throws Exception {
    PropertyValidator validator = new PropertyValidator();
    ViewInstanceDefinition definition = getViewInstanceDefinition();

    definition.getPropertyMap().put(PropertyValidator.WEBHDFS_URL, "hdfs://hostname.internal:8020");

    assertTrue(validator.validateProperty(PropertyValidator.WEBHDFS_URL,
        definition, Validator.ValidationContext.PRE_CREATE).isValid());

    definition.getPropertyMap().put(PropertyValidator.WEBHDFS_URL, "webhdfs://hostname.internal:50070");

    assertTrue(validator.validateProperty(PropertyValidator.WEBHDFS_URL,
        definition, Validator.ValidationContext.PRE_CREATE).isValid());

    definition.getPropertyMap().put(PropertyValidator.WEBHDFS_URL, "http://hostname.internal:50070");

    assertFalse(validator.validateProperty(PropertyValidator.WEBHDFS_URL,
        definition, Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyATSCom() throws Exception {
    PropertyValidator validator = new PropertyValidator();
    ViewInstanceDefinition definition = getViewInstanceDefinition();

    definition.getPropertyMap().put(PropertyValidator.YARN_ATS_URL, "http://hostname.com:8088");

    assertTrue(validator.validateProperty(PropertyValidator.YARN_ATS_URL,
        definition, Validator.ValidationContext.PRE_CREATE).isValid());
  }

  @Test
  public void testValidatePropertyATSInternal() throws Exception {
    PropertyValidator validator = new PropertyValidator();
    ViewInstanceDefinition definition = getViewInstanceDefinition();

    definition.getPropertyMap().put(PropertyValidator.YARN_ATS_URL, "http://hostname.internal:8088");

    assertTrue(validator.validateProperty(PropertyValidator.YARN_ATS_URL,
        definition, Validator.ValidationContext.PRE_CREATE).isValid());
  }

  private ViewInstanceDefinition getViewInstanceDefinition() {
    ViewInstanceDefinition definition = EasyMock.createNiceMock(ViewInstanceDefinition.class);
    expect(definition.getClusterHandle()).andReturn(null).anyTimes();
    Map<String, String> properties = new HashMap<String, String>();
    expect(definition.getPropertyMap()).andReturn(properties).anyTimes();
    replay(definition);
    return definition;
  }
}