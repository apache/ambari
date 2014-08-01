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
.controller('LDAPModalCtrl',['$scope', 'LDAP', 'resourceType', '$modalInstance', function($scope, LDAP, resourceType, $modalInstance) {
  $scope.resourceType = resourceType;
  $scope.isConfigured = false;
  $scope.isDataLoaded = false;

  $scope.items = [
  ];


  LDAP.get(resourceType).then(function(data) {
    $scope.isConfigured = data['LDAP']['configured'];
    $scope.items = data['LDAP'][resourceType];
    $scope.isDataLoaded = true;
  });

  $scope.sync = function() {
    if($scope.items){
      var itemsToSync = $scope.items.filter(function(item) {return item.selected}).map(function(item) {
        return item.name
      });

      LDAP.sync(resourceType, itemsToSync).then(function() {
        $modalInstance.close();
      });
    }
  };

  $scope.cancel = function() {
    $modalInstance.dismiss();
  };
}]);