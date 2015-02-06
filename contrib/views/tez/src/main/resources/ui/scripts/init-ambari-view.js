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

App.deferReadiness();

var TEZ_PARAM = "tezPath";

function createParam(path) {
  return '%@=%@'.fmt(TEZ_PARAM, encodeURIComponent(path));
}

function getSetParam(queryString, path) {
  if(queryString) {
    var params = queryString.split('&'),
        param;

    for(var i = 0, count = params.length; i < count; i++) {
      param = params[i];

      if(param.substr(0, TEZ_PARAM.length) == TEZ_PARAM) {
        if(path != undefined) {
          if(path == '') {
            params.splice(i, 1);
          }
          else {
            params[i] = createParam(path);
          }
          return params.join('&');
        }
        else {
          return decodeURIComponent(param.split('=')[1]);
        }
      }
    }
  }
  // If param was empty and path is available, set.
  return (path != undefined && path != '') ? createParam(path) : '';
}

// Redirect if required
function redirectionCheck() {
  var href = window.location.href;

  // If opened outside ambari, redirect
  if(window.parent == window) {
    var hrefParts = href.split('/#/'),
        ambariLink = hrefParts[0].replace('/views/', '/#/main/views/');

    href = '%@?%@'.fmt(ambariLink, getSetParam('', hrefParts[1] || ''));
  }
  // If opened inside ambari with a path different from current path redirect iframe
  else {
    var parentHref = window.parent.location.href.split('?'),
        tezPath = getSetParam(parentHref[1]),
        currentPath = href.split('/#/')[1];

    if(tezPath && tezPath != currentPath) {
      href = '%@#/%@'.fmt(parentHref[0].replace('/#/main/views/', '/views/'), tezPath);
    }
  }

  if(href != window.location.href) {
    window.location = href;
    return true;
  }
}

App.Helpers.ambari = (function () {
  /**
   * Stores parameters from Ambari.
   */
  var ambariParameters = {},

  /**
   * Constructs URL for fetching Ambari view instance parameters.
   * @return {String}
   */
  getURL = function () {
    var urlParts = location.pathname.split('/');

    return "/api/v1/views/%@/versions/%@/instances/%@/resources/status".fmt(
      urlParts[2],
      urlParts[3],
      urlParts[4]
    );
  };

  return {
    /**
     * Key for the property representing ATS url.
     */
    TIMELINE_URL: 'yarn.timeline-server.url',
    RM_URL: 'yarn.resourcemanager.url',

    /**
     * Returns parameter value.
     * @param {String} name
     * @return {String}
     * @method getParam
     */
    getParam: function (name) {
      Ember.assert('Parameter not found!', ambariParameters[name]);
      return ambariParameters[name];
    },

    /**
     * Get view instance properties provided by user.
     * @returns {$.ajax}
     * @method getInstanceParameters
     */
    getInstanceParameters: function () {
      var hashArray = location.pathname.split('/'),
      params = {
        view: hashArray[2],
        version: hashArray[3],
        instanceName: hashArray[4],
        clusterName: App.get('clusterName')
      };

      return $.ajax({
        type: 'GET',
        dataType: 'json',
        async: true,
        context: this,
        url: getURL(),
        success: this.getInstanceParametersSuccessCallback,
        error: this.getInstanceParametersErrorCallback,
      });
    },

    /**
     * Success callback for getInstanceParameters-request.
     * @param {object} data
     * @method getInstanceParametersSuccessCallback
     */
    getInstanceParametersSuccessCallback: function (data) {
      Ember.assert('Ambari instance parameter weren`t returned by the service!', data.parameters);
      ambariParameters = data.parameters;
    },

    /**
     * Error callback for getInstanceParameters-request.
     * @method getInstanceParametersErrorCallback
     */
    getInstanceParametersErrorCallback: function (request, ajaxOptions, error) {
      Ember.assert('Ambari instance parameter fetch failed: ' + error);
    }
  };

})();

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

function loadParams() {
  App.Helpers.ambari.getInstanceParameters().then(function () {
    $.extend(true, App.Configs, {
      envDefaults: {
        isStandalone: false,
        timelineBaseUrl: App.Helpers.ambari.getParam(App.Helpers.ambari.TIMELINE_URL),
        RMWebUrl: App.Helpers.ambari.getParam(App.Helpers.ambari.RM_URL)
      }
    });

    App.advanceReadiness();
  });
}

function onPathChange() {
  var path = window.location.hash.substr(2).trim(),
      urlParts = window.parent.location.href.split('?'),
      queryParam = getSetParam(urlParts[1], path);
  window.parent.history.replaceState(
    null,
    null,
    queryParam ? '%@?%@'.fmt(urlParts[0], queryParam) : urlParts[0]
  );
}

if(!redirectionCheck()) {
  App.ApplicationRoute.reopen({
    actions: {
      didTransition: function (arguments) {
        setTimeout(onPathChange, 100);
      }
    }
  });

  allowFullScreen();
  loadParams();

  Ember.$.ajaxSetup({
    beforeSend: function (jqXHR, settings) {
      settings.url = location.pathname + 'proxy?url=' + encodeURIComponent(settings.url);
    }
  });
}
