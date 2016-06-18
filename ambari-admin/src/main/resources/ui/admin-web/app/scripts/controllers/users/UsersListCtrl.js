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
  .controller('UsersListCtrl',['$scope', 'User', '$modal', '$rootScope', 'UserConstants', '$translate', 'Settings', function($scope, User, $modal, $rootScope, UserConstants, $translate, Settings) {
  var $t = $translate.instant;
  $scope.constants = {
    admin: $t('users.ambariAdmin'),
    users: $t('common.users').toLowerCase()
  };
  $scope.users = [];
  $scope.usersPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalUsers = 1;
  $scope.currentNameFilter = '';
  $scope.maxVisiblePages=20;
  $scope.tableInfo = {
    total: 0,
    showed: 0
  };
  $scope.isNotEmptyFilter = true;

  $scope.pageChanged = function() {
    $scope.loadUsers();
  };
  $scope.usersPerPageChanges = function() {
    $scope.resetPagination();
  };

  $scope.loadUsers = function(){
    User.list({
      currentPage: $scope.currentPage,
      usersPerPage: $scope.usersPerPage,
      searchString: $scope.currentNameFilter,
      user_type: $scope.currentTypeFilter.value,
      active: $scope.currentActiveFilter.value,
      admin: $scope.adminFilter
    }).then(function(data) {
      $scope.totalUsers = data.data.itemTotal;
      $scope.users = data.data.items.map(User.makeUser);
      $scope.tableInfo.showed = data.data.items.length;
      $scope.tableInfo.total = data.data.itemTotal;
    });
  };

  $scope.resetPagination = function() {
    $scope.currentPage = 1;
    $scope.loadUsers();
  };

  $scope.activeFilterOptions = [
    {label: $t('common.all'), value: '*'},
    {label: $t('users.active'), value: true},
    {label: $t('users.inactive'), value:false}
  ];
  $scope.currentActiveFilter = $scope.activeFilterOptions[0];

  $scope.typeFilterOptions = [{ label: $t('common.all'), value: '*'}]
    .concat(Object.keys(UserConstants.TYPES).map(function(key) {
      return {
        label: $t(UserConstants.TYPES[key].LABEL_KEY),
        value: UserConstants.TYPES[key].VALUE
      };
    }));

  $scope.currentTypeFilter = $scope.typeFilterOptions[0];

  $scope.adminFilter = false;
  $scope.toggleAdminFilter = function() {
    $scope.adminFilter = !$scope.adminFilter;
    $scope.resetPagination();
    $scope.loadUsers();
  };

  $scope.clearFilters = function () {
    $scope.currentNameFilter = '';
    $scope.currentTypeFilter = $scope.typeFilterOptions[0];
    $scope.currentActiveFilter = $scope.activeFilterOptions[0];
    $scope.adminFilter = false;
    $scope.resetPagination();
  };

  $scope.loadUsers();

  $scope.$watch(
    function (scope) {
      return Boolean(scope.currentNameFilter || (scope.currentActiveFilter && scope.currentActiveFilter.value !== '*')
        || (scope.currentTypeFilter && scope.currentTypeFilter.value !== '*') || $scope.adminFilter);
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
      $scope.loadUsers();
    }
  });
}]);
