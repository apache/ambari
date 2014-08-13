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
.controller('NavbarCtrl',['$scope', 'Cluster', '$location', 'uiAlert', 'ROUTES', 'LDAP', 'ConfirmationModal', function($scope, Cluster, $location, uiAlert, ROUTES, LDAP, ConfirmationModal) {
  $scope.cluster = null;
  Cluster.getStatus().then(function(cluster) {
    $scope.cluster = cluster;
  }).catch(function(data) {
  	uiAlert.danger(data.status, data.message);
  });

  $scope.isActive = function(path) {
  	var route = ROUTES;
  	angular.forEach(path.split('.'), function(routeObj) {
  		route = route[routeObj];
  	});
  	var r = new RegExp( route.url.replace(/(:\w+)/, '\\w+'));
  	return r.test($location.path());
  };

  $scope.isLDAPConfigured = false;
  $scope.ldapData = {};
  LDAP.get().then(function(data) {
    $scope.ldapData = data.data;
    $scope.isLDAPConfigured = data.data['LDAP']['configured'];
  });

  $scope.syncLDAP = function() {
    ConfirmationModal.show('Sync LDAP', 'Are you sure you want to sync LDAP?').then(function() {
      LDAP.sync($scope.ldapData['LDAP'].groups, $scope.ldapData['LDAP'].users).then(function() {
        uiAlert.success('LDAP synced successful');
      }).catch(function(data) {
        uiAlert.danger(data.data.status, data.data.message);
      });
    });
  };
}]);