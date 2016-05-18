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
 * KerberosPrincipalDoesNotExistException is a KerberosOperationException thrown in the event a
 * request to modify an existing principal was made but the princial does not exist in the KDC.
 */
public class KerberosPrincipalDoesNotExistException extends KerberosOperationException {

  /**
   * Creates a new KerberosPrincipalDoesNotExistException with a message
   *
   * @param message a String containing the message indicating the reason for this exception
   */
  public KerberosPrincipalDoesNotExistException(String message) {
    super(message);
  }

  /**
   * Creates a new KerberosPrincipalDoesNotExistException with a message and a cause
   *
   * @param message a String containing the message indicating the reason for this exception
   * @param cause   a Throwable declaring the previously thrown Throwable that led to this exception
   */
  public KerberosPrincipalDoesNotExistException(String message, Throwable cause) {
    super(message, cause);
  }
}
