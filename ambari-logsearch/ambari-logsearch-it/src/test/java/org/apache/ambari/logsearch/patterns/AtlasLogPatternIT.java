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
import java.nio.file.Paths;

import org.junit.Test;

public class AtlasLogPatternIT extends PatternITBase {

  @Test
  public void testAtlasLogLayout() {
    String layout = Log4jXml.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "ATLAS", "configuration", "atlas-log4j.xml").toString())).getLayout("FILE");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testAtlas() throws Exception {
    String layout = Log4jXml.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "ATLAS", "configuration", "atlas-log4j.xml").toString())).getLayout("FILE");

    testServiceLog("atlas_app", layout, inputConfigTemplate(new File(HDP_SERVICES_FOLDER,"ATLAS/package/templates/input.config-atlas.json.j2")));
  }
}
