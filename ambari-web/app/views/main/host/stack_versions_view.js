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

App.MainHostStackVersionsView = App.TableView.extend({
  templateName: require('templates/main/host/stack_versions'),
  classNames: ['host-tab-content'],

  host: function () {
    return App.router.get('mainHostDetailsController.content');
  }.property('App.router.mainHostDetailsController.content'),

  content: function () {
    return App.HostStackVersion.find();
  }.property(),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredContentInfo: function () {
    return this.t('hosts.host.stackVersions.table.filteredInfo').format(this.get('filteredCount'), this.get('totalCount'));
  }.property('filteredCount', 'totalCount'),

  sortView: sort.wrapperView,
  stackNameSort: sort.fieldView.extend({
    column: 1,
    name: 'stackName',
    displayName: Em.I18n.t('common.stack'),
    type: 'version'
  }),
  versionSort: sort.fieldView.extend({
    column: 2,
    name: 'version',
    displayName: Em.I18n.t('common.version'),
    type: 'version'
  }),
  statusSort: sort.fieldView.extend({
    column: 3,
    name: 'status',
    displayName: Em.I18n.t('common.status')
  }),

  /**
   * Filter view for stackName column
   * Based on <code>filters</code> library
   */
  stackNameFilterView: filters.createSelectView({
    column: 1,
    fieldType: 'filter-input-width',
    content: function () {
      return [
        {
          value: '',
          label: Em.I18n.t('common.all')
        }
      ].concat(this.get('parentView.content').mapProperty('stackName').uniq().map(function (item) {
        return {
          value: item,
          label: item
        }
      }));
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  /**
   * Filter view for version column
   * Based on <code>filters</code> library
   */
  versionFilterView: filters.createSelectView({
    column: 2,
    fieldType: 'filter-input-width',
    content: function () {
      return [
        {
          value: '',
          label: Em.I18n.t('common.all')
        }
      ].concat(this.get('parentView.content').map(function (item) {
        return {
          value: item.get('version'),
          label: item.get('version')
        }
      }));
    }.property('App.router.clusterController.isLoaded'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  /**
   * Filter view for status column
   * Based on <code>filters</code> library
   */
  statusFilterView: filters.createSelectView({
    column: 3,
    fieldType: 'filter-input-width',
    content: function () {
      return [
        {
          value: '',
          label: Em.I18n.t('common.all')
        }
      ].concat(App.HostStackVersion.statusDefinition.map(function (status) {
        return {
          value: status,
          label: App.HostStackVersion.formatStatus(status)
        }
      }));
    }.property(),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'stackName';
    associations[2] = 'version';
    associations[3] = 'status';
    return associations;
  }.property()

});
