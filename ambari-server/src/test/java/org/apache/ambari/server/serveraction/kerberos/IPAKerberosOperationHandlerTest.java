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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class IPAKerberosOperationHandlerTest extends KerberosOperationHandlerTest {
  private static final String DEFAULT_ADMIN_PRINCIPAL = "admin";
  private static final String DEFAULT_ADMIN_PASSWORD = "Hadoop12345";

  private static final String DEFAULT_REALM = "IPA01.LOCAL";

  private static Injector injector;

  private static boolean hasIpa = false;

  private static final Map<String, String> KERBEROS_ENV_MAP = new HashMap<String, String>() {
    {
      put(IPAKerberosOperationHandler.KERBEROS_ENV_ENCRYPTION_TYPES, null);
      put(IPAKerberosOperationHandler.KERBEROS_ENV_KDC_HOSTS, "localhost");
      put(IPAKerberosOperationHandler.KERBEROS_ENV_ADMIN_SERVER_HOST, "localhost");
      put(IPAKerberosOperationHandler.KERBEROS_ENV_USER_PRINCIPAL_GROUP, "");
    }
  };

  @BeforeClass
  public static void beforeClass() throws AmbariException {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Configuration configuration = EasyMock.createNiceMock(Configuration.class);
        expect(configuration.getServerOsFamily()).andReturn("redhat6").anyTimes();
        replay(configuration);

        bind(Clusters.class).toInstance(EasyMock.createNiceMock(Clusters.class));
        bind(Configuration.class).toInstance(configuration);
        bind(OsFamily.class).toInstance(EasyMock.createNiceMock(OsFamily.class));
      }
    });
    if (System.getenv("HAS_IPA") != null) {
      hasIpa = true;
    }
  }

  @Test
  public void testSetPrincipalPasswordExceptions() throws Exception {
    if (!hasIpa) {
      return;
    }

    IPAKerberosOperationHandler handler = injector.getInstance(IPAKerberosOperationHandler.class);
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

  @Test
  public void testCreateServicePrincipal_Exceptions() throws Exception {
    if (!hasIpa) {
      return;
    }

    IPAKerberosOperationHandler handler = new IPAKerberosOperationHandler();
    handler.open(new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD), DEFAULT_REALM, KERBEROS_ENV_MAP);

    try {
      handler.createPrincipal(DEFAULT_ADMIN_PRINCIPAL, null, false);
      Assert.fail("KerberosOperationException not thrown for null password");
    } catch (Throwable t) {
      Assert.fail("KerberosOperationException thrown on null password with IPA");
    }

    try {
      handler.createPrincipal(DEFAULT_ADMIN_PRINCIPAL, "", false);
    } catch (Throwable t) {
      Assert.fail("KerberosOperationException thrown for empty password");
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

}
