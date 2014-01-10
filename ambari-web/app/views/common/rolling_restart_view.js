/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

/**
 * View content of the rolling restart dialog.
 *
 * Callers provide the context in which this dialog is invoked.
 */
App.RollingRestartView = Em.View.extend({
  templateName : require('templates/common/rolling_restart_view'),
  hostComponentName : null,
  staleConfigsOnly : false,
  batchSize : -1,
  interBatchWaitTimeSeconds : -1,
  tolerateSize : -1,
  errors : null,
  initialize : function() {
    if (this.get('batchSize') == -1 && this.get('interBatchWaitTimeSeconds') == -1 && this.get('tolerateSize') == -1) {
      var restartCount = this.get('restartHostComponents');
      var batchSize = 1;
      if (restartCount > 10) {
        batchSize = Math.ceil(restartCount / 10);
      }
      var tolerateCount = batchSize;
      this.set('batchSize', batchSize);
      this.set('tolerateSize', tolerateCount);
      this.set('interBatchWaitTimeSeconds', 120);
    }
  },
  validate : function() {
    var displayName = this.get('hostComponentDisplayName');
    var totalCount = this.get('restartHostComponents.length');
    var bs = this.get('batchSize');
    var ts = this.get('tolerateSize');
    var wait = this.get('interBatchWaitTimeSeconds');
    var errors = [];
    if (totalCount < 1) {
      errors.push(Em.I18n.t('rollingrestart.dialog.msg.noRestartHosts').format(displayName));
    } else {
      if (!bs) {
        errors.push(Em.I18n.t('rollingrestart.dialog.err.empty.batchsize'));
      } else if (bs > totalCount || bs < 0) {
        errors.push(Em.I18n.t('rollingrestart.dialog.err.invalid.batchsize').format(totalCount));
      }
      if (!ts) {
        errors.push(Em.I18n.t('rollingrestart.dialog.err.empty.waittime'));
      } else if (ts < 0) {
        errors.push(Em.I18n.t('rollingrestart.dialog.err.invalid.toleratesize'));
      }
    }
    if (!wait) {
      errors.push(Em.I18n.t('rollingrestart.dialog.err.empty.tolerate'));
    } else if (wait < 0) {
      errors.push(Em.I18n.t('rollingrestart.dialog.err.invalid.waitTime'));
    }
    if (errors.length < 1) {
      errors = null;
    }
    this.set('errors', errors);
  }.observes('batchSize', 'interBatchWaitTimeSeconds', 'tolerateSize', 'restartHostComponents', 'hostComponentDisplayName'),
  hostComponentDisplayName : function() {
    return App.format.role(this.get('hostComponentName'));
  }.property('hostComponentName'),
  allHostComponents : function() {
    return App.HostComponent.find().filterProperty('componentName', this.get('hostComponentName'));
  }.property('hostComponentName'),
  nonMaintainanceHostComponents : function() {
    var hostComponents = this.get('allHostComponents');
    hostComponents = hostComponents.filter(function(item) {
      if (item.get('workStatus') !== App.HostComponentStatus.maintenance) {
        return true;
      }
    });
    return hostComponents;
  }.property('allHostComponents', 'allHostComponents.@each.workStatus'),
  restartHostComponents : function() {
    var hostComponents = this.get('nonMaintainanceHostComponents');
    if (this.get('staleConfigsOnly')) {
      hostComponents = hostComponents.filterProperty('staleConfigs', true);
    }
    return hostComponents;
  }.property('nonMaintainanceHostComponents', 'staleConfigsOnly'),
  restartMessage : function() {
    var rhc = this.get('restartHostComponents.length');
    if (rhc > 1) {
      return Em.I18n.t('rollingrestart.dialog.msg.restart.plural').format(rhc, this.get('hostComponentDisplayName'))
    }
    return Em.I18n.t('rollingrestart.dialog.msg.restart').format(rhc, this.get('hostComponentDisplayName'))
  }.property('restartHostComponents', 'hostComponentDisplayName'),
  maintainanceMessage : function() {
    var allCount = this.get('allHostComponents.length');
    var nonMaintainCount = this.get('nonMaintainanceHostComponents.length');
    var count = allCount - nonMaintainCount;
    if (count > 0) {
      var name = this.get('hostComponentDisplayName');
      if (count > 1) {
        return Em.I18n.t('rollingrestart.dialog.msg.maintainance.plural').format(count, name)
      }
      return Em.I18n.t('rollingrestart.dialog.msg.maintainance').format(count, name)
    }
    return null;
  }.property('allHostComponents', 'nonMaintainanceHostComponents', 'hostComponentDisplayName'),
  batchSizeMessage : function() {
    return Em.I18n.t('rollingrestart.dialog.msg.componentsAtATime').format(this.get('hostComponentDisplayName'));
  }.property('hostComponentDisplayName'),
  staleConfigsOnlyMessage : function() {
    return Em.I18n.t('rollingrestart.dialog.msg.staleConfigsOnly').format(this.get('hostComponentDisplayName'));
  }.property('hostComponentDisplayName')
});
