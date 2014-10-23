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

package org.apache.ambari.server.utils;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import junit.framework.TestCase;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.CertGenerationTest;
import org.apache.ambari.server.security.CertificateManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

public class TestShellCommandUtil extends TestCase {

  public TemporaryFolder temp = new TemporaryFolder();

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
    try {
      temp.create();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  @Test
  public void testOSDetection() throws Exception {
    // At least check, that only one OS is selected
    assertTrue(ShellCommandUtil.LINUX ^ ShellCommandUtil.WINDOWS
            ^ ShellCommandUtil.MAC);
    assertTrue(ShellCommandUtil.LINUX || ShellCommandUtil.MAC ==
            ShellCommandUtil.UNIX_LIKE);
  }

  @Test
  public void testUnixFilePermissions() throws Exception {
    File dummyFile = new File(temp.getRoot() + File.separator + "dummy");
    new FileOutputStream(dummyFile).close();
    if (ShellCommandUtil.LINUX) {
      ShellCommandUtil.setUnixFilePermissions("600",
              dummyFile.getAbsolutePath());
      String p = ShellCommandUtil.getUnixFilePermissions(
              dummyFile.getAbsolutePath());
      assertEquals("600", p);

      ShellCommandUtil.setUnixFilePermissions("444",
              dummyFile.getAbsolutePath());
      p = ShellCommandUtil.getUnixFilePermissions(
              dummyFile.getAbsolutePath());
      assertEquals("444", p);

      ShellCommandUtil.setUnixFilePermissions("777",
              dummyFile.getAbsolutePath());
      p = ShellCommandUtil.getUnixFilePermissions(
              dummyFile.getAbsolutePath());
      assertEquals("777", p);

    } else {
      // Next command is silently ignored, it's OK
      ShellCommandUtil.setUnixFilePermissions(ShellCommandUtil.MASK_OWNER_ONLY_RW,
              dummyFile.getAbsolutePath());
      // On Windows/Mac, output is always MASK_EVERYBODY_RWX
      String p = ShellCommandUtil.getUnixFilePermissions(
              dummyFile.getAbsolutePath());
      assertEquals(p, ShellCommandUtil.MASK_EVERYBODY_RWX);
    }
  }


  @Test
  public void testRunCommand() throws Exception {
    ShellCommandUtil.Result result = null;
    if (ShellCommandUtil.LINUX) {
      result = ShellCommandUtil.
              runCommand(new String [] {"echo", "dummy"});
      assertEquals(0, result.getExitCode());
      assertEquals("dummy\n", result.getStdout());
      assertEquals("", result.getStderr());
      assertTrue(result.isSuccessful());

      result = ShellCommandUtil.
              runCommand(new String [] {"false"});
      assertEquals(1, result.getExitCode());
      assertFalse(result.isSuccessful());
    } else {
      // Skipping this test under Windows/Mac
    }
  }
  
  @Test
  public void testHideOpenSslPassword(){
    String command_pass = "openssl ca -config ca.config -in agent_hostname1.csr -out "+
            "agent_hostname1.crt -batch -passin pass:1234 -keyfile ca.key -cert ca.crt";
    String command_key = "openssl ca -create_serial -out /var/lib/ambari-server/keys/ca.crt -days 365 -keyfile /var/lib/ambari-server/keys/ca.key " +
        "-key 1234 -selfsign -extensions jdk7_ca " +
        "-config /var/lib/ambari-server/keys/ca.config -batch " +
        "-infiles /var/lib/ambari-server/keys/ca.csr";
    assertFalse(ShellCommandUtil.hideOpenSslPassword(command_pass).contains("1234"));
    assertFalse(ShellCommandUtil.hideOpenSslPassword(command_key).contains("1234"));
  }

  public void testResultsClassIsPublic() throws Exception {
    Class resultClass = ShellCommandUtil.Result.class;

    assertEquals(Modifier.PUBLIC, resultClass.getModifiers() & Modifier.PUBLIC);

    for(Method method : resultClass.getMethods()) {
      assertEquals(method.getName(), Modifier.PUBLIC, (method.getModifiers() & Modifier.PUBLIC));
    }
  }
}
