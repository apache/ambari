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

package org.apache.ambari.server.controller;

import org.junit.Test;

import static org.junit.Assert.*;

public class AuthToLocalBuilderTest {

  @Test
  public void testExpectedRules() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();

    builder.append("nn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.append("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.append("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.append("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.append("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.append("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.append("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.append("rs/_HOST@EXAMPLE.COM", "hbase");

    builder.append("foobar@EXAMPLE.COM", "hdfs");

    assertEquals("RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "DEFAULT",
        builder.generate("EXAMPLE.COM"));
  }

  public void testUnexpectedRules() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();

    builder.append("nn/c6501.ambari.apache.org", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.append("nn/c6502.ambari.apache.org@EXAMPLE.COM", "hdfs");
    builder.append("dn/c6501.ambari.apache.org@EXAMPLE.COM", "hdfs");
    builder.append("jn/c6501.ambari.apache.org@EXAMPLE.COM", "hdfs");
    builder.append("rm/c6501.ambari.apache.org@EXAMPLE.COM", "yarn");
    builder.append("jhs/c6501.ambari.apache.org@EXAMPLE.COM", "mapred");
    builder.append("hm/c6501.ambari.apache.org@EXAMPLE.COM", "hbase");
    builder.append("rs/c6501.ambari.apache.org@EXAMPLE.COM", "hbase");

    builder.append("hdfs@EXAMPLE.COM", "hdfs");
    builder.append("hdfs/admin@EXAMPLE.COM", "hdfs");

    // This is an unexpected invalid principal format, it should be ignored
    builder.append("hdfs:admin@EXAMPLE.COM", "hdfs");

    assertEquals("RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
            "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
            "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
            "RULE:[1:$1@$0](hdfs@EXAMPLE.COM)s/.*/hdfs/\n" +
            "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
            "DEFAULT",
        builder.generate("EXAMPLE.COM"));
  }

}