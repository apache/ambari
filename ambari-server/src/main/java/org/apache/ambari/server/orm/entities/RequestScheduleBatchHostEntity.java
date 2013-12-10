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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Table(name = "requestschedulebatchhost")
@Entity
@NamedQueries({
  @NamedQuery(name = "batchHostsBySchedule", query =
    "SELECT batchHost FROM RequestScheduleBatchHostEntity batchHost " +
      "WHERE batchHost.scheduleId=:id")
})
public class RequestScheduleBatchHostEntity {

  @Id
  @Column(name = "schedule_id", nullable = false, insertable = true, updatable = true)
  private Long scheduleId;

  @Id
  @Column(name = "batch_id", nullable = false, insertable = true, updatable = true)
  private Long batchId;

  @Id
  @Column(name = "host_name", nullable = false, insertable = true, updatable = true)
  private String hostName;

  @Column(name = "batch_name")
  private String batchName;

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "host_name", referencedColumnName = "host_name", nullable = false, insertable = false, updatable = false) })
  private HostEntity hostEntity;

  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "schedule_id", referencedColumnName = "schedule_id", nullable = false, insertable = false, updatable = false) })
  private RequestScheduleEntity requestScheduleEntity;

  public Long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getBatchName() {
    return batchName;
  }

  public void setBatchName(String batchName) {
    this.batchName = batchName;
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
  }

  public RequestScheduleEntity getRequestScheduleEntity() {
    return requestScheduleEntity;
  }

  public void setRequestScheduleEntity(RequestScheduleEntity requestScheduleEntity) {
    this.requestScheduleEntity = requestScheduleEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequestScheduleBatchHostEntity that = (RequestScheduleBatchHostEntity) o;

    if (!batchId.equals(that.batchId)) return false;
    if (!hostName.equals(that.hostName)) return false;
    if (!scheduleId.equals(that.scheduleId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = scheduleId.hashCode();
    result = 31 * result + batchId.hashCode();
    result = 31 * result + hostName.hashCode();
    return result;
  }
}
