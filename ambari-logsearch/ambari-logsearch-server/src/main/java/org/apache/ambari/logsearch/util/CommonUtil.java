/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.util;

import java.security.SecureRandom;

import org.springframework.security.authentication.encoding.Md5PasswordEncoder;

public class CommonUtil {
  private CommonUtil() {
    throw new UnsupportedOperationException();
  }
  
  private static SecureRandom secureRandom = new SecureRandom();
  private static int counter = 0;

  public static String genGUI() {
    return System.currentTimeMillis() + "_" + secureRandom.nextInt(1000) + "_" + counter++;
  }
  
  private static final Md5PasswordEncoder md5Encoder = new Md5PasswordEncoder();
  public static String encryptPassword(String username, String password) {
    return md5Encoder.encodePassword(password, username);
  }
}
