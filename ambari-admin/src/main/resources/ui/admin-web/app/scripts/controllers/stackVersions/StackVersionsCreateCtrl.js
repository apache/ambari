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
.controller('StackVersionsCreateCtrl', ['$scope', 'Stack', '$routeParams', '$location', 'Alert', function($scope, Stack, $routeParams, $location, Alert) {
  $scope.clusterName = $routeParams.clusterName;
  $scope.upgradeStack = {
    value: null,
    options: []
  };
  $scope.fetchStackVersionFilterList = function () {
    Stack.allStackVersions()
    .then(function (allStackVersions) {
      $scope.upgradeStack.options = allStackVersions;
      $scope.upgradeStack.value = allStackVersions[allStackVersions.length - 1].value;
    })
    .catch(function (data) {
      Alert.error('Fetch stack version filter list error', data.message);
    });
  };
  $scope.fetchStackVersionFilterList();
  $scope.upgradeStack.value = $scope.upgradeStack.options[0];

  // TODO retrieve operating systems and repo names from stack definition
  $scope.repositories = [
    {
      os: 'redhat5',
      packages: [
        {label:'HDP', value: null},
        {label:'HDP-UTILS', value: null}
      ],
      selected: false
    },
    {
      os: 'redhat6',
      packages: [
        {label:'HDP', value: null},
        {label:'HDP-UTILS', value: null}
      ],
      selected: false
    },
    {
      os: 'sles11',
      packages: [
        {label:'HDP', value: null},
        {label:'HDP-UTILS', value: null}
      ],
      selected: false
    },
    {
      os: 'ubuntu12',
      packages: [
        {label:'HDP', value: null},
        {label:'HDP-UTILS', value: null}
      ],
      selected: false
    }
  ];

  $scope.create = function () {
    Stack.addRepo($scope.upgradeStack.value, $scope.versionName, $scope.repositories)
    .success(function () {
      var versionName = $scope.upgradeStack.value + '.' + $scope.versionName;
      Alert.success('Created version <a href="#/stackVersions/' + versionName + '/edit">' + versionName + '</a>');
      $location.path('/stackVersions');
    })
    .error(function (data) {
        Alert.error('Version creation error', data.message);
    });
  };
}]);
