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
.factory('Stack', ['$http', '$q', 'Settings', function ($http, $q, Settings) {
  var statusMap = {
    'INSTALLED': {
      label: 'Installed',
      class: 'label-default'
    },
    'IN_USE': {
      label: 'In Use',
      class: 'label-info'
    },
    'CURRENT': {
      label: 'Current',
      class: 'label-success'
    }
  };
  /**
   * parse raw json to formatted objects
   * @param data
   * @return {Array}
   */
  function parse(data) {
    data.forEach(function (item) {
      var mapItem = statusMap[item.status];
      if (mapItem) {
        item.statusClass = mapItem.class;
        item.statusLabel = mapItem.label;
      }
    });
    return data;
  }

  return {
    allStackVersions: function () {
      var url = Settings.baseUrl + '/stacks?fields=versions/*';
      var deferred = $q.defer();
      $http.get(url, {mock: 'stack/allStackVersions.json'})
      .success(function (data) {
        var allStackVersions = [];
        angular.forEach(data.items, function (stack) {
          angular.forEach(stack.versions, function (version) {
            var stack_name = version.Versions.stack_name;
            var stack_version = version.Versions.stack_version;
            var upgrade_packs = version.Versions.upgrade_packs;
            var active = version.Versions.active;
            allStackVersions.push({
              stack_name: stack_name,
              stack_version: stack_version,
              displayName: stack_name + '-' + stack_version,
              upgrade_packs: upgrade_packs,
              active: active
            });
          });
        });
        deferred.resolve(allStackVersions)
      })
      .error(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    },

    allRepos: function (filter, pagination) {
      var versionFilter = filter.version;
      var url = '/stacks?fields=versions/repository_versions/RepositoryVersions';
      if (versionFilter) {
        url += '&versions/repository_versions/RepositoryVersions/display_name.matches(.*' + versionFilter + '.*)';
      }
      var deferred = $q.defer();
      $http.get(Settings.baseUrl + url, {mock: 'version/versions.json'})
      .success(function (data) {
        var repos = [];
        angular.forEach(data.items, function(stack) {
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
        // prepare response data with client side pagination
        var response = {};
        response.itemTotal = repos.length;
        var from = (pagination.currentPage - 1) * pagination.itemsPerPage;
        var to = (repos.length - from > pagination.itemsPerPage)? from + pagination.itemsPerPage : repos.length;
        response.items = repos.slice(from, to);
        response.showed = to - from;
        deferred.resolve(response)
      })
      .error(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    },

    addRepo: function (stack, repoSubversion, osList) {
      var url = '/stacks/' + stack.stack_name + '/versions/' + stack.stack_version + '/repository_versions/';
      var payload = {};
      var payloadWrap = { RepositoryVersions : payload };
      payload.repository_version = stack.stack_version + '.' + repoSubversion;
      payload.display_name = stack.stack_name + '-' + payload.repository_version;
      payloadWrap.operating_systems = [];
      osList.forEach(function (osItem) {
        if (osItem.selected)
        {
          payloadWrap.operating_systems.push({
            "OperatingSystems" : {
              "os_type" : osItem.OperatingSystems.os_type
            },
            "repositories" : osItem.repositories.map(function (repo) {
              return {
                "Repositories" : {
                  "repo_id": repo.Repositories.repo_id,
                  "repo_name": repo.Repositories.repo_name,
                  "base_url": repo.Repositories.base_url
                }
              };
            })
          });
        }
      });
      return $http.post(Settings.baseUrl + url, payloadWrap);
    },

    getRepo: function (repoVersion, stack_name) {
      var url = Settings.baseUrl + '/stacks/' + stack_name + '/versions?' +
      'fields=repository_versions/operating_systems/repositories/*' +
      ',repository_versions/RepositoryVersions/display_name' +
      '&repository_versions/RepositoryVersions/repository_version=' + repoVersion;
      var deferred = $q.defer();
      $http.get(url, {mock: 'version/version.json'})
      .success(function (data) {
        data = data.items[0];
        var response = {
          id : data.repository_versions[0].RepositoryVersions.id,
          stackVersion : data.Versions.stack_version,
          stack: data.Versions.stack_name + '-' + data.Versions.stack_version,
          stackName: data.Versions.stack_name,
          versionName: data.repository_versions[0].RepositoryVersions.repository_version,
          displayName : data.repository_versions[0].RepositoryVersions.display_name,
          repoVersionFullName : data.Versions.stack_name + '-' + data.repository_versions[0].RepositoryVersions.repository_version,
          osList: data.repository_versions[0].operating_systems,
          updateObj: data.repository_versions[0]
        };
        deferred.resolve(response);
      })
      .error(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    },

    updateRepo: function (stackName, stackVersion, id, payload) {
      var url = Settings.baseUrl + '/stacks/' + stackName + '/versions/' + stackVersion + '/repository_versions/' + id;
      var deferred = $q.defer();
      $http.put(url, payload)
      .success(function (data) {
        deferred.resolve(data)
      })
      .error(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    },

    deleteRepo: function (stackName, stackVersion, id) {
      var url = Settings.baseUrl + '/stacks/' + stackName + '/versions/' + stackVersion + '/repository_versions/' + id;
      var deferred = $q.defer();
      $http.delete(url)
      .success(function (data) {
        deferred.resolve(data)
      })
      .error(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    },

    getSupportedOSList: function (stackName, stackVersion) {
      var url = Settings.baseUrl + '/stacks/' + stackName + '/versions/' + stackVersion + '?fields=operating_systems/repositories/Repositories'
      var deferred = $q.defer();
      $http.get(url, {mock: 'stack/operatingSystems.json'})
      .success(function (data) {
        deferred.resolve(data);
      })
      .error(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    },

    validateBaseUrls: function(skip, osList, stack) {
      var deferred = $q.defer(),
        url = Settings.baseUrl + '/stacks/' + stack.stack_name + '/versions/' + stack.stack_version,
        totalCalls = 0,
        invalidUrls = [];

      if (skip) {
        deferred.resolve(invalidUrls);
      } else {
        osList.forEach(function (os) {
          if (os.selected) {
            os.repositories.forEach(function (repo) {
              totalCalls++;
              $http.post(url + '/operating_systems/' + os.OperatingSystems.os_type + '/repositories/' + repo.Repositories.repo_id + '?validate_only=true',
                {
                  "Repositories": {
                    "base_url": repo.Repositories.base_url
                  }
                },
                {
                  repo: repo
                }
              )
                .success(function () {
                  totalCalls--;
                  if (totalCalls === 0) deferred.resolve(invalidUrls);
                })
                .error(function (response, status, callback, params) {
                  invalidUrls.push(params.repo);
                  totalCalls--;
                  if (totalCalls === 0) deferred.resolve(invalidUrls);
                });
            });
          }
        });
      }
      return deferred.promise;
    },

    highlightInvalidUrls :function(invalidrepos) {
      invalidrepos.forEach(function(repo) {
        repo.hasError = true;
      });
    }

  };
}]);
