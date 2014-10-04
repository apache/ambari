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
.controller('ClustersManageAccessCtrl', ['$scope', 'Cluster', '$routeParams', 'Alert', 'PermissionLoader', 'PermissionSaver', function($scope, Cluster, $routeParams, Alert, PermissionLoader, PermissionSaver) {
  $scope.identity = angular.identity;
  function reloadClusterData(){
    PermissionLoader.getClusterPermissions({
      clusterId: $routeParams.id
    }).then(function(permissions) {
      // Refresh data for rendering
      $scope.permissionsEdit = permissions;
      $scope.permissions = angular.copy(permissions);
    })
    .catch(function(data) {
      Alert.error('Cannot load cluster data', data.data.message);
    });;
  }
 
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
      Alert.error('Cannot save permissions', data.data.message);
      reloadClusterData();
    });
    $scope.isEditMode = false;
  };

  $scope.$watch(function() {
    return $scope.permissionsEdit;
  }, function(newValue) {
    if(newValue){
      $scope.save();
    }
  }, true);
}]);