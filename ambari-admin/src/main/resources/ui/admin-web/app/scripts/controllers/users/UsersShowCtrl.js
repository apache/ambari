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
.controller('UsersShowCtrl', ['$scope', '$routeParams', 'Cluster', 'User', '$modal', '$location', 'ConfirmationModal', 'Alert', 'Auth', 'getDifference', 'Group', '$q', '$translate', function($scope, $routeParams, Cluster, User, $modal, $location, ConfirmationModal, Alert, Auth, getDifference, Group, $q, $translate) {

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
        Alert.error($t('users.alert.cannotAddUser'), data.data.message);
      }));
    });
    $q.all(promises).then(function() {
      loadUserInfo();
    });
    $scope.isGroupEditing = false;
  };

  $scope.cancelUpdate = function() {
    $scope.isGroupEditing = false;
    $scope.editingGroupsList = '';
  };

  $scope.openChangePwdDialog = function() {
    var modalInstance = $modal.open({
      templateUrl: 'views/users/modals/changePassword.html',
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
        User.setActive($scope.user.user_name, $scope.user.active);
      })
      .catch(function() {
        $scope.user.active = !$scope.user.active;
      });
    }
  };    
  $scope.toggleUserAdmin = function() {
    if(!$scope.isCurrentUser){
      var action = $scope.user.admin ? 'revoke' : 'grant';
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
        });
      })
      .catch(function() {
        $scope.user.admin = !$scope.user.admin;
      });

    }
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
        var privilegesIds = [];
        var deleteCallback = function () {
          User.delete($scope.user.user_name).then(function() {
            $location.path('/users');
          });
        };
        if (data.items && data.items.length) {
          angular.forEach(data.items[0].privileges, function(privilege) {
            privilegesIds.push(privilege.PrivilegeInfo.privilege_id);
          });
        }
        if (privilegesIds.length) {
          Cluster.deleteMultiplePrivileges($routeParams.id, privilegesIds).then(function() {
            deleteCallback();
          });
        } else {
          deleteCallback();
        }
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
          privileges.clusters[privilege.cluster_name] = privileges.clusters[privilege.cluster_name] || [];
          privileges.clusters[privilege.cluster_name].push(privilege.permission_label);
        } else if ( privilege.type === 'VIEW'){
          privileges.views[privilege.instance_name] = privileges.views[privilege.instance_name] || { privileges:[]};
          privileges.views[privilege.instance_name].version = privilege.version;
          privileges.views[privilege.instance_name].view_name = privilege.view_name;
          privileges.views[privilege.instance_name].privileges.push(privilege.permission_label);

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
