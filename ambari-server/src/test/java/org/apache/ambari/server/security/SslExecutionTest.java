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
package org.apache.ambari.server.security;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Properties;

import static org.easymock.EasyMock.createNiceMock;

public class SslExecutionTest {

  private static Log LOG = LogFactory.getLog(SslExecutionTest.class);
  public TemporaryFolder temp = new TemporaryFolder();

  Injector injector;

  private static CertificateManager certMan;

  @Inject
  static void init(CertificateManager instance) {
    certMan = instance;
  }


  private class SecurityModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(Properties.class).toInstance(buildTestProperties());
      bind(Configuration.class).toConstructor(getConfigurationConstructor());
      bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      requestStaticInjection(SslExecutionTest.class);
    }
  }

  protected Properties buildTestProperties() {
    try {
      temp.create();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Properties properties = new Properties();
    properties.setProperty(Configuration.SRVR_KSTR_DIR_KEY, temp.getRoot().getAbsolutePath());

    return properties;
  }

  protected Constructor<Configuration> getConfigurationConstructor() {
    try {
      return Configuration.class.getConstructor(Properties.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Expected constructor not found in Configuration.java", e);
    }
  }

  @Before
  public void setUp() throws IOException {

    injector = Guice.createInjector(new SecurityModule());
    certMan = injector.getInstance(CertificateManager.class);

    certMan.initRootCert();

  }

  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  @Test
  public void testSslLogging() throws Exception {
    LOG.info("Testing sign");

    certMan.configs.getConfigsMap().put(Configuration.PASSPHRASE_KEY, "123123");

    LOG.info("key dir = " + certMan.configs.getConfigsMap().get(Configuration.SRVR_KSTR_DIR_KEY));

    SignCertResponse signAgentCrt = certMan.signAgentCrt("somehost", "gdfgdfg", "123123");
    LOG.info("-------------RESPONCE-------------");
    LOG.info("-------------MESSAGE--------------");
    LOG.info(signAgentCrt.getMessage());
    LOG.info("---------------------------------");
    LOG.info("-------------RESULT--------------");
    LOG.info(signAgentCrt.getResult());
    LOG.info("---------------------------------");
    assertTrue(SignCertResponse.ERROR_STATUS.equals(signAgentCrt.getResult()));
  }

}
