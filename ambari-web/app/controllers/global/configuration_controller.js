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

  /**
   * get configs by tags
   * return Deferred object with configs as argument
   * @param tags
   * @return {object}
   */
  getConfigsByTags: function (tags) {
    var storedTags = [];
    App.db.getConfigs().forEach(function (site) {
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
    var dfd = $.Deferred();
    var configs = App.db.getConfigs().filter(function (site) {
      return (siteNames.contains(site.type));
    });
    dfd.resolve(configs);
    return dfd.promise()
  },
  /**
   * load configs from server
   * and update them in local DB
   * @param tags
   * @return {Array}
   */
  loadFromServer: function (tags) {
    var dfd = $.Deferred();
    var loadedConfigs = [];
    var self = this;

    App.config.loadConfigsByTags(tags).done(function (data) {
      if (data.items) {
        data.items.forEach(function (item) {
          App.config.loadedConfigurationsCache[item.type + "_" + item.tag] = item.properties;
          loadedConfigs.push(item);
        });
      }
    }).complete(function () {
        self.saveToDB(loadedConfigs);
        dfd.resolve(loadedConfigs);
      });
    return dfd.promise();
  },

  /**
   * save properties obtained from server to local DB
   * @param loadedConfigs
   */
  saveToDB: function (loadedConfigs) {
    var storedConfigs = App.db.getConfigs();
    loadedConfigs.forEach(function (loadedSite) {
      var storedSite = storedConfigs.findProperty('type', loadedSite.type);
      if (storedSite) {
        storedSite.tag = loadedSite.tag;
        storedSite.properties = loadedSite.properties;
        storedSite.properties_attributes = loadedSite.properties_attributes;
      } else {
        storedConfigs.push(loadedSite);
      }
    });
    App.db.setConfigs(storedConfigs);
  }
});
