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
.controller('CreateViewInstanceCtrl',
['$scope', 'View','RemoteCluster' , 'Alert', 'Cluster', '$routeParams', '$location', 'UnsavedDialog', '$translate', '$modalInstance', 'views', '$q',
function($scope, View, RemoteCluster, Alert, Cluster, $routeParams, $location, UnsavedDialog, $translate, $modalInstance, views, $q) {

  var $t = $translate.instant;
  var viewToVersionMap = {};
  var instances = {};
  $scope.form = {};
  $scope.nameValidationPattern = /^\s*\w*\s*$/;
  $scope.isLoading = false;
  $scope.isLocalTypeChosen = true;
  $scope.views = views;
  $scope.viewOptions = [];
  $scope.versionOptions = [];
  $scope.localClusters = [];
  $scope.remoteClusters = [];
  $scope.clusterOptions = [];
  $scope.isInstanceExists = false;
  $scope.formData = {
    view: null,
    version: null,
    instanceName: '',
    displayName: '',
    description: '',
    clusterName: null,
    visible: true
  };

  $scope.updateVersionOptions = function () {
    if (viewToVersionMap[$scope.formData.view.value]) {
      $scope.versionOptions = viewToVersionMap[$scope.formData.view.value];
      $scope.formData.version = $scope.versionOptions[0];
    }
  };

  $scope.switchClusterType = function(bool) {
    $scope.isLocalTypeChosen = bool;
    if ($scope.isLocalTypeChosen) {
      $scope.clusterOptions = $scope.localClusters;
    } else {
      $scope.clusterOptions = $scope.remoteClusters;
    }
    $scope.formData.clusterName = $scope.clusterOptions[0];
  };

  $scope.save = function () {
    var instanceName = $scope.form.instanceCreateForm.instanceName.$viewValue;
    $scope.form.instanceCreateForm.submitted = true;
    if ($scope.form.instanceCreateForm.$valid) {
      View.createInstance({
        instance_name: instanceName,
        label: $scope.form.instanceCreateForm.displayName.$viewValue,
        visible: $scope.form.instanceCreateForm.visible.$viewValue,
        icon_path: '',
        icon64_path: '',
        description: $scope.form.instanceCreateForm.description.$viewValue,
        view_name: $scope.form.instanceCreateForm.view.$viewValue.value,
        version: $scope.form.instanceCreateForm.version.$viewValue.value,
        properties: [],
        clusterId: $scope.form.instanceCreateForm.clusterName.$viewValue.id,
        clusterType: $scope.isLocalTypeChosen ? 'LOCAL_AMBARI': 'REMOTE_AMBARI'
      })
        .then(function () {
          $modalInstance.dismiss('created');
          Alert.success($t('views.alerts.instanceCreated', {instanceName: instanceName}));
          $location.path('/views/' + $scope.form.instanceCreateForm.view.$viewValue.value +
            '/versions/' + $scope.form.instanceCreateForm.version.$viewValue.value +
            '/instances/' + instanceName + '/edit');
        })
        .catch(function (data) {
          var errorMessage = data.message;

          if (data.status >= 400) {
            try {
              errorMessage = JSON.parse(errorMessage).detail;
            } catch (e) {
              console.warn(data.message, e);
            }
          }
          Alert.error($t('views.alerts.cannotCreateInstance'), errorMessage);
        });
    }
  };

  $scope.cancel = function () {
    unsavedChangesCheck();
  };

  $scope.checkIfInstanceExist = function() {
    $scope.isInstanceExists = Boolean(instances[$scope.formData.instanceName]);
  };

  function initViewAndVersionSelect () {
    $scope.viewOptions = [];
    angular.forEach($scope.views, function(view) {
      $scope.viewOptions.push({
        label: view.view_name,
        value: view.view_name
      });
      viewToVersionMap[view.view_name] = view.versionsList.map(function(version) {
        angular.forEach(version.instances, function(instance) {
          instances[instance.ViewInstanceInfo.instance_name] = true;
        });
        return {
          label: version.ViewVersionInfo.version,
          value: version.ViewVersionInfo.version
        }
      });
    });
    $scope.formData.view = $scope.viewOptions[0];
    $scope.updateVersionOptions();
  }

  function loadClusters() {
    return Cluster.getAllClusters().then(function (clusters) {
      clusters.forEach(function (cluster) {
        $scope.localClusters.push({
          label: cluster.Clusters.cluster_name,
          value: cluster.Clusters.cluster_name,
          id: cluster.Clusters.cluster_id
        });
      });
    });
  }

  function loadRemoteClusters() {
    return RemoteCluster.listAll().then(function (clusters) {
      clusters.forEach(function (cluster) {
        $scope.remoteClusters.push({
          label: cluster.ClusterInfo.name,
          value: cluster.ClusterInfo.name,
          id: cluster.ClusterInfo.cluster_id
        });
      });
    });
  }

  function loadFormData () {
    $scope.isLoading = true;
    initViewAndVersionSelect();
    $q.all(loadClusters(), loadRemoteClusters()).then(function() {
      $scope.isLoading = false;
      $scope.switchClusterType(true);
    });
  }

  function unsavedChangesCheck() {
    if ($scope.form.instanceCreateForm.$dirty) {
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

  loadFormData();
}]);
