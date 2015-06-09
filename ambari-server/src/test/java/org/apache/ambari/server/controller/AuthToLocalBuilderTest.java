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
  public void testRuleGeneration() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    builder.addRule("foobar@EXAMPLE.COM", "hdfs");

    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
        "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
        "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
        "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
        "DEFAULT",
      builder.generate("EXAMPLE.COM"));
  }


  @Test
  public void testRuleGeneration_caseInsensitiveSupport() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder(true);

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    builder.addRule("foobar@EXAMPLE.COM", "hdfs");

    assertEquals(
      "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*///L\n" +
        "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
        "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
        "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
        "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
        "DEFAULT",
      builder.generate("EXAMPLE.COM"));
  }

  @Test
  public void testRuleGeneration_ExistingRules() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();
    // previously generated non-host specific rules
    builder.addRule("foobar@EXAMPLE.COM", "hdfs");
    // doesn't exist in latter generation
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    String existingRules = builder.generate("EXAMPLE.COM");

    builder = new AuthToLocalBuilder();
    // set previously existing rules
    builder.addRules(existingRules);

    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate of existing rule should not result in duplicate rule generation
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // duplicated again in this builder should not result in duplicate rule generation
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
        "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
        "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
        "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
        "DEFAULT",
        builder.generate("EXAMPLE.COM"));
  }

  @Test
  public void testRuleGeneration_ExistingRules_existingMoreSpecificRule() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();
    // previously generated non-host specific rules
    builder.addRule("foobar@EXAMPLE.COM", "hdfs");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    String existingRules = builder.generate("EXAMPLE.COM");
    // prepend host specific rule
    existingRules = "RULE:[2:$1/$2@$0](dn/somehost.com@EXAMPLE.COM)s/.*/hdfs/\n" + existingRules;
    // append default realm rule for additional realm
    existingRules += "\nRULE:[1:$1@$0](.*@OTHER_REALM.COM)s/@.*//";

    builder = new AuthToLocalBuilder();
    // set previously existing rules
    builder.addRules(existingRules);
    // more specific host qualifed rule exists for dn
    // non-host specific rule should still be generated but occur later in generated string
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // Duplicate principal for secondary namenode, should be filtered out...
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    // duplicate of existing rule
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");


    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "RULE:[1:$1@$0](.*@OTHER_REALM.COM)s/@.*//\n" +
        "RULE:[2:$1/$2@$0](dn/somehost.com@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
        "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
        "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\n" +
        "RULE:[2:$1@$0](rs@EXAMPLE.COM)s/.*/hbase/\n" +
        "DEFAULT",
        builder.generate("EXAMPLE.COM"));
  }

  @Test
  public void testAddNullExistingRule() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();
    builder.addRules(null);

    assertEquals(
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "DEFAULT",
        builder.generate("EXAMPLE.COM")
    );
  }


  @Test
  public void testRulesWithWhitespace() {
    String rulesWithWhitespace =
        "RULE:   [1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[  1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "RULE:[2:   $1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0   ](hm@EXAMPLE.COM)s/.*/hbase/\n" +
        "RULE:[2:$1@$0]   (jhs@EXAMPLE.COM)s/.*/mapred/\n" +
        "RULE:[2:$1@$0](jn@EXAMPLE.COM)   s/.*/hdfs/\n";

    AuthToLocalBuilder builder = new AuthToLocalBuilder();
    builder.addRules(rulesWithWhitespace);

    assertEquals(
        "RULE:[1:$1@$0](foobar@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "RULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](hm@EXAMPLE.COM)s/.*/hbase/\n" +
        "RULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\n" +
        "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "DEFAULT",
        builder.generate("EXAMPLE.COM"));

  }

  @Test
  public void testExistingRuleWithNoRealm() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();
    builder.addRules("RULE:[1:$1](foobar)s/.*/hdfs/");

    assertEquals(
        "RULE:[1:$1](foobar)s/.*/hdfs/\n" +
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "DEFAULT",
        builder.generate("EXAMPLE.COM"));
  }

  @Test
  public void testExistingRuleWithNoRealm2() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();
    builder.addRules("RULE:[1:$1/$2](foobar/someHost)s/.*/hdfs/");

    assertEquals(
        "RULE:[1:$1/$2](foobar/someHost)s/.*/hdfs/\n" +
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "DEFAULT",
        builder.generate("EXAMPLE.COM"));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testAddNewRuleWithNoRealm() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();

    builder.addRule("someUser", "hdfs");
  }

  @Test(expected=IllegalArgumentException.class)
  public void testAddNewRuleWithNoRealm2() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();

    builder.addRule("someUser/someHost", "hdfs");
  }

  @Test
  public void testExistingWildcardRealm() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();
    builder.addRules("RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\n" +
                     "RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\n" +
                     "RULE:[2:$1@$0](.*@EXAMPLE.COM)s/.*/yarn/\n" +
                     "DEFAULT");
    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");

    // ensure that no default realm rule is generated for .* realm and
    // also that that .* realm rules are ordered last in relation to
    // other rules with the same number of expected principal components
    assertEquals(
        "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\n" +
        "RULE:[2:$1@$0](jn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0](.*@EXAMPLE.COM)s/.*/yarn/\n" +
        "RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\n" +
        "RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\n" +
        "DEFAULT",
        builder.generate("EXAMPLE.COM"));
  }

  @Test
  public void testCopy() {
    AuthToLocalBuilder builder = new AuthToLocalBuilder();

    builder.addRule("nn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("dn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("jn/_HOST@EXAMPLE.COM", "hdfs");
    builder.addRule("rm/_HOST@EXAMPLE.COM", "yarn");
    builder.addRule("jhs/_HOST@EXAMPLE.COM", "mapred");
    builder.addRule("hm/_HOST@EXAMPLE.COM", "hbase");
    builder.addRule("rs/_HOST@EXAMPLE.COM", "hbase");

    builder.addRule("foobar@EXAMPLE.COM", "hdfs");

    AuthToLocalBuilder copy = builder.copy();

    assertNotSame(builder, copy);
    assertEquals(copy.generate("EXAMPLE.COM"), builder.generate("EXAMPLE.COM"));

  }
}