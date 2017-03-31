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
.controller('GroupsListCtrl',['$scope', 'Group', '$modal', 'ConfirmationModal', '$rootScope', 'GroupConstants', '$translate', function($scope, Group, $modal, ConfirmationModal, $rootScope, GroupConstants, $translate) {
  var $t = $translate.instant;
  $scope.constants = {
    groups: $t('common.groups').toLowerCase()
  };
  $scope.groups = [];

  $scope.groupsPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalGroups = 1;
  $scope.currentNameFilter = '';
  $scope.maxVisiblePages=20;
  $scope.tableInfo = {
    total: 0,
    showed: 0
  };
  $scope.isNotEmptyFilter = true;

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
      group_type: $scope.currentTypeFilter.value
    }).then(function(groups) {
      $scope.totalGroups = groups.itemTotal;
      $scope.groups = groups.map(Group.makeGroup);
      $scope.tableInfo.total = groups.itemTotal;
      $scope.tableInfo.showed = groups.length;
    })
    .catch(function(data) {
      console.error($t('groups.alerts.getGroupsListError'));
    });
  }

  $scope.typeFilterOptions = [{ label: $t('common.all'), value: '*'}]
    .concat(Object.keys(GroupConstants.TYPES).map(function(key) {
      return {
        label: $t(GroupConstants.TYPES[key].LABEL_KEY),
        value: GroupConstants.TYPES[key].VALUE
      };
  }));
  $scope.currentTypeFilter = $scope.typeFilterOptions[0];

  $scope.clearFilters = function () {
    $scope.currentNameFilter = '';
    $scope.currentTypeFilter = $scope.typeFilterOptions[0];
    $scope.resetPagination();
  };
  
  loadGroups();

  $scope.$watch(
    function (scope) {
      return Boolean(scope.currentNameFilter || (scope.currentTypeFilter && scope.currentTypeFilter.value !== '*'));
    },
    function (newValue, oldValue, scope) {
      scope.isNotEmptyFilter = newValue;
    }
  );

  $rootScope.$watch(function(scope) {
    return scope.LDAPSynced;
  }, function(LDAPSynced) {
    if(LDAPSynced === true){
      $rootScope.LDAPSynced = false;
      loadGroups();
    }
  });

}]);