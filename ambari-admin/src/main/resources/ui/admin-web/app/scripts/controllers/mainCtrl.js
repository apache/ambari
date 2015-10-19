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
.controller('MainCtrl',['$scope','$rootScope','$window','Auth', 'Alert', '$modal', 'Cluster', 'View', function($scope, $rootScope, $window, Auth, Alert, $modal, Cluster, View) {
  $scope.signOut = function() {
    var data = JSON.parse(localStorage.ambari);
    delete data.app.authenticated;
    delete data.app.loginName;
    delete data.app.user;
    localStorage.ambari = JSON.stringify(data);
    $scope.hello = "hello";
    Auth.signout().finally(function() {
      $window.location.pathname = '';
    });
  };

  $scope.ambariVersion = null;

  $scope.about = function() {
   var ambariVersion = $scope.ambariVersion;
  	var modalInstance = $modal.open({
  		templateUrl:'views/modals/AboutModal.html',
  		controller: ['$scope', function($scope) {
  			$scope.ok = function() {
  				modalInstance.close();
  			};
        $scope.ambariVersion = ambariVersion;
  		}]
  	});
  };

  $scope.currentUser = Auth.getCurrentUser();

  $scope.cluster = null;
  $scope.isLoaded = null;

  function loadAmbariVersion() {
    Cluster.getAmbariVersion().then(function(version){
      $scope.ambariVersion = version;
    });
  }

  function loadClusterData(){
    Cluster.getStatus().then(function(cluster) {
      $scope.cluster = cluster;
      $scope.isLoaded = true;
      if(cluster && cluster.Clusters.provisioning_state === 'INIT'){
        setTimeout(loadClusterData, 1000);
      }
    }).catch(function(data) {
      Alert.error('Cannot load cluster status', data.statusText);
    });
  }
  loadClusterData();
  loadAmbariVersion();

  $scope.viewInstances = [];

  $scope.updateInstances = function () {
    View.getAllVisibleInstance().then(function(instances) {
      $scope.viewInstances = instances;
    });
  };

  $scope.gotoViewsDashboard =function() {
    window.location = '/#/main/views';
  };

  $scope.$root.$on('instancesUpdate', function (event, data) {
    $scope.updateInstances();
  });

  $scope.startInactiveTimeoutMonitoring = function(timeout) {
    var TIME_OUT = timeout;
    var active = true;
    var lastActiveTime = Date.now();

    var keepActive = function() {
      //console.error('keepActive');
      if (active) {
        lastActiveTime = Date.now();
      }
    };

    $(window).bind('mousemove', keepActive);
    $(window).bind('keypress', keepActive);
    $(window).bind('click', keepActive);

    var checkActiveness = function() {
      //console.error("checkActiveness " + lastActiveTime + " : " + Date.now());
      if (Date.now() - lastActiveTime > TIME_OUT) {
        //console.error("LOGOUT!");
        active = false;
        $(window).unbind('mousemove', keepActive);
        $(window).unbind('keypress', keepActive);
        $(window).unbind('click', keepActive);
        clearInterval($rootScope.userActivityTimeoutInterval);
        $scope.signOut();
      }
    };
    $rootScope.userActivityTimeoutInterval = window.setInterval(checkActiveness, 1000);
  };

  // Send noop requests every 10 seconds just to keep backend session alive
  $scope.startNoopPolling = function() {
    $rootScope.noopPollingInterval = setInterval(Cluster.getAmbariTimeout, 10000);
  };

  if (!$rootScope.userActivityTimeoutInterval) {
    Cluster.getAmbariTimeout().then(function(timeout) {
      if (Number(timeout) > 0)
        $scope.startInactiveTimeoutMonitoring(timeout * 1000);
    });
  }
  if (!$rootScope.noopPollingInterval) {
    $scope.startNoopPolling();
  }
  $scope.updateInstances();
}]);
