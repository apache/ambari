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

package org.apache.ambari.server.view.configuration;

import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.util.List;

/**
 * ServletMappingConfig tests.
 */
public class ServletMappingConfigTest {
  @Test
  public void testGetName() throws Exception {
    List<ServletMappingConfig> mappingConfigs = getServletMappingConfigs();

    Assert.assertEquals(1, mappingConfigs.size());
    Assert.assertEquals("MyViewServlet", mappingConfigs.get(0).getName());
  }

  @Test
  public void testGetUrlPattern() throws Exception {
    List<ServletMappingConfig> mappingConfigs = getServletMappingConfigs();

    Assert.assertEquals(1, mappingConfigs.size());
    Assert.assertEquals("/ui", mappingConfigs.get(0).getUrlPattern());
  }

  public static List<ServletMappingConfig> getServletMappingConfigs() throws JAXBException {
    ViewConfig viewConfig = ViewConfigTest.getConfig();
    return viewConfig.getMappings();
  }
}
