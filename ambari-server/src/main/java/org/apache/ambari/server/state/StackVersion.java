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

public class StackVersion {
  private String stackVersion;

  public StackVersion(String stackVersion) {
    super();
    this.stackVersion = stackVersion;
  }

  /**
   * @return the stackVersion
   */
  public String getStackVersion() {
    return stackVersion;
  }

  /**
   * @param stackVersion the stackVersion to set
   */
  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof StackVersion)) {
      return false;
    }
    if (this == object) {
      return true;
    }
    StackVersion s = (StackVersion) object;
    return s.equals(this.stackVersion);
  }

  @Override
  public int hashCode() {
    int result = stackVersion != null ? stackVersion.hashCode() : 0;
    return result;
  }

}
