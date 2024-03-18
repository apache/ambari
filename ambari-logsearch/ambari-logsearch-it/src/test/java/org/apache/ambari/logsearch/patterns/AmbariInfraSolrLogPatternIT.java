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
package org.apache.ambari.logsearch.patterns;

import java.io.File;

import org.junit.Test;

public class AmbariInfraSolrLogPatternIT extends PatternITBase {
  // TODO: use hdp_ambari_definitions
  @Test
  public void testAmbariInfraSolrLogLayout() {
    String layout = Log4jProperties.loadFrom(new File(AMBARI_STACK_DEFINITIONS, "AMBARI_INFRA_SOLR/0.1.0/properties/solr-log4j.properties.j2")).getLayout("file");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testAmbariInfraSolrGrokPatter() throws Exception {
    String layout = Log4jProperties.loadFrom(new File(AMBARI_STACK_DEFINITIONS, "AMBARI_INFRA_SOLR/0.1.0/properties/solr-log4j.properties.j2")).getLayout("file");
    testServiceLog("infra_solr", layout, inputConfigTemplate(
            new File(AMBARI_STACK_DEFINITIONS, "AMBARI_INFRA_SOLR/0.1.0/package/templates/input.config-ambari-infra.json.j2")));
  }
}
