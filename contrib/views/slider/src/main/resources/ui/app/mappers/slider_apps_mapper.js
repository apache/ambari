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
 * Mapper for SLIDER_1 status
 * Save mapped data to App properties
 * @type {App.Mapper}
 */
App.SliderAppsMapper = App.Mapper.createWithMixins(App.RunPeriodically, {

  /**
   * Url suffix
   * Used with <code>App.urlPrefix</code>
   * @type {string}
   */
  urlSuffix: 'apps/?fields=*',

  /**
   * Load data from <code>App.urlPrefix + this.urlSuffix</code> one time
   * @method load
   * @return {$.ajax}
   */
  load: function() {
    var self = this,
      url = App.get('testMode') ? '/data/apps/apps.json' : App.get('urlPrefix') + this.get('urlSuffix');

    return $.ajax({
      url: url,
      dataType: 'json',
      async: true,
      success: function(data) {self.parse(data);}
    });
  },

  /**
   * Parse loaded data according to <code>map</code>
   * Set <code>App</code> properties
   * @param {object} data received from server data
   * @method parse
   */
  parse: function(data) {
    var apps = [];
    var quickLinks = [];
    quickLinks.push(
      Ember.Object.create({
        id: 'YARN application',
        label: 'YARN application',
        url: "http://"+window.location.hostname+":8088"
      })
    );
    data.items.forEach(function(app) {
      apps.push(
        Ember.Object.create({
          id: app.id,
          index: app.id,
          yarnId: app.yarnId,
          name: app.name,
          status: app.state,
          user: app.user,
          started: app.startTime,
          ended: app.endTime,
          appType: app.type,
          diagnostics: app.diagnostics,
          components: app.components,
          quickLinks: ["YARN application"],
          runtimeProperties: app.configs
        })
      );
    });
    App.SliderApp.store.pushMany('quickLink', quickLinks);
    App.SliderApp.store.pushMany('sliderApp', apps);
  }
});
