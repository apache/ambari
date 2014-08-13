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
.factory('LDAP', ['$http', '$q', 'Settings', function($http, $q, Settings) {


  return {
    get: function() {
      return $http({
        method: 'GET',
        url: '/api/v1/controllers/ldap'
      });
    },
    sync: function(groupsList, usersList) {
      groupsList = Array.isArray(groupsList) ? groupsList : [];
      usersList = Array.isArray(usersList) ? usersList : [];
      return $http({
        method: 'PUT',
        url: Settings.baseUrl + '/controllers/ldap',
        data:[{
          LDAP:{
            "synced_groups": groupsList.join(','),
            "synced_users": usersList.join(',')
          }
        }]
      });
    },
    syncResource: function(resourceType, items) {
      var items = items.map(function(item) {
        var name = 'LDAP/synced_' + resourceType;
        var obj = {};
        obj['LDAP/synced_' + resourceType] = item;
        return obj;
      });
      
      return $http({
        method: 'POST',
        url: '/api/v1/controllers/ldap',
        data: items
      });
    }
  };
}]);