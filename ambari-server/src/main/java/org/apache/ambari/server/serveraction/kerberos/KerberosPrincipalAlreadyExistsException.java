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

/**
 * KerberosPrincipalAlreadyExistsException is a KerberosOperationException thrown in the event a
 * request to create a new princip7als was made but the princial already exists in the KDC.
 */
public class KerberosPrincipalAlreadyExistsException extends KerberosOperationException {

  /**
   * Creates a new KerberosPrincipalAlreadyExistsException with a message
   *
   * @param message a String containing the message indicating the reason for this exception
   */
  public KerberosPrincipalAlreadyExistsException(String message) {
    super(message);
  }

  /**
   * Creates a new KerberosPrincipalAlreadyExistsException with a message and a cause
   *
   * @param message a String containing the message indicating the reason for this exception
   * @param cause   a Throwable declaring the previously thrown Throwable that led to this exception
   */
  public KerberosPrincipalAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
