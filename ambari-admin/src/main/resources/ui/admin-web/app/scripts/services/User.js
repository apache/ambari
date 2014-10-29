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
.factory('User', ['Restangular', '$http', 'Settings', function(Restangular, $http, Settings) {
  Restangular.addResponseInterceptor(function(data, operation, what, url, response, deferred) {
    var extractedData;
    if(operation === 'getList'){
      extractedData = data.items;
      extractedData.itemTotal = data.itemTotal;
    } else {
      extractedData = data;
    }

    return extractedData;
  });

  var Users = Restangular.all('users');

  return {
    list: function(params) {
      return $http.get(
        Settings.baseUrl + '/users/?' 
        + 'Users/user_name.matches(.*'+params.searchString+'.*)'
        + '&fields=*'
        + '&from=' + (params.currentPage-1)*params.usersPerPage
        + '&page_size=' + params.usersPerPage
        + (params.ldap_user === '*' ? '' : '&Users/ldap_user=' + params.ldap_user)
        + (params.active === '*' ? '' : '&Users/active=' + params.active)
        + (params.admin ? '&Users/admin=true' : '')
      );
    },
    listByName: function(name) {
      return $http.get(
        Settings.baseUrl + '/users?'
        + 'Users/user_name.matches(.*'+name+'.*)'
        + '&from=0&page_size=20'
      );
    },
    get: function(userId) {
      return Restangular.one('users', userId).get();
    },
    create: function(userObj) {
      return Restangular.all('users').post(userObj);
    },
    setActive: function(userId, isActive) {
      return Restangular.one('users', userId).customPUT({'Users/active':isActive});
    },
    setAdmin: function(userId, isAdmin) {
      return Restangular.one('users', userId).customPUT({'Users/admin':isAdmin});
    },
    setPassword: function(user, password, currentUserPassword) {
      return $http({
        method: 'PUT',
        url: Settings.baseUrl + '/users/' + user.user_name,
        data: {
          'Users/password': password,
          'Users/old_password': currentUserPassword
        }
      });
    },
    delete: function(userId) {
      return Restangular.one('users', userId).remove();
    },
    getPrivileges : function(userId) {
      return $http.get(Settings.baseUrl + '/privileges', {
        params:{
          'PrivilegeInfo/principal_type': 'USER',
          'PrivilegeInfo/principal_name': encodeURIComponent(userId),
          'fields': '*'
        }
      });
    }
  };
}]);
