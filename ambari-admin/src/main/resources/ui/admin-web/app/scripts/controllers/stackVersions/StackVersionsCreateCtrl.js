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
  $scope.subversionPattern = /^\d(\.\d)?(\-\d*)?$/;
  $scope.upgradeStack = {
    selected: null,
    options: []
  };
  $scope.fetchStackVersionFilterList = function () {
    return Stack.allStackVersions()
    .then(function (allStackVersions) {
      var versions = [];
      angular.forEach(allStackVersions, function (version) {
        if (version.upgrade_packs.length > 0) {
          versions.push(version);
        }
      });
      $scope.upgradeStack.options = versions;
      $scope.upgradeStack.selected = versions[versions.length - 1];
      $scope.afterStackVersionChange();
    })
    .catch(function (data) {
      Alert.error('Fetch stack version filter list error', data.message);
    });
  };
  $scope.fetchStackVersionFilterList();
  $scope.repositories = [];

  $scope.selectedOS = 0;
  $scope.toggleOSSelect = function () {
    this.repository.selected? $scope.selectedOS++ : $scope.selectedOS--;
  };

  $scope.create = function () {
    return Stack.addRepo($scope.upgradeStack.selected, $scope.repoSubversion, $scope.repositories)
    .success(function () {
      var versionName = $scope.upgradeStack.selected.stack_version + '.' + $scope.repoSubversion;
      var stackName = $scope.upgradeStack.selected.stack_name;
      Alert.success('Created version ' +
      '<a href="#/stackVersions/' + stackName + '/' + versionName + '/edit">'
        + stackName + versionName +
      '</a>');
      $location.path('/stackVersions');
    })
    .error(function (data) {
        Alert.error('Version creation error', data.message);
    });
  };

  $scope.afterStackVersionChange = function () {
    Stack.getSupportedOSList($scope.upgradeStack.selected.stack_name, $scope.upgradeStack.selected.stack_version)
    .then(function (data) {
      var repositories = data.operatingSystems.map(function (os) {
        return {
          os: os.OperatingSystems.os_type,
          packages: [
            {label:'HDP', value: null},
            {label:'HDP-UTILS', value: null}
          ],
          selected: false
        };
      });
      $scope.repositories = repositories;
    })
    .catch(function (data) {
      Alert.error('getSupportedOSList error', data.message);
    });
  };
}]);
