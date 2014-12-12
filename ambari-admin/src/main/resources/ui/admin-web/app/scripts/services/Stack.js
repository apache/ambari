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
      var url = Settings.baseUrl + '/stacks?fields=*';
      var deferred = $q.defer();
      $http.get(url, {mock: 'stack/allStackVersions.json'})
      .success(function (data) {
        var allStackVersions = [];
        angular.forEach(data.items, function (stack) {
          angular.forEach(stack.versions, function (version) {
            var stack_name = version.Versions.stack_name;
            var stack_version = version.Versions.stack_version;
            allStackVersions.push({
              displayName: stack_name + '-' + stack_version,
              value: stack_version
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
      var url = '/stacks/HDP/versions?fields=repository_versions/RepositoryVersions'; // TODO should not hard code HDP
      if (versionFilter) {
        url += '&repository_versions/RepositoryVersions/repository_version.matches(.*' + versionFilter + '.*)';
      }
      url += '&from='+ (pagination.currentPage - 1) * pagination.itemsPerPage;
      url += '&page_size=' + pagination.itemsPerPage;
      var deferred = $q.defer();
      $http.get(Settings.baseUrl + url, {mock: 'version/versions.json'})
      .success(function (data) {
        deferred.resolve(data)
      })
      .error(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    },

    addRepo: function (stack, version, osList) {
      var url = '/stacks/HDP/versions/2.2/repository_versions/';
      var payload = {};
      var payloadWrap = { RepositoryVersions : payload };
      payload.repository_version = stack + '.' + version;
      payload.display_name = 'HDP-' + payload.repository_version;
      payload.upgrade_pack = "upgrade-2.2"; // TODO get this value from backend
      payloadWrap.operating_systems = [];
      angular.forEach(osList, function (osItem) {
        if (osItem.selected)
        {
          payloadWrap.operating_systems.push({
            "OperatingSystems" : {
              "os_type" : osItem.os
            },
            "repositories" : osItem.packages.map(function (pack) {
              return {
                "Repositories" : {
                  "repo_id": (pack.label + '-' + payload.repository_version),
                  "repo_name": pack.label,
                  "base_url": pack.value? pack.value : ''
                }
              };
            })
          });
        }
      });
      return $http.post(Settings.baseUrl + url, payloadWrap);
    },

    getRepo: function (repoVersion) {
      // TODO do not hard code HDP
      var url = Settings.baseUrl + '/stacks/HDP/versions?' +
                'fields=repository_versions/operating_systems/repositories/*' +
                '&repository_versions/RepositoryVersions/repository_version=' + repoVersion;
      var deferred = $q.defer();
      $http.get(url, {mock: 'version/version.json'})
      .success(function (data) {
        data = data.items[0];
        var response = {
          id : data.repository_versions[0].RepositoryVersions.id,
          stackVersion : data.Versions.stack_version,
          stack: data.Versions.stack_name + '-' + data.Versions.stack_version,
          versionName: data.repository_versions[0].RepositoryVersions.repository_version,
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

    updateRepo: function (stackVersion, id, payload) {
      var url = Settings.baseUrl + '/stacks/HDP/versions/' + stackVersion + '/repository_versions/' + id;
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

    deleteRepo: function (stackVersion, id) {
      var url = Settings.baseUrl + '/stacks/HDP/versions/' + stackVersion + '/repository_versions/' + id;
      var deferred = $q.defer();
      $http.delete(url)
      .success(function (data) {
        deferred.resolve(data)
      })
      .error(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    }
  };
}]);
