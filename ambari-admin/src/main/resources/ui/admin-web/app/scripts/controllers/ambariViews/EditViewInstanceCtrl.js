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
.controller('EditViewInstanceCtrl', ['$scope', 'View', 'uiAlert', 'PermissionLoader', 'PermissionSaver', 'instance', '$modalInstance', '$modal', function($scope, View, uiAlert, PermissionLoader, PermissionSaver, instance, $modalInstance, $modal) {

  $scope.instance = instance;
  $scope.settings = {
    'visible': $scope.instance.ViewInstanceInfo.visible,
    'label': $scope.instance.ViewInstanceInfo.label
  };
  $scope.configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
  
  function reloadViewPrivilegies(){
    PermissionLoader.getViewPermissions({
      viewName: $scope.instance.ViewInstanceInfo.view_name,
      version: $scope.instance.ViewInstanceInfo.version,
      instanceId: $scope.instance.ViewInstanceInfo.instance_name
    })
    .then(function(permissions) {
      // Refresh data for rendering
      $scope.permissionsEdit = permissions;
      $scope.permissions = angular.copy(permissions);
    })
    .catch(function(data) {
      uiAlert.danger(data.data.status, data.data.message);
    });
  }

  $scope.permissions = [];
  
  reloadViewPrivilegies();

  $scope.edit = {};
  $scope.edit.editSettingsDisabled = true;
  

  $scope.saveSettings = function() {
    View.updateInstance($scope.instance.ViewInstanceInfo.view_name, $scope.instance.ViewInstanceInfo.version, $scope.instance.ViewInstanceInfo.instance_name, {
      'ViewInstanceInfo':{
        'visible': $scope.settings.visible,
        'label': $scope.settings.label
      }
    })
    .success(function() {
      $scope.edit.editSettingsDisabled = true;
    })
    .catch(function(data) {
      uiAlert.danger(data.data.status, data.data.message);
    });
  };

  $scope.cancelSettings = function() {
    $scope.settings = {
      'visible': $scope.instance.ViewInstanceInfo.visible,
      'label': $scope.instance.ViewInstanceInfo.label
    };
    $scope.edit.editSettingsDisabled = true;
  };

  $scope.edit.editConfigurationDisabled = true;

  $scope.saveConfiguration = function() {
    View.updateInstance($scope.instance.ViewInstanceInfo.view_name, $scope.instance.ViewInstanceInfo.version, $scope.instance.ViewInstanceInfo.instance_name, {
      'ViewInstanceInfo':{
        'properties': $scope.configuration
      }
    })
    .success(function() {
      $scope.edit.editConfigurationDisabled = true;
    })
    .catch(function(data) {
      uiAlert.danger(data.data.status, data.data.message);
    });
  };
  $scope.cancelConfiguration = function() {
    $scope.configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
    $scope.edit.editConfigurationDisabled = true;
  };

  // Permissions edit
  $scope.edit.editPermissionDisabled = true;
  $scope.cancelPermissions = function() {
    $scope.permissionsEdit = angular.copy($scope.permissions); // Reset textedit areaes
    $scope.edit.editPermissionDisabled = true;
  };

  $scope.savePermissions = function() {
    PermissionSaver.saveViewPermissions(
      $scope.permissions,
      $scope.permissionsEdit,
      {
        view_name: $scope.instance.ViewInstanceInfo.view_name,
        version: $scope.instance.ViewInstanceInfo.version,
        instance_name: $scope.instance.ViewInstanceInfo.instance_name
      }
    )
    .then(reloadViewPrivilegies)
    .catch(function(data) {
      reloadViewPrivilegies();
      uiAlert.danger(data.data.status, data.data.message);
    });
    $scope.edit.editPermissionDisabled = true;
  };

  $scope.removePermission = function(permissionName, principalType, principalName) {
    var modalInstance = $modal.open({
      templateUrl: 'views/ambariViews/modals/create.html',
      size: 'lg',
      controller: 'CreateViewInstanceCtrl',
      resolve: {
        viewVersion: function(){
          return '';
        }
      }
    });



    View.deletePrivilege({
      view_name: $scope.instance.ViewInstanceInfo.view_name,
      version: $scope.instance.ViewInstanceInfo.version,
      instance_name: $scope.instance.ViewInstanceInfo.instance_name,
      permissionName: permissionName,
      principalType: principalType,
      principalName: principalName
    })
    .then(reloadViewPrivilegies)
    .catch(function(data) {
      reloadViewPrivilegies();
      uiAlert.danger(data.data.status, data.data.message);
    });
  };

  $scope.close = function() {
    $modalInstance.close();
  };
}]);