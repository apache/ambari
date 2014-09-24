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

App.MainDatasetJobsController = Em.Controller.extend({
  name: 'mainDatasetJobsController',

  suspend: function () {
    App.ajax.send({
      name: 'mirroring.suspend_entity',
      sender: this,
      data: {
        name: this.get('content.prefixedName'),
        type: 'feed',
        falconServer: App.get('falconServerURL')
      },
      success: 'onSuspendSuccess',
      error: 'onError'
    });
  },

  onSuspendSuccess: function() {
    this.set('content.status', 'SUSPENDED');
    this.get('content.datasetJobs').filterProperty('status', 'RUNNING').setEach('status', 'SUSPENDED');
  },

  resume: function () {
    App.ajax.send({
      name: 'mirroring.resume_entity',
      sender: this,
      data: {
        name: this.get('content.prefixedName'),
        type: 'feed',
        falconServer: App.get('falconServerURL')
      },
      success: 'onResumeSuccess',
      error: 'onError'
    });
  },

  onResumeSuccess: function() {
    this.set('content.status', 'RUNNING');
    this.get('content.datasetJobs').filterProperty('status', 'SUSPENDED').setEach('status', 'RUNNING');
  },

  schedule: function () {
    App.ajax.send({
      name: 'mirroring.schedule_entity',
      sender: this,
      data: {
        name: this.get('content.prefixedName'),
        type: 'feed',
        falconServer: App.get('falconServerURL')
      },
      success: 'onScheduleSuccess',
      error: 'onError'
    });
  },

  onScheduleSuccess: function() {
    this.set('content.status', 'RUNNING');
  },


  delete: function () {
    var self = this;
    App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'mirroring.delete_entity',
        sender: self,
        data: {
          name: self.get('content.prefixedName'),
          type: 'feed',
          falconServer: App.get('falconServerURL')
        },
        success: 'onDeleteSuccess',
        error: 'onError'
      });
    });
  },

  onDeleteSuccess: function() {
    var dataset = this.get('content');
    dataset.deleteRecord();
    App.store.commit();
    dataset.get('stateManager').transitionTo('loading');
    this.set('content', null);
    App.router.get('mainMirroringController').set('selectedDataset', null);
    App.router.send('gotoShowJobs');
  },

  suspendInstance: function (event) {
    App.ajax.send({
      name: 'mirroring.suspend_instance',
      sender: this,
      data: {
        feed: this.get('content.prefixedName'),
        name: event.context.get('name'),
        job: event.context,
        falconServer: App.get('falconServerURL')
      },
      success: 'onSuspendInstanceSuccess',
      error: 'onError'
    });
  },

  onSuspendInstanceSuccess: function () {
    this.get('content.datasetJobs').filterProperty('name', arguments[2].name).setEach('status', 'SUSPENDED');
  },

  resumeInstance: function (event) {
    App.ajax.send({
      name: 'mirroring.resume_instance',
      sender: this,
      data: {
        feed: this.get('content.prefixedName'),
        name: event.context.get('name'),
        job: event.context,
        falconServer: App.get('falconServerURL')
      },
      success: 'onResumeInstanceSuccess',
      error: 'onError'
    });
  },

  onResumeInstanceSuccess: function () {
    this.get('content.datasetJobs').filterProperty('name', arguments[2].name).setEach('status', 'RUNNING');
  },

  killInstance: function (event) {
    App.ajax.send({
      name: 'mirroring.kill_instance',
      sender: this,
      data: {
        feed: this.get('content.prefixedName'),
        name: event.context.get('name'),
        job: event.context,
        falconServer: App.get('falconServerURL')
      },
      success: 'onKillInstanceSuccess',
      error: 'onError'
    });
  },

  onKillInstanceSuccess: function () {
    this.get('content.datasetJobs').filterProperty('name', arguments[2].name).setEach('status', 'KILLED');
  },

  onError: function () {
    App.showAlertPopup(Em.I18n.t('common.error'), arguments[2]);
  },

  downloadEntity: function () {
    var xml = this.formatDatasetXML(this.get('content'));
    if ($.browser.msie && $.browser.version < 10) {
      this.openInfoInNewTab(xml);
    } else {
      try {
        var blob = new Blob([xml], {type: 'text/xml;charset=utf-8;'});
        saveAs(blob, Em.I18n.t('mirroring.dataset.entity') + '.xml');
      } catch (e) {
        this.openInfoInNewTab(xml);
      }
    }
  },

  openInfoInNewTab: function (xml) {
    var newWindow = window.open('');
    var newDocument = newWindow.document;
    newDocument.write('<pre>' + xml + '</pre>');
    newWindow.focus();
  },

  formatDatasetXML: function (dataset) {
    return '<?xml version="1.0"?>\n' + '<feed description="" name="' + dataset.get('name') + '" xmlns="uri:falcon:feed:0.1">\n' +
        '<frequency>' + dataset.get('frequencyUnit') + '(' + dataset.get('frequency') + ')' + '</frequency>\n' +
        '<clusters>\n<cluster name="' + dataset.get('sourceClusterName') + '" type="source">\n' +
        '<validity start="' + dataset.get('scheduleStartDate') + '" end="' + dataset.get('scheduleEndDate') + '"/>\n' +
        '<retention limit="days(7)" action="delete"/>\n</cluster>\n<cluster name="' + dataset.get('targetClusterName') +
        '" type="target">\n<validity start="' + dataset.get('scheduleStartDate') + '" end="' + dataset.get('scheduleEndDate') + '"/>\n' +
        '<retention limit="months(1)" action="delete"/>\n<locations>\n<location type="data" path="' + dataset.get('targetDir') + '" />\n' +
        '</locations>\n</cluster>\n</clusters>\n<locations>\n<location type="data" path="' + dataset.get('sourceDir') + '" />\n' +
        '</locations>\n<ACL owner="hue" group="users" permission="0755" />\n<schema location="/none" provider="none"/>\n</feed>';
  }
});
