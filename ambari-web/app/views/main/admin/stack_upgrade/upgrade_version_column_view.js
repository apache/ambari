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
    App.tooltip($('.link-tooltip'), {title: Em.I18n.t('admin.stackVersions.version.linkTooltip')});
    App.tooltip($('.out-of-sync-badge'), {title: Em.I18n.t('hosts.host.stackVersions.status.out_of_sync')});

    //set the width, height of each version colum dynamically
    var widthFactor = App.RepositoryVersion.find().get('length') > 3 ? 0.18: 0.31;
    $('.version-column').width($('.versions-slides').width() * widthFactor);
    var height = App.Service.find().get('length') > 10 ? ((App.Service.find().get('length') - 10) * 40 + 500) : 500;
    $('.version-column').height(height);

    // fix the line up minor diff issue in FireFox
    if ($.browser.mozilla) {
      $('.line-separator').css('top', '-6px');
      $('.line-separator-bottom').css('top', '-4px');
    }
  },

  services: function() {
    var repoRecord = App.RepositoryVersion.find(this.get('content.id'));
    var originalServices = repoRecord.get('services');
    // sort the services in the order the same as service menu
    var sorted = App.Service.find().map(function (service) {
      var latestVersion = '';
      if (originalServices.someProperty('name', service.get('serviceName'))){
        latestVersion = originalServices.filterProperty('name', service.get('serviceName'))[0].get('latestVersion');
      }
      return Em.Object.create({
        displayName: service.get('displayName'),
        name: service.get('serviceName'),
        latestVersion: latestVersion,
        isVersionInvisible: latestVersion == false
      });
    });
    return sorted;
  }.property(),

  /**
   * map of properties which correspond to particular state of Upgrade version
   * @type {object}
   */
  statePropertiesMap: {
    'CURRENT': {
      isLabel: true,
      text: Em.I18n.t('common.current'),
      class: 'label label-success'
    },
    'INIT': {
      isButton: true,
      text: Em.I18n.t('common.install'),
      action: 'installRepoVersionConfirmation'
    },
    'LOADING': {
      isSpinner: true,
      class: 'spinner'
    },
    'INSTALLING': {
      iconClass: 'icon-cog',
      isLink: true,
      text: Em.I18n.t('hosts.host.stackVersions.status.installing'),
      action: 'showProgressPopup'
    },
    'INSTALLED': {
      iconClass: 'icon-ok',
      isLink: true,
      text: Em.I18n.t('common.installed'),
      action: null
    },
    'SUSPENDED': {
      isButton: true,
      text: Em.I18n.t('admin.stackUpgrade.dialog.resume'),
      action: 'resumeUpgrade'
    }
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

