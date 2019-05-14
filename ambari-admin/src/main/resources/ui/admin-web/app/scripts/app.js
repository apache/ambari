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

angular.module('ambariAdminConsole', [
  'ngRoute',
  'ngAnimate',
  'ui.bootstrap',
  'restangular',
  'toggle-switch',
  'pascalprecht.translate'
])
.constant('Settings', {
  siteRoot: '{proxy_root}/'.replace(/\{.+\}/g, ''),
	baseUrl: '{proxy_root}/api/v1'.replace(/\{.+\}/g, ''),
  testMode: (window.location.port == 8000),
  mockDataPrefix: 'assets/data/',
  isLDAPConfigurationSupported: false,
  isLoginActivitiesSupported: false
})
.config(['RestangularProvider', '$httpProvider', '$provide', 'Settings', function(RestangularProvider, $httpProvider, $provide, Settings) {
  // Config Ajax-module
  RestangularProvider.setBaseUrl(Settings.baseUrl);
  RestangularProvider.setDefaultHeaders({'X-Requested-By':'ambari'});

  $httpProvider.defaults.headers.post['Content-Type'] = 'plain/text';
  $httpProvider.defaults.headers.put['Content-Type'] = 'plain/text';

  $httpProvider.defaults.headers.post['X-Requested-By'] = 'ambari';
  $httpProvider.defaults.headers.put['X-Requested-By'] = 'ambari';
  $httpProvider.defaults.headers.common['X-Requested-By'] = 'ambari';

  $httpProvider.interceptors.push(['Settings', '$q', function(Settings, $q) {
    return {
      'request': function(config) {
        if (Settings.testMode) {
          if (config.method === 'GET') {
            config.url = (config.mock) ? Settings.mockDataPrefix + config.mock : config.url;
          } else {
            config.method = "GET";
          }
        }
        return config;
      }
    };
  }]);

  $httpProvider.interceptors.push(['$rootScope', '$q', function (scope, $q) {
    function success(response) {
      return response;
    }

    function error(response) {
      if (response.status == 403) {
        window.location = Settings.siteRoot;
        return;
      }
      return $q.reject(response);
    }

    return function (promise) {
      return promise.then(success, error);
    }
  }]);

  $provide.factory('TimestampHttpInterceptor', [function($q) {
    return {
      request: function(config) {
        if (config && config.method === 'GET' && config.url.indexOf('html') === -1) {
          config.url += config.url.indexOf('?') < 0 ? '?' : '&';
          config.url += '_=' + new Date().getTime();
         }
         return config || $q.when(config);
      }
   };
  }]);
  $httpProvider.interceptors.push('TimestampHttpInterceptor');

  $provide.decorator('ngFormDirective', ['$delegate', function($delegate) {
    var ngForm = $delegate[0], controller = ngForm.controller;
    ngForm.controller = ['$scope', '$element', '$attrs', '$injector', function(scope, element, attrs, $injector) {
    var $interpolate = $injector.get('$interpolate');
      attrs.$set('name', $interpolate(attrs.name || '')(scope));
      $injector.invoke(controller, this, {
        '$scope': scope,
        '$element': element,
        '$attrs': attrs
      });
    }];
    return $delegate;
  }]);

  $provide.decorator('ngModelDirective', ['$delegate', function($delegate) {
    var ngModel = $delegate[0], controller = ngModel.controller;
    ngModel.controller = ['$scope', '$element', '$attrs', '$injector', function(scope, element, attrs, $injector) {
      var $interpolate = $injector.get('$interpolate');
      attrs.$set('name', $interpolate(attrs.name || '')(scope));
      $injector.invoke(controller, this, {
        '$scope': scope,
        '$element': element,
        '$attrs': attrs
      });
    }];
    return $delegate;
  }]);

  $provide.decorator('formDirective', ['$delegate', function($delegate) {
    var form = $delegate[0], controller = form.controller;
    form.controller = ['$scope', '$element', '$attrs', '$injector', function(scope, element, attrs, $injector) {
      var $interpolate = $injector.get('$interpolate');
      attrs.$set('name', $interpolate(attrs.name || attrs.ngForm || '')(scope));
        $injector.invoke(controller, this, {
        '$scope': scope,
        '$element': element,
        '$attrs': attrs
      });
    }];
    return $delegate;
  }]);

  if (!Array.prototype.find) {
    Array.prototype.find = function (callback, context) {
      if (this == null) {
        throw new TypeError('Array.prototype.find called on null or undefined');
      }
      if (typeof callback !== 'function') {
        throw new TypeError(callback + ' is not a function');
      }
      var list = Object(this),
        length = list.length >>> 0,
        value;
      for (var i = 0; i < length; i++) {
        value = list[i];
        if (callback.call(context, value, i, list)) {
          return value;
        }
      }
      return undefined;
    };
  }
}]);
