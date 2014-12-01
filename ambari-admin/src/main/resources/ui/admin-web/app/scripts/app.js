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
	baseUrl: '/api/v1',
  testMode: (window.location.port == 8000),
  mockDataPrefix: 'assets/data/'
})
.config(['RestangularProvider', '$httpProvider', '$provide', function(RestangularProvider, $httpProvider, $provide) {
  // Config Ajax-module
  RestangularProvider.setBaseUrl('/api/v1');
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

  $httpProvider.responseInterceptors.push(['$rootScope', '$q', function (scope, $q) {
    function success(response) {
      return response;
    }

    function error(response) {
      if (response.status == 403) {
        window.location = "/";
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
}]);
