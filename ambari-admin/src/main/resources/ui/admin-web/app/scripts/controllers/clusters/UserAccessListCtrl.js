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
.controller('UserAccessListCtrl',['$scope', '$location', 'Cluster', '$modal', '$rootScope', '$routeParams', 'PermissionSaver', 'Alert',
function($scope, $location, Cluster, $modal, $rootScope, $routeParams, PermissionSaver, Alert) {
  $scope.users = [];
  $scope.usersPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalUsers = 1;
  $scope.currentNameFilter = '';
  $scope.maxVisiblePages = 20;
  $scope.roles = [];
  $scope.clusterId = $routeParams.id;

  $scope.pageChanged = function() {
    $scope.loadUsers();
  };
  $scope.usersPerPageChanges = function() {
    $scope.resetPagination();
  };

  $scope.loadUsers = function(){
    Cluster.getPrivilegesWithFilters({
      clusterId: $routeParams.id,
      nameFilter: $scope.currentNameFilter,
      typeFilter: $scope.currentTypeFilter,
      roleFilter: $scope.currentRoleFilter,
      currentPage: $scope.currentPage,
      usersPerPage: $scope.usersPerPage
    }).then(function(data) {
      $scope.totalUsers = data.itemTotal;
      $scope.users = data.items.map(function (user) {
        var name = encodeURIComponent(user.PrivilegeInfo.principal_name);
        var type = user.PrivilegeInfo.principal_type.toLowerCase();
        user.PrivilegeInfo.encoded_name = name;
        user.PrivilegeInfo.url = type == 'user'? (type + 's/' + name) : (type + 's/' + name + '/edit');
        return user.PrivilegeInfo;
      });
      $scope.loadRoles();
    });
  };

  $scope.loadRoles = function() {
    Cluster.getPermissions({
      clusterId: $routeParams.id
    }).then(function(data) {
      $scope.roles = data.map(function(item) {
        return item.PermissionInfo;
      });
    });
  };

  // TODO change to PUT after it's available
  $scope.save = function(user) {
    Cluster.deletePrivilege(
    $routeParams.id,
    user.privilege_id
    ).then(
      function() {
        Cluster.createPrivileges(
        {
          clusterId: $routeParams.id
        },
        [{PrivilegeInfo: {
          permission_name: user.permission_name,
          principal_name: user.principal_name,
          principal_type: user.principal_type
        }}]
        ).then(function() {
          Alert.success(user.principal_name + " changed to " + user.permission_label);
          $scope.loadUsers();
        })
        .catch(function(data) {
          Alert.error('Cannot save permissions', data.data.message);
          $scope.loadUsers();
        });
      }
    );

  };

  $scope.resetPagination = function() {
    $scope.currentPage = 1;
    $scope.loadUsers();
  };

  $scope.roleFilterOptions = [
    {label: 'All', value: ''},
    {label: 'Cluster User', value: 'CLUSTER.USER'},
    {label:'Cluster Administrator', value: 'CLUSTER.ADMINISTRATOR'},
    {label:'Cluster Operator', value: 'CLUSTER.OPERATOR'},
    {label:'Service Administrator', value: 'SERVICE.ADMINISTRATOR'},
    {label:'Service Operator', value: 'SERVICE.OPERATOR'}
  ];
  $scope.currentRoleFilter = $scope.roleFilterOptions[0];


  $scope.typeFilterOptions = [
    {label:'All', value:''},
    {label:'Group', value:'GROUP'},
    {label:'User', value:'USER'}
  ];
  $scope.currentTypeFilter = $scope.typeFilterOptions[0];


  $scope.loadUsers();

  $rootScope.$watch(function(scope) {
    return scope.LDAPSynced;
  }, function(LDAPSynced) {
    if(LDAPSynced === true){
      $rootScope.LDAPSynced = false;
      $scope.loadUsers();
    }
  });

  $scope.switchToBlock = function() {
    $location.url('/clusters/' + $routeParams.id + '/manageAccess');
  }
}]);
