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

var PATH_PARAM_NAME = "viewPath";
var ENV = ENV || {};

/**
 * Returns view name, version and instance name from location.pathname
 *
 * @return {String[]} [view name, version, instance name]
 */
function getViewInfoFromPathname() {
  return location.pathname.split('/').filter(function(i) {
    return i !== "";
  }).filter(function(i, index, arr) {
    return index >= arr.length - 3;
  });
};

/**
 * Constructs URL for fetching Ambari view instance parameters.
 * @return {String}
 */
function getStatusURL() {
  var urlParts = getViewInfoFromPathname();

  return "/api/v1/views/%@/versions/%@/instances/%@/resources/status".fmt(
    urlParts[0],
    urlParts[1],
    urlParts[2]
  );
}

function getStatus() {
  var hashArray = location.pathname.split('/');

  return $.ajax({
    type: 'GET',
    dataType: 'json',
    async: true,
    context: this,
    url: getStatusURL(),
  });
}

/**
 * Creates an object from query string
 * @param getQueryObject {String}
 * @return {Object}
 */
function getQueryObject(queryString) {
  queryString = queryString ? queryString.replace('?', '') : '';

  return queryString.split('&').reduce(function (obj, param) {
    var paramParts;
    if(param.trim()) {
      paramParts = param.split('=');
      if(paramParts[0] == PATH_PARAM_NAME) {
        paramParts[1] = decodeURIComponent(paramParts[1]);
      }
      obj[paramParts[0]] = paramParts[1];
    }
    return obj;
  }, {});
}

/**
 * Creates query string from an object
 * @param getQueryObject {String}
 * @return {Object}
 */
function getQueryString(object) {
  var params = [];

  function addParam(key, value) {
    params.push('%@=%@'.fmt(key, value));
  }

  object = $.extend({}, object);

  // Because of the way Ambari handles viewPath, its better to put it at the front
  if(object.hasOwnProperty(PATH_PARAM_NAME)) {
    addParam(
      PATH_PARAM_NAME,
      encodeURIComponent(object[PATH_PARAM_NAME])
    );
    delete object[PATH_PARAM_NAME];
  }
  $.each(object, addParam);

  return params.join('&');
}

// Redirect if required
function redirectionCheck() {
  var href = window.location.href;

  // Ember expects the url to have /#/
  if(href.indexOf('?') != -1 && href.indexOf('/#/') == -1) {
    href = href.replace('?', '/#/?');
  }

  // If opened outside ambari, redirect
  if(window.parent == window) {
    var hrefParts = href.split('/#/'),
        pathParts = hrefParts[1].split('?'),
        queryParams =getQueryObject(pathParts[1]);

        if(pathParts[0]) {
          queryParams[PATH_PARAM_NAME] = '/#/' + pathParts[0];
        }

    href = '%@?%@'.fmt(
      hrefParts[0].replace('/views/', '/#/main/views/'),
      getQueryString(queryParams)
    );
  }

  // Normalize href
  href = href.replace(/\/\//g, '/').replace(':/', '://');

  if(href != window.location.href) {
    window.location = href;
    return true;
  }
}

// Force Ambari iframe to support fullscreen
function allowFullScreen() {
  if(window.parent) {
    var arrFrames = parent.document.getElementsByTagName("IFRAME"),
        iframe;
    for (var i = 0; i < arrFrames.length; i++) {
      if (arrFrames[i].contentWindow === window) {
        iframe = arrFrames[i];
        break;
      }
    }

    if(iframe) {
      iframe.setAttribute('AllowFullScreen', true);
    }
  }
}

// Replace Ambari's url on view URL change
function onPathChange() {
  var path = window.location.hash.substr(2).trim(),
      pathParts = path.split('?'),

      parentUrlParts = window.parent.location.href.split('?'),
      parentQueryParam = getQueryObject(pathParts[1]);

  if(pathParts[0]) {
    parentQueryParam[PATH_PARAM_NAME] = '/#/' + pathParts[0];
  }

  path = getQueryString(parentQueryParam);
  window.parent.history.replaceState(
    null,
    null,
    path ? '%@?%@'.fmt(parentUrlParts[0], path) : parentUrlParts[0]
  );
}

function scheduleChangeHandler() {
  if(typeof this._super === 'function') {
    this._super.apply(this, arguments);
  }
  setTimeout(onPathChange, 100);
}

function getConfigs(parameters) {
  var host = window.location.protocol +
      "//" +
      window.location.hostname +
      (window.location.port ? ':' + window.location.port: ''),
      urlParts = getViewInfoFromPathname(),
      // .replace() call is necessary to work properly behind the proxy
      resourcesPrefix = '/api/v1/views/%@/versions/%@/instances/%@/resources/'.replace(/^\//, '').fmt(
        urlParts[0],
        urlParts[1],
        urlParts[2]
      );

  parameters = parameters || {};

  return {
    host: host,
    yarnProtocol: parameters["yarn.protocol"],
    resourcesPrefix: resourcesPrefix,
    namespaces: {
      webService: {
        timeline: '%@atsproxy/ws/v1/timeline'.fmt(resourcesPrefix),
        appHistory: '%@atsproxy/ws/v1/applicationhistory'.fmt(resourcesPrefix),
        rm: '%@rmproxy/ws/v1/cluster'.fmt(resourcesPrefix),
        am: '%@rmproxy/proxy/{app_id}/ws/v{version:2}/tez'.fmt(resourcesPrefix)
      },
      web: {
        rm: '%@rmredirect/cluster'.fmt(resourcesPrefix)
      },
    }
  };
}

function setUI1Configs(parameters) {
  var configs = getConfigs(parameters);

  $.extend(true, App.Configs, {
    envDefaults: {
      isStandalone: false,
      timelineBaseUrl: configs.host,
      RMWebUrl: configs.host,
      yarnProtocol: configs.yarnProtocol,
    },
    restNamespace: {
      timeline: configs.namespaces.webService.timeline,
      applicationHistory: configs.namespaces.webService.appHistory,

      aminfo: '%@rmproxy/proxy/__app_id__/ws/v1/tez'.fmt(configs.resourcesPrefix),
      aminfoV2: '%@rmproxy/proxy/__app_id__/ws/v2/tez'.fmt(configs.resourcesPrefix),
      cluster: configs.namespaces.webService.rm
    },
    otherNamespace: {
      cluster: configs.namespaces.web.rm
    }
  });

  App.TimelineRESTAdapter.reopen({
    namespace: App.Configs.restNamespace.timeline
  });
}

function setUI2Configs(parameters) {
  var configs = getConfigs(parameters);

  $.extend(true, ENV, {
    isStandalone: false,
    yarnProtocol: configs.yarnProtocol,
    hosts: {
      timeline: configs.host,
      rm: configs.host,
    },
    namespaces: configs.namespaces
  });
};

function setAppRoute(AppRoute) {
  AppRoute.reopen({
    actions: {
      didTransition: scheduleChangeHandler,
      queryParamsDidChange: scheduleChangeHandler
    }
  });
}

function loadParams(app, callBack) {
  app.deferReadiness();
  getStatus().always(function(status) {
    callBack(status || {});
    app.advanceReadiness();
  });
}

if(!redirectionCheck()) {
  var initializerOptions = {
        name: 'configs',
      },
      App = window.App;

  if(App) {
    initializerOptions.initialize = function () {
      setAppRoute(App.ApplicationRoute);
      loadParams(App, setUI1Configs);
    };
  }
  else {
    initializerOptions.before = 'env';
    initializerOptions.initialize = function (app) {
      setAppRoute(app.get("__container__").lookup('route:application'));
      loadParams(app, setUI2Configs);
    };
  }
  Ember.Application.initializer(initializerOptions);

  allowFullScreen();
}
