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
  .controller('StackVersionsListCtrl', ['$scope', 'Cluster', 'Stack', '$routeParams', function ($scope, Cluster, Stack, $routeParams) {
  $scope.clusterName = $routeParams.clusterName;
  $scope.filter = {
    version: '',
    cluster: {
      options: [],
      current: null
    }
  };

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
  $scope.dropDownClusters = [];
  $scope.selectedCluster = $scope.dropDownClusters[0];

  $scope.resetPagination = function () {
    $scope.pagination.currentPage = 1;
    $scope.fetchRepos();
  };

  $scope.goToCluster = function() {
    window.location.replace('/#/main/admin/versions/updates');
  };

  $scope.clearFilters = function () {
    $scope.filter.version = '';
    $scope.filter.cluster.current = $scope.filter.cluster.options[0];
    $scope.resetPagination();
  };

  $scope.fetchRepoClusterStatus = function (repos) {
    var clusterName = $scope.clusters[0].Clusters.cluster_name; // only support one cluster at the moment
    angular.forEach(repos, function (repo) {
      Cluster.getRepoVersionStatus(clusterName, repo.id).then(function (response) {
        repo.status = response.status;
        repo.totalHosts = response.totalHosts;
        repo.currentHosts = response.currentHosts;
        repo.cluster = response.status == 'current'? clusterName : '';
      });
    });
  };

  $scope.fetchRepos = function () {
    return Stack.allRepos($scope.filter, $scope.pagination).then(function (stacks) {
      $scope.pagination.totalStacks = stacks.items.length;
      var repos = [];
      angular.forEach(stacks.items, function(stack) {
        angular.forEach(stack.versions, function (version) {
          var repoVersions = version.repository_versions;
          if (repoVersions.length > 0) {
            repos = repos.concat(repoVersions);
          }
        });
      });
      repos = repos.map(function (stack) {
        return stack.RepositoryVersions;
      });
      $scope.repos = repos;
      $scope.tableInfo.total = stacks.length;
      $scope.tableInfo.showed = stacks.length;
      $scope.fetchRepoClusterStatus($scope.repos);
    });
  };

  $scope.fillClusters = function (clusters) {
    $scope.dropDownClusters = [{
      Clusters: {
        cluster_name: 'Configure on...'
      }
    }].concat(clusters);
    $scope.selectedCluster = $scope.dropDownClusters[0];
    angular.forEach(clusters, function (cluster) {
      var options = [{label: "All", value: ''}];
      angular.forEach(clusters, function (cluster) {
        options.push({
          label: cluster.Clusters.cluster_name,
          value: cluster.Clusters.cluster_name
        });
      });
      $scope.filter.cluster.options = options;
      $scope.filter.cluster.current = options[0];
    });
  };

  $scope.fetchClusters = function () {
    Cluster.getAllClusters().then(function (clusters) {
      $scope.clusters = clusters;
      $scope.fillClusters(clusters);
      $scope.fetchRepos();
    });
  };
  $scope.fetchClusters();
}]);
