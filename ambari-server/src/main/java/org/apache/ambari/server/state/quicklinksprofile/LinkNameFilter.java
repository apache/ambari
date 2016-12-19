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

package org.apache.ambari.server.state.quicklinksprofile;

import java.util.Objects;

import org.apache.ambari.server.state.quicklinks.Link;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A filter that accepts quicklinks based on name match.
 */
public class LinkNameFilter extends Filter {

  static final String LINK_NAME = "link_name";

  @JsonProperty(LINK_NAME)
  private String linkName;

  public String getLinkName() {
    return linkName;
  }

  public void setLinkName(String linkName) {
    this.linkName = linkName;
  }

  @Override
  public boolean accept(Link link) {
    return Objects.equals(link.getName(), linkName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LinkNameFilter that = (LinkNameFilter) o;
    return isVisible() == that.isVisible() && Objects.equals(linkName, that.linkName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isVisible(), linkName);
  }
}
