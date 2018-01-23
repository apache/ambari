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
.controller('UsersListCtrl',
['$scope', 'User', '$modal', '$rootScope', 'UserConstants', '$translate', 'Cluster', 'View', 'ConfirmationModal', 'Settings',
function($scope, User, $modal, $rootScope, UserConstants, $translate, Cluster, View, ConfirmationModal, Settings) {
  var $t = $translate.instant;
  $scope.constants = {
    admin: $t('users.ambariAdmin'),
    users: $t('common.users').toLowerCase()
  };
  $scope.minRowsToShowPagination = Settings.minRowsToShowPagination;
  $scope.isLoading = false;
  $scope.users = [];
  $scope.usersPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalUsers = 0;
  $scope.filters = {
    name: '',
    status: null,
    type: null
  };
  $scope.maxVisiblePages = 20;
  $scope.tableInfo = {
    total: 0,
    showed: 0
  };
  $scope.isNotEmptyFilter = true;

  function loadUsers() {
    $scope.isLoading = true;
    User.list({
      currentPage: $scope.currentPage,
      usersPerPage: $scope.usersPerPage,
      searchString: $scope.filters.name,
      user_type: $scope.filters.type.value,
      active: $scope.filters.status.value
    }).then(function (data) {
      $scope.totalUsers = data.data.itemTotal;
      $scope.users = data.data.items.map(User.makeUser);
      $scope.tableInfo.showed = data.data.items.length;
      $scope.tableInfo.total = data.data.itemTotal;
    }).finally(function () {
      $scope.isLoading = false;
    });
  }

  $scope.pageChanged = function () {
    loadUsers();
  };
  $scope.usersPerPageChanges = function () {
    $scope.resetPagination();
  };

  $scope.resetPagination = function () {
    $scope.currentPage = 1;
    loadUsers();
  };

  $scope.activeFilterOptions = [
    {label: $t('common.all'), value: '*'},
    {label: $t('users.active'), value: true},
    {label: $t('users.inactive'), value: false}
  ];
  $scope.filters.status = $scope.activeFilterOptions[0];

  $scope.typeFilterOptions = [{label: $t('common.all'), value: '*'}]
    .concat(Object.keys(UserConstants.TYPES).map(function (key) {
      return {
        label: $t(UserConstants.TYPES[key].LABEL_KEY),
        value: UserConstants.TYPES[key].VALUE
      };
    }));

  $scope.filters.type = $scope.typeFilterOptions[0];

  $scope.clearFilters = function () {
    $scope.filters.name = '';
    $scope.filters.type = $scope.typeFilterOptions[0];
    $scope.filters.status = $scope.activeFilterOptions[0];
    $scope.resetPagination();
  };

  $scope.$watch(
    function (scope) {
      return Boolean(scope.filters.name || (scope.filters.status && scope.filters.status.value !== '*')
        || (scope.filters.type && scope.filters.type.value !== '*'));
    },
    function (newValue, oldValue, scope) {
      scope.isNotEmptyFilter = newValue;
    }
  );

  $rootScope.$watch(function (scope) {
    return scope.LDAPSynced;
  }, function (LDAPSynced) {
    if (LDAPSynced === true) {
      $rootScope.LDAPSynced = false;
      loadUsers();
    }
  });

  $scope.createUser = function () {
    var modalInstance = $modal.open({
      templateUrl: 'views/userManagement/modals/userCreate.html',
      controller: 'UserCreateCtrl',
      backdrop: 'static'
    });

    modalInstance.result.finally(loadUsers);
  };

  $scope.deleteUser = function (user) {
    ConfirmationModal.show(
      $t('common.delete', {
        term: $t('common.user')
      }),
      $t('common.deleteConfirmation', {
        instanceType: $t('common.user').toLowerCase(),
        instanceName: '"' + user.user_name + '"'
      })
    ).then(function () {
      Cluster.getPrivilegesForResource({
        nameFilter: user.user_name,
        typeFilter: {value: 'USER'}
      }).then(function (data) {
        var clusterPrivilegesIds = [];
        var viewsPrivileges = [];
        if (data.items && data.items.length) {
          angular.forEach(data.items[0].privileges, function (privilege) {
            if (privilege.PrivilegeInfo.principal_type === 'USER') {
              if (privilege.PrivilegeInfo.type === 'VIEW') {
                viewsPrivileges.push({
                  id: privilege.PrivilegeInfo.privilege_id,
                  view_name: privilege.PrivilegeInfo.view_name,
                  version: privilege.PrivilegeInfo.version,
                  instance_name: privilege.PrivilegeInfo.instance_name
                });
              } else {
                clusterPrivilegesIds.push(privilege.PrivilegeInfo.privilege_id);
              }
            }
          });
        }
        User.delete(user.user_name).then(function () {
          if (clusterPrivilegesIds.length) {
            Cluster.deleteMultiplePrivileges($rootScope.cluster.Clusters.cluster_name, clusterPrivilegesIds);
          }
          angular.forEach(viewsPrivileges, function (privilege) {
            View.deletePrivilege(privilege);
          });
          loadUsers();
        });
      });
    });
  };

  loadUsers();

}]);
