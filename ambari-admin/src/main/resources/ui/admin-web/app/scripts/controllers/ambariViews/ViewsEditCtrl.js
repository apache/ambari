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
.controller('ViewsEditCtrl', ['$scope', '$routeParams' , 'View', '$http', function($scope, $routeParams, View, $http) {

  /*
    Perissions structure
  */
  $scope.permissions = [];
  View.getPermissions($routeParams.viewId, $routeParams.version)
  .then(function(permissionData) {
    $scope.permissions = permissionData.data.ViewVersionInfo.permissions;
    var permissionsObject = {};
    // Fill permissions with empty arrays
    angular.forEach(permissionData.data.ViewVersionInfo.permissions, function(permission) {
      var permissionLabel = permission.PermissionInfo.permission_name.replace('VIEW.', '');
      permissionsObject[permission.PermissionInfo.permission_name] = {
        name: permission.PermissionInfo.permission_name,
        label: permissionLabel,
        user: [],
        group: []
      };
    });

    // Load instance data, after View permissions meta loaded
    View.getInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId)
    .then(function(instance) {
      $scope.instance = instance;
      $scope.settings = {
        'visible': $scope.instance.ViewInstanceInfo.visible,
        'label': $scope.instance.ViewInstanceInfo.label
      };
      $scope.configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
      angular.forEach(instance.privileges, function(privilegie) {
        permissionsObject[privilegie.Privileges.permission_name][privilegie.Privileges.principal_type] = privilegie.Privileges.principal_name;
      });
    });

  });

    

  $scope.editSettingsDisabled = true;
  

  $scope.saveSettings = function() {
    View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, {
      'ViewInstanceInfo':{
        'visible': $scope.settings.visible,
        'label': $scope.settings.label
      }
    })
    .success(function() {
      $scope.editSettingsDisabled = true;
    });
  };
  $scope.cancelSettings = function() {
    $scope.settings = {
      'visible': $scope.instance.ViewInstanceInfo.visible,
      'label': $scope.instance.ViewInstanceInfo.label
    };
    $scope.editSettingsDisabled = true;
  };

  $scope.editConfigurationDisabled = true;

  $scope.saveConfiguration = function() {
    View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, {
      'ViewInstanceInfo':{
        'properties': $scope.configuration
      }
    })
    .success(function() {
      $scope.editConfigurationDisabled = true;
    });
  };
  $scope.cancelConfiguration = function() {
    $scope.configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
    $scope.editConfigurationDisabled = true;
  };

  

}]);