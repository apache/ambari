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

/**
 * Mapper for <code>App.SliderApp</code> and <code>App.QuickLink</code> models
 * @type {App.Mapper}
 */
App.SliderAppsMapper = App.Mapper.createWithMixins(App.RunPeriodically, {

  /**
   * List of app state display names
   */
  stateMap: {
    'FROZEN': 'STOPPED',
    'THAWED': 'RUNNING'
  },

  /**
   * @type {bool}
   */
  isWarningPopupShown: false,

  /**
   * @type {bool}
   */
  isChained: true,
  /**
   * Load data from <code>App.urlPrefix + this.urlSuffix</code> one time
   * @method load
   * @return {$.ajax}
   */
  load: function () {
    var self = this;
    var dfd = $.Deferred();

    App.ajax.send({
      name: 'mapper.applicationApps',
      sender: this,
      success: 'parse'
    }).fail(function(jqXHR, textStatus){
        App.__container__.lookup('controller:application').set('hasConfigErrors', true);
        if (!self.get('isWarningPopupShown')) {
          var message = textStatus === "timeout" ? "timeout" : jqXHR.responseText;
          self.set('isWarningPopupShown', true);
          window.App.__container__.lookup('controller:SliderApps').showUnavailableAppsPopup(message);
        }
      }).complete(function(){
        dfd.resolve();
      });
    return dfd.promise();
  },

  /**
   * close warning popup if apps became available
   * @return {*}
   */
  closeWarningPopup: function() {
    if (Bootstrap.ModalManager.get('apps-warning-modal')) {
      Bootstrap.ModalManager.close('apps-warning-modal');
    }
  },

  /**
   * Parse loaded data
   * Load <code>App.Alert</code> model
   * @param {object} data received from server data
   * @method parse
   */
  parseAlerts: function (data) {
    var alerts = [],
      appId = data.id;

    if (data.alerts && data.alerts.detail) {
      data.alerts.detail.forEach(function (alert) {
        alerts.push({
          id: appId + alert.description,
          title: alert.description,
          serviceName: alert.service_name,
          status: alert.status,
          message: alert.output,
          hostName: alert.host_name,
          lastTime: alert.status_time,
          appId: appId,
          lastCheck: alert.last_status_time
        });
      });
      alerts = alerts.sortBy('title');
      App.SliderApp.store.pushMany('sliderAppAlert', alerts);
    }
    return alerts.mapProperty('id');
  },

  /**
   * Parse loaded data
   * Load <code>App.SliderAppComponent</code> model
   * @param {object} data received from server data
   * @method parse
   */
  parseComponents: function (data) {
    var components = [],
      appId = data.id;

    Object.keys(data.components).forEach(function (key) {
      var component = data.components[key],
        activeContainers = Object.keys(component.activeContainers);
      for (var i = 0; i < component.instanceCount; i++) {
        components.pushObject(
          Ember.Object.create({
            id: appId + component.componentName + i,
            status: activeContainers[i] ? "Running" : "Stopped",
            host: activeContainers[i] ? component.activeContainers[activeContainers[i]].host : "",
            containerId: activeContainers[i] ? component.activeContainers[activeContainers[i]].name : "",
            componentName: component.componentName,
            appId: appId
          })
        );
      }
    });
    App.SliderApp.store.pushMany('sliderAppComponent', components);
    return components.mapProperty('id');
  },

  /**
   * Parse loaded data
   * Load <code>App.SliderApp.configs</code> model
   * @param {object} data received from server data
   * @method parse
   */
  parseConfigs: function (data) {
    var configs = {};
    Object.keys(data.configs).forEach(function (key) {
      configs[key] = data.configs[key];
    });
    return configs;
  },

  /**
   * Parse loaded data
   * Load <code>App.QuickLink</code> model
   * @param {object} data received from server data
   * @method parse
   */
  parseQuickLinks : function(data) {
    var quickLinks = [],
      appId = data.id,
      yarnAppId = appId,
      index = appId.lastIndexOf('_');
    if (index > 0) {
      yarnAppId = appId.substring(0, index + 1);
      for (var k = (appId.length - index - 1); k < 4; k++) {
        yarnAppId += '0';
      }
      yarnAppId += appId.substring(index + 1);
    }
    var yarnUI = "http://"+window.location.hostname+":8088",
      viewConfigs = App.SliderApp.store.all('sliderConfig');
    if (!Em.isNone(viewConfigs)) {
      var viewConfig = viewConfigs.findBy('viewConfigName', 'yarn.rm.webapp.url');
      if (!Em.isNone(viewConfig)) {
        yarnUI = 'http://' + viewConfig.get('value');
      }
    }
    quickLinks.push(
      Ember.Object.create({
        id: 'YARN application ' + yarnAppId,
        label: 'YARN application',
        url: yarnUI + '/cluster/app/application_' + yarnAppId
      })
    );

    if(!data.urls){
      App.SliderApp.store.pushMany('QuickLink', quickLinks);
      return quickLinks.mapProperty('id');
    }

    Object.keys(data.urls).forEach(function (key) {
      quickLinks.push(
        Ember.Object.create({
          id: appId+key,
          label: key,
          url: data.urls[key]
        })
      );
    });
    App.SliderApp.store.pushMany('QuickLink', quickLinks);
    return quickLinks.mapProperty('id');
  },

  parseObject: function (o) {
    if (Ember.typeOf(o) !== 'object') return [];
    return Ember.keys(o).map(function (key) {
      return {key: key, value: o[key]};
    });
  },

  /**
   * Concatenate <code>supportedMetrics</code> into one string
   * @param {object} app
   * @returns {string}
   * @method parseMetricNames
   */
  parseMetricNames : function(app) {
    if (app.supportedMetrics) {
      return app.supportedMetrics.join(",");
    }
    return "";
  },

  /**
   * Parse loaded data
   * Load <code>App.SliderApp</code> model
   * @param {object} data received from server data
   * @method parse
   */
  parse: function (data) {
    var apps = [],
      self = this,
      appsToDelete = App.SliderApp.store.all('sliderApp').mapBy('id');

    App.__container__.lookup('controller:application').set('hasConfigErrors', false);

    if (this.get('isWarningPopupShown')) {
      this.closeWarningPopup();
      this.set('isWarningPopupShown', false);
    }

    data.apps.forEach(function (app) {
      var componentsId = app.components ? self.parseComponents(app) : [],
        configs = app.configs ? self.parseConfigs(app) : {},
        quickLinks = self.parseQuickLinks(app),
        alerts = self.parseAlerts(app),
        jmx = self.parseObject(app.jmx),
        metricNames = self.parseMetricNames(app),
        masterActiveTime = jmx.findProperty('key', 'MasterActiveTime'),
        masterStartTime = jmx.findProperty('key', 'MasterStartTime');
      if (masterActiveTime) {
        masterActiveTime.value = new Date(Date.now() - masterActiveTime.value).getHours() + "h:" + new Date(Date.now() - masterActiveTime.value).getMinutes() + "m";
      }
      if (masterStartTime) {
        masterStartTime.value = (new Date(parseInt(masterStartTime.value)).toUTCString());
      }
      apps.push(
        Ember.Object.create({
          id: app.id,
          yarnId: app.yarnId,
          name: app.name,
          status: app.state,
          displayStatus: self.stateMap[app.state] || app.state,
          user: app.user,
          started: app.startTime || 0,
          ended: app.endTime  || 0,
          appType: app.typeId,
          diagnostics: app.diagnostics || "-",
          description: app.description || "-",
          components: componentsId,
          quickLinks: quickLinks,
          alerts: alerts,
          configs: configs,
          jmx: jmx,
          runtimeProperties: app.configs,
          supportedMetricNames: metricNames
        })
      );

      appsToDelete = appsToDelete.without(app.id);
    });
    appsToDelete.forEach(function (app) {
      var appRecord = App.SliderApp.store.getById('sliderApp', app);
      if (appRecord) {
        appRecord.deleteRecord();
      }
    });
    apps.forEach(function(app) {
      App.SliderApp.store.push('sliderApp', app, true);
    });
  }
});
