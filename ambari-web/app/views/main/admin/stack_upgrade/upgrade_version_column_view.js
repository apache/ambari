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
var stringUtils = require('utils/string_utils');

App.UpgradeVersionColumnView = App.UpgradeVersionBoxView.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_version_column'),
  isVersionColumnView: true,
  classNames: ['version-column'],

  didInsertElement: function () {
    App.tooltip($('.out-of-sync-badge'), {title: Em.I18n.t('hosts.host.stackVersions.status.out_of_sync')});
    App.tooltip($('.not-upgradable'), {title: Em.I18n.t('admin.stackVersions.version.service.notUpgradable')});
    if (!this.get('content.isCompatible')) {
      App.tooltip(this.$(".repo-version-tooltip"), {
        title: Em.I18n.t('admin.stackVersions.version.noCompatible.tooltip')
      });
    }

    var height = App.Service.find().get('length') > 10 ? ((App.Service.find().get('length') - 10) * 40 + 500) : 500;
    $('.version-column').height(height);

    // fix the line up minor diff issue in FireFox
    if ($.browser.mozilla) {
      $('.line-separator').css('top', '-6px');
      $('.line-separator-bottom').css('top', '-4px');
    }
  },

  services: function() {
    var originalServices = this.get('content.stackServices');
    var isStandard = this.get('content.isStandard');
    // sort the services in the order the same as service menu
    return App.Service.find().map(function (service) {
      var stackService = originalServices.findProperty('name', service.get('serviceName'));
      var isAvailable = this.isStackServiceAvailable(stackService);
      return Em.Object.create({
        displayName: service.get('displayName'),
        name: service.get('serviceName'),
        latestVersion: stackService ? stackService.get('latestVersion') : '',
        isVersionInvisible: !stackService,
        notUpgradable: !this.get('content.isStandard')  && isAvailable && !stackService.get('isUpgradable'),
        isAvailable: isAvailable
      });
    }, this);
  }.property(),

  /**
   * @param {Em.Object} stackService
   * @returns {boolean}
   */
  isStackServiceAvailable: function(stackService) {
    var appliedPatchVersions = App.RepositoryVersion.find().filterProperty('isPatch').filterProperty('status', 'CURRENT');
    var appliedPatchServices = [];
    appliedPatchVersions.forEach(function(version) {
      var availableServices = version.get('stackServices').toArray().filterProperty('isAvailable').mapProperty('name');
      appliedPatchServices = appliedPatchServices.concat(availableServices).uniq();
    });
    if (stackService) {
      if (this.get('content.isStandard') && appliedPatchServices.contains(stackService.get('name'))) {
        return false;
      }
      return stackService.get('isAvailable');
    }
    return false;
  },

  /**
   * on click handler for "show details" link
   */
  openVersionBoxPopup: function (event) {
    var content = this.get('content');
    var parentView = this.get('parentView');

    return App.ModalPopup.show({
      classNames: ['version-box-popup'],
      bodyClass: App.UpgradeVersionBoxView.extend({
        classNames: ['version-box-in-popup'],
        content: content,
        parentView: parentView
      }),
      header: Em.I18n.t('admin.stackVersions.version.column.showDetails.title'),
      primary: Em.I18n.t('common.dismiss'),
      secondary: null
    });
  }
});

