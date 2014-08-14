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
.controller('UsersListCtrl',['$scope', 'User', '$modal', function($scope, User, $modal) {
  $scope.users = [];
  $scope.usersPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalUsers = 1;
  $scope.currentNameFilter = '';
  $scope.maxVisiblePages=20;

  $scope.pageChanged = function() {
    $scope.loadUsers();
  };
  $scope.usersPerPageChanges = function() {
    $scope.loadUsers();
  };

  $scope.loadUsers = function(){
    User.list({
      currentPage: $scope.currentPage, 
      usersPerPage: $scope.usersPerPage, 
      searchString: $scope.currentNameFilter,
      ldap_user: $scope.currentTypeFilter.value,
      active: $scope.currentActiveFilter.value
    }).then(function(data) {
      $scope.totalUsers = data.data.itemTotal;
      $scope.users = data.data.items;
    });
  };

  $scope.resetPagination = function() {
    $scope.currentPage = 1;
    $scope.loadUsers();
  };

  $scope.actvieFilterOptions = [
    {label: 'All', value: '*'}, 
    {label: 'Active', value: true}, 
    {label:'Inactive', value:false}
  ];
  $scope.currentActiveFilter = $scope.actvieFilterOptions[0];
  

  $scope.typeFilterOptions = [
    {label:'All', value:'*'},
    {label:'Local', value:false},
    {label:'LDAP', value:true}
  ];
  $scope.currentTypeFilter = $scope.typeFilterOptions[0];

  $scope.loadUsers();
}]);