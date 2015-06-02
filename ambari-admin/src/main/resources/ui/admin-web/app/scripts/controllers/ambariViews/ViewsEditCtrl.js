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
.controller('ViewsEditCtrl', ['$scope', '$routeParams' , 'Cluster', 'View', 'Alert', 'PermissionLoader', 'PermissionSaver', 'ConfirmationModal', '$location', 'UnsavedDialog', function($scope, $routeParams, Cluster, View, Alert, PermissionLoader, PermissionSaver, ConfirmationModal, $location, UnsavedDialog) {
  $scope.identity = angular.identity;
  $scope.isConfigurationEmpty = true;
  $scope.isSettingsEmpty = true;

  function reloadViewInfo(section){
    // Load instance data, after View permissions meta loaded
    View.getInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId)
    .then(function(instance) {
      $scope.instance = instance;
      $scope.settings = {
        'visible': $scope.instance.ViewInstanceInfo.visible,
        'label': $scope.instance.ViewInstanceInfo.label,
        'description': $scope.instance.ViewInstanceInfo.description
      };
      switch (section) {
        case "details" :
          initConfigurations();
          initCtrlVariables(instance);
          break;
        case "settings" :
          initConfigurations(true);
          break;
        case "cluster" :
          initCtrlVariables(instance);
          break;
      }
    })
    .catch(function(data) {
      Alert.error('Cannot load instance info', data.data.message);
    });
  }

  function initCtrlVariables(instance) {
    if(instance.ViewInstanceInfo.cluster_handle) {
      $scope.isLocalCluster = true;
      $scope.cluster = instance.ViewInstanceInfo.cluster_handle;
    }else{
      $scope.isLocalCluster = false;
      $scope.cluster = $scope.clusters.length > 0 ? $scope.clusters[0] : "No Clusters";
    }
    $scope.originalLocalCluster = $scope.isLocalCluster;
    $scope.isConfigurationEmpty = !$scope.numberOfClusterConfigs;
    $scope.isSettingsEmpty = !$scope.numberOfSettingsConfigs;
  }

  function isClusterConfig(name) {
    var configurationMeta = $scope.configurationMeta;
    var clusterConfigs = configurationMeta.filter(function(el) {
      return el.clusterConfig;
    }).map(function(el) {
      return el.name;
    });
    return clusterConfigs.indexOf(name) !== -1;
  }

  function initConfigurations(initClusterConfig) {
    var initAllConfigs = !initClusterConfig;
    var configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
    if (initAllConfigs) {
      $scope.configuration = angular.copy($scope.instance.ViewInstanceInfo.properties);
    }
    for (var confName in configuration) {
      if (configuration.hasOwnProperty(confName)) {
        if (!isClusterConfig(confName) || initAllConfigs) {
          $scope.configuration[confName] = configuration[confName] === 'null' ? '' : configuration[confName];
        }
      }
    }
  }

  function filterClusterConfigs() {
    $scope.configurationMeta.forEach(function (element) {
      if (element.masked && !$scope.editConfigurationDisabled && element.clusterConfig && !$scope.isLocalCluster) {
        $scope.configuration[element.name] = '';
      }
      if(!element.clusterConfig) {
        delete $scope.configurationBeforeEdit[element.name];
      }
    });
  }

  // Get META for properties
  View.getMeta($routeParams.viewId, $routeParams.version).then(function(data) {
    $scope.configurationMeta = data.data.ViewVersionInfo.parameters;
    $scope.clusterConfigurable = data.data.ViewVersionInfo.cluster_configurable;
    $scope.clusterConfigurableErrorMsg = $scope.clusterConfigurable ? "" : "This view cannot use this option";
    angular.forEach($scope.configurationMeta, function (item) {
      item.displayName = item.name.replace(/\./g, '\.\u200B');
      item.clusterConfig = !!item.clusterConfig;
      if (!item.clusterConfig) {
        $scope.numberOfSettingsConfigs++;
      }
      $scope.numberOfClusterConfigs = $scope.numberOfClusterConfigs + !!item.clusterConfig;
    });
    reloadViewInfo("details");
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

  $scope.clusterConfigurable = false;
  $scope.clusterConfigurableErrorMsg = "";
  $scope.clusters = [];
  $scope.cluster = null;
  $scope.noClusterAvailible = true;


  $scope.editSettingsDisabled = true;
  $scope.editDetailsSettingsDisabled = true;
  $scope.numberOfClusterConfigs = 0;
  $scope.numberOfSettingsConfigs = 0;

  $scope.enableLocalCluster = function() {
    angular.extend($scope.configuration, $scope.configurationBeforeEdit);
    $scope.propertiesForm.$setPristine();
  };

  $scope.disableLocalCluster = function() {
    filterClusterConfigs();
  };

  $scope.toggleSettingsEdit = function() {
    $scope.editSettingsDisabled = !$scope.editSettingsDisabled;
    $scope.settingsBeforeEdit = angular.copy($scope.configuration);
    $scope.configurationMeta.forEach(function (element) {
      if (element.masked && !$scope.editSettingsDisabled && !element.clusterConfig) {
        $scope.configuration[element.name] = '';
      }
      if(element.clusterConfig) {
        delete $scope.settingsBeforeEdit[element.name];
      }
    });
  };

  $scope.toggleDetailsSettingsEdit = function() {
    $scope.editDetailsSettingsDisabled = !$scope.editDetailsSettingsDisabled;
    $scope.settingsBeforeEdit = angular.copy($scope.configuration);
    $scope.configurationMeta.forEach(function (element) {
      if (element.masked && !$scope.editDetailsSettingsDisabled && !element.clusterConfig) {
        $scope.configuration[element.name] = '';
      }
      if(element.clusterConfig) {
        delete $scope.settingsBeforeEdit[element.name];
      }
    });
  };

  Cluster.getAllClusters().then(function (clusters) {
    if(clusters.length >0){
      clusters.forEach(function(cluster) {
        $scope.clusters.push(cluster.Clusters.cluster_name)
      });
      $scope.noClusterAvailible = false;
    }else{
      $scope.clusters.push("No Clusters");
    }
    $scope.cluster = $scope.clusters[0];
  });


  $scope.saveSettings = function(callback) {
    if( $scope.settingsForm.$valid ){
      var data = {
        'ViewInstanceInfo':{
          'properties':{}
        }
      };
      $scope.configurationMeta.forEach(function (element) {
        if(!element.clusterConfig) {
          data.ViewInstanceInfo.properties[element.name] = $scope.configuration[element.name];
        }
      });
      return View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, data)
      .success(function() {
        if( callback ){
          callback();
        } else {
          reloadViewInfo("settings");
          $scope.editSettingsDisabled = true;
          $scope.settingsForm.$setPristine();
        }
      })
      .catch(function(data) {
        Alert.error('Cannot save settings', data.data.message);
      });
    }
  };
  $scope.cancelSettings = function() {
    angular.extend($scope.configuration, $scope.settingsBeforeEdit);

    $scope.editSettingsDisabled = true;
    $scope.settingsForm.$setPristine();
  };

  $scope.saveDetails = function(callback) {
    if( $scope.detailsForm.$valid ){
      var data = {
        'ViewInstanceInfo':{
          'visible': $scope.settings.visible,
          'label': $scope.settings.label,
          'description': $scope.settings.description
        }
      };
      return View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, data)
      .success(function() {
        $scope.$root.$emit('instancesUpdate');
        if( callback ){
          callback();
        } else {
          reloadViewInfo("cluster");
          $scope.editDetailsSettingsDisabled = true;
          $scope.settingsForm.$setPristine();
        }
      })
      .catch(function(data) {
        Alert.error('Cannot save settings', data.data.message);
      });
    }
  };
  $scope.cancelDetails = function() {
    $scope.settings = {
      'visible': $scope.instance.ViewInstanceInfo.visible,
      'label': $scope.instance.ViewInstanceInfo.label,
      'description': $scope.instance.ViewInstanceInfo.description
    };
    $scope.editDetailsSettingsDisabled = true;
    $scope.settingsForm.$setPristine();
  };


  $scope.editConfigurationDisabled = true;
  $scope.togglePropertiesEditing = function () {
    $scope.editConfigurationDisabled = !$scope.editConfigurationDisabled;
    $scope.configurationBeforeEdit = angular.copy($scope.configuration);
    filterClusterConfigs();
  };
  $scope.saveConfiguration = function() {
    if( $scope.propertiesForm.$valid ){
      var data = {
        'ViewInstanceInfo':{
          'properties':{}
        }
      };
      if($scope.isLocalCluster) {
        data.ViewInstanceInfo.cluster_handle = $scope.cluster;
      } else {
        data.ViewInstanceInfo.cluster_handle = null;
        $scope.configurationMeta.forEach(function (element) {
          if(element.clusterConfig) {
            data.ViewInstanceInfo.properties[element.name] = $scope.configuration[element.name];
          }
        });
      }
      $scope.originalLocalCluster = $scope.isLocalCluster;
      return View.updateInstance($routeParams.viewId, $routeParams.version, $routeParams.instanceId, data)
      .success(function() {
        $scope.editConfigurationDisabled = true;
        $scope.propertiesForm.$setPristine();
      })
      .catch(function(data) {
        var errorMessage = data.data.message;

        //TODO: maybe the BackEnd should sanitize the string beforehand?
        errorMessage = errorMessage.substr(errorMessage.indexOf("\{"));

        if (data.status >= 400 && !$scope.isLocalCluster) {
          try {
            var errorObject = JSON.parse(errorMessage);
            errorMessage = errorObject.detail;
            angular.forEach(errorObject.propertyResults, function (item, key) {
              $scope.propertiesForm[key].validationError = !item.valid;
              if (!item.valid) {
                $scope.propertiesForm[key].validationMessage = item.detail;
              }
            });
          } catch (e) {
            console.error('Unable to parse error message:', data.message);
          }
        }
        Alert.error('Cannot save properties', errorMessage);
      });
    }
  };
  $scope.cancelConfiguration = function() {
    angular.extend($scope.configuration, $scope.configurationBeforeEdit);
    $scope.isLocalCluster = $scope.originalLocalCluster;
    $scope.editConfigurationDisabled = true;
    $scope.propertiesForm.$setPristine();
  };

  // Permissions edit
  $scope.editPermissionDisabled = true;
  $scope.cancelPermissions = function() {
    $scope.permissionsEdit = angular.copy($scope.permissions); // Reset textedit areaes
    $scope.editPermissionDisabled = true;
  };

  $scope.savePermissions = function() {
    $scope.editPermissionDisabled = true;
    return PermissionSaver.saveViewPermissions(
      $scope.permissionsEdit,
      {
        view_name: $routeParams.viewId,
        version: $routeParams.version,
        instance_name: $routeParams.instanceId
      }
    )
    .then(reloadViewPrivileges)
    .catch(function(data) {
      reloadViewPrivileges();
      Alert.error('Cannot save permissions', data.data.message);
    });
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

  $scope.$on('$locationChangeStart', function(event, targetUrl) {
    if( $scope.settingsForm.$dirty || $scope.propertiesForm.$dirty){
      UnsavedDialog().then(function(action) {
        targetUrl = targetUrl.split('#').pop();
        switch(action){
          case 'save':
            if($scope.settingsForm.$valid &&  $scope.propertiesForm.$valid ){
              $scope.saveSettings(function() {
                $scope.saveConfiguration().then(function() {
                  $scope.propertiesForm.$setPristine();
                  $scope.settingsForm.$setPristine();
                  $location.path(targetUrl);
                });
              });
            }
            break;
          case 'discard':
            $scope.propertiesForm.$setPristine();
            $scope.settingsForm.$setPristine();
            $location.path(targetUrl);
            break;
          case 'cancel':
            targetUrl = '';
            break;
        }
      });
      event.preventDefault();
    }
  });
}]);
