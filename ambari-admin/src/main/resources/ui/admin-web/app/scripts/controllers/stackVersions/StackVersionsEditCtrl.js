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
.controller('StackVersionsEditCtrl', ['$scope', '$location', 'Stack', '$routeParams', 'ConfirmationModal', 'Alert', function($scope, $location, Stack, $routeParams, ConfirmationModal, Alert) {
  function loadStackVersionInfo () {
    return Stack.getRepo($routeParams.versionId, $routeParams.stackName).then(function (response) {
      $scope.id = response.id;
      $scope.stack = response.stack;
      $scope.stackName = response.stackName;
      $scope.versionName = response.versionName;
      $scope.stackVersion = response.stackVersion;
      $scope.updateObj = response.updateObj;
      $scope.repoVersionFullName = response.repoVersionFullName;
      angular.forEach(response.osList, function (os) {
        os.selected = true;
      });
      $scope.osList = response.osList;
    });
  }

  $scope.skipValidation = false;
  $scope.deleteEnabled = true;

  $scope.save = function () {
    $scope.editVersionDisabled = true;

    delete $scope.updateObj.href;
    Stack.updateRepo($scope.stackName, $scope.stackVersion, $scope.id, $scope.updateObj).then(function () {
      Alert.success('Edited version <a href="#/stackVersions/' + $scope.stackName + '/' + $scope.versionName + '/edit">' + $scope.repoVersionFullName + '</a>');
      $location.path('/stackVersions');
    }).catch(function (data) {
      Alert.error('Version update error', data.message);
    });
  };

  $scope.cancel = function () {
    $scope.editVersionDisabled = true;
    $location.path('/stackVersions');
  };

  $scope.delete = function () {
    ConfirmationModal.show('Delete Version', 'Are you sure you want to delete version "'+ $scope.versionName +'"?').then(function() {
      Stack.deleteRepo($scope.stackName, $scope.stackVersion, $scope.id).then( function () {
        $location.path('/stackVersions');
      }).catch(function (data) {
        Alert.error('Version delete error', data.message);
      });
    });
  };
  loadStackVersionInfo();
}]);
