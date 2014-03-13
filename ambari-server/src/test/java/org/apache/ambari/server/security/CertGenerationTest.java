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

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

import com.google.common.io.Files;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.TestCase;

public class CertGenerationTest extends TestCase {
	
  private static Log LOG = LogFactory.getLog(CertGenerationTest.class);
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
      requestStaticInjection(CertGenerationTest.class);
    }
  }
  
  protected Properties buildTestProperties() {
    try {
      temp.create();
    } catch (IOException e) {
      e.printStackTrace();
    }
	  Properties properties = new Properties();
	  properties.setProperty(Configuration.SRVR_KSTR_DIR_KEY,
      temp.getRoot().getAbsolutePath());
    System.out.println(properties.get(Configuration.SRVR_CRT_PASS_KEY));
	
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

    //Test using actual ca.config.
    try {
      File caConfig = new File("conf/unix/ca.config");
      File caConfigTest  =
        new File(temp.getRoot().getAbsolutePath(), "ca.config");
      File newCertsDir = new File(temp.getRoot().getAbsolutePath(), "newcerts");
      newCertsDir.mkdirs();
      File indexTxt = new File(temp.getRoot().getAbsolutePath(), "index.txt");
      indexTxt.createNewFile();

      String content = IOUtils.toString(new FileInputStream(caConfig));
      content = content.replaceAll("/var/lib/ambari-server/keys/db", temp.getRoot().getAbsolutePath());
      IOUtils.write(content, new FileOutputStream(caConfigTest));
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }

    certMan.initRootCert();
  }
	
  @After
  public void tearDown() throws IOException {
    temp.delete();
  }
	
  @Test
  public void testServerCertGen() throws Exception {

    File serverCrt = new File(temp.getRoot().getAbsoluteFile() +
    						  File.separator + Configuration.SRVR_CRT_NAME_DEFAULT);
    assertTrue(serverCrt.exists());
  }

  @Test
  public void testServerKeyGen() throws Exception {

    File serverKey = new File(temp.getRoot().getAbsoluteFile() +
    						  File.separator + Configuration.SRVR_KEY_NAME_DEFAULT);
    assertTrue(serverKey.exists());
  }

  @Test
  public void testServerKeystoreGen() throws Exception {

    File serverKeyStrore = new File(temp.getRoot().getAbsoluteFile() +
    						  File.separator + Configuration.KSTR_NAME_DEFAULT);
    assertTrue(serverKeyStrore.exists());
  }

  @Test
  public void testRevokeExistingAgentCert() throws Exception {

    Map<String,String> config = certMan.configs.getConfigsMap();
    config.put(Configuration.PASSPHRASE_KEY,"passphrase");

    String agentHostname = "agent_hostname1";
    SignCertResponse scr = certMan.signAgentCrt(agentHostname,
      "incorrect_agentCrtReqContent", "passphrase");
    //Revoke command wasn't executed
    assertFalse(scr.getMessage().contains("-revoke"));

    //Emulate existing agent certificate
    File fakeAgentCertFile = new File(temp.getRoot().getAbsoluteFile() +
      File.separator + agentHostname + ".crt");
    assertTrue(fakeAgentCertFile.exists());

    //Revoke command was executed
    scr = certMan.signAgentCrt(agentHostname,
      "incorrect_agentCrtReqContent", "passphrase");
    assertTrue(scr.getMessage().contains("-revoke"));
  }

}
