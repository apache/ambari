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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.topology;

import java.io.IOException;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class ProvisionClusterTemplateTest {

  public static final String CLUSTER_TEMPLATE = getResource("blueprintv2/cluster_template_v2.json");


  @Test
  public void testProvisionClusterTemplate() throws Exception {
    ProvisionClusterTemplateFactory factory = new ProvisionClusterTemplateFactory();
    ProvisionClusterTemplate template = factory.convertFromJson(CLUSTER_TEMPLATE);
    System.out.println(template);
  }


  private static String getResource(String fileName) {
    try {
      return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
