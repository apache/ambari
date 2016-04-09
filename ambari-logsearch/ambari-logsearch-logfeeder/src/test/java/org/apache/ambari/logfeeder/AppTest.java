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
package org.apache.ambari.logfeeder;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.logfeeder.filter.FilterGrok;
import org.apache.log4j.Logger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
  static Logger logger = Logger.getLogger(AppTest.class);

  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public AppTest(String testName) {
    super(testName);
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite() {
    return new TestSuite(AppTest.class);
  }

  /**
   * Rigourous Test :-)
   */
  public void testApp() {
    assertTrue(true);
  }

  public void testGrok() {
    logger.info("testGrok()");
    FilterGrok grokFilter = new FilterGrok();
    try {
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("message_pattern",
        "^%{LOGLEVEL:level}%{SPACE}%{GREEDYDATA:log_message}");
      grokFilter.loadConfig(map);
      grokFilter.init();
      String out = grokFilter.grokParse("INFO This is a test");
      logger.info("out=" + out);

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      assertFalse(true);
    }

    assertTrue(true);
  }

  public void testGrokUGI() {
    logger.info("testGrok()");
    String[] ugis = new String[]{"user1@xyz.com (auth:TOKEN)",
      "ambari-qa@example.com (auth:kerberos)",
      "my_user@example.com (auth:kerberos)",
      "hive/bdurai-dojran-2.novalocal@example.com (auth:kerberos)",
      "just_me",
      "ambari-qa (auth:PROXY) via hive/myhost.novalocal@EXAMPLE.COM (auth:KERBEROS)"};

    FilterGrok grokFilter = new FilterGrok();
    try {
      Map<String, Object> map = new HashMap<String, Object>();
      // map.put("message_pattern",
      // "(?<user>([\\w\\d\\-]+))\\/|(?<user>([\\w\\d\\-]+))@|(?<user>([\\w\\d\\-]+))/[\\w\\d\\-.]+@|(?<user>([\\w\\d.\\-_]+))[\\s(]+");
      // map.put("message_pattern",
      // "(?<user>([\\w\\d\\-]+))/[\\w\\d\\-.]+@");
      // *(auth:(?<auth>[\\w\\d\\-]+))
      // GOOD: map.put("message_pattern", "(?<user>([\\w\\d\\-]+)).+auth:(?<auth>([\\w\\d\\-]+))");
      // OK: map.put("message_pattern", "(?<user>([\\w\\d\\-]+)).+auth:(?<auth>([\\w\\d\\-]+))|%{USERNAME:xuser}");
      //map.put("message_pattern", "%{USERNAME:user}.+auth:%{USERNAME:authType}|%{USERNAME:x_user}");
      map.put("message_pattern", "%{USERNAME:p_user}.+auth:%{USERNAME:p_authType}.+via %{USERNAME:k_user}.+auth:%{USERNAME:k_authType}|%{USERNAME:user}.+auth:%{USERNAME:authType}|%{USERNAME:x_user}");
      grokFilter.loadConfig(map);
      grokFilter.init();
      for (String ugi : ugis) {
        String out = grokFilter.grokParse(ugi);
        logger.info(ugi + "=" + out);
      }

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      assertFalse(true);
    }
    assertTrue(true);
  }
}
