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
  .factory('User', ['Restangular', '$http', 'Settings', 'UserConstants', '$translate', function(Restangular, $http, Settings, UserConstants, $translate) {
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
  var $t = $translate.instant;
  var Users = Restangular.all('users');

  return {
    list: function(params) {
      return $http.get(
        Settings.baseUrl + '/users/?'
        + 'Users/user_name.matches(.*'+params.searchString+'.*)'
        + '&fields=*'
        + '&from=' + (params.currentPage-1)*params.usersPerPage
        + '&page_size=' + params.usersPerPage
        + (params.user_type === '*' ? '' : '&Users/user_type=' + params.user_type)
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
      return $http.get(Settings.baseUrl + '/users/' + userId + '/privileges', {
        params:{
          'fields': '*'
        }
      });
    },
    /**
     * Generate user info to display by response data from API.
     * Generally this is a single point to manage all required and useful data
     * needed to use as context for views/controllers.
     *
     * @param {Object} user - object from API response
     * @returns {Object}
     */
    makeUser: function(user) {
      user.Users.encoded_name = encodeURIComponent(user.Users.user_name);
      user.Users.userTypeName = $t(UserConstants.TYPES[user.Users.user_type].LABEL_KEY);
      user.Users.ldap_user = user.Users.user_type === UserConstants.TYPES.LDAP.VALUE;

      return user;
    }
  };
}]);
