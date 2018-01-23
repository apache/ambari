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
.controller('GroupsListCtrl',
['$scope', 'Group', '$modal', 'ConfirmationModal', '$rootScope', '$translate', 'Settings', 'Cluster', 'View', 'Alert',
function($scope, Group, $modal, ConfirmationModal, $rootScope, $translate, Settings, Cluster, View, Alert) {
  var $t = $translate.instant;
  $scope.constants = {
    groups: $t('common.groups').toLowerCase()
  };
  $scope.minRowsToShowPagination = Settings.minRowsToShowPagination;
  $scope.isLoading = false;
  $scope.groups = [];

  $scope.groupsPerPage = 10;
  $scope.currentPage = 1;
  $scope.totalGroups = 0;
  $scope.filter = {
    name: '',
    type: null
  };
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
    $scope.isLoading = true;
    Group.all({
      currentPage: $scope.currentPage, 
      groupsPerPage: $scope.groupsPerPage, 
      searchString: $scope.filter.name,
      group_type: $scope.filter.type.value
    }).then(function(groups) {
      $scope.isLoading = false;
      $scope.totalGroups = groups.itemTotal;
      $scope.groups = groups.map(Group.makeGroup);
      $scope.tableInfo.total = groups.itemTotal;
      $scope.tableInfo.showed = groups.length;
    })
    .catch(function(data) {
      Alert.error($t('groups.alerts.getGroupsListError'), data.data.message);
    });
  }

  $scope.typeFilterOptions = [{ label: $t('common.all'), value: '*'}]
    .concat(Object.keys(Group.getTypes()).map(function(key) {
      return {
        label: $t(Group.getTypes()[key].LABEL_KEY),
        value: Group.getTypes()[key].VALUE
      };
  }));
  $scope.filter.type = $scope.typeFilterOptions[0];

  $scope.clearFilters = function () {
    $scope.filter.name = '';
    $scope.filter.type = $scope.typeFilterOptions[0];
    $scope.resetPagination();
  };
  
  loadGroups();

  $scope.$watch(
    function (scope) {
      return Boolean(scope.filter.name || (scope.filter.type && scope.filter.type.value !== '*'));
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

  $scope.createGroup = function () {
    var modalInstance = $modal.open({
      templateUrl: 'views/userManagement/modals/groupCreate.html',
      controller: 'GroupCreateCtrl',
      backdrop: 'static'
    });

    modalInstance.result.catch(loadGroups);
  };

  $scope.deleteGroup = function(group) {
    ConfirmationModal.show(
      $t('common.delete', {
        term: $t('common.group')
      }),
      $t('common.deleteConfirmation', {
        instanceType: $t('common.group').toLowerCase(),
        instanceName: '"' + group.group_name + '"'
      })
    ).then(function() {
      Cluster.getPrivilegesForResource({
        nameFilter : group.group_name,
        typeFilter : {value: 'GROUP'}
      }).then(function(data) {
        var clusterPrivilegesIds = [];
        var viewsPrivileges = [];
        if (data.items && data.items.length) {
          angular.forEach(data.items[0].privileges, function(privilege) {
            if (privilege.PrivilegeInfo.principal_type === 'GROUP') {
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
        group.destroy().then(function() {
          if (clusterPrivilegesIds.length) {
            Cluster.deleteMultiplePrivileges($rootScope.cluster.Clusters.cluster_name, clusterPrivilegesIds);
          }
          angular.forEach(viewsPrivileges, function(privilege) {
            View.deletePrivilege(privilege);
          });
          loadGroups();
        });
      });
    });
  };

}]);