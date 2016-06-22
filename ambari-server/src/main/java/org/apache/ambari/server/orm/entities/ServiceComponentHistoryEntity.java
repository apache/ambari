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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.base.Objects;

/**
 * The {@link ServiceComponentHistoryEntity} class is used to represent an
 * upgrade or downgrade which was performed on an individual service component.
 */
@Entity
@Table(name = "servicecomponent_history")
@TableGenerator(
    name = "servicecomponent_history_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "servicecomponent_history_id_seq",
    initialValue = 0)
@NamedQueries({ @NamedQuery(
    name = "ServiceComponentHistoryEntity.findByComponent",
    query = "SELECT history FROM ServiceComponentHistoryEntity history WHERE history.m_serviceComponentDesiredStateEntity.clusterId = :clusterId AND history.m_serviceComponentDesiredStateEntity.serviceName = :serviceName AND history.m_serviceComponentDesiredStateEntity.componentName = :componentName") })
public class ServiceComponentHistoryEntity {

  @Id
  @GeneratedValue(
      strategy = GenerationType.TABLE,
      generator = "servicecomponent_history_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private long m_id;

  @ManyToOne(optional = false, cascade = { CascadeType.MERGE })
  @JoinColumn(name = "component_id", referencedColumnName = "id", nullable = false)
  private ServiceComponentDesiredStateEntity m_serviceComponentDesiredStateEntity;

  @ManyToOne(optional = false)
  @JoinColumn(name = "from_stack_id", referencedColumnName = "stack_id", nullable = false)
  private StackEntity m_fromStack;

  @ManyToOne(optional = false)
  @JoinColumn(name = "to_stack_id", referencedColumnName = "stack_id", nullable = false)
  private StackEntity m_toStack;

  @ManyToOne(optional = false)
  @JoinColumn(name = "upgrade_id", referencedColumnName = "upgrade_id", nullable = false)
  private UpgradeEntity m_upgradeEntity;

  public ServiceComponentDesiredStateEntity getServiceComponentDesiredState() {
    return m_serviceComponentDesiredStateEntity;
  }

  /**
   * Sets the component associated with this historical entry.
   *
   * @param serviceComponentDesiredStateEntity
   *          the component to associate with this historical entry (not
   *          {@code null}).
   */
  public void setServiceComponentDesiredState(ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity) {
    m_serviceComponentDesiredStateEntity = serviceComponentDesiredStateEntity;
  }

  /**
   * @return the id
   */
  public long getId() {
    return m_id;
  }

  /**
   * @return the fromStack
   */
  public StackEntity getFromStack() {
    return m_fromStack;
  }

  /**
   * @param fromStack
   *          the fromStack to set
   */
  public void setFromStack(StackEntity fromStack) {
    m_fromStack = fromStack;
  }

  /**
   * @return the toStack
   */
  public StackEntity getToStack() {
    return m_toStack;
  }

  /**
   * @param toStack
   *          the toStack to set
   */
  public void setToStack(StackEntity toStack) {
    m_toStack = toStack;
  }

  /**
   * @return the upgradeEntity
   */
  public UpgradeEntity getUpgrade() {
    return m_upgradeEntity;
  }

  /**
   * @param upgradeEntity
   *          the upgradeEntity to set
   */
  public void setUpgrade(UpgradeEntity upgradeEntity) {
    m_upgradeEntity = upgradeEntity;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
    hashCodeBuilder.append(m_fromStack);
    hashCodeBuilder.append(m_toStack);
    hashCodeBuilder.append(m_upgradeEntity);
    hashCodeBuilder.append(m_serviceComponentDesiredStateEntity);
    return hashCodeBuilder.toHashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    final ServiceComponentHistoryEntity other = (ServiceComponentHistoryEntity) obj;
    return Objects.equal(m_fromStack, other.m_fromStack)
        && Objects.equal(m_toStack, other.m_toStack)
        && Objects.equal(m_upgradeEntity, other.m_upgradeEntity) 
        && Objects.equal(m_serviceComponentDesiredStateEntity, other.m_serviceComponentDesiredStateEntity);
  }
}
