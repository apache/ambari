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
.factory('StackVersions', ['$http', '$q', 'Settings', function ($http, $q, Settings) {
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
    list: function (filter, pagination) {
      var stackFilter = filter.stack.current.value;
      var versionFilter = filter.version;
      var clusterFilter = filter.cluster.current.value;
      var url = '/stacks/HDP/versions?fields=repository_versions/RepositoryVersions'; // TODO should not hard code HDP
      if (stackFilter) {
        url += '&repository_versions/RepositoryVersions/stack_version.matches(.*' + stackFilter + '.*)';
      }
      if (versionFilter) {
        url += '&repository_versions/RepositoryVersions/repository_version.matches(.*' + versionFilter + '.*)';
      }
      if (clusterFilter) {
        url += '';
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

    add: function (stack, version, osList) {
      var url = '/stacks/HDP/versions/2.2/repository_versions/';
      var payload = {};
      payload.repository_version = stack + '.' + version;
      payload.display_name = 'HDP-' + payload.repository_version;
      payload.upgrade_pack = "upgrade-2.2"; // TODO get this value from backend
      payload.operating_systems = [];
      angular.forEach(osList, function (osItem) {
        if (osItem.selected)
        {
          payload.operating_systems.push({
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
      var payloadWrap = { RepositoryVersions : payload };
      return $http.post(Settings.baseUrl + url, payloadWrap);
    },

    get: function (version) {
      var url = Settings.baseUrl + '/stacks?versions/RepositoryVersions/repository_version=' + version +'&fields=versions/RepositoryVersions';
      return $http.get(url, {mock: 'version/version.json'});
    },

    getStackRepositories: function (version) {
      var url = Settings.baseUrl + '/stacks/HDP/versions/' + version + '/operating_systems?fields=*';
      return $http.get(url, {mock: 'stack/stack.json'});
    }
  };
}]);
