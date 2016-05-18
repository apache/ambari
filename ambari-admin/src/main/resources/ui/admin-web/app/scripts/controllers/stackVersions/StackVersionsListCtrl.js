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
.controller('StackVersionsListCtrl', ['$scope', 'Cluster', 'Stack', '$routeParams', '$translate', function ($scope, Cluster, Stack, $routeParams, $translate) {
  var $t = $translate.instant;
  $scope.getConstant = function (key) {
    return $t('common.' + key).toLowerCase();
  }
  $scope.clusterName = $routeParams.clusterName;

  $scope.filter = {
    version: '',
    cluster: {
      options: [],
      current: null
    }
  };

  $scope.pagination = {
    totalRepos: 100,
    maxVisiblePages: 1,
    itemsPerPage: 100,
    currentPage: 1
  };
  $scope.allRepos = [];
  $scope.stackVersions = [];

  /**
   *  Formatted object to display all repos:
   *
   *  [{ 'name': 'HDP-2.3',
   *     'repos': ['2.3.6.0-2343', '2.3.4.1', '2.3.4.0-56']
   *   },
   *   { 'name': 'HDP-2.2',
   *     'repos': ['2.2.6.0', '2.2.4.5', '2.2.4.0']
   *   }
   *  ]
   *
   */
  $scope.fetchRepos = function () {
    return Stack.allRepos($scope.filter, $scope.pagination).then(function (repos) {
      $scope.allRepos = repos.items.sort(function(a, b){return a.repository_version < b.repository_version});
      var existingStackHash = {};
      var stackVersions = [];
      angular.forEach($scope.allRepos, function (repo) {
        var stackVersionName = repo.stack_name + '-' + repo.stack_version;
        if (!existingStackHash[stackVersionName]) {
          existingStackHash[stackVersionName] = true;
          stackVersions.push({
            'name': stackVersionName,
            'isOpened': true,
            'repos': [repo]
          });
        } else {
          if (stackVersions[stackVersions.length -1].repos) {
            stackVersions[stackVersions.length -1].repos.push(repo);
          }
        }
      });
      $scope.stackVersions = stackVersions;
    });
  };

  $scope.fetchRepos();

}]);
