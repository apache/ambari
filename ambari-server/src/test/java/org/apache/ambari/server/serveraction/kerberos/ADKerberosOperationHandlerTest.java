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

import junit.framework.Assert;
import org.apache.ambari.server.AmbariException;
import org.junit.Before;
import org.junit.Test;

public class ADKerberosOperationHandlerTest  {

  @Test
  public void testOpenExceptionLdapUrlNotProvided() throws Exception {
    try {
      KerberosOperationHandler handler = new ADKerberosOperationHandler();
      KerberosCredential kc = new KerberosCredential(
                "Administrator@knox.com", "adminpass", null);  // null keytab

      handler.open(kc, "KNOX.COM");
      Assert.fail("AmbariException not thrown for null ldapUrl");
    } catch (Throwable t) {
      Assert.assertEquals(AmbariException.class, t.getClass());
    }
  }

    @Test
    public void testOpenExceptionPrincipalContainerDnNotProvided() throws Exception {
        try {
            KerberosOperationHandler handler = new ADKerberosOperationHandler();
            KerberosCredential kc = new KerberosCredential(
                    "Administrator@knox.com", "adminpass", null);  // null keytab

            handler.open(kc, "KNOX.COM", "ldaps://dillwin12.knox.com:636", null);
            Assert.fail("AmbariException not thrown for null principalContainerDn");
        } catch (Throwable t) {
            Assert.assertEquals(AmbariException.class, t.getClass());
        }
    }

}
