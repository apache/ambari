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

import java.io.Serializable;
import java.security.SecureRandom;

public class CommonUtil implements Serializable {
  /**
   *
   */
  private static final long serialVersionUID = -7284237762948427019L;

  static SecureRandom secureRandom = new SecureRandom();
  static int counter = 0;

  static public String genGUI() {
    return System.currentTimeMillis() + "_" + secureRandom.nextInt(1000)
      + "_" + counter++;
  }

  static public String genGUI(int length) {
    String str = "";
    for (int i = 0; i < length; i++) {
      int ascii = genInteger(65, 90);
      str += (char) ascii;
    }
    return str;
  }

  static public int genInteger() {
    return secureRandom.nextInt();
  }

  static public int genInteger(int min, int max) {
    int value = secureRandom.nextInt(max - min);
    return value + min;
  }

  /**
   * @return
   */
  public static long genLong() {
    return secureRandom.nextLong();
  }

  static public int genInteger(int n) {
    return secureRandom.nextInt();
  }
}
