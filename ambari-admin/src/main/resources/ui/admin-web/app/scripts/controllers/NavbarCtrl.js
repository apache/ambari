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
.controller('NavbarCtrl',['$scope', 'Cluster', '$location', 'Alert', 'ROUTES', 'ConfirmationModal', '$rootScope', 'Stack', '$translate', 'Settings', function($scope, Cluster, $location, Alert, ROUTES, ConfirmationModal, $rootScope, Stack, $translate, Settings) {
  var $t = $translate.instant;
  $scope.cluster = null;
  $scope.totalRepos = 0;
  $scope.editCluster = {
    name        : '',
    editingName : false
  };
  $scope.settings = Settings;

  function loadClusterData() {
    Cluster.getStatus().then(function (cluster) {
      $scope.cluster = cluster;
      Stack.allRepos({version: '',
        cluster: {
          options: [],
          current: null
        }}, {}).then(function (repos) {
          $scope.totalRepos = repos.itemTotal;
        });
      if (cluster && cluster.Clusters.provisioning_state === 'INIT') {
        setTimeout(loadClusterData, 1000);
      }
    }).catch(function (data) {
      Alert.error($t('common.alerts.cannotLoadClusterStatus'), data.statusText);
    });
  }
  loadClusterData();

  $scope.toggleEditName = function($event) {
    if ($event && $event.keyCode !== 27) {
      // 27 = Escape key
      return false;
    }

    $scope.editCluster.name         = $scope.cluster.Clusters.cluster_name;
    $scope.editCluster.editingName  = !$scope.editCluster.editingName;
  };

  $scope.clusterDisplayName = function () {
    var name="";
    if($scope.cluster && $scope.cluster.Clusters)
    {
       name = $scope.cluster.Clusters.cluster_name;
    }
    return name.length > 13 ? name.substr(0, 13) + "..." : name;
  };

  $scope.confirmClusterNameChange = function() {
    ConfirmationModal.show(
      $t('common.clusterNameChangeConfirmation.title'),
      $t('common.clusterNameChangeConfirmation.message', {
        clusterName: $scope.editCluster.name
      })
    )
      .then(function() {
        $scope.saveClusterName();
      }).catch(function() {
        // user clicked cancel
        $scope.toggleEditName();
      });
  };

  $scope.saveClusterName = function() {
    var oldClusterName = $scope.cluster.Clusters.cluster_name,
        newClusterName = $scope.editCluster.name;

    Cluster.editName(oldClusterName, newClusterName).then(function(data) {
      $scope.cluster.Clusters.cluster_name = newClusterName;
      Alert.success($t('common.alerts.clusterRenamed', {clusterName: newClusterName}));
    }).catch(function(data) {
      Alert.error($t('common.alerts.cannotRenameCluster', {clusterName: newClusterName}), data.data.message);
    });

    $scope.toggleEditName();
  };

  $scope.isActive = function(path) {
  	var route = ROUTES;
  	angular.forEach(path.split('.'), function(routeObj) {
  		route = route[routeObj];
  	});
  	var r = new RegExp( route.url.replace(/(:\w+)/, '\\w+'));
  	return r.test($location.path());
  };
}]);
