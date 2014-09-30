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
.controller('MainCtrl',['$scope', 'Auth', 'uiAlert', '$modal', 'Cluster', function($scope, Auth, uiAlert, $modal, Cluster) {
  $scope.signOut = function() {
    Auth.signout().then(function() {
     window.location.pathname = ''; // Change location hard, because Angular works only with relative urls
    }).catch(function(data) {
      uiAlert.danger(data.data.status, data.data.message);
    });
  };

  $scope.about = function() {
  	var modalInstance = $modal.open({
  		templateUrl:'views/modals/AboutModal.html',
  		controller: ['$scope', function($scope) {
  			$scope.ok = function() {
  				modalInstance.close();
  			};
  		}]
  	});
  };

  $scope.currentUser = Auth.getCurrentUser();

  $scope.cluster = null;
  $scope.isLoaded = null;

  Cluster.getStatus().then(function(cluster) {
    $scope.cluster = cluster;
    $scope.isLoaded = true;
  }).catch(function(data) {
      uiAlert.danger(data.status, data.message);
  });

}]);
