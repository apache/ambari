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
.controller('GroupCreateCtrl',
['$scope', '$rootScope', 'Group', '$location', 'Alert', 'UnsavedDialog', '$translate', '$modalInstance', 'Cluster',
function($scope, $rootScope, Group, $location, Alert, UnsavedDialog, $translate, $modalInstance, Cluster) {
  var $t = $translate.instant;

  $scope.form = {};
  $scope.formData = {
    groupName: '',
    members: [],
    role: ''
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
    if ($scope.form.groupCreateForm.$dirty) {
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

  function saveMembers(group, members) {
    group.members = members.filter(function(item) {
      return item.trim();
    }).map(function(item) {
      return item.trim();
    });
    group.saveMembers().catch(function(data) {
      Alert.error($t('groups.alerts.cannotUpdateGroupMembers'), "<div class='break-word'>" + data.message + "</div>");
    });
  }

  $scope.save = function () {
    $scope.form.groupCreateForm.submitted = true;
    if ($scope.form.groupCreateForm.$valid) {
      var group = new Group($scope.formData.groupName);
      group.save().then(function () {
        saveMembers(group, $scope.formData.members);
        saveRole();
        $modalInstance.dismiss('created');
        Alert.success($t('groups.alerts.groupCreated', {groupName: $scope.formData.groupName}));
      })
      .catch(function (data) {
        Alert.error($t('groups.alerts.groupCreationError'), data.data.message);
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
        principal_name: $scope.formData.groupName,
        principal_type: 'GROUP'
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