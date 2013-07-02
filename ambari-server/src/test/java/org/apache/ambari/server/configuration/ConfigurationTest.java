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

package org.apache.ambari.server.configuration;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationTest {

  private Injector injector;

  @Inject
  private Configuration config;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.injectMembers(this);
  }

  @After
  public void teardown() throws AmbariException {
  }

  /**
   * ambari.properties doesn't contain "security.server.two_way_ssl" option
   * @throws Exception
   */
  @Test
  public void testDefaultTwoWayAuthNotSet() throws Exception {
    Assert.assertFalse(config.getTwoWaySsl());
  }

  /**
   * ambari.properties contains "security.server.two_way_ssl=true" option
   * @throws Exception
   */
  @Test
  public void testTwoWayAuthTurnedOn() throws Exception {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty("security.server.two_way_ssl", "true");
    Configuration conf = new Configuration(ambariProperties);
    Assert.assertTrue(conf.getTwoWaySsl());
  }

  /**
   * ambari.properties contains "security.server.two_way_ssl=false" option
   * @throws Exception
   */
  @Test
  public void testTwoWayAuthTurnedOff() throws Exception {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty("security.server.two_way_ssl", "false");
    Configuration conf = new Configuration(ambariProperties);
    Assert.assertFalse(conf.getTwoWaySsl());
  }

  @Test
  public void testGetClientSSLApiPort() throws Exception {
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty(Configuration.CLIENT_API_SSL_PORT_KEY, "6666");
    Configuration conf = new Configuration(ambariProperties);
    Assert.assertEquals(6666, conf.getClientSSLApiPort());
    conf = new Configuration();
    Assert.assertEquals(8443, conf.getClientSSLApiPort());
  }

  @Test
  public void testGetClientHTTPSSettings() throws IOException {

    File passFile = File.createTempFile("https.pass.", "txt");
    passFile.deleteOnExit();
    
    String password = "pass12345";
    
    FileUtils.writeStringToFile(passFile, password);
    
    Properties ambariProperties = new Properties();
    ambariProperties.setProperty(Configuration.API_USE_SSL, "true");
    ambariProperties.setProperty(
        Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY,
        passFile.getParent());
    ambariProperties.setProperty(
        Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY,
        passFile.getName());
    
    Configuration conf = new Configuration(ambariProperties);
    Assert.assertTrue(conf.getApiSSLAuthentication());

    //Different certificates for two-way SSL and HTTPS
    Assert.assertFalse(conf.getConfigsMap().get(Configuration.KSTR_NAME_KEY).
      equals(conf.getConfigsMap().get(Configuration.CLIENT_API_SSL_KSTR_NAME_KEY)));
    Assert.assertFalse(conf.getConfigsMap().get(Configuration.SRVR_CRT_NAME_KEY).
      equals(conf.getConfigsMap().get(Configuration.CLIENT_API_SSL_CRT_NAME_KEY)));

    Assert.assertEquals("https.keystore.p12", conf.getConfigsMap().get(
      Configuration.CLIENT_API_SSL_KSTR_NAME_KEY));
    Assert.assertEquals(passFile.getName(), conf.getConfigsMap().get(
      Configuration.CLIENT_API_SSL_CRT_PASS_FILE_NAME_KEY));
    Assert.assertEquals(password, conf.getConfigsMap().get(Configuration.CLIENT_API_SSL_CRT_PASS_KEY));

  }

}
