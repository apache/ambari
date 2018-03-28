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
package org.apache.solr.security;

import org.apache.hadoop.security.authentication.server.AuthenticationToken;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InfraKerberosHostValidatorTest {

  private static final String DEFAULT_SERVICE_USER = "logsearch";

  private InfraKerberosHostValidator underTest = new InfraKerberosHostValidator();
  private AuthenticationToken principal;


  @Before
  public void setUp() {
    principal = new AuthenticationToken(DEFAULT_SERVICE_USER, DEFAULT_SERVICE_USER + "/c6401.ambari.apache.org@EXAMPLE.COM", "kerberos");
  }

  @Test
  public void testValidateHosts() {
    // GIVEN
    Map<String, Set<String>> userHostsMap = generateUserHostMap("c6401.ambari.apache.org");
    // WHEN
    boolean result = underTest.validate(principal, userHostsMap, new HashMap<String, String>());
    // THEN
    assertTrue(result);
  }

  @Test
  public void testValidateHostsValid() {
    // GIVEN
    Map<String, Set<String>> userHostsMap = generateUserHostMap("c6402.ambari.apache.org");
    // WHEN
    boolean result = underTest.validate(principal, userHostsMap, new HashMap<String, String>());
    // THEN
    assertFalse(result);

  }

  @Test
  public void testValidateHostRegex() {
    // GIVEN
    Map<String, String> userHostRegex = generateRegexMap("c\\d+.*.apache.org");
    // WHEN
    boolean result = underTest.validate(principal, new HashMap<String, Set<String>>(), userHostRegex);
    // THEN
    assertTrue(result);

  }

  @Test
  public void testValidateHostRegexInvalid() {
    // GIVEN
    Map<String, String> userHostRegex = generateRegexMap("c\\d+.*.org.apache");
    // WHEN
    boolean result = underTest.validate(principal, new HashMap<String, Set<String>>(), userHostRegex);
    // THEN
    assertFalse(result);
  }

  @Test
  public void testPrecedence() {
    // GIVEN
    Map<String, Set<String>> userHostsMap = generateUserHostMap("c6402.ambari.apache.org");
    Map<String, String> userHostRegex = generateRegexMap("c\\d+.*.apache.org");
    // WHEN
    boolean result = underTest.validate(principal, userHostsMap, userHostRegex);
    // THEN
    assertTrue(result);
  }

  private Map<String, Set<String>> generateUserHostMap(String... hosts) {
    Map<String, Set<String>> map = new HashMap<>();
    Set<String> hostSet = new HashSet<>();
    for (String host : hosts) {
      hostSet.add(host);
    }
    map.put(DEFAULT_SERVICE_USER, hostSet);
    return map;
  }

  private Map<String, String> generateRegexMap(String regex) {
    Map<String, String> map = new HashMap<>();
    map.put(DEFAULT_SERVICE_USER, regex);
    return map;
  }
}
