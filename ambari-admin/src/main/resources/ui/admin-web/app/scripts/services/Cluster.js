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
    repoStatusCache : {},

    getAllClusters: function() {
      var deferred = $q.defer();
      $http.get(Settings.baseUrl + '/clusters', {mock: 'cluster/clusters.json'})
      .then(function(data, status, headers) {
        deferred.resolve(data.data.items);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    getStatus: function() {
      var deferred = $q.defer();

      $http.get(Settings.baseUrl + '/clusters?fields=Clusters/provisioning_state', {mock: 'cluster/init.json'})
      .then(function(data, status, headers) {
        deferred.resolve(data.data.items[0]);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    getAmbariVersion: function() {
      var deferred = $q.defer();

      $http.get(Settings.baseUrl + '/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/component_version,RootServiceComponents/properties/server.os_family&minimal_response=true', {mock: '2.1'})
      .then(function(data) {
        deferred.resolve(data.data.RootServiceComponents.component_version);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    getAmbariTimeout: function() {
      var deferred = $q.defer();
      var url = '/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/properties/user.inactivity.timeout.default'
      $http.get(Settings.baseUrl + url)
      .then(function(data) {
        var properties = data.data.RootServiceComponents.properties;
        var timeout = properties? properties['user.inactivity.timeout.default'] : 0;
        deferred.resolve(timeout);
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
        mock: 'permission/permissions.json',
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
    },
    getRepoVersionStatus: function (clusterName, repoId ) {
      var me = this;
      var deferred = $q.defer();
      var url = Settings.baseUrl + '/clusters/' + clusterName +
        '/stack_versions?fields=*&ClusterStackVersions/repository_version=' + repoId;
      $http.get(url, {mock: 'cluster/repoVersionStatus.json'})
      .success(function (data) {
        data = data.items;
        var response = {};
        if (data.length > 0) {
          var hostStatus = data[0].ClusterStackVersions.host_states;
          var currentHosts = hostStatus['CURRENT'].length;
          var installedHosts = hostStatus['INSTALLED'].length;
          var totalHosts = 0;
          // collect hosts on all status
          angular.forEach(hostStatus, function(status) {
            totalHosts += status.length;
          });
          response.status = currentHosts > 0? 'current' :
                            installedHosts > 0? 'installed' : '';
          response.currentHosts = currentHosts;
          response.installedHosts = installedHosts;
          response.totalHosts = totalHosts;
          response.stackVersionId = data[0].ClusterStackVersions.id;
        } else {
          response.status = '';
        }
        me.repoStatusCache[repoId] = response.status;
        deferred.resolve(response);
      })
      .catch(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    }
  };
}]);
