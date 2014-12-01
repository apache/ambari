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
    list: function (filter) {
      return $http.get(Settings.baseUrl + '/stacks?fields=versions/RepositoryVersions', {mock: 'version/versions.json'});
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
