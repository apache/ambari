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
.controller('GroupsListCtrl',['$scope', 'Group', '$modal', 'ConfirmationModal', '$rootScope', function($scope, Group, $modal, ConfirmationModal, $rootScope) {
  $scope.groups = [];

  $scope.groupsPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalGroups = 1;
  $scope.currentNameFilter = '';
  $scope.maxVisiblePages=20;

  $scope.pageChanged = function() {
    loadGroups();
  };
  $scope.groupsPerPageChanges = function() {
    loadGroups();
  };

  $scope.resetPagination = function() {
    $scope.currentPage = 1;
    loadGroups();
  };

  function loadGroups(){
    Group.all({
      currentPage: $scope.currentPage, 
      groupsPerPage: $scope.groupsPerPage, 
      searchString: $scope.currentNameFilter,
      ldap_group: $scope.currentTypeFilter.value
    }).then(function(groups) {
      $scope.totalGroups = groups.itemTotal;
      $scope.groups = groups;
    })
    .catch(function(data) {
      console.error('Get groups list error');
    });
  }

  $scope.typeFilterOptions = [
    {label:'All', value:'*'},
    {label:'Local', value: false},
    {label:'LDAP', value:true}
  ];
  $scope.currentTypeFilter = $scope.typeFilterOptions[0];
  
  loadGroups();

  $rootScope.$watch(function(scope) {
    return scope.LDAPSynced;
  }, function(LDAPSynced) {
    if(LDAPSynced === true){
      $rootScope.LDAPSynced = false;
      loadGroups();
    }
  });

}]);