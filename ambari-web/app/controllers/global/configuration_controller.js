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

var App = require('app');

App.ConfigurationController = Em.Controller.extend({
  name: 'configurationController',

  getConfigsByTags: function (tags) {
    var storedTags = [];
    App.db.getConfigs().forEach(function(site){
      storedTags.push({
        siteName: site.type,
        tagName: site.tag
      })
    });
    if (this.checkTagsChanges(tags, storedTags)) {
      return this.loadFromServer(tags);
    } else {
      return this.loadFromDB(tags.mapProperty('siteName'));
    }
  },
  /**
   * check whether tag versions have been changed
   * if they are different then return true
   * otherwise false
   * @param tags
   * @param storedTags
   * @return {Boolean}
   */
  checkTagsChanges: function (tags, storedTags) {
    var isDifferent = false;
    var i = 0;
    while (i < tags.length && !isDifferent) {
      var storedTag = storedTags.findProperty('siteName', tags[i].siteName);
      isDifferent = (!storedTag || storedTag.tagName !== tags[i].tagName);
      i++;
    }
    return isDifferent;
  },
  loadFromDB: function (siteNames) {
    var configs = App.db.getConfigs();
    return configs.filter(function (site) {
      return (siteNames.contains(site.type));
    })
  },
  /**
   * load configs from server
   * and update them in local DB
   * @param tags
   * @return {Array}
   */
  loadFromServer: function (tags) {
    var loadedConfigs = App.config.loadConfigsByTags(tags);
    var storedConfigs = App.db.getConfigs();
    loadedConfigs.forEach(function (loadedSite) {
      var storedSite = storedConfigs.findProperty('type', loadedSite.type);
      if (storedSite) {
        storedConfigs.tag = loadedSite.tag;
        storedConfigs.properties = loadedSite.properties;
      } else {
        storedConfigs.push(loadedSite);
      }
    });
    App.db.setConfigs(storedConfigs);
    return loadedConfigs;
  }


});
