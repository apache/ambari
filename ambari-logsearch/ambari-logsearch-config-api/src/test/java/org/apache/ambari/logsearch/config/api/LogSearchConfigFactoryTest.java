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

package org.apache.ambari.logsearch.config.api;

import java.util.Collections;

import org.apache.ambari.logsearch.config.api.LogSearchConfig;
import org.apache.ambari.logsearch.config.api.LogSearchConfigFactory;
import org.apache.ambari.logsearch.config.api.LogSearchConfig.Component;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import junit.framework.Assert;

public class LogSearchConfigFactoryTest {

  @Test
  public void testDefaultConfig() throws Exception {
    LogSearchConfig config = LogSearchConfigFactory.createLogSearchConfig(Component.SERVER,
        Collections.<String, String> emptyMap(), LogSearchConfigClass1.class);
    
    Assert.assertSame(config.getClass(), LogSearchConfigClass1.class);
  }

  @Test
  public void testCustomConfig() throws Exception {
    LogSearchConfig config = LogSearchConfigFactory.createLogSearchConfig(Component.SERVER,
        ImmutableMap.of("logsearch.config.class", "org.apache.ambari.logsearch.config.api.LogSearchConfigClass2"),
        LogSearchConfigClass1.class);
    
    Assert.assertSame(config.getClass(), LogSearchConfigClass2.class);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testNonConfigClass() throws Exception {
    LogSearchConfigFactory.createLogSearchConfig(Component.SERVER,
        ImmutableMap.of("logsearch.config.class", "org.apache.ambari.logsearch.config.api.NonLogSearchConfigClass"),
        LogSearchConfigClass1.class);
  }
}
