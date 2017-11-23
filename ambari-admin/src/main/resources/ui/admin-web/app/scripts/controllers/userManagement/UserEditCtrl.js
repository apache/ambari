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
.controller('UserEditCtrl', ['$scope', '$routeParams', 'Cluster', 'User', 'View', '$modal', '$location', 'ConfirmationModal', 'Alert', 'Auth', 'getDifference', 'Group', '$q', 'UserConstants', '$translate', function($scope, $routeParams, Cluster, User, View, $modal, $location, ConfirmationModal, Alert, Auth, getDifference, Group, $q, UserConstants, $translate) {

  var $t = $translate.instant;

  $scope.constants = {
    user: $t('common.user'),
    status: $t('users.status'),
    admin: $t('users.admin'),
    password: $t('users.password'),
    view: $t('common.view').toLowerCase(),
    cluster: $t('common.cluster').toLowerCase()
  };

  function loadUserInfo(){
    User.get($routeParams.id).then(function(data) {
      $scope.user = User.makeUser(data).Users;
      $scope.isCurrentUser = $scope.user.user_name === Auth.getCurrentUser();
      $scope.editingGroupsList = angular.copy($scope.user.groups);
    });
  }

  loadUserInfo();
  $scope.user;
  $scope.isCurrentUser = true;
  $scope.dataLoaded = false;

  $scope.isGroupEditing = false;
  $scope.enableGroupEditing = function() {
    $scope.isGroupEditing = true;
    $scope.editingGroupsList = angular.copy($scope.user.groups);
  };

  $scope.$watch(function() {
    return $scope.editingGroupsList;
  }, function(newValue) {
    if(newValue){
      if( !angular.equals(newValue, $scope.user.groups) ){
        $scope.updateGroups();
      }
    }
  }, true);

  $scope.updateGroups = function() {
    var groups = $scope.editingGroupsList.toString().split(',').filter(function(item) {return item.trim();}).map(function(item) {return item.trim()});
    var diff = getDifference($scope.user.groups, groups);
    var promises = [];
    // Remove user from groups
    angular.forEach(diff.del, function(groupName) {
      promises.push(Group.removeMemberFromGroup(groupName, $scope.user.user_name).catch(function(data) {
        Alert.error($t('users.alerts.removeUserError'), data.data.message);
      }));
    });
    // Add user to groups
    angular.forEach(diff.add, function(groupName) {
      promises.push(Group.addMemberToGroup(groupName, $scope.user.user_name).catch(function(data) {
        Alert.error($t('users.alerts.cannotAddUser'), data.data.message);
      }));
    });
    $q.all(promises).then(function() {
      loadUserInfo();
    });
    $scope.isGroupEditing = false;
  };

  $scope.getUserMembership = function(userType) {
    if(userType) {
	return $t(UserConstants.TYPES[userType].LABEL_KEY) + " " + $t('users.groupMembership');
    }
  };

  $scope.cancelUpdate = function() {
    $scope.isGroupEditing = false;
    $scope.editingGroupsList = '';
  };

  $scope.openChangePwdDialog = function() {
    var modalInstance = $modal.open({
      templateUrl: 'views/userManagement/modals/changePassword.html',
      resolve: {
        userName: function() {
          return $scope.user.user_name;
        }
      },
      controller: ['$scope', 'userName', function($scope, userName) {
        $scope.passwordData = {
          password: '',
          currentUserPassword: ''
        };

        $scope.form = {};
        $scope.userName = userName;

        $scope.ok = function() {
          $scope.form.passwordChangeForm.submitted = true;
          if($scope.form.passwordChangeForm.$valid){

            modalInstance.close({
              password: $scope.passwordData.password, 
              currentUserPassword: $scope.passwordData.currentUserPassword
            });
          }
        };
        $scope.cancel = function() {
          modalInstance.dismiss('cancel');
        };
      }]
    });

    modalInstance.result.then(function(data) {
      User.setPassword($scope.user, data.password, data.currentUserPassword).then(function() {
        Alert.success($t('users.alerts.passwordChanged'));
      }).catch(function(data) {
        Alert.error($t('users.alerts.cannotChangePassword'), data.data.message);
      });
    }); 
  };

  $scope.toggleUserActive = function() {
    if(!$scope.isCurrentUser){
      var newStatusKey = $scope.user.active ? 'inactive' : 'active',
        newStatus = $t('users.' + newStatusKey).toLowerCase();
      ConfirmationModal.show(
        $t('users.changeStatusConfirmation.title'),
        $t('users.changeStatusConfirmation.message', {
          userName: $scope.user.user_name,
          status: newStatus
        })
      ).then(function() {
        User.setActive($scope.user.user_name, $scope.user.active)
          .catch(function(data) {
            Alert.error($t('common.alerts.cannotUpdateStatus'), data.data.message);
            $scope.user.active = !$scope.user.active;
          });
      })
      .catch(function() {
        $scope.user.active = !$scope.user.active;
      });
    }
  };    
  $scope.toggleUserAdmin = function() {
    if(!$scope.isCurrentUser){
      var action = $scope.user.admin ?
        $t('users.changePrivilegeConfirmation.revoke') : $t('users.changePrivilegeConfirmation.grant');
      ConfirmationModal.show(
        $t('users.changePrivilegeConfirmation.title'),
        $t('users.changePrivilegeConfirmation.message', {
          action: action,
          userName: $scope.user.user_name
        })
      ).then(function() {
        User.setAdmin($scope.user.user_name, $scope.user.admin)
        .then(function() {
          loadPrivileges();
        })
        .catch(function (data) {
          Alert.error($t('common.alerts.cannotUpdateAdminStatus'), data.data.message);
          $scope.user.admin = !$scope.user.admin;
        });
      })
      .catch(function() {
        $scope.user.admin = !$scope.user.admin;
      });

    }
  };

  $scope.removePrivilege = function(name, privilege) {
    var privilegeObject = {
        id: privilege.privilege_id,
        view_name: privilege.view_name,
        version: privilege.version,
        instance_name: name
    };
    View.deletePrivilege(privilegeObject).then(function() {
      loadPrivileges();
    });
  };

  $scope.deleteUser = function() {
    ConfirmationModal.show(
      $t('common.delete', {
        term: $t('common.user')
      }),
      $t('common.deleteConfirmation', {
        instanceType: $t('common.user').toLowerCase(),
        instanceName: '"' + $scope.user.user_name + '"'
      })
    ).then(function() {
      Cluster.getPrivilegesForResource({
        nameFilter : $scope.user.user_name,
        typeFilter : {value: 'USER'}
      }).then(function(data) {
        var clusterPrivilegesIds = [];
        var viewsPrivileges = [];
        if (data.items && data.items.length) {
          angular.forEach(data.items[0].privileges, function(privilege) {
            if (privilege.PrivilegeInfo.principal_type === 'USER') {
              if (privilege.PrivilegeInfo.type === 'VIEW') {
                viewsPrivileges.push({
                  id: privilege.PrivilegeInfo.privilege_id,
                  view_name: privilege.PrivilegeInfo.view_name,
                  version: privilege.PrivilegeInfo.version,
                  instance_name: privilege.PrivilegeInfo.instance_name
                });
              } else {
                clusterPrivilegesIds.push(privilege.PrivilegeInfo.privilege_id);
              }
            }
          });
        }
        User.delete($scope.user.user_name).then(function() {
          $location.path('/userManagement');
          if (clusterPrivilegesIds.length) {
            Cluster.getAllClusters().then(function (clusters) {
              var clusterName = clusters[0].Clusters.cluster_name;
              Cluster.deleteMultiplePrivileges(clusterName, clusterPrivilegesIds);
            });
          }
          angular.forEach(viewsPrivileges, function(privilege) {
            View.deletePrivilege(privilege);
          });
        });
      });
    });
  };

  // Load privileges
  function loadPrivileges(){
    User.getPrivileges($routeParams.id).then(function(data) {
      var privileges = {
        clusters: {},
        views: {}
      };
      angular.forEach(data.data.items, function(privilege) {
        privilege = privilege.PrivilegeInfo;
        if(privilege.type === 'CLUSTER'){
          // This is cluster
          if (privileges.clusters[privilege.cluster_name]) {
            var preIndex = Cluster.orderedRoles.indexOf(privileges.clusters[privilege.cluster_name].permission_name);
            var curIndex = Cluster.orderedRoles.indexOf(privilege.permission_name);
            // replace when cur is a more powerful role
            if (curIndex < preIndex) {
              privileges.clusters[privilege.cluster_name] = privilege;
            }
          } else {
            privileges.clusters[privilege.cluster_name] = privilege;
          }
        } else if ( privilege.type === 'VIEW'){
          privileges.views[privilege.instance_name] = privileges.views[privilege.instance_name] || { privileges:[]};
          privileges.views[privilege.instance_name].version = privilege.version;
          privileges.views[privilege.instance_name].view_name = privilege.view_name;
          privileges.views[privilege.instance_name].privilege_id = privilege.privilege_id;
          if (privileges.views[privilege.instance_name].privileges.indexOf(privilege.permission_label) == -1) {
            privileges.views[privilege.instance_name].privileges.push(privilege.permission_label);
          }
        }
      });

      $scope.privileges = data.data.items.length ? privileges : null;
      $scope.noClusterPriv = $.isEmptyObject(privileges.clusters);
      $scope.noViewPriv = $.isEmptyObject(privileges.views);
      $scope.hidePrivileges = $scope.noClusterPriv && $scope.noViewPriv;
      $scope.dataLoaded = true;

    }).catch(function(data) {
      Alert.error($t('common.alerts.cannotLoadPrivileges'), data.data.message);
    });
  }
  loadPrivileges();
}]);
