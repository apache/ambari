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
   * Load data from <code>App.urlPrefix + this.urlSuffix</code> one time
   * @method load
   * @return {$.ajax}
   */
  load: function() {
    return App.ajax.send({
      name: 'mapper.applicationApps',
      sender: this,
      success: 'parse'
    });
  },

  /**
   * Parse loaded data
   * Load <code>App.SliderAppComponent</code> model
   * @param {object} data received from server data
   * @method parse
   */
  parseComponents: function(data) {
    var components = [],
    appId = data.id;

    Object.keys(data.components).forEach(function (key) {
      var component = data.components[key];
      activeContainers = Object.keys(component.activeContainers);
      for(var i= 0; i < component.instanceCount; i++){
        components.pushObject(
          Ember.Object.create({
            id: appId + component.componentName + i,
            status: activeContainers[i] ? "Running" : "Stopped",
            host: activeContainers[i] ? component.activeContainers[activeContainers[i]].host : "",
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
  parseConfigs : function(data) {
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
    appId = data.id;
    quickLinks.push(
      Ember.Object.create({
        id: 'YARN application',
        label: 'YARN application',
        url: "http://"+window.location.hostname+":8088"
      })
    );

    if(!data.urls){
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

  parseObject: function(o) {
    if (Ember.typeOf(o) !== 'object') return [];
    return Ember.keys(o).map(function(key) {
      return {key: key, value: o[key]};
    });
  },

  /**
   * Parse loaded data
   * Load <code>App.SliderApp</code> model
   * @param {object} data received from server data
   * @method parse
   */
  parse: function(data) {
    var apps = [],
    self = this,
    appsToDelete = App.SliderApp.store.all('sliderApp').get('content').mapProperty('id');

    data.items.forEach(function(app) {
      var componentsId = app.components ? self.parseComponents(app) : [],
      configs = app.configs ? self.parseConfigs(app) : {},
      quickLinks = self.parseQuickLinks(app),
      jmx = self.parseObject(app.jmx),
      masterActiveTime = jmx.findProperty('key', 'MasterActiveTime'),
      masterStartTime = jmx.findProperty('key', 'MasterStartTime');
      if(masterActiveTime){
        masterActiveTime.value = new Date(Date.now() - masterActiveTime.value).getHours() + "h:" + new Date(Date.now() - masterActiveTime.value).getMinutes() + "m";
      }
      if(masterStartTime){
        masterStartTime.value = (new Date(parseInt(masterStartTime.value)).toUTCString());
      }
      apps.push(
        Ember.Object.create({
          id: app.id,
          yarnId: app.yarnId,
          name: app.name,
          status: app.state,
          user: app.user,
          started: app.startTime ? (new Date(app.startTime).toUTCString()) : "-",
          ended: app.endTime ? (new Date(app.endTime).toUTCString()) : "-",
          appType: app.type.toUpperCase(),
          diagnostics: app.diagnostics || "-",
          description: app.description || "-",
          components: componentsId,
          quickLinks: quickLinks,
          configs: configs,
          jmx: jmx,
          runtimeProperties: app.configs
        })
      );

      appsToDelete.splice(appsToDelete.indexOf(app.id), 1);
    });

    appsToDelete.forEach(function (app) {
      var appRecord = App.SliderApp.store.getById('sliderApp', app);
      if(appRecord){
        appRecord.destroyRecord();
      }
    });
    App.SliderApp.store.pushMany('sliderApp', apps);
  }
});
