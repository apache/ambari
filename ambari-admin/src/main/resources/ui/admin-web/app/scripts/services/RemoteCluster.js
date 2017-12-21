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
  .factory('RemoteCluster', ['$http', '$q', 'Settings', function($http, $q, Settings) {

    function RemoteCluster(){
    }

    RemoteCluster.edit = function(payload, config){
      var deferred = $q.defer();

      $http.put(Settings.baseUrl + '/remoteclusters/' + payload.ClusterInfo.name , payload, config)
        .success(function (data) {
          deferred.resolve(data)
        })
        .error(function (data) {
          deferred.reject(data);
        });
      return deferred.promise;
    }


    RemoteCluster.getDetails = function(clusterName) {
      var deferred = $q.defer();

      $http.get( Settings.baseUrl  + '/remoteclusters/' + clusterName)
        .success(function(response) {
          deferred.resolve(response);
        })
        .error(function(data) {
          deferred.reject(data);
        });

      return deferred.promise;

    };

    RemoteCluster.deregister = function(clusterName){
      var deferred = $q.defer();

      $http.delete( Settings.baseUrl  + '/remoteclusters/' + clusterName)
        .success(function(response) {
          deferred.resolve(response);
        })
        .error(function(data) {
          deferred.reject(data);
        });

      return deferred.promise;

    };

    RemoteCluster.register = function(payload, config){
      var deferred = $q.defer();

      $http.post(Settings.baseUrl + '/remoteclusters/' + payload.ClusterInfo.name , payload, config)
        .success(function (data) {
          deferred.resolve(data)
        })
        .error(function (data) {
          deferred.reject(data);
        });
        return deferred.promise;
    }

    RemoteCluster.all = function() {
      var deferred = $q.defer();

      $http.get(Settings.baseUrl + "/remoteclusters")
        .success(function(response) {
          deferred.resolve(response);
        })
        .error(function(data) {
          deferred.reject(data);
        });
      return deferred.promise;
    };

    RemoteCluster.affectedViews = function(clustername) {
      var deferred = $q.defer();

      $http.get(Settings.baseUrl + '/views?'
          + 'fields=versions%2Finstances/ViewInstanceInfo/cluster_handle,versions%2Finstances/ViewInstanceInfo/cluster_type&versions%2FViewVersionInfo%2Fsystem=false&versions%2Finstances/ViewInstanceInfo/cluster_type=REMOTE_AMBARI&versions%2Finstances/ViewInstanceInfo/cluster_handle=' + clustername

        )
        .success(function(response) {
          deferred.resolve(response);
        })
        .error(function(data) {
          deferred.reject(data);
        });
      return deferred.promise;
    };

    RemoteCluster.listAll = function() {
      var deferred = $q.defer();

      /* TODO :: Add params like RemoteCluster.matches and &from , &page_size */
      $http.get(Settings.baseUrl + "/remoteclusters?fields=ClusterInfo/services,ClusterInfo/cluster_id")
        .success(function(response) {
          deferred.resolve(response.items);
        })
        .error(function(data) {
          deferred.reject(data);
        });
      return deferred.promise;
    };

    return RemoteCluster;

  }]);
