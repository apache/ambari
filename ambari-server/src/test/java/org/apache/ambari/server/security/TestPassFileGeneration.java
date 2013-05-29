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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.Random;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.TestCase;

public class TestPassFileGeneration extends TestCase {

  private static final int PASS_FILE_NAME_LEN = 20;
  private static final float MAX_PASS_LEN = 100;

  public TemporaryFolder temp = new TemporaryFolder();

  Injector injector;

  private static CertificateManager certMan;
  private String passFileName;
  private int passLen;

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
    properties.setProperty(Configuration.SRVR_KSTR_DIR_KEY, temp.getRoot()
        .getAbsolutePath());

    passLen = (int) Math.abs((new Random().nextFloat() * MAX_PASS_LEN));

    properties.setProperty(Configuration.SRVR_CRT_PASS_LEN_KEY,
        String.valueOf(passLen));

    passFileName = RandomStringUtils.randomAlphabetic(PASS_FILE_NAME_LEN);
    properties.setProperty(Configuration.SRVR_CRT_PASS_FILE_KEY, passFileName);

    return properties;
  }

  protected Constructor<Configuration> getConfigurationConstructor() {
    try {
      return Configuration.class.getConstructor(Properties.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(
          "Expected constructor not found in Configuration.java", e);
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
  public void testPassFileGen() throws Exception {

    File passFile = new File(temp.getRoot().getAbsolutePath() + File.separator
        + passFileName);

    assertTrue(passFile.exists());

    String pass = FileUtils.readFileToString(passFile);

    assertEquals(pass.length(), passLen);

    if (ShellCommandUtil.LINUX) {
      String permissions = ShellCommandUtil.
              getUnixFilePermissions(passFile.getAbsolutePath());
      assertEquals(ShellCommandUtil.MASK_OWNER_ONLY_RW, permissions);
    } else {
      //Do nothing
    }
    // Cleanup
    passFile.delete();
  }

}
