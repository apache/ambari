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

App.MainAdminServiceAutoStartView = Em.View.extend({
  templateName: require('templates/main/admin/service_auto_start'),
  /**
   * Value used in the checkbox.
   *
   * @property switcherValue
   * @type {boolean}
   */
  switcherValue: false,

  savedRecoveryEnabled: false,

  didInsertElement: function () {
    var self = this;
    this.get('controller').loadClusterConfig().done(function (data) {
      var tag = [
        {
          siteName: 'cluster-env',
          tagName: data.Clusters.desired_configs['cluster-env'].tag,
          newTagName: null
        }
      ];
      App.router.get('configurationController').getConfigsByTags(tag).done(function (data) {
        self.set('controller.clusterConfigs', data[0].properties);
        self.set('switcherValue', data[0].properties.recovery_enabled === 'true');
        self.set('savedRecoveryEnabled', self.get('switcherValue'));
        // plugin should be initiated after applying binding for switcherValue
        Em.run.later('sync', function() {
          self.initSwitcher();
        }.bind(self), 10);
        self.get('controller').loadComponentsConfigs();
      });
    });
  },

  /**
   * Init switcher plugin.
   *
   * @method initSwitcher
   */
  updateClusterConfigs: function (state){
    this.set('switcherValue', state);
    this.set('controller.clusterConfigs.recovery_enabled', '' + state);
    this.set('controller.valueChanged', this.get('savedRecoveryEnabled') !== state);
  },

  /**
   * Init switcher plugin.
   *
   * @method initSwitcher
   */
  initSwitcher: function () {
    var self = this;
    if (this.$()) {
      this.$("input:eq(0)").bootstrapSwitch({
        onText: Em.I18n.t('common.enabled'),
        offText: Em.I18n.t('common.disabled'),
        offColor: 'default',
        onColor: 'success',
        handleWidth: Math.max(Em.I18n.t('common.enabled').length, Em.I18n.t('common.disabled').length) * 8,
        onSwitchChange: function (event, state) {
          self.updateClusterConfigs(state);
        }
      });
    }
  }
});

