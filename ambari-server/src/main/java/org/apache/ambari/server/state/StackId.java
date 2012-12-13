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

package org.apache.ambari.server.state;

public class StackId {
  private String stackName;
  private String stackVersion;

  public StackId() {
    super();
    this.stackName = "";
    this.stackVersion = "";
  }

  public StackId(String stackId) {
    super();
    parseStackIdHelper(this, stackId);
  }

  public StackId(StackInfo stackInfo) {
    this.stackName = stackInfo.getName();
    this.stackVersion = stackInfo.getVersion();
  }

  /**
   * @return the stackName
   */
  public String getStackName() {
    return stackName;
  }

  /**
   * @return the stackVersion
   */
  public String getStackVersion() {
    return stackVersion;
  }

  /**
   * @return the stackVersion
   */
  public String getStackId() {
    if (stackName.isEmpty()
        && stackVersion.isEmpty()) {
      return "";
    }
    return stackName + "-" + stackVersion;
  }

  /**
   * @param stackId the stackVersion to set
   */
  public void setStackId(String stackId) {
    parseStackIdHelper(this, stackId);
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof StackId)) {
      return false;
    }
    if (this == object) {
      return true;
    }
    StackId s = (StackId) object;
    return stackVersion.equals(s.stackVersion);
  }

  @Override
  public int hashCode() {
    int result = stackVersion != null ? stackVersion.hashCode() : 0;
    return result;
  }

  public String toString() {
    return getStackId();
  }

  public static void parseStackIdHelper(StackId stackVersion,
      String stackId) {
    if (stackId.isEmpty()) {
      stackVersion.stackName = "";
      stackVersion.stackVersion = "";
      return;
    }
    int pos = stackId.indexOf('-');
    if (pos == -1
        || (stackId.length() <= (pos+1))) {
      throw new RuntimeException("Could not parse invalid Stack Id"
          + ", stackId=" + stackId);
    }
    stackVersion.stackName = stackId.substring(0, pos);
    stackVersion.stackVersion = stackId.substring(pos+1);
  }

}
