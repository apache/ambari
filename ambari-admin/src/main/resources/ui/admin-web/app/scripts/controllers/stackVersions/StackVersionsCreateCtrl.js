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
.controller('StackVersionsCreateCtrl', ['$scope', 'Stack', '$routeParams', '$location', 'Alert', '$translate', 'Cluster', function($scope, Stack, $routeParams, $location, Alert, $translate, Cluster) {
  var $t = $translate.instant;
  $scope.createController = true;
  $scope.osList = [];
  $scope.skipValidation = false;

  $scope.clusterName = $routeParams.clusterName;
  $scope.subversionPattern = /^\d+\.\d+(-\d+)?$/;
  $scope.upgradeStack = {
    stack_name: '',
    stack_version: '',
    display_name: ''
  };

  $scope.option1 = {
    index: 1,
    displayName: 'Upload Version Definition File',
    url: 'files://',
    hasError: false
  };
  $scope.option2 = {
    index: 2,
    displayName: 'Version Definition File URL',
    url: 'https://',
    hasError: false
  };
  $scope.selectedOption = 1;

  /**
   * User can select ONLY one option to upload version definition file
   */
  $scope.toggleOptionSelect = function () {
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
      $scope.upgradeStack = {
        stack_name: response.stackName,
        stack_version: response.stackVersion,
        display_name: response.displayName
      };
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
      $scope.osList = response.osList;
      // load supported os type base on stack version
      $scope.afterStackVersionRead();
    });
  };

  /**
   * Load supported OS list
   */
  $scope.afterStackVersionRead = function () {
    Stack.getSupportedOSList($scope.upgradeStack.stack_name, $scope.upgradeStack.stack_version)
      .then(function (data) {
        var operatingSystems = data.operating_systems;
        operatingSystems.map(function (os) {
          var existingOSHash = {};
          angular.forEach($scope.osList, function (os) {
            existingOSHash[os.OperatingSystems.os_type] = os;
          });
          // if os not in the list, mark as un-selected, add this to the osList
          if (!existingOSHash[os.OperatingSystems.os_type]) {
            os.selected = false;
            os.repositories.forEach(function(repo) {
              repo.Repositories.base_url = '';
            });
            $scope.osList.push(os);
          }
        });
      })
      .catch(function (data) {
        Alert.error($t('versions.alerts.osListError'), data.message);
      });
  };

  /**
   * On click handler for removing OS
   */
  $scope.removeOS = function() {
    this.os.selected = false;
    if (this.os.repositories) {
      this.os.repositories.forEach(function(repo) {
        repo.hasError = false;
      });
    }
  };
  /**
   * On click handler for adding new OS
   */
  $scope.addOS = function() {
    this.os.selected = true;
    if (this.os.repositories) {
      this.os.repositories.forEach(function(repo) {
        repo.hasError = false;
      });
    }
  };

  $scope.isSaveButtonDisabled = function() {
    var enabled = false;
    $scope.osList.forEach(function(os) {
      if (os.selected) {
        enabled = true
      }
    });
    return !enabled;
  }

  $scope.save = function () {
    return Stack.validateBaseUrls($scope.skipValidation, $scope.osList, $scope.upgradeStack).then(function (invalidUrls) {
      if (invalidUrls.length === 0) {
        Stack.addRepo($scope.upgradeStack, $scope.actualVersion, $scope.osList)
          .success(function () {
            Alert.success($t('versions.alerts.versionCreated', {
              stackName: $scope.upgradeStack.stack_name,
              versionName: $scope.actualVersion
            }));
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

  $scope.cancel = function () {
    $scope.editVersionDisabled = true;
    $location.path('/stackVersions');
  };

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

  $scope.clearError = function() {
    this.repository.hasError = false;
  };

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
}]);
