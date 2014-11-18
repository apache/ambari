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

  /**
   * installation status of stack version on hosts
   * {String}
   */
  status: function() {
    if (this.get('controller.installInProgress'))  {
      return 'INSTALLING'
    } else if (this.get('controller.allInstalled')) {
      return 'ALL_INSTALLED';
    } else {
      return 'INSTALL';
    }
  }.property('controller.installInProgress', 'controller.allInstalled'),

  /**
   * text on install buttons
   * {String}
   */
  stackTextStatus: function() {
    switch(this.get('status')) {
      case 'INSTALLING':
        return Em.I18n.t('admin.stackVersions.datails.hosts.btn.installing');
        break;
      case 'ALL_INSTALLED':
        return Em.I18n.t('admin.stackVersions.datails.hosts.btn.nothing');
        break;
      case 'INSTALL':
        return Em.I18n.t('admin.stackVersions.datails.hosts.btn.install').format(this.get('controller.content.notInstalledHostStacks.length'));
        break;
    }
  }.property('status', 'controller.content.notInstalledHostStacks.length'),

  /**
   * class on install buttons
   * {String}
   */
  statusClass: function() {
    switch (this.get('status')) {
      case 'INSTALL':
        return 'btn-success';
        break;
      case 'INSTALLING':
        return 'btn-primary';
        break;
      default:
        return 'disabled';
    }
  }.property('status')

});
