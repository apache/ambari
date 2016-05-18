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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests FixLzoCodecPath logic
 */
public class FixLzoCodecPathTest {

  @Test
  public void testFixLzoJarPath() throws Exception {
    // Test replacements in a big list of paths (typical mapreduce.application.classpath value)
    String oldContent = "$PWD/mr-framework/hadoop/share/hadoop/mapreduce/*:" +
      "$PWD/mr-framework/hadoop/share/hadoop/mapreduce/lib/*:" +
      "$PWD/mr-framework/hadoop/share/hadoop/common/*:" +
      "$PWD/mr-framework/hadoop/share/hadoop/common/lib/*:" +
      "$PWD/mr-framework/hadoop/share/hadoop/yarn/*:/usr/hdp/2.3.4.0-3485/hadoop/lib/hadoop-lzo-0.6.0.2.3.4.0-3485.jar:" +
      "$PWD/mr-framework/hadoop/share/hadoop/yarn/lib/*:$PWD/mr-framework/hadoop/share/hadoop/hdfs/*:" +
      "$PWD/mr-framework/hadoop/share/hadoop/hdfs/lib/*:$PWD/mr-framework/hadoop/share/hadoop/tools/lib/*:" +
      "/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar:/etc/hadoop/conf/secure";
    String newContent = FixLzoCodecPath.fixLzoJarPath(oldContent);
    assertEquals("$PWD/mr-framework/hadoop/share/hadoop/mapreduce/*:" +
      "$PWD/mr-framework/hadoop/share/hadoop/mapreduce/lib/*:" +
      "$PWD/mr-framework/hadoop/share/hadoop/common/*:$PWD/mr-framework/hadoop/share/hadoop/common/lib/*:" +
      "$PWD/mr-framework/hadoop/share/hadoop/yarn/*:/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar:" +
      "$PWD/mr-framework/hadoop/share/hadoop/yarn/lib/*:$PWD/mr-framework/hadoop/share/hadoop/hdfs/*:" +
      "$PWD/mr-framework/hadoop/share/hadoop/hdfs/lib/*:$PWD/mr-framework/hadoop/share/hadoop/tools/lib/*:" +
      "/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar:/etc/hadoop/conf/secure", newContent);
    
    // Test replacements in typical tez.cluster.additional.classpath.prefix value
    oldContent = "/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar:/etc/hadoop/conf/secure";
    newContent = FixLzoCodecPath.fixLzoJarPath(oldContent);
    assertEquals("/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar:/etc/hadoop/conf/secure", newContent);

    oldContent = "/usr/hdp/2.3.4.0-3485/hadoop/lib/hadoop-lzo-0.6.0.2.3.4.0-345.jar:/etc/hadoop/conf/secure";
    newContent = FixLzoCodecPath.fixLzoJarPath(oldContent);
    assertEquals("/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar:/etc/hadoop/conf/secure", newContent);

    oldContent = "/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.2.3.4.0-345.jar:/etc/hadoop/conf/secure";
    newContent = FixLzoCodecPath.fixLzoJarPath(oldContent);
    assertEquals("/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar:/etc/hadoop/conf/secure", newContent);

    oldContent = "/usr/hdp/2.3.4.0-3485/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar:/etc/hadoop/conf/secure";
    newContent = FixLzoCodecPath.fixLzoJarPath(oldContent);
    assertEquals("/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar:/etc/hadoop/conf/secure", newContent);

    // Other corner cases tests
    oldContent = "/usr/hdp/2.3.4.0-3485/hadoop/lib/hadoop-lzo-0.6.0.2.3.4.0-1.jar";
    newContent = FixLzoCodecPath.fixLzoJarPath(oldContent);
    assertEquals("/usr/hdp/${hdp.version}/hadoop/lib/hadoop-lzo-0.6.0.${hdp.version}.jar", newContent);

    oldContent = "/etc/hadoop/conf/secure";
    newContent = FixLzoCodecPath.fixLzoJarPath(oldContent);
    assertEquals("/etc/hadoop/conf/secure", newContent);
  }

}
