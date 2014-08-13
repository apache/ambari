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
  $scope.search = '';
  $scope.maxVisiblePages=20;

  $scope.pageChanged = function() {
    loadUsers();
  };
  $scope.usersPerPageChanges = function() {
    loadUsers();
  };

  function loadUsers(){
    User.list($scope.currentPage, $scope.usersPerPage, $scope.search).then(function(data) {
      $scope.totalUsers = data.itemTotal;
      $scope.users = data.items;
    });
  }

  loadUsers();


  $scope.actvieFilterOptions = ['All', 'Active', 'Inactive'];
  $scope.currentActiveFilter = 'All';
  $scope.activeFilter = function(user) {
    var af = $scope.currentActiveFilter;
    if (af === 'All') {
      return user;
    } else if(af === 'Active' && user.Users.active){
      return user;
    } else if(af === 'Inactive' && !user.Users.active){
      return user;
    }
  };

  $scope.typeFilterOptions = ['All', 'Local', 'LDAP'];
  $scope.currentTypeFilter = 'All';
  $scope.typeFilter = function(user) {
    var tf = $scope.currentTypeFilter;
    if (tf === 'All') {
      return user;
    } else if(tf === 'Local' && !user.Users.ldap_user){
      return user;
    } else if(tf === 'LDAP' && user.Users.ldap_user){
      return user;
    }
  };
}]);