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
package org.apache.ambari.server.security.authorization;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;


/**
 * Helper class to take care of the cluster inherited permission for any view.
 */
public class ClusterInheritedPermissionHelper {

  /**
   * Predicate which validates if the principalType passed is valid or not.
   */
  public static final Predicate<String> validPrincipalTypePredicate = new Predicate<String>() {
    @Override
    public boolean apply(String principalType) {
      return isValidPrincipalType(principalType);
    }
  };

  /**
   * Predicate which validates if the privilegeEntity has resourceEntity of type {@see ResourceType.CLUSTER}
   */
  public static final Predicate<PrivilegeEntity> clusterPrivilegesPredicate = new Predicate<PrivilegeEntity>() {
    @Override
    public boolean apply(PrivilegeEntity privilegeEntity) {
      String resourceTypeName = privilegeEntity.getResource().getResourceType().getName();
      return ResourceType.translate(resourceTypeName) == ResourceType.CLUSTER;
    }
  };

  /**
   * Predicate which validates if view instance entity is cluster associated
   */
  public static final Predicate<ViewInstanceEntity> clusterAssociatedViewInstancePredicate = new Predicate<ViewInstanceEntity>() {
    @Override
    public boolean apply(ViewInstanceEntity viewInstanceEntity) {
      return viewInstanceEntity.getClusterHandle() != null;
    }
  };

  /**
   * Predicate to validate if the privilege entity has a principal which has a cluster inherited principal type
   */
  public static final Predicate<PrivilegeEntity> privilegeWithClusterInheritedPermissionTypePredicate = new Predicate<PrivilegeEntity>() {
    @Override
    public boolean apply(PrivilegeEntity privilegeEntity) {
      String principalTypeName = privilegeEntity.getPrincipal().getPrincipalType().getName();
      return principalTypeName.startsWith("ALL.");
    }
  };

  /**
   * Mapper to return the Permission Name from the cluster inherited privilege name. Example: "ALL.CLUSTER.USER" becomes "CLUSTER.USER"
   */
  public static final Function<PrivilegeEntity, String> permissionNameFromClusterInheritedPrivilege = new Function<PrivilegeEntity, String>() {
    @Override
    public String apply(PrivilegeEntity input) {
      return input.getPrincipal().getPrincipalType().getName().substring(4);
    }
  };

  /**
   * Mapper to return resources from view instance entity.
   */
  public static final Function<ViewInstanceEntity, ResourceEntity> resourceFromViewInstanceMapper = new Function<ViewInstanceEntity, ResourceEntity>() {
    @Override
    public ResourceEntity apply(ViewInstanceEntity viewInstanceEntity) {
      return viewInstanceEntity.getResource();
    }
  };

  /**
   * Mapper to return all privileges from resource entity
   */
  public static final Function<ResourceEntity, Iterable<PrivilegeEntity>> allPrivilegesFromResoucesMapper = new Function<ResourceEntity, Iterable<PrivilegeEntity>>() {
    @Override
    public Iterable<PrivilegeEntity> apply(ResourceEntity resourceEntity) {
      return resourceEntity.getPrivileges();
    }
  };

  /**
   * Mapper to return permission name from privilege
   */
  public static final Function<PrivilegeEntity, String> permissionNameFromPrivilegeMapper = new Function<PrivilegeEntity, String>() {
    @Override
    public String apply(PrivilegeEntity privilegeEntity) {
      return privilegeEntity.getPermission().getPermissionName();
    }
  };

  /**
   * Predicate to validate if the cluster inherited principal type for privilege entity is present in the valid permission type set passed
   * @param validSet - valid set of permission types
   * @return Predicate to check the condition
   */
  public static final Predicate<PrivilegeEntity> principalTypeInSetFrom(final Collection<String> validSet) {
    return new Predicate<PrivilegeEntity>() {
      @Override
      public boolean apply(PrivilegeEntity privilegeEntity) {
        String permissionName = privilegeEntity.getPrincipal().getPrincipalType().getName().substring(4);
        return validSet.contains(permissionName);
      }
    };
  }

  /**
   * Predicate to filter out privileges which are already existing in the passed privileges set.
   * @param existingPrivileges - Privileges set to which the comparison will be made
   * @return Predicate to check the validation
   */
  public static Predicate<PrivilegeEntity> removeIfExistingPrivilegePredicate(final Set<PrivilegeEntity> existingPrivileges) {
    return new Predicate<PrivilegeEntity>() {
      @Override
      public boolean apply(final PrivilegeEntity privilegeEntity) {
        return !FluentIterable.from(existingPrivileges).anyMatch(new com.google.common.base.Predicate<PrivilegeEntity>() {
          @Override
          public boolean apply(PrivilegeEntity directPrivilegeEntity) {
            return directPrivilegeEntity.getResource().getId().equals(privilegeEntity.getResource().getId())
              && directPrivilegeEntity.getPermission().getId().equals(privilegeEntity.getPermission().getId());
          }
        });
      }
    };
  }

  /**
   * Validates if the principal type is valid for cluster inherited permissions.
   * @param principalType - Principal type
   * @return true if the principalType is in ("ALL.CLUSTER.ADMINISTRATOR", "ALL.CLUSTER.OPERATOR",
   * "ALL.CLUSTER.USER", "ALL.SERVICE.OPERATOR", "ALL.SERVICE.USER")
   */
  public static boolean isValidPrincipalType(String principalType) {
    return PrincipalTypeEntity.CLUSTER_ADMINISTRATOR_PRINCIPAL_TYPE_NAME.equalsIgnoreCase(principalType)
      || PrincipalTypeEntity.CLUSTER_OPERATOR_PRINCIPAL_TYPE_NAME.equalsIgnoreCase(principalType)
      || PrincipalTypeEntity.CLUSTER_USER_PRINCIPAL_TYPE_NAME.equalsIgnoreCase(principalType)
      || PrincipalTypeEntity.SERVICE_ADMINISTRATOR_PRINCIPAL_TYPE_NAME.equalsIgnoreCase(principalType)
      || PrincipalTypeEntity.SERVICE_OPERATOR_PRINCIPAL_TYPE_NAME.equalsIgnoreCase(principalType);
  }

  /**
   * Returns the view privileges for which cluster permissions has been specified. This filters out all the privileges
   * which are related to view resources attached to a cluster and are configured to have cluster level permissions. Then
   * It checks if the user has cluster level permissions and further filters down the privilege list to the ones for which
   * the user should have privilege.
   * @param userDirectPrivileges - direct privileges for the user.
   * @return - Filtered list of privileges for view resource for which the user should have access.
   */
  public static Set<PrivilegeEntity> getViewPrivilegesWithClusterPermission(final ViewInstanceDAO viewInstanceDAO, final PrivilegeDAO privilegeDAO,
                                                                            final Set<PrivilegeEntity> userDirectPrivileges) {

    final Set<String> clusterPrivileges = FluentIterable.from(userDirectPrivileges)
      .filter(ClusterInheritedPermissionHelper.clusterPrivilegesPredicate)
      .transform(ClusterInheritedPermissionHelper.permissionNameFromPrivilegeMapper)
      .toSet();

    Set<Long> resourceIds = FluentIterable.from(viewInstanceDAO.findAll())
      .filter(ClusterInheritedPermissionHelper.clusterAssociatedViewInstancePredicate)
      .transform(ClusterInheritedPermissionHelper.resourceFromViewInstanceMapper)
      .transform(new Function<ResourceEntity, Long>() {
        @Nullable
        @Override
        public Long apply(@Nullable ResourceEntity input) {
          return input.getId();
        }
      }).toSet();

    Set<PrivilegeEntity> allPrivileges = FluentIterable.from(resourceIds)
      .transformAndConcat(new Function<Long, Iterable<PrivilegeEntity>>() {
        @Nullable
        @Override
        public Iterable<PrivilegeEntity> apply(@Nullable Long input) {
          return privilegeDAO.findByResourceId(input);
        }
      }).toSet();

    return FluentIterable.from(allPrivileges)
      .filter(ClusterInheritedPermissionHelper.privilegeWithClusterInheritedPermissionTypePredicate)
      .filter(ClusterInheritedPermissionHelper.principalTypeInSetFrom(clusterPrivileges))
      .filter(ClusterInheritedPermissionHelper.removeIfExistingPrivilegePredicate(userDirectPrivileges))
      .toSet();
  }
}
