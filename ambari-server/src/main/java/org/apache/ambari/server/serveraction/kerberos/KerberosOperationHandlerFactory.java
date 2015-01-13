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

import com.google.inject.Singleton;

/**
 * KerberosOperationHandlerFactory gets relevant KerberosOperationHandlers given a KDCType.
 */
@Singleton
public class KerberosOperationHandlerFactory {

  /**
   * Gets a relevant KerberosOperationHandler give some KDCType.
   * <p/>
   * If no KDCType is specified, {@link org.apache.ambari.server.serveraction.kerberos.KDCType#MIT_KDC}
   * will be assumed.
   *
   * @param kdcType the relevant KDCType
   * @return a KerberosOperationHandler
   */
  public KerberosOperationHandler getKerberosOperationHandler(KDCType kdcType) {
    KerberosOperationHandler handler = null;

    // If not specified, use KDCType.MIT_KDC as a default
    if (kdcType == null) {
      kdcType = KDCType.MIT_KDC;
    }

    switch (kdcType) {

      case MIT_KDC:
        handler = new MITKerberosOperationHandler();
        break;
      case ACTIVE_DIRECTORY:
        handler = new ADKerberosOperationHandler();
        break;
    }

    return handler;
  }
}
