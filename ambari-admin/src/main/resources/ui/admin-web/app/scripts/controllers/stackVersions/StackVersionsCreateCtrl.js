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
.controller('StackVersionsCreateCtrl', ['$scope', 'Stack', '$routeParams', '$location', 'Alert', '$translate', function($scope, Stack, $routeParams, $location, Alert, $translate) {
  var $t = $translate.instant;
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
      Alert.error($t('versions.alerts.filterListError'), data.message);
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
            Alert.success($t('versions.alerts.versionCreated'), {stackName: stackName, versionName: versionName});
            $location.path('/stackVersions');
          })
          .error(function (data) {
              Alert.error($t('versions.alerts.versionCreationError'), data.message);
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
      Alert.error($t('versions.alerts.osListError'), data.message);
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

  // two options to upload version definition file
  $scope.option1 = {
    index: 1,
    displayName: 'Upload Version Definition File',
    url: 'files://',
    //selected: true,
    hasError: false
  };
  $scope.option2 = {
    index: 2,
    displayName: 'Version Definition File URL',
    url: 'https://',
    //selected: false,
    hasError: false
  };
  $scope.selectedOption = 1;

  /**
   * User can select ONLY one option to upload version definition file
   */
  $scope.toggleOptionSelect = function () {
    //$scope.option1.selected = $scope.selectedOption == $scope.option1.index;
    //$scope.option2.selected = $scope.selectedOption == $scope.option2.index;
    $scope.option1.hasError = false;
    $scope.option2.hasError = false;
  };
  $scope.clearOptionsError = function () {
    $scope.option1.hasError = false;
    $scope.option2.hasError = false;
  };
  $scope.readInfoButtonDisabled = function () {
    return $scope.option1.selected ? !$scope.option1.url : !$scope.option2.url;
  };

  $scope.onFileSelect = function(){
    return {
      link: function($scope,el){
        el.bind("change", function(e){
          $scope.file = (e.srcElement || e.target).files[0];
          $scope.getFile();
        })
      }
    }
  };

//  $scope.uploadFile = function(){
//    var file = $scope.myFile;
//    console.log('file is ' );
//    console.dir(file);
//    var uploadUrl = "/fileUpload";
//    fileUpload.uploadFileToUrl(file, uploadUrl);
//  };

  /**
   * Load selected file to current page content
   */
  $scope.readVersionInfo = function(){
    if ($scope.option2.selected) {
      var url = $scope.option2.url;
    }
    /// POST url first then get the version definition info
    return Stack.getLatestRepo('HDP').then(function (response) {
      $scope.id = response.id;
      $scope.isPatch = response.type == 'PATCH';
      $scope.stackNameVersion = response.stackNameVersion || 'n/a';
      $scope.displayName = response.displayName || 'n/a';
      $scope.version = response.version || 'n/a';
      $scope.actualVersion = response.actualVersion || 'n/a';
      $scope.services = response.services || [];
      //save default values of repos to check if they were changed
      $scope.defaulfOSRepos = {};
      response.updateObj.operating_systems.forEach(function(os) {
        $scope.defaulfOSRepos[os.OperatingSystems.os_type] = {
          defaultBaseUrl: os.repositories[0].Repositories.base_url,
          defaultUtilsUrl: os.repositories[1].Repositories.base_url
        };
      });
      $scope.repoVersionFullName = response.repoVersionFullName;
      angular.forEach(response.osList, function (os) {
        os.selected = true;
      });
      $scope.selectedOS = response.osList.length;
      $scope.osList = response.osList;
      // if user reach here from UI click, repo status should be cached
      // otherwise re-fetch repo status from cluster end point.
//      $scope.repoStatus = Cluster.repoStatusCache[$scope.id];
//      if (!$scope.repoStatus) {
//        $scope.fetchClusters()
//          .then(function () {
//            return $scope.fetchRepoClusterStatus();
//          })
//          .then(function () {
//            $scope.deleteEnabled = $scope.isDeletable();
//          });
//      } else {
//        $scope.deleteEnabled = $scope.isDeletable();
//      }
      //$scope.addMissingOSList();
    });
  };
}]);
