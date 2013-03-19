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
package org.apache.ambari.server.controller.ivory;

/**
 * Ivory feed.
 */
public class Feed {

  private final String name;
  private final String description;
  private final String status;
  private final String schedule;
  private final String sourceClusterName;
  private final String targetClusterName;

  /**
   * Construct a feed.
   *
   * @param name               the feed name
   * @param description        the description
   * @param status             the status
   * @param schedule           the schedule
   * @param sourceClusterName  the source cluster name
   * @param targetClusterName  the target cluster name
   */
  public Feed(String name, String description, String status, String schedule, String sourceClusterName, String targetClusterName) {
    this.name = name;
    this.description = description;
    this.status = status;
    this.schedule = schedule;
    this.sourceClusterName = sourceClusterName;
    this.targetClusterName = targetClusterName;
  }

  /**
   * Get the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the status.
   *
   * @return the status
   */
  public String getStatus() {
    return status;
  }

  /**
   * Get the schedule.
   *
   * @return the schedule
   */
  public String getSchedule() {
    return schedule;
  }

  /**
   * Get the source cluster name.
   *
   * @return the source cluster name
   */
  public String getSourceClusterName() {
    return sourceClusterName;
  }

  /**
   * Get the target cluster name.
   *
   * @return the target cluster name
   */
  public String getTargetClusterName() {
    return targetClusterName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Feed feed = (Feed) o;

    return !(description != null ? !description.equals(feed.description) : feed.description != null) &&
        !(name != null              ? !name.equals(feed.name)                           : feed.name != null) &&
        !(schedule != null          ? !schedule.equals(feed.schedule)                   : feed.schedule != null) &&
        !(sourceClusterName != null ? !sourceClusterName.equals(feed.sourceClusterName) : feed.sourceClusterName != null) &&
        !(status != null            ? !status.equals(feed.status)                       : feed.status != null) &&
        !(targetClusterName != null ? !targetClusterName.equals(feed.targetClusterName) : feed.targetClusterName != null);

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (schedule != null ? schedule.hashCode() : 0);
    result = 31 * result + (sourceClusterName != null ? sourceClusterName.hashCode() : 0);
    result = 31 * result + (targetClusterName != null ? targetClusterName.hashCode() : 0);
    return result;
  }
}
