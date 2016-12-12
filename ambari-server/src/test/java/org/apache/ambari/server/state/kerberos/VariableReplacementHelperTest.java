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

package org.apache.ambari.server.state.kerberos;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import junit.framework.Assert;

@Category({ category.KerberosTest.class})
public class VariableReplacementHelperTest {
  VariableReplacementHelper helper = new VariableReplacementHelper();

  @Test
  public void testReplaceVariables() throws AmbariException {
    Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>() {
      {
        put("", new HashMap<String, String>() {{
          put("global_variable", "Hello World");
          put("variable-name", "dash");
          put("variable_name", "underscore");
          put("variable.name", "dot");
        }});

        put("config_type", new HashMap<String, String>() {{
          put("variable-name", "config_type_dash");
          put("variable_name", "config_type_underscore");
          put("variable.name", "config_type_dot");
        }});

        put("config.type", new HashMap<String, String>() {{
          put("variable-name", "config.type_dash");
          put("variable_name", "config.type_underscore");
          put("variable.name", "config.type_dot");
        }});

        put("config-type", new HashMap<String, String>() {{
          put("variable.name", "Replacement1");
          put("variable.name1", "${config-type2/variable.name}");
          put("variable.name2", "");
        }});

        put("config-type2", new HashMap<String, String>() {{
          put("variable.name", "Replacement2");
          put("self_reference", "${config-type2/self_reference}");  // This essentially references itself.
          put("${config-type/variable.name}_reference", "Replacement in the key");
        }});
      }
    };

    Assert.assertEquals("concrete",
        helper.replaceVariables("concrete", configurations));

    Assert.assertEquals("Hello World",
        helper.replaceVariables("${global_variable}", configurations));

    Assert.assertEquals("Replacement1",
        helper.replaceVariables("${config-type/variable.name}", configurations));

    Assert.assertEquals("Replacement1|Replacement2",
        helper.replaceVariables("${config-type/variable.name}|${config-type2/variable.name}", configurations));

    Assert.assertEquals("Replacement1|Replacement2|${config-type3/variable.name}",
        helper.replaceVariables("${config-type/variable.name}|${config-type2/variable.name}|${config-type3/variable.name}", configurations));

    Assert.assertEquals("Replacement2|Replacement2",
        helper.replaceVariables("${config-type/variable.name1}|${config-type2/variable.name}", configurations));

    Assert.assertEquals("Replacement1_reference",
        helper.replaceVariables("${config-type/variable.name}_reference", configurations));

    Assert.assertEquals("dash",
        helper.replaceVariables("${variable-name}", configurations));

    Assert.assertEquals("underscore",
        helper.replaceVariables("${variable_name}", configurations));

    Assert.assertEquals("config_type_dot",
        helper.replaceVariables("${config_type/variable.name}", configurations));

    Assert.assertEquals("config_type_dash",
        helper.replaceVariables("${config_type/variable-name}", configurations));

    Assert.assertEquals("config_type_underscore",
        helper.replaceVariables("${config_type/variable_name}", configurations));

    Assert.assertEquals("config.type_dot",
        helper.replaceVariables("${config.type/variable.name}", configurations));

    Assert.assertEquals("config.type_dash",
        helper.replaceVariables("${config.type/variable-name}", configurations));

    Assert.assertEquals("config.type_underscore",
        helper.replaceVariables("${config.type/variable_name}", configurations));

    Assert.assertEquals("dot",
        helper.replaceVariables("${variable.name}", configurations));

    // Replacement yields an empty string
    Assert.assertEquals("",
        helper.replaceVariables("${config-type/variable.name2}", configurations));


    // This might cause an infinite loop... we assume protection is in place...
    try {
      Assert.assertEquals("${config-type2/self_reference}",
          helper.replaceVariables("${config-type2/self_reference}", configurations));
      Assert.fail(String.format("%s expected to be thrown", AmbariException.class.getName()));
    } catch (AmbariException e) {
      // This is expected...
    }
  }

  @Test
  public void testReplaceComplicatedVariables() throws AmbariException {
    Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>() {
      {
        put("", new HashMap<String, String>() {{
          put("host", "c6401.ambari.apache.org");
          put("realm", "EXAMPLE.COM");
        }});
      }
    };

    Assert.assertEquals("hive.metastore.local=false,hive.metastore.uris=thrift://c6401.ambari.apache.org:9083,hive.metastore.sasl.enabled=true,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse,hive.exec.mode.local.auto=false,hive.metastore.kerberos.principal=hive/_HOST@EXAMPLE.COM",
        helper.replaceVariables("hive.metastore.local=false,hive.metastore.uris=thrift://${host}:9083,hive.metastore.sasl.enabled=true,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse,hive.exec.mode.local.auto=false,hive.metastore.kerberos.principal=hive/_HOST@${realm}", configurations));

    Assert.assertEquals("Hello my realm is {EXAMPLE.COM}",
        helper.replaceVariables("Hello my realm is {${realm}}", configurations));

    Assert.assertEquals("$c6401.ambari.apache.org",
        helper.replaceVariables("$${host}", configurations));
  }

  @Test
  public void testReplaceVariablesWithFunctions() throws AmbariException {
    Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>() {
      {
        put("", new HashMap<String, String>() {{
          put("delimited.data", "one,two,three,four");
          put("realm", "UNIT.TEST");
        }});

        put("kafka-broker", new HashMap<String, String>() {{
          put("listeners", "PLAINTEXT://localhost:6667");
        }});
        
        put("clusterHostInfo", new HashMap<String, String>() {{
          put("hive_metastore_host", "host1.unit.test, host2.unit.test , host3.unit.test"); // spaces are there on purpose.
        }});

        put("foobar-site", new HashMap<String, String>() {{
          put("data", "one, two, three,    four"); // spaces are there on purpose.
          put("hello", "hello");
          put("hello_there", "hello, there");
          put("hello_there_one", "hello, there, one");
        }});
      }
    };

    Assert.assertEquals("test=thrift://one:9083\\,thrift://two:9083\\,thrift://three:9083\\,thrift://four:9083",
        helper.replaceVariables("test=${delimited.data|each(thrift://%s:9083, \\\\,, \\s*\\,\\s*)}", configurations));

    Assert.assertEquals("hive.metastore.local=false,hive.metastore.uris=thrift://host1.unit.test:9083\\,thrift://host2.unit.test:9083\\,thrift://host3.unit.test:9083,hive.metastore.sasl.enabled=true,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse,hive.exec.mode.local.auto=false,hive.metastore.kerberos.principal=hive/_HOST@UNIT.TEST",
        helper.replaceVariables("hive.metastore.local=false,hive.metastore.uris=${clusterHostInfo/hive_metastore_host | each(thrift://%s:9083, \\\\,, \\s*\\,\\s*)},hive.metastore.sasl.enabled=true,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse,hive.exec.mode.local.auto=false,hive.metastore.kerberos.principal=hive/_HOST@${realm}", configurations));

    List<String> expected;
    List<String> actual;

    expected = new LinkedList<String>(Arrays.asList("four", "hello", "one", "three", "two"));
    actual = new LinkedList<String>(Arrays.asList(helper.replaceVariables("${foobar-site/hello | append(foobar-site/data, \\,, true)}", configurations).split(",")));
    Collections.sort(expected);
    Collections.sort(actual);
    Assert.assertEquals(expected, actual);

    expected = new LinkedList<String>(Arrays.asList("four", "hello", "one", "there", "three", "two"));
    actual = new LinkedList<String>(Arrays.asList(helper.replaceVariables("${foobar-site/hello_there | append(foobar-site/data, \\,, true)}", configurations).split(",")));
    Collections.sort(expected);
    Collections.sort(actual);
    Assert.assertEquals(expected, actual);

    expected = new LinkedList<String>(Arrays.asList("four", "hello", "one", "there", "three", "two"));
    actual = new LinkedList<String>(Arrays.asList(helper.replaceVariables("${foobar-site/hello_there_one | append(foobar-site/data, \\,, true)}", configurations).split(",")));
    Collections.sort(expected);
    Collections.sort(actual);
    Assert.assertEquals(expected, actual);

    expected = new LinkedList<String>(Arrays.asList("four", "hello", "one", "one", "there", "three", "two"));
    actual = new LinkedList<String>(Arrays.asList(helper.replaceVariables("${foobar-site/hello_there_one | append(foobar-site/data, \\,, false)}", configurations).split(",")));
    Collections.sort(expected);
    Collections.sort(actual);
    Assert.assertEquals(expected, actual);

    // Test invalid number of arguments.
    try {
      helper.replaceVariables("${foobar-site/hello_there_one | append(foobar-site/data, \\,)}", configurations);
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      // Ignore this is expected.
    }

    Assert.assertEquals("test=unit.test", helper.replaceVariables("test=${realm|toLower()}", configurations));
  
    Assert.assertEquals("PLAINTEXTSASL://localhost:6667", helper.replaceVariables("${kafka-broker/listeners|replace(\\bPLAINTEXT\\b,PLAINTEXTSASL)}", configurations)); 
  }

}
