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
.controller('RemoteClustersListCtrl', ['$scope', '$routeParams', '$translate', 'RemoteCluster', function ($scope, $routeParams, $translate, RemoteCluster) {
  var $t = $translate.instant;

  $scope.clusterName = $routeParams.clusterName;

  $scope.constants = {
    groups: $t('common.clusters').toLowerCase()
  };

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
    loadRemoteClusters();
  };
  $scope.groupsPerPageChanges = function() {
    loadRemoteClusters();
  };

  $scope.resetPagination = function() {
    $scope.currentPage = 1;
    loadRemoteClusters();
  };

  $scope.typeFilterOptions = [
    $t('common.any')
  ];

  $scope.currentTypeFilter = $scope.typeFilterOptions[0];

  $scope.clearFilters = function () {
    $scope.currentNameFilter = '';
    $scope.currentTypeFilter = $scope.typeFilterOptions[0];
    $scope.resetPagination();
  };

  function loadRemoteClusters(){
      RemoteCluster.all({
        currentPage: $scope.currentPage,
        groupsPerPage: $scope.groupsPerPage,
        searchString: $scope.currentNameFilter,
        service: $scope.currentTypeFilter
      }).then(function(remoteclusters) {

        $scope.totalGroups = remoteclusters.itemTotal;
        $scope.tableInfo.total = remoteclusters.itemTotal;
        $scope.tableInfo.showed = remoteclusters.items.length;

        $scope.remoteClusters = remoteclusters.items;

        remoteclusters.items.map(function(clusteritem){
          clusteritem.ClusterInfo.services.map(function(service){
            var serviceIndex = $scope.typeFilterOptions.indexOf(service);
            if(serviceIndex == -1){
              $scope.typeFilterOptions.push(service);
            }
          })
        })

      })
      .catch(function(data) {
        console.error('Error in fetching remote clusters.', data);
      });
  };

  loadRemoteClusters();

  $scope.$watch(
    function (scope) {
      return Boolean(scope.currentNameFilter || (scope.currentTypeFilter));
    },
    function (newValue, oldValue, scope) {
      scope.isNotEmptyFilter = newValue;
    }
  );

}]);
