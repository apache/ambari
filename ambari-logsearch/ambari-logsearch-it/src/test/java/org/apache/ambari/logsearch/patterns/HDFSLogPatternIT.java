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

public class HDFSLogPatternIT extends PatternITBase {

  @Test
  public void testHDFSLogLayout() {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "HDFS", "configuration", "hdfs-log4j.xml").toString())).getLayout("RFA");
    assertThatDateIsISO8601(layout);
  }

  @Test
  public void testHDFS() throws Exception {
    String layout = Log4jProperties.unwrapFrom(new File(HDP_SERVICES_FOLDER, Paths.get(
            "HDFS", "configuration", "hdfs-log4j.xml").toString())).getLayout("RFA");

    testServiceLog("hdfs_namenode", layout, inputConfigTemplate(new File(HDP_SERVICES_FOLDER, "HDFS/package/templates/input.config-hdfs.json.j2")));
  }
}
