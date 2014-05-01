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
package org.apache.ambari.server.state.stack;

public enum OsFamily {
  REDHAT5("centos5", "redhat5", "oraclelinux5", "rhel5"),
  REDHAT6("centos6", "redhat6", "oraclelinux6", "rhel6"),
  SUSE11("suse11", "sles11"),
  DEBIAN12("debian12", "ubuntu12");
  
  private String[] osTypes;
  private OsFamily(String... oses) {
    osTypes = oses;
  }
  
  static String[] findTypes(String os) {
    for (OsFamily f : values()) {
      for (String t : f.osTypes) {
        if (t.equals(os)) {
          return f.osTypes;
        }
      }
    }
    return new String[0];
  }

  /**
   * Finds the family for the specific os
   * @param os the os
   * @return the family, or <code>null</code> if not defined
   */
  public static OsFamily find(String os) {
    for (OsFamily f : values()) {
      for (String t : f.osTypes) {
        if (t.equals(os)) {
          return f;
        }
      }
    }
    return null;
  }
}