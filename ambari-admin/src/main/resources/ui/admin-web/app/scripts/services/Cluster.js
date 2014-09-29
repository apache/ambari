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
.factory('Cluster', ['$http', '$q', 'Settings', function($http, $q, Settings) {
  return {
    getStatus: function() {
      var deferred = $q.defer();

      $http.get(Settings.baseUrl + '/clusters?fields=Clusters/provisioning_state')
      .then(function(data, status, headers) {
        deferred.resolve(data.data.items[0]);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    getPermissions: function() {
      var deferred = $q.defer();

      $http({
        method: 'GET',
        url: Settings.baseUrl + '/permissions',
        params: {
          fields: 'PermissionInfo',
          'PermissionInfo/resource_name': 'CLUSTER'
        }
      })
      .success(function(data) {
        deferred.resolve(data.items);
      })
      .catch(function(data) {
        deferred.reject(data); });

      return deferred.promise;
    },
    getPrivileges: function(params) {
      var deferred = $q.defer();

      $http({
        method: 'GET',
        url: Settings.baseUrl + '/clusters/'+params.clusterId,
        params : {
          'fields': 'privileges/PrivilegeInfo'
        }
      })
      .success(function(data) {
        deferred.resolve(data.privileges);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    createPrivileges: function(params, data) {
      return $http({
        method: 'POST',
        url: Settings.baseUrl + '/clusters/'+params.clusterId+'/privileges',
        data: data
      });
    },
    deletePrivileges: function(params, data) {
      return $http({
        method: 'DELETE',
        url: Settings.baseUrl + '/clusters/'+params.clusterId+'/privileges',
        data: data
      });
    },
    updatePrivileges: function(params, privileges) {
      return $http({
        method: 'PUT',
        url: Settings.baseUrl + '/clusters/' + params.clusterId + '/privileges',
        data: privileges
      });
    },
    deletePrivilege: function(clusterId, permissionName, principalType, principalName) {
      return $http({
        method: 'DELETE',
        url: Settings.baseUrl + '/clusters/'+clusterId+'/privileges',
        params: {
          'PrivilegeInfo/principal_type': principalType,
          'PrivilegeInfo/principal_name': principalName,
          'PrivilegeInfo/permission_name': permissionName
        }
      });
    },
    editName: function(oldName, newName) {
      return $http({
        method: 'PUT',
        url: Settings.baseUrl + '/clusters/' + oldName,
        data: {
          Clusters: {
            "cluster_name": newName
          }
        }
      });
    }
  };
}]);