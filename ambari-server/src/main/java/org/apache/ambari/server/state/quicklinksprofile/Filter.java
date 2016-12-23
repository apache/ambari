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

import org.apache.ambari.server.state.quicklinks.Link;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Base class to represent a quicklink filter. A quicklink filter has two important features:
 * <ul>
 *   <li>It can tell if it applies to a link (see {@link #accept(Link)} method).</li>
 *   <li>It can specify the visibility of the links it applies to (see {@link #isVisible()} method).</li>
 * </ul>
 */
public abstract class Filter {

  @JsonProperty("visible")
  private boolean visible;

  /**
   * @return a boolean indicating whether links accepted by this filter should be shown or hidden
   */
  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  /**
   * @param link the link to examine
   * @return if this filter applies to the link in the parameter
   */
  public abstract boolean accept(Link link);

  // Factory methods

  static AcceptAllFilter acceptAllFilter(boolean visible) {
    AcceptAllFilter acceptAllFilter = new AcceptAllFilter();
    acceptAllFilter.setVisible(visible);
    return acceptAllFilter;
  }

  static LinkNameFilter linkNameFilter(String linkName, boolean visible) {
    LinkNameFilter linkNameFilter = new LinkNameFilter();
    linkNameFilter.setLinkName(linkName);
    linkNameFilter.setVisible(visible);
    return linkNameFilter;
  }

  static LinkAttributeFilter linkAttributeFilter(String linkAttribute, boolean visible) {
    LinkAttributeFilter linkAttributeFilter = new LinkAttributeFilter();
    linkAttributeFilter.setLinkAttribute(linkAttribute);
    linkAttributeFilter.setVisible(visible);
    return linkAttributeFilter;
  }
}
