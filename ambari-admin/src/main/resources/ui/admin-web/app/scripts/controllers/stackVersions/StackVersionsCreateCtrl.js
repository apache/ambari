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
.controller('StackVersionsCreateCtrl', ['$scope', 'Stack', '$routeParams', '$location', 'Alert', 'AddRepositoryModal',  function($scope, Stack, $routeParams, $location, Alert, AddRepositoryModal) {
  $scope.createController = true;
  $scope.osList = [];
  $scope.skipValidation = false;
  $scope.selectedOS = 0;
  $scope.repoSubversion = "";

  $scope.clusterName = $routeParams.clusterName;
  $scope.subversionPattern = /^\d+\.\d+(-\d+)?$/;
  $scope.upgradeStack = {
    selected: null,
    options: []
  };
  $scope.fetchStackVersionFilterList = function () {
    return Stack.allStackVersions()
    .then(function (allStackVersions) {
      var versions = [];
      angular.forEach(allStackVersions, function (version) {
        if (version.upgrade_packs.length > 0 && version.active) {
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

  $scope.save = function () {
    return Stack.validateBaseUrls($scope.skipValidation, $scope.osList, $scope.upgradeStack.selected).then(function (invalidUrls) {
      if (invalidUrls.length === 0) {
        Stack.addRepo($scope.upgradeStack.selected, $scope.repoSubversion, $scope.osList)
          .success(function () {
            var versionName = $scope.upgradeStack.selected.stack_version + '.' + $scope.repoSubversion;
            var stackName = $scope.upgradeStack.selected.stack_name;
            Alert.success('Created version ' +
            '<a href="#/stackVersions/' + stackName + '/' + versionName + '/edit">'
              + stackName + '-' + versionName +
            '</a>');
            $location.path('/stackVersions');
          })
          .error(function (data) {
              Alert.error('Version creation error', data.message);
          });
      } else {
        Stack.highlightInvalidUrls(invalidUrls);
      }
    });
  };

  $scope.afterStackVersionChange = function () {
    Stack.getSupportedOSList($scope.upgradeStack.selected.stack_name, $scope.upgradeStack.selected.stack_version)
    .then(function (data) {
      var operatingSystems = data.operating_systems;
        $scope.osList = operatingSystems.map(function (os) {
          os.selected = false;
          os.repositories.forEach(function(repo) {
            repo.Repositories.base_url = '';
          });
          return os;
        });
    })
    .catch(function (data) {
      Alert.error('getSupportedOSList error', data.message);
    });
  };

  $scope.updateCurrentVersionInput = function () {
    $scope.currentVersionInput = $scope.upgradeStack.selected.displayName + '.' + angular.element('[name="version"]')[0].value;
  };

  /**
   * TODO create parent controller for StackVersionsEditCtrl and StackVersionsCreateCtrl and
   * move this method to it
   */
  $scope.clearErrors = function() {
    if ($scope.osList) {
      $scope.osList.forEach(function(os) {
        if (os.repositories) {
          os.repositories.forEach(function(repo) {
            repo.hasError = false;
          })
        }
      });
    }
  };
  /**
   * TODO create parent controller for StackVersionsEditCtrl and StackVersionsCreateCtrl and
   * move this method to it
   */
  $scope.clearError = function() {
    this.repository.hasError = false;
  };
  /**
   * TODO create parent controller for StackVersionsEditCtrl and StackVersionsCreateCtrl and
   * move this method to it
   */
  $scope.toggleOSSelect = function () {
    this.os.repositories.forEach(function(repo) {
      repo.hasError = false;
    });
    this.os.selected ? $scope.selectedOS++ : $scope.selectedOS--;
  };
  /**
   * TODO create parent controller for StackVersionsEditCtrl and StackVersionsCreateCtrl and
   * move this method to it
   */
  $scope.hasValidationErrors = function() {
    var hasErrors = false;
    if ($scope.osList) {
      $scope.osList.forEach(function(os) {
        if (os.repositories) {
          os.repositories.forEach(function(repo) {
            if (repo.hasError) {
              hasErrors = true;
            }
          })
        }
      });
    }
    return hasErrors;
  };
  /**
   * TODO create parent controller for StackVersionsEditCtrl and StackVersionsCreateCtrl and
   * move this method to it
   */
  $scope.cancel = function () {
    $scope.editVersionDisabled = true;
    $location.path('/stackVersions');
  };

  /**
   * TODO create parent controller for StackVersionsEditCtrl and StackVersionsCreateCtrl and
   * move this method to it
   */
  $scope.addRepository = function() {
    AddRepositoryModal.show($scope.osList, $scope.stackName, $scope.stackVersion, $scope.id);
  };

}]);
