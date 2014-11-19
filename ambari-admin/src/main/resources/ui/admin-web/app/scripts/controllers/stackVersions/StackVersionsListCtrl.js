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
  .controller('StackVersionsListCtrl', ['$scope', 'StackVersions', '$routeParams', function ($scope, StackVersions, $routeParams) {
  $scope.clusterName = $routeParams.clusterName;
  $scope.filter = {
    stack: {
      options: [
        {label: 'All', value: ''},
        {label: 'HDP 2.2', value: 'hdp2.2'},
        {label: 'HDP 2.3', value: 'hdp2.3'},
        {label: 'HDP 2.4', value: 'hdp2.4'}
      ],
      current: null
    },
    version: '',
    cluster: {
      options: [
        {label: 'All', value: ''},
        {label: 'MyCluster', value: 'MyCluster'},
        {label: 'Another Cluster', value: 'anotherCluster'}
      ],
      current: null
    }
  };

  $scope.filter.stack.current = $scope.filter.stack.options[0];
  $scope.filter.cluster.current = $scope.filter.cluster.options[0];

  $scope.pagination = {
    totalStacks: 10,
    maxVisiblePages: 20,
    itemsPerPage: 10,
    currentPage: 1
  };

  $scope.tableInfo = {
    total: 0,
    showed: 0,
    filtered: 0
  };

  $scope.stacks = [];

  $scope.getStackVersions = function () {
    return StackVersions.list($scope.filter).then(function (stacks) {
      $scope.stacks = stacks;
      $scope.tableInfo.total = stacks.length;
      $scope.tableInfo.showed = stacks.length;
    });
  };
  $scope.getStackVersions();
}]);
