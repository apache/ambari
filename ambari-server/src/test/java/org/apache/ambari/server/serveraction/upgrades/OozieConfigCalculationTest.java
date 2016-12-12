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
package org.apache.ambari.server.serveraction.upgrades;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

/**
 * Tests OozieConfigCalculation logic
 */
public class OozieConfigCalculationTest {

  /**
   * Checks that -Dhdp.version is added to $HADOOP_OPTS variable at oozie-env
   * content.
   * Also checks that it is not added multiple times during upgrades
   * @throws Exception
   */
  @Test
  public void testOozieEnvWithMissingParam() throws Exception {
    // Test case when old content does not contain $HADOOP_OPTS variable at all
    String oldContent = "#!/bin/bash\n" +
      "\n" +
      "if [ -d \"/usr/lib/bigtop-tomcat\" ]; then\n" +
      "  export OOZIE_CONFIG=${OOZIE_CONFIG:-/etc/oozie/conf}\n" +
      "  export CATALINA_BASE=${CATALINA_BASE:-{{oozie_server_dir}}}\n" +
      "  export CATALINA_TMPDIR=${CATALINA_TMPDIR:-/var/tmp/oozie}\n" +
      "  export OOZIE_CATALINA_HOME=/usr/lib/bigtop-tomcat\n" +
      "fi\n" +
      "\n" +
      "# export OOZIE_BASE_URL=\"http://${OOZIE_HTTP_HOSTNAME}:${OOZIE_HTTP_PORT}/oozie\"\n" +
      "export JAVA_LIBRARY_PATH={{hadoop_lib_home}}/native/Linux-amd64-64";
    String newContent = OozieConfigCalculation.processPropertyValue(oldContent);
    assertTrue(newContent.endsWith("export HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\" "));
    // Test case when old content contains proper $HADOOP_OPTS variable
    oldContent = newContent;
    newContent = OozieConfigCalculation.processPropertyValue(oldContent);
    assertEquals(newContent, oldContent);
    assertEquals(1, StringUtils.countMatches(newContent, "-Dhdp.version"));
    // Test case when old content contains $HADOOP_OPTS variable with some value
    oldContent = "#!/bin/bash\n" +
      "\n" +
      "if [ -d \"/usr/lib/bigtop-tomcat\" ]; then\n" +
      "  export OOZIE_CONFIG=${OOZIE_CONFIG:-/etc/oozie/conf}\n" +
      "  export CATALINA_BASE=${CATALINA_BASE:-{{oozie_server_dir}}}\n" +
      "  export CATALINA_TMPDIR=${CATALINA_TMPDIR:-/var/tmp/oozie}\n" +
      "  export OOZIE_CATALINA_HOME=/usr/lib/bigtop-tomcat\n" +
      "  export HADOOP_OPTS=-Dsome.option1 -Dsome.option1 $HADOOP_OPTS\n" +
      "fi\n" +
      "\n" +
      "# export OOZIE_BASE_URL=\"http://${OOZIE_HTTP_HOSTNAME}:${OOZIE_HTTP_PORT}/oozie\"\n" +
      "export JAVA_LIBRARY_PATH={{hadoop_lib_home}}/native/Linux-amd64-64";
    newContent = OozieConfigCalculation.processPropertyValue(oldContent);
    assertEquals("#!/bin/bash\n" +
      "\n" +
      "if [ -d \"/usr/lib/bigtop-tomcat\" ]; then\n" +
      "  export OOZIE_CONFIG=${OOZIE_CONFIG:-/etc/oozie/conf}\n" +
      "  export CATALINA_BASE=${CATALINA_BASE:-{{oozie_server_dir}}}\n" +
      "  export CATALINA_TMPDIR=${CATALINA_TMPDIR:-/var/tmp/oozie}\n" +
      "  export OOZIE_CATALINA_HOME=/usr/lib/bigtop-tomcat\n" +
      "  export HADOOP_OPTS=-Dsome.option1 -Dsome.option1 $HADOOP_OPTS\n" +
      "fi\n" +
      "\n" +
      "# export OOZIE_BASE_URL=\"http://${OOZIE_HTTP_HOSTNAME}:${OOZIE_HTTP_PORT}/oozie\"\n" +
      "export JAVA_LIBRARY_PATH={{hadoop_lib_home}}/native/Linux-amd64-64\n" +
      "export HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\" ", newContent);
  }

}
