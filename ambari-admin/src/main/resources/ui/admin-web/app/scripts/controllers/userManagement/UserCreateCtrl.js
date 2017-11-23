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
.controller('UserCreateCtrl',
['$scope', '$rootScope', 'User', '$location', 'Alert', 'UnsavedDialog', '$translate', 'Cluster', '$modalInstance',
function($scope, $rootScope, User, $location, Alert, UnsavedDialog, $translate, Cluster, $modalInstance) {
  var $t = $translate.instant;

  $scope.form = {};
  $scope.formData = {
    userName: '',
    password: '',
    confirmPassword: '',
    role: null,
    isAdmin: false,
    isActive: true
  };
  $scope.roleOptions = [];

  function loadRoles() {
    Cluster.getPermissions().then(function(data) {
      $scope.roleOptions = data.map(function(item) {
        return item.PermissionInfo;
      });
    });
  }

  function unsavedChangesCheck() {
    if ($scope.form.userCreateForm.$dirty) {
      UnsavedDialog().then(function (action) {
        switch (action) {
          case 'save':
            $scope.save();
            break;
          case 'discard':
            $modalInstance.close('discard');
            break;
          case 'cancel':
            break;
        }
      });
    } else {
      $modalInstance.close('discard');
    }
  }

  $scope.save = function () {
    $scope.form.userCreateForm.submitted = true;
    if ($scope.form.userCreateForm.$valid) {
      User.create({
        'Users/user_name': $scope.formData.userName,
        'Users/password': $scope.formData.password,
        'Users/active': Boolean($scope.formData.isActive),
        'Users/admin': Boolean($scope.formData.isAdmin)
      }).then(function () {
        saveRole();
        $modalInstance.dismiss('created');
        Alert.success($t('users.alerts.userCreated', {
          userName: $scope.formData.userName,
          encUserName: encodeURIComponent($scope.formData.userName)
        }));
      }).catch(function (data) {
        Alert.error($t('users.alerts.userCreationError'), data.data.message);
      });
    }
  };

  function saveRole() {
    Cluster.createPrivileges(
      {
        clusterId: $rootScope.cluster.Clusters.cluster_name
      },
      [{PrivilegeInfo: {
        permission_name: $scope.roleOptions.filter(function(role) {
          return role.permission_id == Number($scope.formData.role);
        })[0].permission_name,
        principal_name: $scope.formData.userName,
        principal_type: 'USER'
      }}]
    )
    .catch(function(data) {
      Alert.error($t('common.alerts.cannotSavePermissions'), data.data.message);
    });
  }

  $scope.cancel = function () {
    unsavedChangesCheck();
  };

  loadRoles();
}]);
