/** Licensed to the Apache Software Foundation (ASF) under one
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

App.RepoVersionsView = App.TableView.extend({
  templateName: require('templates/main/admin/stack_versions/repo_versions'),

  content: function () {
    return this.get('controller.content');
  }.property('controller.content'),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
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
    associations[1] = 'repositoryVersion';
    associations[2] = 'displayName';
    associations[3] = 'operatingSystems';
    return associations;
  }.property(),

  sortView: sort.wrapperView,
  repoNameSort: sort.fieldView.extend({
    column: 1,
    name: 'repositoryVersion',
    displayName: Em.I18n.t('admin.stackVersions.table.header.stack'),
    type: 'version',
    classNames: ['first']
  }),
  repoVersionSort: sort.fieldView.extend({
    column: 2,
    name: 'displayName',
    displayName: Em.I18n.t('admin.stackVersions.table.header.version'),
    type: 'version'
  }),
  osSort: sort.fieldView.extend({
    column: 3,
    name: 'operatingSystems.length',
    displayName: Em.I18n.t('admin.stackVersions.table.header.os'),
    type: 'number'
  }),

  repoNameFilterView: filters.createSelectView({
    column: 1,
    fieldType: 'filter-input-width',
    content: function () {
      var names = this.get('parentView.content').mapProperty('repositoryVersion').uniq();
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
    }.property('App.router.repoVersionsController.dataIsLoaded'),
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'select');
    }
  }),

  repoVersionFilterView: filters.createTextView({
    column: 2,
    fieldType: 'filter-input-width',
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  osFilterView: filters.createSelectView({
    column: 3,
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

  didInsertElement: function() {
    this.get('controller').load();
  },

  RepositoryVersionView: Em.View.extend({
    tagName: 'tr',
    didInsertElement: function () {
      App.tooltip(this.$("[rel='Tooltip']"));
      this.set('isOsCollapsed', true);
    },

    toggleOs: function(event) {
      this.set('isOsCollapsed', !this.get('isOsCollapsed'));
      this.$('.operating-systems').toggle();
    },

    labels: function() {
      return this.get('content.operatingSystems') &&
        this.get('content.operatingSystems').getEach('osType').join("<br/>");
    }.property('content.operatingSystems.length')
  })

});
