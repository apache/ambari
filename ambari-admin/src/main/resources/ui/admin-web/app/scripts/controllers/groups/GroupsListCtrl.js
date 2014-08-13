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
.controller('GroupsListCtrl',['$scope', 'Group', '$modal', 'ConfirmationModal', function($scope, Group, $modal, ConfirmationModal) {
  $scope.groups = [];

  $scope.groupsPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalGroups = 1;
  $scope.search = '';
  $scope.maxVisiblePages=20;

  $scope.pageChanged = function() {
    loadGroups();
  };
  $scope.usersPerPageChanges = function() {
    loadGroups();
  };

  function loadGroups(){
    Group.all($scope.currentPage, $scope.groupsPerPage, $scope.search).then(function(groups) {
      $scope.totalGroups = groups.itemTotal;
      $scope.groups = groups;
    })
    .catch(function(data) {
      console.error('Get groups list error');
    });
  }

  loadGroups();

  $scope.typeFilterOptions = ['All', 'Local', 'LDAP'];
  $scope.currentTypeFilter = 'All';
  $scope.typeFilter = function(group) {
    var tf = $scope.currentTypeFilter;
    if (tf === 'All') {
      return group;
    } else if(tf === 'Local' && !group.ldap_group){
      return group;
    } else if(tf === 'LDAP' && group.ldap_group){
      return group;
    }
  };

  
}]);