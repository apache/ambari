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
package org.apache.ambari.server.orm.entities;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.state.StackId;

@Deprecated
@Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
public class RepositoryVersionEntity {

  public RepositoryVersionEntity() {

  }

  public Long getId() {
    return null;
  }

  /**
   * Gets the repository version's stack.
   *
   * @return the stack.
   */
  public StackEntity getStack() {
    return null;
  }

  public String getVersion() {
    return null;
  }

  public String getDisplayName() {
    return null;
  }

  public String getStackName() {
    return null;
  }

  public String getStackVersion() {
    return null;
  }

  public StackId getStackId() {
    return null;
  }

  /**
   * @return the XML that is the basis for the version
   */
  public String getVersionXml() {
    return null;
  }

  /**
   * @return The url used for the version.  Optional in case the XML was loaded via blob.
   */
  public String getVersionUrl() {
    return null;
  }

  /**
   * @return the XSD name extracted from the XML.
   */
  public String getVersionXsd() {
    return null;
  }
}
