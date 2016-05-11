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
.controller('ClustersManageAccessCtrl', ['$scope', '$location', 'Cluster', '$routeParams', 'Alert', 'PermissionLoader', 'PermissionSaver', '$translate', 'RoleDetailsModal', '$timeout', function($scope, $location, Cluster, $routeParams, Alert, PermissionLoader, PermissionSaver, $translate, RoleDetailsModal, $timeout) {
  var $t = $translate.instant;
  $scope.getConstant = function (key) {
    return $t('common.' + key).toLowerCase();
  };
  $scope.identity = angular.identity;
  function reloadClusterData(){
    PermissionLoader.getClusterPermissions({
      clusterId: $routeParams.id
    }).then(function(permissions) {
      // Refresh data for rendering
      $scope.permissionsEdit = permissions;
      $scope.permissions = angular.copy(permissions);
      //"$scope.isDataLoaded" should be set to true on initial load after "$scope.permissionsEdit" watcher
      $timeout(function() {
        $scope.isDataLoaded = true;
      });
      var orderedRoles = Cluster.orderedRoles;
      var pms = [];
      for (var key in orderedRoles) {
        pms.push($scope.permissions[orderedRoles[key]]);
      }
      $scope.permissions = pms;
    })
    .catch(function(data) {
      Alert.error($t('clusters.alerts.cannotLoadClusterData'), data.data.message);
    });
  }

  $scope.isDataLoaded = false;
  reloadClusterData();
  $scope.isEditMode = false;
  $scope.permissions = {};
  $scope.clusterName = $routeParams.id;


  $scope.toggleEditMode = function() {
    $scope.isEditMode = !$scope.isEditMode;
  };

  $scope.cancel = function() {
    $scope.isEditMode = false;
    $scope.permissionsEdit = angular.copy($scope.permissions); // Reset textedit areaes
  };

  $scope.save = function() {
    PermissionSaver.saveClusterPermissions(
      $scope.permissionsEdit,
      {
        clusterId: $routeParams.id
      }
    ).then(reloadClusterData)
    .catch(function(data) {
      Alert.error($t('common.alerts.cannotSavePermissions'), data.data.message);
      reloadClusterData();
    });
    $scope.isEditMode = false;
  };

  $scope.$watch(function() {
    return $scope.permissionsEdit;
  }, function(newValue) {
    if (newValue && $scope.isDataLoaded) {
      $scope.save();
    }
  }, true);

  $scope.switchToList = function() {
    $location.url('/clusters/' + $routeParams.id + '/userAccessList');
  };

  $scope.showHelpPage = function() {
    Cluster.getRolesWithAuthorizations().then(function(roles) {
      RoleDetailsModal.show(roles);
    });
  };
}]);
