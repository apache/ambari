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

App.MainStackVersionsDetailsView = Em.View.extend({

  templateName: require('templates/main/admin/stack_versions/stack_version_details'),

  content: function() {
    return this.get('controller.content')
  }.property('controller.content'),
  /**
   * text on install buttons
   * {String}
   */
  stackTextStatus: function() {
    var self = this;
    switch(this.get('content.state')) {
      case 'UPGRADING':
      case 'INSTALLING':
        return self.get('content.state').toCapital().concat("...");
        break;
      case 'INSTALLED':
        return Em.I18n.t('admin.stackVersions.datails.hosts.btn.nothing');
        break;
      case 'INIT':
        return Em.I18n.t('admin.stackVersions.datails.hosts.btn.install').format(self.get('totalHostCount') - self.get('content.installedHosts.length'));
        break;
      default:
        return self.get('content.state') && self.get('content.state').toCapital();
    }
  }.property('content.state', 'content.notInstalledHostStacks.length'),

  /**
   * class on install buttons
   * {String}
   */
  statusClass: function() {
    switch (this.get('content.state')) {
      case 'INSTALL':
        return 'btn-success';
        break;
      case 'INSTALLING':
        return 'btn-primary';
        break;
      default:
        return 'disabled';
    }
  }.property('content.state'),

  didInsertElement: function() {
    App.get('router.mainStackVersionsController').set('isPolling', true);
    App.get('router.mainStackVersionsController').load();
    App.get('router.mainStackVersionsController').doPolling();
  },


  willDestroyElement: function () {
    App.get('router.mainStackVersionsController').set('isPolling', false);
    clearTimeout(App.get('router.mainStackVersionsController.timeoutRef'));
  }
});
