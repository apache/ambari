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

package org.apache.ambari.server.serveraction.kerberos;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMockBuilder;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class MITKerberosOperationHandlerTest extends KerberosOperationHandlerTest {

  private static final String DEFAULT_ADMIN_PRINCIPAL = "admin/admin";
  private static final String DEFAULT_ADMIN_PASSWORD = "hadoop";
  private static final String DEFAULT_REALM = "EXAMPLE.COM";

  private static Injector injector;

  private static Method methodExecuteCommand;

  private static final Map<String, String> KERBEROS_ENV_MAP = new HashMap<String, String>() {
    {
      put(MITKerberosOperationHandler.KERBEROS_ENV_ENCRYPTION_TYPES, null);
      put(MITKerberosOperationHandler.KERBEROS_ENV_KDC_HOSTS, "localhost");
      put(MITKerberosOperationHandler.KERBEROS_ENV_ADMIN_SERVER_HOST, "localhost");
      put(MITKerberosOperationHandler.KERBEROS_ENV_AD_CREATE_ATTRIBUTES_TEMPLATE, "AD Create Template");
      put(MITKerberosOperationHandler.KERBEROS_ENV_KDC_CREATE_ATTRIBUTES, "-attr1 -attr2 foo=345");
    }
  };

  @BeforeClass
  public static void beforeClass() throws Exception {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Configuration configuration = EasyMock.createNiceMock(Configuration.class);
        expect(configuration.getServerOsFamily()).andReturn("redhat6").anyTimes();
        expect(configuration.getKerberosOperationRetryTimeout()).andReturn(1).anyTimes();
        replay(configuration);

        bind(Clusters.class).toInstance(EasyMock.createNiceMock(Clusters.class));
        bind(Configuration.class).toInstance(configuration);
        bind(OsFamily.class).toInstance(EasyMock.createNiceMock(OsFamily.class));
      }
    });

    methodExecuteCommand = KerberosOperationHandler.class.getDeclaredMethod(
        "executeCommand",
        String[].class,
        Map.class,
        ShellCommandUtil.InteractiveHandler.class);
  }

  @Test
  public void testSetPrincipalPasswordExceptions() throws Exception {
    MITKerberosOperationHandler handler = injector.getInstance(MITKerberosOperationHandler.class);
    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);

    try {
      handler.setPrincipalPassword(DEFAULT_ADMIN_PRINCIPAL, null);
      Assert.fail("KerberosOperationException not thrown for null password");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.setPrincipalPassword(DEFAULT_ADMIN_PRINCIPAL, "");
      Assert.fail("KerberosOperationException not thrown for empty password");
      handler.createPrincipal("", "1234", false);
      Assert.fail("AmbariException not thrown for empty principal");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.setPrincipalPassword(null, DEFAULT_ADMIN_PASSWORD);
      Assert.fail("KerberosOperationException not thrown for null principal");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.setPrincipalPassword("", DEFAULT_ADMIN_PASSWORD);
      Assert.fail("KerberosOperationException not thrown for empty principal");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }
  }

  @Test(expected = KerberosPrincipalDoesNotExistException.class)
  public void testSetPrincipalPasswordPrincipalDoesNotExist() throws Exception {

    MITKerberosOperationHandler handler = createMockBuilder(MITKerberosOperationHandler.class)
        .addMockedMethod(methodExecuteCommand)
        .createNiceMock();
    injector.injectMembers(handler);
    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(0).anyTimes();
            expect(result.isSuccessful()).andReturn(true).anyTimes();
            expect(result.getStderr())
                .andReturn("change_password: Principal does not exist while changing password for \"nonexistant@EXAMPLE.COM\".")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.setPrincipalPassword("nonexistant@EXAMPLE.COM", "password");
    handler.close();
  }

  @Test
  public void testCreateServicePrincipal_AdditionalAttributes() throws Exception {
    Method invokeKAdmin = MITKerberosOperationHandler.class.getDeclaredMethod("invokeKAdmin", String.class, String.class);

    Capture<? extends String> query = newCapture();
    Capture<? extends String> password = newCapture();

    ShellCommandUtil.Result result1 = createNiceMock(ShellCommandUtil.Result.class);
    expect(result1.getStderr()).andReturn("").anyTimes();
    expect(result1.getStdout()).andReturn("Principal \"" + DEFAULT_ADMIN_PRINCIPAL + "\" created\"").anyTimes();

    ShellCommandUtil.Result result2 = createNiceMock(ShellCommandUtil.Result.class);
    expect(result2.getStderr()).andReturn("").anyTimes();
    expect(result2.getStdout()).andReturn("Key: vno 1").anyTimes();

    MITKerberosOperationHandler handler = createMockBuilder(MITKerberosOperationHandler.class)
        .addMockedMethod(invokeKAdmin)
        .createStrictMock();

    expect(handler.invokeKAdmin(capture(query), anyString())).andReturn(result1).once();
    expect(handler.invokeKAdmin("get_principal " + DEFAULT_ADMIN_PRINCIPAL, null)).andReturn(result2).once();

    replay(handler, result1, result2);

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.createPrincipal(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD, false);

    verify(handler, result1, result2);

    Assert.assertTrue(query.getValue().contains(" " + KERBEROS_ENV_MAP.get(MITKerberosOperationHandler.KERBEROS_ENV_KDC_CREATE_ATTRIBUTES) + " "));
  }

  @Test(expected = KerberosPrincipalAlreadyExistsException.class)
  public void testCreatePrincipalPrincipalAlreadyNotExists() throws Exception {
    MITKerberosOperationHandler handler = createMock();

    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(0).anyTimes();
            expect(result.isSuccessful()).andReturn(true).anyTimes();
            expect(result.getStderr())
                .andReturn("add_principal: Principal or policy already exists while creating \"existing@EXAMPLE.COM\".")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.createPrincipal("existing@EXAMPLE.COM", "password", false);
    handler.close();
  }

  @Test
  public void testCreateServicePrincipal_Exceptions() throws Exception {
    MITKerberosOperationHandler handler = new MITKerberosOperationHandler();
    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);

    try {
      handler.createPrincipal(DEFAULT_ADMIN_PRINCIPAL, null, false);
      Assert.fail("KerberosOperationException not thrown for null password");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.createPrincipal(DEFAULT_ADMIN_PRINCIPAL, "", false);
      Assert.fail("KerberosOperationException not thrown for empty password");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.createPrincipal(null, DEFAULT_ADMIN_PASSWORD, false);
      Assert.fail("KerberosOperationException not thrown for null principal");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.createPrincipal("", DEFAULT_ADMIN_PASSWORD, false);
      Assert.fail("KerberosOperationException not thrown for empty principal");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testTestAdministratorCredentialsIncorrectAdminPassword() throws Exception {
    MITKerberosOperationHandler handler = createMock();

    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(1).anyTimes();
            expect(result.isSuccessful()).andReturn(false).anyTimes();
            expect(result.getStderr())
                .andReturn("kadmin: Incorrect password while initializing kadmin interface")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testTestAdministratorCredentialsIncorrectAdminPrincipal() throws Exception {
    MITKerberosOperationHandler handler = createMock();

    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(1).anyTimes();
            expect(result.isSuccessful()).andReturn(false).anyTimes();
            expect(result.getStderr())
                .andReturn("kadmin: Client not found in Kerberos database while initializing kadmin interface")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosRealmException.class)
  public void testTestAdministratorCredentialsInvalidRealm() throws Exception {
    MITKerberosOperationHandler handler = createMock();

    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(1).anyTimes();
            expect(result.isSuccessful()).andReturn(false).anyTimes();
            expect(result.getStderr())
                .andReturn("kadmin: Missing parameters in krb5.conf required for kadmin client while initializing kadmin interface")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosRealmException.class)
  public void testTestAdministratorCredentialsInvalidRealm2() throws Exception {
    MITKerberosOperationHandler handler = createMock();

    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(1).anyTimes();
            expect(result.isSuccessful()).andReturn(false).anyTimes();
            expect(result.getStderr())
                .andReturn("kadmin: Cannot find KDC for requested realm while initializing kadmin interface")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosKDCConnectionException.class)
  public void testTestAdministratorCredentialsKDCConnectionException() throws Exception {
    MITKerberosOperationHandler handler = createMock();

    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(1).anyTimes();
            expect(result.isSuccessful()).andReturn(false).anyTimes();
            expect(result.getStderr())
                .andReturn("kadmin: Cannot contact any KDC for requested realm while initializing kadmin interface")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test(expected = KerberosKDCConnectionException.class)
  public void testTestAdministratorCredentialsKDCConnectionException2() throws Exception {
    MITKerberosOperationHandler handler = createMock();

    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(1).anyTimes();
            expect(result.isSuccessful()).andReturn(false).anyTimes();
            expect(result.getStderr())
                .andReturn("kadmin: Cannot resolve network address for admin server in requested realm while initializing kadmin interface")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test
  public void testTestAdministratorCredentialsNotFound() throws Exception {
    MITKerberosOperationHandler handler = createMock();

    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(0).anyTimes();
            expect(result.isSuccessful()).andReturn(true).anyTimes();
            expect(result.getStderr())
                .andReturn("get_principal: Principal does not exist while retrieving \"admin/admi@EXAMPLE.COM\".")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    Assert.assertFalse(handler.testAdministratorCredentials());
    handler.close();
  }

  @Test
  public void testTestAdministratorCredentialsSuccess() throws Exception {
    MITKerberosOperationHandler handler = createMock();

    expect(handler.executeCommand(anyObject(String[].class), EasyMock.<Map<String, String>>anyObject(), anyObject(MITKerberosOperationHandler.InteractivePasswordHandler.class)))
        .andAnswer(new IAnswer<ShellCommandUtil.Result>() {
          @Override
          public ShellCommandUtil.Result answer() throws Throwable {
            ShellCommandUtil.Result result = createMock(ShellCommandUtil.Result.class);

            expect(result.getExitCode()).andReturn(0).anyTimes();
            expect(result.isSuccessful()).andReturn(true).anyTimes();
            expect(result.getStderr())
                .andReturn("")
                .anyTimes();
            expect(result.getStdout())
                .andReturn("Authenticating as principal admin/admin with password.\n" +
                    "Principal: admin/admin@EXAMPLE.COM\n" +
                    "Expiration date: [never]\n" +
                    "Last password change: Thu Jan 08 13:09:52 UTC 2015\n" +
                    "Password expiration date: [none]\n" +
                    "Maximum ticket life: 1 day 00:00:00\n" +
                    "Maximum renewable life: 0 days 00:00:00\n" +
                    "Last modified: Thu Jan 08 13:09:52 UTC 2015 (root/admin@EXAMPLE.COM)\n" +
                    "Last successful authentication: [never]\n" +
                    "Last failed authentication: [never]\n" +
                    "Failed password attempts: 0\n" +
                    "Number of keys: 6\n" +
                    "Key: vno 1, aes256-cts-hmac-sha1-96, no salt\n" +
                    "Key: vno 1, aes128-cts-hmac-sha1-96, no salt\n" +
                    "Key: vno 1, des3-cbc-sha1, no salt\n" +
                    "Key: vno 1, arcfour-hmac, no salt\n" +
                    "Key: vno 1, des-hmac-sha1, no salt\n" +
                    "Key: vno 1, des-cbc-md5, no salt\n" +
                    "MKey: vno 1\n" +
                    "Attributes:\n" +
                    "Policy: [none]")
                .anyTimes();

            replay(result);
            return result;
          }
        });

    replayAll();

    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test
  @Ignore
  public void testTestAdministratorCredentialsLive() throws KerberosOperationException {
    MITKerberosOperationHandler handler = new MITKerberosOperationHandler();
    String principal = System.getProperty("principal");
    String password = System.getProperty("password");
    String realm = System.getProperty("realm");

    if (principal == null) {
      principal = DEFAULT_ADMIN_PRINCIPAL;
    }

    if (password == null) {
      password = DEFAULT_ADMIN_PASSWORD;
    }

    if (realm == null) {
      realm = DEFAULT_REALM;
    }

    PrincipalKeyCredential credentials = new PrincipalKeyCredential(principal, password);

    handler.open(credentials, realm, KERBEROS_ENV_MAP);
    handler.testAdministratorCredentials();
    handler.close();
  }

  @Test
  public void testInteractivePasswordHandler() {
    MITKerberosOperationHandler.InteractivePasswordHandler handler = new MITKerberosOperationHandler.InteractivePasswordHandler("admin_password", "user_password");

    handler.start();
    Assert.assertEquals("admin_password", handler.getResponse("password"));
    Assert.assertFalse(handler.done());
    Assert.assertEquals("user_password", handler.getResponse("password"));
    Assert.assertFalse(handler.done());
    Assert.assertEquals("user_password", handler.getResponse("password"));
    Assert.assertTrue(handler.done());

    // Test restarting
    handler.start();
    Assert.assertEquals("admin_password", handler.getResponse("password"));
    Assert.assertFalse(handler.done());
    Assert.assertEquals("user_password", handler.getResponse("password"));
    Assert.assertFalse(handler.done());
    Assert.assertEquals("user_password", handler.getResponse("password"));
    Assert.assertTrue(handler.done());
  }

  private MITKerberosOperationHandler createMock(){
    return createMock(false);
  }

  private MITKerberosOperationHandler createMock(boolean strict) {
    IMockBuilder<MITKerberosOperationHandler> mockBuilder = createMockBuilder(MITKerberosOperationHandler.class)
        .addMockedMethod(methodExecuteCommand);
    MITKerberosOperationHandler result;
    if(strict){
      result = mockBuilder.createStrictMock();
    } else {
      result = mockBuilder.createNiceMock();
    }
    injector.injectMembers(result);
    return result;
  }
}