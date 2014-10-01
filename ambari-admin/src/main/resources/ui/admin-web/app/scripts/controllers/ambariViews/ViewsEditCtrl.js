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
.controller('ViewsEditCtrl', ['$scope', '$routeParams' , 'View', 'Alert', 'PermissionLoader', 'PermissionSaver', 'ConfirmationModal', '$location', function($scope, $routeParams, View, Alert, PermissionLoader, PermissionSaver, ConfirmationModal, $location) {
  $scope.identity = angular.identity;
  $scope.isConfigurationEmpty = true;
  function reloadViewInfo(){
    // Load instance data, after View permissions meta loaded
    View.getInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId)
    .then(function(instance) {
      $scope.instance = instance;

      $scope.settings = {
        'visible': $scope.instance.ViewInstanceInfo.visible,
        'label': $scope.instance.ViewInstanceInfo.label,
        'description': $scope.instance.ViewInstanceInfo.description
      };

      $scope.configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
      for(var confName in $scope.configuration){
        if( $scope.configuration.hasOwnProperty(confName) ){
          $scope.configuration[confName] = $scope.configuration[confName] === 'null' ? '' : $scope.configuration[confName];
        }
      }
      $scope.isConfigurationEmpty = angular.equals({}, $scope.configuration);
    })
    .catch(function(data) {
      Alert.error('Cannot load instance info', data.data.message);
    });
  }


  // Get META for properties
  View.getMeta($routeParams.viewId, $routeParams.version).then(function(data) {
    $scope.configurationMeta = data.data.ViewVersionInfo.parameters;
    reloadViewInfo();
  });

  function reloadViewPrivileges(){
    PermissionLoader.getViewPermissions({
      viewName: $routeParams.viewId,
      version: $routeParams.version,
      instanceId: $routeParams.instanceId
    })
    .then(function(permissions) {
      // Refresh data for rendering
      $scope.permissionsEdit = permissions;
      $scope.permissions = angular.copy(permissions);
      $scope.isPermissionsEmpty = angular.equals({}, $scope.permissions);
    })
    .catch(function(data) {
      Alert.error('Cannot load permissions', data.data.message);
    });
  }

  $scope.permissions = [];
  
  reloadViewPrivileges();

  $scope.editSettingsDisabled = true;
  $scope.toggleSettingsEdit = function() {
    $scope.editSettingsDisabled = !$scope.editSettingsDisabled;
  };

  $scope.saveSettings = function() {
    if( $scope.settingsForm.$valid ){
      View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, {
        'ViewInstanceInfo':{
          'visible': $scope.settings.visible,
          'label': $scope.settings.label,
          'description': $scope.settings.description
        }
      })
      .success(function() {
        reloadViewInfo();
        $scope.editSettingsDisabled = true;
      })
      .catch(function(data) {
        Alert.error('Cannot save settings', data.data.message);
      });
    }
  };
  $scope.cancelSettings = function() {
    $scope.settings = {
      'visible': $scope.instance.ViewInstanceInfo.visible,
      'label': $scope.instance.ViewInstanceInfo.label,
      'description': $scope.instance.ViewInstanceInfo.description
    };
    $scope.editSettingsDisabled = true;
  };

  
  $scope.editConfigurationDisabled = true;
  $scope.togglePropertiesEditing = function() {
     $scope.editConfigurationDisabled = !$scope.editConfigurationDisabled;
  }
  $scope.saveConfiguration = function() {
    if( $scope.propertiesForm.$valid ){
      View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, {
        'ViewInstanceInfo':{
          'properties': $scope.configuration
        }
      })
      .success(function() {
        $scope.editConfigurationDisabled = true;
      })
      .catch(function(data) {
        Alert.error('Cannot save properties', data.data.message);
      });
    }
  };
  $scope.cancelConfiguration = function() {
    $scope.configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
    $scope.editConfigurationDisabled = true;
  };

  // Permissions edit
  $scope.editPermissionDisabled = true;
  $scope.cancelPermissions = function() {
    $scope.permissionsEdit = angular.copy($scope.permissions); // Reset textedit areaes
    $scope.editPermissionDisabled = true;
  };

  $scope.savePermissions = function() {
    PermissionSaver.saveViewPermissions(
      $scope.permissionsEdit,
      {
        view_name: $routeParams.viewId,
        version: $routeParams.version,
        instance_name: $routeParams.instanceId,
      }
    )
    .then(reloadViewPrivileges)
    .catch(function(data) {
      reloadViewPrivileges();
      Alert.error('Cannot save permissions', data.data.message);
    });
    $scope.editPermissionDisabled = true;
  };

  $scope.$watch(function() {
    return $scope.permissionsEdit;
  }, function(newValue) {
    if(newValue){
      $scope.savePermissions();
    }
  }, true);  

  $scope.deleteInstance = function(instance) {
    ConfirmationModal.show('Delete View Instance', 'Are you sure you want to delete View Instance '+ instance.ViewInstanceInfo.label +'?').then(function() {
      View.deleteInstance(instance.ViewInstanceInfo.view_name, instance.ViewInstanceInfo.version, instance.ViewInstanceInfo.instance_name)
      .then(function() {
        $location.path('/views');
      })
      .catch(function(data) {
        Alert.error('Cannot delete instance', data.data.message);
      });
    });
  };
}]);
