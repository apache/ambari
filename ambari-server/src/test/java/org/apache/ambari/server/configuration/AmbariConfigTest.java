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
package org.apache.ambari.server.configuration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AmbariConfigTest {

  @Test
  public void http() {
    assertEquals("http://hostname:8080/jdk_path", AmbariConfig.getAmbariServerURI("/jdk_path", "http", "hostname", 8080));
  }

  @Test
  public void https() {
    assertEquals("https://somesecuredhost:8443/mysql_path", AmbariConfig.getAmbariServerURI("/mysql_path", "https", "somesecuredhost", 8443));
  }

  @Test
  public void longerPath() {
    assertEquals("https://othersecuredhost:8443/oracle/ojdbc/", AmbariConfig.getAmbariServerURI("/oracle/ojdbc/", "https", "othersecuredhost", 8443));
  }

  @Test
  public void withQueryString() {
    assertEquals("http://hostname:8080/jdk_path?query", AmbariConfig.getAmbariServerURI("/jdk_path?query", "http", "hostname", 8080));
  }
}
