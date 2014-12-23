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

App.MainStackVersionsView = App.TableView.extend({
  templateName: require('templates/main/admin/stack_versions/stack_versions'),

  content: function () {
    return this.get('controller.content');
  }.property('controller.content'),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @type {String}
   */
  filteredContentInfo: function () {
    return this.t('tableView.filters.filteredConfigVersionInfo').format(this.get('filteredCount'), this.get('content.length'));
  }.property('filteredCount', 'content.length'),

  /**
   * associations between stack version property and column index
   * @type {Array}
   */
  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'repositoryVersion.displayName';
    associations[2] = 'repositoryVersion.operatingSystems';
    associations[3] = 'installedHosts.length';
    associations[4] = 'currentHosts.length';
    return associations;
  }.property(),

  sortView: sort.wrapperView,
  stackVersionSort: sort.fieldView.extend({
    column: 1,
    name: 'repositoryVersion.displayName',
    displayName: Em.I18n.t('admin.stackVersions.table.header.version'),
    type: 'version',
    classNames: ['first']
  }),
  osSort: sort.fieldView.extend({
    column: 2,
    name: 'repositoryVersion.operatingSystems.length',
    displayName: Em.I18n.t('admin.stackVersions.table.header.os'),
    type: 'number'
  }),
  installedSort: sort.fieldView.extend({
    column: 3,
    name: 'installedHosts.length',
    displayName: Em.I18n.t('admin.stackVersions.table.header.installed'),
    type: "number"
  }),
  currentSort: sort.fieldView.extend({
    column: 4,
    name: 'currentHosts.length',
    displayName: Em.I18n.t('admin.stackVersions.table.header.current'),
    type: "number"
  }),

  stackVersionFilterView: filters.createTextView({
    column: 1,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  osFilterView: filters.createSelectView({
    column: 2,
    fieldType: 'filter-input-width',
    content: function () {
      var names = App.OS.find().mapProperty('osType').uniq();
      return [
        {
          value: '',
          label: Em.I18n.t('common.all')
        }
      ].concat(names.map(function (name) {
          return {
            value: name,
            label: name
          }
        }));
    }.property('App.router.mainStackVersionsController.dataIsLoaded'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'os');
    }
  }),

  installedFilterView: filters.createTextView({
    column: 3,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  currentFilterView: filters.createTextView({
    column: 4,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  didInsertElement: function() {
    this.set('controller.isPolling', true);
    this.get('controller').load();
    this.get('controller').doPolling();
  },

  willDestroyElement: function () {
    this.set('controller.isPolling', false);
    clearTimeout(this.get('controller.timeoutRef'));
  },

  StackVersionView: Em.View.extend({
    tagName: 'tr',

    versionStateMap: {
      'current': {
        'id': 'current',
        'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.current')
      },
      'installed': {
        'id': 'installed',
        'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.installed')
      },
      'not_installed': {
        'id': 'installing',
        'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.not_installed')
      }
    },
  })

});
