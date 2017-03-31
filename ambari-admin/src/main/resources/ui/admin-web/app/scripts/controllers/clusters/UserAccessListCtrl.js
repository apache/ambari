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
'use strict';

angular.module('ambariAdminConsole')
.controller('UserAccessListCtrl',['$scope', '$location', 'Cluster', '$modal', '$rootScope', '$routeParams', 'PermissionSaver', 'Alert', '$translate', 'RoleDetailsModal',
function($scope, $location, Cluster, $modal, $rootScope, $routeParams, PermissionSaver, Alert, $translate, RoleDetailsModal) {
  var $t = $translate.instant;
  $scope.constants = {
    users: $t('common.users').toLowerCase(),
    groups: $t('common.groups').toLowerCase()
  };
  $scope.users = [];
  $scope.usersPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalUsers = 1;
  $scope.currentNameFilter = '';
  $scope.maxVisiblePages = 20;
  $scope.roles = [];
  $scope.clusterId = $routeParams.id;
  $scope.tableInfo = {
    total: 0,
    showed: 0,
    filtered: 0
  };
  $scope.isNotEmptyFilter = true;
  $scope.NONE_ROLE = {
    "permission_label" : $t('common.none'),
    "permission_name" : "CLUSTER.NONE"
  };
  $scope.ALL_ROLE = {
    "permission_label" : $t('common.all'),
    "permission_name" : ""
  };
  $scope.AMBARI_ADMIN_ROLE = {
    "permission_label" : $t('users.roles.ambariAdmin'),
    "permission_name" : "AMBARI.ADMINISTRATOR"
  };

  $scope.pageChanged = function() {
    $scope.loadUsers();
  };
  $scope.usersPerPageChanges = function() {
    $scope.resetPagination();
  };

  $scope.loadUsers = function(){
    Cluster.getPrivilegesWithFilters({
      nameFilter: $scope.currentNameFilter,
      typeFilter: $scope.currentTypeFilter,
      roleFilter: $scope.currentRoleFilter,
      currentPage: $scope.currentPage,
      usersPerPage: $scope.usersPerPage
    }).then(function(data) {
      $scope.totalUsers = data.itemTotal;
      $scope.users = data.items.map(function (user) {
        var privilege = $scope.pickEffectivePrivilege(user.privileges);
        // Redefine principal_name and principal type in case of None
        privilege.principal_name = user.Users? user.Users.user_name : user.Groups.group_name;
        if (privilege.permission_label === "None") {
          privilege.principal_type = user.Users ? 'USER' : 'GROUP';
        }
        var name = encodeURIComponent(privilege.principal_name);
        privilege.encoded_name = name;
        privilege.original_perm = privilege.permission_name;
        privilege.url = user.Users? ('users/' + name) : ('groups/' + name + '/edit');
        privilege.editable = Cluster.ineditableRoles.indexOf(privilege.permission_name) == -1;
        return privilege;
      });
      $scope.tableInfo.total = data.itemTotal;
      $scope.tableInfo.showed = data.items.length;
    });
  };

  $scope.pickEffectivePrivilege = function(privileges) {
    if (privileges && privileges.length > 1) {
      return privileges.reduce(function(prev, cur) {
        var prevIndex = $scope.getRoleRank(prev.PrivilegeInfo.permission_name);
        var curIndex = $scope.getRoleRank(cur.PrivilegeInfo.permission_name)
        return (prevIndex < curIndex) ? prev : cur;
      }).PrivilegeInfo;
    } else if (privileges && privileges.length == 1 && privileges[0].PrivilegeInfo.permission_name !== "VIEW.USER") {
      return privileges[0].PrivilegeInfo;
    } else {
      return angular.copy($scope.NONE_ROLE);
    }
  };

  $scope.loadRoles = function() {
    Cluster.getPermissions().then(function(data) {
      $scope.roles = data.map(function(item) {
        return item.PermissionInfo;
      });
      // [All, Administrator, ...roles..., None]
      $scope.roles.unshift(angular.copy($scope.AMBARI_ADMIN_ROLE));
      $scope.roles.unshift(angular.copy($scope.ALL_ROLE));
      $scope.roles.push(angular.copy($scope.NONE_ROLE));

      // create filter select list
      $scope.roleFilterOptions = angular.copy($scope.roles);
      $scope.roleFilterOptions.pop();  // filter does not support None
      $scope.roleFilterOptions = $scope.roleFilterOptions.map(function(o) {
        return {label: o.permission_label, value: o.permission_name};
      });
      $scope.currentRoleFilter = $scope.roleFilterOptions[0];

      // create value select list
      $scope.roleValueOptions = angular.copy($scope.roles)
      $scope.roleValueOptions.shift(); // value change does not support all/administrator
      $scope.roleValueOptions.shift();
    });
  };

  $scope.getRoleRank = function(permission_name) {
    var orderedRoles = Cluster.orderedRoles.concat(['VIEW.USER','CLUSTER.NONE']);
    var index = orderedRoles.indexOf(permission_name);
    return index;
  };

  $scope.save = function(user) {
    var fromNone = (user.original_perm === $scope.NONE_ROLE.permission_name);
    if (fromNone) {
      $scope.addPrivilege(user);
      return;
    }

    if ($scope.isUserActive) {
      Cluster.getPrivilegesForResource({
          nameFilter : user.user_name,
          typeFilter : $scope.currentTypeFilter
      }).then(function(data) {
        var arrayOfPrivileges = data.items[0].privileges;
        var privilegesOfTypeUser = [];
        var privilegesOfTypeGroup = [];
        for (var i = 0; i < arrayOfPrivileges.length; i++) {
          if(arrayOfPrivileges[i].PrivilegeInfo.permission_name != "VIEW.USER") {
            if(arrayOfPrivileges[i].PrivilegeInfo.principal_type === "GROUP"){
              privilegesOfTypeGroup.push(arrayOfPrivileges[i]);
            } else {
              privilegesOfTypeUser.push(arrayOfPrivileges[i].PrivilegeInfo);
            }
          }
        }

        var effectivePrivilege = $scope.pickEffectivePrivilege(arrayOfPrivileges);
        var effectivePrivilegeFromGroups = $scope.pickEffectivePrivilege(privilegesOfTypeGroup);
        user.principal_type = 'USER';
        user.original_perm = effectivePrivilege.permission_name;
        user.editable = (Cluster.ineditableRoles.indexOf(effectivePrivilege.permission_name) === -1);

        var userIndex = $scope.getRoleRank(user.permission_name);
        var groupIndex = $scope.getRoleRank(effectivePrivilegeFromGroups.permission_name);

        // Process when it's NONE privilege or higher than current effective group privilege
        if (userIndex <= groupIndex || user.permission_name == $scope.NONE_ROLE.permission_name) {
          var privilege_ids = privilegesOfTypeUser.filter(function(privilegeOfTypeUser) {
            return privilegeOfTypeUser.principal_type !== 'ROLE';
          }).map(function (privilegeOfTypeUser) {
            return privilegeOfTypeUser.privilege_id;
          });

          // Purge existing user level privileges if there is any
          if(privilege_ids.length !== 0) {
            Cluster.deleteMultiplePrivileges(
                $routeParams.id,
                privilege_ids
            )
            .then(function() {
              $scope.addPrivilege(user);
            });
          } else {
            $scope.addPrivilege(user);
          }
        } else {
          Alert.error($t('common.alerts.cannotSavePermissions'),
              $t('users.alerts.usersEffectivePrivilege', {user_name : user.user_name})
          );
          $scope.loadUsers();
        }
      });
    } else {
      Cluster.getPrivilegesForResource({
          nameFilter : user.group_name,
          typeFilter : $scope.currentTypeFilter
      }).then(function(data) {
        var arrayOfPrivileges = data.items[0].privileges;
        var privilegesOfTypeGroup = [];
        var privilege = $scope.pickEffectivePrivilege(arrayOfPrivileges);
        user.principal_type = 'GROUP';
        user.original_perm = privilege.permission_name;
        user.editable = (Cluster.ineditableRoles.indexOf(privilege.permission_name) === -1);

        arrayOfPrivileges.forEach(function(privilegeOfTypeGroup) {
          if(privilegeOfTypeGroup.PrivilegeInfo.permission_name != "VIEW.USER") {
            if (privilegeOfTypeGroup.PrivilegeInfo.principal_type === "GROUP") {
              privilegesOfTypeGroup.push(privilegeOfTypeGroup.PrivilegeInfo);
            }
          }
        });

        var privilege_ids = [];
        privilegesOfTypeGroup.forEach(function(privilegeOfTypeGroup) {
          privilege_ids.push(privilegeOfTypeGroup.privilege_id);
        });

        //delete all privileges of type GROUP, if they exist
        //then add the privilege for the group, after which the group displays the effective privilege
        if(privilege_ids.length !== 0) {
          Cluster.deleteMultiplePrivileges(
              $routeParams.id,
              privilege_ids
          )
          .then(function() {
            $scope.addPrivilege(user);
          });
        } else {
          $scope.addPrivilege(user);
        }
      });
    }
  };

  $scope.cancel = function(user) {
    user.permission_name = user.original_perm;
  };

  $scope.addPrivilege = function(user) {
    var changeToNone = user.permission_name == $scope.NONE_ROLE.permission_name;
    if (changeToNone) {
      if ($scope.isUserActive) {
        Alert.success($t('users.alerts.roleChangedToNone', {
            user_name : user.user_name
        }));
      } else {
        $scope.showSuccess(user);
      }
      $scope.loadUsers();
      return;
    }
    Cluster.createPrivileges(
      {
        clusterId: $routeParams.id
      },
      [{PrivilegeInfo: {
        permission_name: user.permission_name,
        principal_name: user.principal_name,
        principal_type: user.principal_type
      }}]
    ).then(function() {
        $scope.showSuccess(user);
        $scope.loadUsers();
      })
      .catch(function(data) {
        Alert.error($t('common.alerts.cannotSavePermissions'), data.data.message);
        $scope.loadUsers();
      });
  };

  $scope.showSuccess = function(user) {
    Alert.success($t('users.alerts.roleChanged', {
      name: user.principal_name,
      role: $scope.roles.filter(function(r){
          return r.permission_name == user.permission_name}
      )[0].permission_label
    }));
  };

  $scope.resetPagination = function() {
    $scope.currentPage = 1;
    $scope.loadUsers();
  };
  $scope.currentRoleFilter = { label:$t('common.all'), value: '' };


  $scope.typeFilterOptions = [
    {label: $t('common.user'), value: 'USER'},
    {label: $t('common.group'), value: 'GROUP'}
  ];

  $scope.isUserActive = true;

  $scope.currentTypeFilter = $scope.typeFilterOptions[0];

  $scope.switchToUser = function() {
    if (!$scope.isUserActive) {
      $scope.currentTypeFilter = $scope.typeFilterOptions[0];
      $scope.isUserActive = true;
      $scope.resetPagination();
    }
  };

  $scope.switchToGroup = function() {
    if ($scope.isUserActive) {
      $scope.currentTypeFilter = $scope.typeFilterOptions[1];
      $scope.isUserActive = false;
      $scope.resetPagination();
    }
  };

  $scope.clearFilters = function() {
    $scope.currentNameFilter = '';
    $scope.isUserActive = true;
    $scope.currentTypeFilter = $scope.typeFilterOptions[0];
    $scope.currentRoleFilter = $scope.roleFilterOptions[0];
    $scope.resetPagination();
  };

  $scope.loadRoles();
  $scope.loadUsers();

  $scope.$watch(
    function (scope) {
      return Boolean(scope.currentNameFilter || (scope.currentTypeFilter && scope.currentTypeFilter.value)
        || (scope.currentRoleFilter && scope.currentRoleFilter.value));
    },
    function (newValue, oldValue, scope) {
      scope.isNotEmptyFilter = newValue;
    }
  );

  $rootScope.$watch(function(scope) {
    return scope.LDAPSynced;
  }, function(LDAPSynced) {
    if(LDAPSynced === true){
      $rootScope.LDAPSynced = false;
      $scope.loadUsers();
    }
  });

  $scope.switchToBlock = function() {
    $location.url('/clusters/' + $routeParams.id + '/manageAccess');
  };

  $scope.showHelpPage = function() {
    Cluster.getRolesWithAuthorizations().then(function(roles) {
      RoleDetailsModal.show(roles);
    });
  };
}]);
