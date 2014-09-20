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

package org.apache.ambari.server.orm.entities;

import java.util.List;

/**
 * LDAP sync event entity.
 */
public class LdapSyncEventEntity {

  /**
   * The unique id.
   */
  private final long id;

  /**
   * The status of this event.
   */
  private Status status;

  /**
   * The detail status message for this event.
   */
  private String statusDetail;

  /**
   * Sync event times.
   */
  private long startTime;
  private long endTime;

  /**
   * Results of sync event.
   */
  private Integer usersFetched;
  private Integer usersCreated;
  private Integer usersUpdated;
  private Integer usersRemoved;
  private Integer groupsFetched;
  private Integer groupsCreated;
  private Integer groupsUpdated;
  private Integer groupsRemoved;
  private Integer membershipsFetched;
  private Integer membershipsCreated;
  private Integer membershipsUpdated;

  /**
   * The specifications that define the sync event.
   */
  private List<LdapSyncSpecEntity> specs;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an LdapSyncEventEntity.
   *
   * @param id  the unique id
   */
  public LdapSyncEventEntity(long id) {
    this.id = id;
    this.status = Status.Pending;
  }


  // ----- LdapSyncEventEntity -----------------------------------------------

  /**
   * Get the id.
   *
   * @return the id
   */
  public long getId() {
    return id;
  }

  /**
   * Get the event status.
   *
   * @return the status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Set the event status.
   *
   * @param status  the status
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Get the event status detail message.
   *
   * @return the event detail
   */
  public String getStatusDetail() {
    return statusDetail;
  }

  /**
   * Set the event status detail message.
   *
   * @param statusDetail  the event status detail message
   */
  public void setStatusDetail(String statusDetail) {
    this.statusDetail = statusDetail;
  }

  /**
   * Get the event specifications.
   *
   * @return the event specs
   */
  public List<LdapSyncSpecEntity> getSpecs() {
    return specs;
  }

  /**
   * Set the event specifications.
   *
   * @param specs  the event specifications
   */
  public void setSpecs(List<LdapSyncSpecEntity> specs) {
    this.specs = specs;
  }

  /**
   * Get the time that the sync started.
   *
   * @return the sync start time (millis)
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Set the sync start time.
   *
   * @param startTime  the sync start time (millis)
   */
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  /**
   * Get the time the the sync event ended.
   *
   * @return  the end time (millis)
   */
  public long getEndTime() {
    return endTime;
  }

  /**
   * Set the sync end time.
   *
   * @param endTime  the end time (millis)
   */
  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public Integer getUsersFetched() {
    return usersFetched;
  }

  public void setUsersFetched(Integer usersFetched) {
    this.usersFetched = usersFetched;
  }

  public Integer getUsersCreated() {
    return usersCreated;
  }

  public void setUsersCreated(Integer usersCreated) {
    this.usersCreated = usersCreated;
  }

  public Integer getUsersUpdated() {
    return usersUpdated;
  }

  public void setUsersUpdated(Integer usersUpdated) {
    this.usersUpdated = usersUpdated;
  }

  public Integer getUsersRemoved() {
    return usersRemoved;
  }

  public void setUsersRemoved(Integer usersRemoved) {
    this.usersRemoved = usersRemoved;
  }

  public Integer getGroupsFetched() {
    return groupsFetched;
  }

  public void setGroupsFetched(Integer groupsFetched) {
    this.groupsFetched = groupsFetched;
  }

  public Integer getGroupsCreated() {
    return groupsCreated;
  }

  public void setGroupsCreated(Integer groupsCreated) {
    this.groupsCreated = groupsCreated;
  }

  public Integer getGroupsUpdated() {
    return groupsUpdated;
  }

  public void setGroupsUpdated(Integer groupsUpdated) {
    this.groupsUpdated = groupsUpdated;
  }

  public Integer getGroupsRemoved() {
    return groupsRemoved;
  }

  public void setGroupsRemoved(Integer groupsRemoved) {
    this.groupsRemoved = groupsRemoved;
  }

  public Integer getMembershipsFetched() {
    return membershipsFetched;
  }

  public void setMembershipsFetched(Integer membershipsFetched) {
    this.membershipsFetched = membershipsFetched;
  }

  public Integer getMembershipsCreated() {
    return membershipsCreated;
  }

  public void setMembershipsCreated(Integer membershipsCreated) {
    this.membershipsCreated = membershipsCreated;
  }

  public Integer getMembershipsUpdated() {
    return membershipsUpdated;
  }

  public void setMembershipsUpdated(Integer membershipsUpdated) {
    this.membershipsUpdated = membershipsUpdated;
  }


  // ----- enum : Status -----------------------------------------------------

  /**
   * LDAP sync event status
   */
  public enum Status {
    Pending,
    Running,
    Error,
    Complete
  }
}
