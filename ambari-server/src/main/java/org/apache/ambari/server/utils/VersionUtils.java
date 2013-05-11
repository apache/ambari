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

/**
 * Provides various utility functions to be used for version handling.
 * The compatibility matrix between server, agent, store can also be maintained
 * in this class. Currently, exact match is required between all the three.
 */
public class VersionUtils {
  /**
   * Compares two versions strings of the form N.N.N.N
   *
   * @param version1
   * @param version2
   * @param maxLengthToCompare The maximum length to compare - 2 means only Major and Minor
   *                           0 to compare the whole version strings
   * @return 0 if both are equal up to the length compared, -1 if first one is lower, +1 otherwise
   */
  public static int compareVersions(String version1, String version2, int maxLengthToCompare) {
    if (version1 == null || version1.isEmpty()) {
      throw new IllegalArgumentException("version1 cannot be null or empty");
    }
    if (version2 == null || version2.isEmpty()) {
      throw new IllegalArgumentException("version2 cannot be null or empty");
    }
    if (maxLengthToCompare < 0) {
      throw new IllegalArgumentException("maxLengthToCompare cannot be less than 0");
    }

    String[] version1Parts = version1.split("\\.");
    String[] version2Parts = version2.split("\\.");

    int length = Math.max(version1Parts.length, version2Parts.length);
    length = maxLengthToCompare == 0 || maxLengthToCompare > length ? length : maxLengthToCompare;
    for (int i = 0; i < length; i++) {
      int stack1Part = i < version1Parts.length ?
          Integer.parseInt(version1Parts[i]) : 0;
      int stack2Part = i < version2Parts.length ?
          Integer.parseInt(version2Parts[i]) : 0;
      if (stack1Part < stack2Part) {
        return -1;
      }
      if (stack1Part > stack2Part) {
        return 1;
      }
    }

    return 0;
  }

  /**
   * Compares two versions strings of the form N.N.N.N
   *
   * @param version1
   * @param version2
   * @return 0 if both are equal, -1 if first one is lower, +1 otherwise
   */
  public static int compareVersions(String version1, String version2) {
    return compareVersions(version1, version2, 0);
  }

  /**
   * Compares two version for equality
   *
   * @param version1
   * @param version2
   * @return true if versions are equal; false otherwise
   */
  public static boolean areVersionsEqual(String version1, String version2) {
    return 0 == compareVersions(version1, version2, 0);
  }

  /**
   * Checks if the two versions are compatible.
   * TODO A relaxed check can be implemented where a server version is compatible to
   * TODO more than one versions of agent and store.
   *
   * @param serverVersion The version of the server
   * @param versionToCheck The version of the agent or the store
   * @return true if the versions are compatible
   */
  public static boolean areVersionsCompatible(String serverVersion, String versionToCheck) {
    return areVersionsEqual(serverVersion, versionToCheck);
  }
}
