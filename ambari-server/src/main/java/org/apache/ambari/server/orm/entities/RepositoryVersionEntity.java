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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

@Entity
@Table(name = "repo_version", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"display_name"}),
    @UniqueConstraint(columnNames = {"stack_id", "version"})
})
@TableGenerator(name = "repository_version_id_generator",
    table = "ambari_sequences",
    pkColumnName = "sequence_name",
    valueColumnName = "sequence_value",
    pkColumnValue = "repo_version_id_seq",
    initialValue = 0
    )
@NamedQueries({
    @NamedQuery(name = "repositoryVersionByDisplayName", query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.displayName=:displayname"),
    @NamedQuery(name = "repositoryVersionByStack", query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.stack.stackName=:stackName AND repoversion.stack.stackVersion=:stackVersion"),
        @NamedQuery(name = "repositoryVersionByStackNameAndVersion", query = "SELECT repoversion FROM RepositoryVersionEntity repoversion WHERE repoversion.stack.stackName=:stackName AND repoversion.version=:version")
})
@StaticallyInject
public class RepositoryVersionEntity {

  private static Logger LOG = LoggerFactory.getLogger(RepositoryVersionEntity.class);

  @Inject
  private static Provider<RepositoryVersionHelper> repositoryVersionHelperProvider;

  @Id
  @Column(name = "repo_version_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "repository_version_id_generator")
  private Long id;

  /**
   * Unidirectional one-to-one association to {@link StackEntity}
   */
  @OneToOne
  @JoinColumn(name = "stack_id", unique = false, nullable = false, insertable = true, updatable = true)
  private StackEntity stack;

  @Column(name = "version")
  private String version;

  @Column(name = "display_name")
  private String displayName;

  @Lob
  @Column(name = "repositories")
  private String operatingSystems;

  @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "repositoryVersion")
  private Set<ClusterVersionEntity> clusterVersionEntities;

  @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "repositoryVersion")
  private Set<HostVersionEntity> hostVersionEntities;

  // ----- RepositoryVersionEntity -------------------------------------------------------

  public RepositoryVersionEntity() {

  }

  public RepositoryVersionEntity(StackEntity stack, String version,
      String displayName, String operatingSystems) {
    this.stack = stack;
    this.version = version;
    this.displayName = displayName;
    this.operatingSystems = operatingSystems;
  }

  /**
   * Update one-to-many relation without rebuilding the whole entity
   * @param entity many-to-one entity
   */
  public void updateClusterVersionEntityRelation(ClusterVersionEntity entity){
    clusterVersionEntities.add(entity);
  }

  /**
   * Update one-to-many relation without rebuilding the whole entity
   * @param entity many-to-one entity
   */
  public void updateHostVersionEntityRelation(HostVersionEntity entity){
    hostVersionEntities.add(entity);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Gets the repository version's stack.
   *
   * @return the stack.
   */
  public StackEntity getStack() {
    return stack;
  }

  /**
   * Sets the repository version's stack.
   *
   * @param stack
   *          the stack to set for the repo version (not {@code null}).
   */
  public void setStack(StackEntity stack) {
    this.stack = stack;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getOperatingSystemsJson() {
    return operatingSystems;
  }

  public void setOperatingSystems(String repositories) {
    operatingSystems = repositories;
  }

  /**
   * Getter which hides json nature of operating systems and returns them as entities.
   *
   * @return empty list if stored json is invalid
   */
  public List<OperatingSystemEntity> getOperatingSystems() {
    if (StringUtils.isNotBlank(operatingSystems)) {
      try {
        return repositoryVersionHelperProvider.get().parseOperatingSystems(operatingSystems);
      } catch (Exception ex) {
        // Should never happen as we validate json before storing it to DB
        LOG.error("Could not parse operating systems json stored in database:" + operatingSystems, ex);
      }
    }
    return Collections.emptyList();
  }

  public String getStackName() {
    return getStackId().getStackName();
  }

  public String getStackVersion() {
    return getStackId().getStackVersion();
  }

  public StackId getStackId() {
    if (null == stack) {
      return null;
    }

    return new StackId(stack.getStackName(), stack.getStackVersion());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RepositoryVersionEntity that = (RepositoryVersionEntity) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    if (stack != null ? !stack.equals(that.stack) : that.stack != null) {
      return false;
    }
    if (version != null ? !version.equals(that.version) : that.version != null) {
      return false;
    }
    if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) {
      return false;
    }
    if (operatingSystems != null ? !operatingSystems.equals(that.operatingSystems) : that.operatingSystems != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (stack != null ? stack.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (operatingSystems != null ? operatingSystems.hashCode() : 0);
    return result;
  }

  /**
   * Determine if the version belongs to the stack.
   * Right now, this is only applicable for the HDP stack.
   * @param stackId Stack, such as HDP-2.3
   * @param version Version number, such as 2.3.0.0, or 2.3.0.0-1234
   * @return Return true if the version starts with the digits of the stack.
   */
  public static boolean isVersionInStack(StackId stackId, String version) {
    if (null != version && !StringUtils.isBlank(version)) {
      // HDP Stack
      if (stackId.getStackName().equalsIgnoreCase(StackId.HDP_STACK) ||
          stackId.getStackName().equalsIgnoreCase(StackId.HDPWIN_STACK)) {

        String leading = stackId.getStackVersion();  // E.g, 2.3
        // In some cases during unit tests, the leading can contain 3 digits, so only the major number (first two parts) are needed.
        String[] leadingParts = leading.split("\\.");
        if (null != leadingParts && leadingParts.length > 2) {
          leading = leadingParts[0] + "." + leadingParts[1];
        }
        return version.startsWith(leading);
      }
      // For other stacks, don't make the check.
      return true;
    }
    return false;
  }
}
