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
.controller('StackVersionsCreateCtrl', ['$scope', 'Stack', '$routeParams', '$location', 'Alert', '$translate', 'Cluster', 'AddRepositoryModal', function($scope, Stack, $routeParams, $location, Alert, $translate, Cluster, AddRepositoryModal) {
  var $t = $translate.instant;
  $scope.constants = {
    os: $t('versions.os')
  };
  $scope.createController = true;
  $scope.osList = [];
  $scope.skipValidation = false;
  $scope.useRedhatSatellite = false;

  $scope.clusterName = $routeParams.clusterName;
  $scope.subversionPattern = /^\d+\.\d+(-\d+)?$/;
  $scope.upgradeStack = {
    stack_name: '',
    stack_version: '',
    display_name: ''
  };

  $scope.publicOption = {
    index: 1,
    hasError: false
  };
  $scope.localOption = {
    index: 2,
    hasError: false
  };
  $scope.option1 = {
    index: 3,
    displayName: $t('versions.uploadFile'),
    file: '',
    hasError: false
  };
  $scope.option2 = {
    index: 4,
    displayName: $t('versions.enterURL'),
    url: $t('versions.defaultURL'),
    hasError: false
  };
  $scope.selectedOption = {
    index: 1
  };
  $scope.selectedLocalOption = {
    index: 3
  };

  /**
   * User can select ONLY one option to upload version definition file
   */
  $scope.togglePublicLocalOptionSelect = function () {
    $scope.option1.hasError = false;
    $scope.option2.hasError = false;
  };
  $scope.toggleOptionSelect = function () {
    $scope.option1.hasError = false;
    $scope.option2.hasError = false;
  };
  $scope.togglePublicLocalOptionSelect = function () {
    if ($scope.selectedOption.index == $scope.publicOption.index && $scope.selectedPublicRepoVersion) {
      $scope.setVersionSelected($scope.selectedPublicRepoVersion);
    }
  };
  $scope.clearOptionsError = function () {
    $scope.option1.hasError = false;
    $scope.option2.hasError = false;
  };
  $scope.readInfoButtonDisabled = function () {
    if ($scope.selectedOption.index == $scope.publicOption.index) return true;
    return $scope.option1.index == $scope.selectedLocalOption.index ? !$scope.option1.file : !$scope.option2.url;
  };
  $scope.isAddOsButtonDisabled = function () {
    var selectedCnt = 0;
    angular.forEach($scope.osList, function (os) {
      if (os.selected) {
        selectedCnt ++;
      }
    });
    return $scope.osList.length == selectedCnt;
  };

  $scope.allInfoCategoriesBlank = function () {
    return !$scope.upgradeStack.stack_name;
  };

  $scope.onFileSelect = function(e){
    if (e.files && e.files.length == 1) {
      var file = e.files[0];
      var reader = new FileReader();
      reader.onload = (function () {
        return function (e) {
          $scope.option1.file = e.target.result;
        };
      })(file);
      reader.readAsText(file);
    } else {
      $scope.option1.file = '';
    }
  };

  /**
   * Load selected file to current page content
   */
  $scope.readVersionInfo = function(){
    var data = {};
    var isXMLdata = false;
    if ($scope.option2.index == $scope.selectedLocalOption.index) {
      var url = $scope.option2.url;
      data = {
        "VersionDefinition": {
          "version_url": url
        }
      };
    } else if ($scope.option1.index == $scope.selectedLocalOption.index) {
      isXMLdata = true;
      // load from file browser
      data = $scope.option1.file;
    }

    return Stack.postVersionDefinitionFile(isXMLdata, data).then(function (versionInfo) {
      if (versionInfo.id && versionInfo.stackName && versionInfo.stackVersion) {
        Stack.getRepo(versionInfo.id, versionInfo.stackName, versionInfo.stackVersion)
          .then(function (response) {
            $scope.setVersionSelected(response);
        });
      }
    })
    .catch(function (data) {
      Alert.error($t('versions.alerts.readVersionInfoError'), data.message);
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

  /**
   * On click handler for adding a new repository
   */
  $scope.addRepository = function() {
    AddRepositoryModal.show($scope.osList, $scope.upgradeStack.stack_name, $scope.upgradeStack.stack_version, $scope.id);
  };

  $scope.isSaveButtonDisabled = function() {
    var enabled = false;
    $scope.osList.forEach(function(os) {
      if (os.selected) {
        enabled = true
      }
    });
    return !enabled;
  };

  $scope.defaulfOSRepos = {};

  $scope.save = function () {
    $scope.editVersionDisabled = true;
    delete $scope.updateObj.href;
    $scope.updateObj.operating_systems = [];
    var updateRepoUrl = false;
    angular.forEach($scope.osList, function (os) {
      var savedUrls = $scope.defaulfOSRepos[os.OperatingSystems.os_type];
      os.OperatingSystems.ambari_managed_repositories = !$scope.useRedhatSatellite;
      if (os.selected) {
        var currentRepos = os.repositories;
        if (!savedUrls || currentRepos[0].Repositories.base_url != savedUrls.defaultBaseUrl
          || currentRepos[1].Repositories.base_url != savedUrls.defaultUtilsUrl) {
          updateRepoUrl = true;
        }
        $scope.updateObj.operating_systems.push(os);
      } else if (savedUrls) {
        updateRepoUrl = true;
      }
    });
    if ($scope.isPublicVersion) {
      return Stack.validateBaseUrls($scope.skipValidation, $scope.osList, $scope.upgradeStack).then(function (invalidUrls) {
        if (invalidUrls.length === 0) {
          var data = {
            "VersionDefinition": {
              "available": $scope.id
            }
          };
          var repoUpdate = {
            operating_systems: $scope.updateObj.operating_systems
          };
          Stack.postVersionDefinitionFile(false, data).then(function (versionInfo) {
            if (versionInfo.id && versionInfo.stackName && versionInfo.stackVersion) {
              Stack.updateRepo(versionInfo.stackName, versionInfo.stackVersion, versionInfo.id, repoUpdate).then(function () {
                Alert.success($t('versions.alerts.versionEdited', {
                  stackName: $scope.upgradeStack.stack_name,
                  versionName: $scope.actualVersion,
                  displayName: $scope.displayName
                }));
                $location.path('/stackVersions');
              }).catch(function (data) {
                Alert.error($t('versions.alerts.versionUpdateError'), data.message);
              });
            }
          })
          .catch(function (data) {
            Alert.error($t('versions.alerts.readVersionInfoError'), data.message);
          });
        } else {
          Stack.highlightInvalidUrls(invalidUrls);
        }
      });
    } else {
      $scope.updateRepoVersions();
    }
  };

  $scope.updateRepoVersions = function () {
    return Stack.validateBaseUrls($scope.skipValidation, $scope.osList, $scope.upgradeStack).then(function (invalidUrls) {
      if (invalidUrls.length === 0) {
        Stack.updateRepo($scope.upgradeStack.stack_name, $scope.upgradeStack.stack_version, $scope.id, $scope.updateObj).then(function () {
          Alert.success($t('versions.alerts.versionEdited', {
            stackName: $scope.upgradeStack.stack_name,
            versionName: $scope.actualVersion,
            displayName: $scope.repoVersionFullName
          }));
          $location.path('/stackVersions');
        }).catch(function (data) {
          Alert.error($t('versions.alerts.versionUpdateError'), data.message);
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


  $scope.setVersionSelected = function (version) {
    var response = version;
    $scope.id = response.id;
    $scope.isPatch = response.type == 'PATCH';
    $scope.stackNameVersion = response.stackNameVersion || $t('common.NA');
    $scope.displayName = response.displayName || $t('common.NA');
    $scope.actualVersion = response.repositoryVersion || response.actualVersion || $t('common.NA');
    $scope.isPublicVersion = response.showAvailable == true;
    $scope.updateObj = response.updateObj;
    $scope.upgradeStack = {
      stack_name: response.stackName,
      stack_version: response.stackVersion,
      display_name: response.displayName || $t('common.NA')
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
  };

  $scope.selectRepoInList = function() {
    $scope.selectedPublicRepoVersion = this.version;
    $scope.setVersionSelected(this.version);
  };

  $scope.fetchPublicVersions = function () {
    return Stack.allPublicStackVersions().then(function (versions) {
      if (versions && versions.length) {
        $scope.selectedPublicRepoVersion = versions[0];
        $scope.setVersionSelected(versions[0]);
        $scope.availableStackRepoList = versions.length == 1 ? [] : versions;
      }
    });
  };

  $scope.fetchPublicVersions();
}]);
