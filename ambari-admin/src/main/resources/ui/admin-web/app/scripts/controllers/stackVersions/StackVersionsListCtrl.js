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
  .controller('StackVersionsListCtrl', ['$scope', 'Cluster', 'Stack', '$routeParams', '$translate', 'Settings', function ($scope, Cluster, Stack, $routeParams, $translate, Settings) {
    var $t = $translate.instant;
    $scope.getConstant = function (key) {
      return $t('common.' + key).toLowerCase();
    };
    $scope.clusterName = $routeParams.clusterName;
    $scope.filter = {
      name: '',
      version: '',
      type: '',
      cluster: {
        options: [],
        current: null
      },
      stack: {
        options: [],
        current: null
      }
    };
    $scope.isNotEmptyFilter = true;

    $scope.pagination = {
      totalRepos: 10,
      maxVisiblePages: 20,
      itemsPerPage: 10,
      currentPage: 1
    };

    $scope.tableInfo = {
      total: 0,
      showed: 0,
      filtered: 0
    };

    $scope.repos = [];
    $scope.dropDownClusters = [];
    $scope.selectedCluster = $scope.dropDownClusters[0];

    $scope.resetPagination = function () {
      $scope.pagination.currentPage = 1;
      $scope.loadAllData();
    };

    $scope.pageChanged = function () {
      $scope.loadAllData();
    };

    $scope.goToCluster = function() {
      window.location.replace(Settings.siteRoot + '#/main/admin/stack/versions');
    };

    $scope.clearFilters = function () {
      $scope.filter.name = '';
      $scope.filter.version = '';
      $scope.filter.cluster.current = $scope.filter.cluster.options[0];
      $scope.filter.stack.current = $scope.filter.stack.options[0];
      $scope.resetPagination();
    };

    $scope.fetchRepoClusterStatus = function () {
      var clusterName = ($scope.clusters && $scope.clusters.length > 0) ? $scope.clusters[0].Clusters.cluster_name : null; // only support one cluster at the moment
      if (clusterName) {
        angular.forEach($scope.repos, function (repo) {
          Cluster.getRepoVersionStatus(clusterName, repo.id).then(function (response) {
            repo.status = response.status;
            repo.totalHosts = response.totalHosts;
            repo.currentHosts = response.currentHosts;
            repo.installedHosts = response.installedHosts;
            repo.stackVersionId = response.stackVersionId;
            repo.cluster = (repo.status == 'current' || repo.status == 'installed') ? clusterName : '';
          });
        });
      }
    };

    $scope.fetchRepos = function () {
      return Stack.allRepos($scope.filter, $scope.pagination).then(function (repos) {
        $scope.pagination.totalRepos = repos.itemTotal;
        $scope.repos = repos.items;
        $scope.tableInfo.total = repos.itemTotal;
        $scope.tableInfo.showed = repos.showed;
      });
    };

    $scope.fillClusters = function (clusters) {
      $scope.dropDownClusters = [].concat(clusters);
      var options = [{label: $t('common.all'), value: ''}];
      angular.forEach(clusters, function (cluster) {
        options.push({
          label: cluster.Clusters.cluster_name,
          value: cluster.Clusters.cluster_name
        });
      });
      $scope.filter.cluster.options = options;
      if (!$scope.filter.cluster.current) {
        $scope.filter.cluster.current = options[0];
      }
    };

    $scope.fetchClusters = function () {
      return Cluster.getAllClusters().then(function (clusters) {
        if (clusters && clusters.length > 0) {
          $scope.clusters = clusters;
          $scope.fillClusters(clusters);
        }
      });
    };

    $scope.fetchStacks = function () {
      return Stack.allStackVersions().then(function (clusters) {
        if (clusters && clusters.length > 0) {
          $scope.stacks = clusters;
          $scope.fillStacks(clusters);
        }
      });
    };

    $scope.fillStacks = function() {
      var options = [{label: $t('common.all'), value: ''}];
      angular.forEach($scope.stacks, function (stack) {
        if (stack.active) {
          options.push({
            label: stack.displayName,
            value: stack.displayName
          });
        }
      });
      $scope.filter.stack.options = options;
      if (!$scope.filter.stack.current) {
        $scope.filter.stack.current = options[0];
      }
    };

    $scope.loadAllData = function () {
      $scope.fetchStacks()
        .then(function () {
          return $scope.fetchClusters();
        })
        .then(function () {
          return $scope.fetchRepos();
        })
        .then(function () {
          $scope.fetchRepoClusterStatus();
        });
    };

    $scope.loadAllData();

    $scope.$watch('filter', function (filter) {
      $scope.isNotEmptyFilter = Boolean(filter.name
        || filter.version
        || filter.type
        || (filter.cluster.current && filter.cluster.current.value)
        || (filter.stack.current && filter.stack.current.value));
    }, true);

    $scope.toggleVisibility = function (repo) {
      repo.isProccessing = true;
      var payload = {
        RepositoryVersions:{
          hidden: repo.hidden
        }
      }
      Stack.updateRepo(repo.stack_name, repo.stack_version, repo.id, payload).then( null, function () {
        repo.hidden = !repo.hidden;
      }).finally( function () {
        delete repo.isProccessing;
      });
    }

    $scope.isHideCheckBoxEnabled = function ( repo ) {
      return !repo.isProccessing && ( !repo.cluster || repo.isPatch && ( repo.status === 'installed' || repo.status === 'install_failed') );
    }
  }]);
