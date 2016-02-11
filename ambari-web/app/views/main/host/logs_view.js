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
var filters = require('views/common/filter_view');
var sort = require('views/common/sort_view');

App.MainHostLogsView = App.TableView.extend({
  templateName: require('templates/main/host/logs'),

  classNames: ['logs-tab-content'],

  /**
   * @type {Ember.Object}
   */
  host: Em.computed.alias('App.router.mainHostDetailsController.content'),

  content: function() {
    return [
      Em.Object.create({
        serviceName: 'HDFS',
        componentName: 'DATANODE',
        fileExtension: '.log',
        fileName: 'HDFS_DATANODE.log'
      })
    ];
  }.property(),
  /**
   * @type {Ember.View}
   */
  sortView: sort.wrapperView,

  serviceNameSort: sort.fieldView.extend({
    column: 1,
    name: 'serviceName',
    displayName: Em.I18n.t('common.service')
  }),

  componentNameSort: sort.fieldView.extend({
    column: 2,
    name: 'componentName',
    displayName: Em.I18n.t('common.component')
  }),

  fileExtensionsSort: sort.fieldView.extend({
    column: 3,
    name: 'extension',
    displayName: Em.I18n.t('common.extension')
  }),

  serviceNameFilterView: filters.createSelectView({
    column: 1,
    fieldType: 'filter-input-width',
    didInsertElement: function() {
      this.setValue(Em.getWithDefault(this, 'controller.serializedQuery.service_name', ''));
      this._super();
    },
    content: function() {
      return [{
        value: '',
        label: Em.I18n.t('common.all')
      }].concat(App.Service.find().mapProperty('serviceName').uniq().map(function(item) {
        return {
          value: item,
          label: item
        }
      }));
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function() {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  componentNameFilterView: filters.createSelectView({
    column: 2,
    fieldType: 'filter-input-width',
    didInsertElement: function() {
      this.setValue(Em.getWithDefault(this, 'controller.serializedQuery.component_name', ''));
      this._super();
    },
    content: function() {
      var hostName = this.get('parentView').get('host.hostName'),
        hostComponents = App.HostComponent.find().filterProperty('hostName', hostName),
        componentsMap = hostComponents.map(function(item) {
          return {
            value: item.get('componentName'),
            label: item.get('displayName')
          }
        });
      return [{
        value: '',
        label: Em.I18n.t('common.all')
      }].concat(componentsMap);
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function() {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  fileExtensionsFilter: filters.createSelectView({
    column: 3,
    fieldType: 'filter-input-width',
    didInsertElement: function() {
      this.setValue(Em.getWithDefault(this, 'controller.serializedQuery.file_extension', ''));
      this._super();
    },
    content: function() {
      return [{
        value: '',
        label: Em.I18n.t('common.all')
      }].concat([
        '.out',
         '.log'
       ].map(function(item) {
        return {
          value: item,
          label: item
        }
      }))
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function() {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  /**
   * @type {string[]}
   */
  colPropAssoc: function () {
    var ret = [];
    ret[1] = 'serviceName';
    ret[2] = 'componentName';
    ret[3] = 'fileExtension';
    return ret;
  }.property(),

  openLogFile: function(e) {
    var fileData = e.context;
    if (e.context) {
      App.LogFileSearchPopup(fileData.fileName);
    }
  }
});
