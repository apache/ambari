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

App.upgradeGroupView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_group'),

  /**
   * @type {Array}
   */
  taskDetailsProperties: ['status', 'stdout', 'stderr', 'error_log', 'host_name', 'output_log'],

  /**
   * Only one UpgradeGroup or UpgradeItem could be expanded at a time
   * @param {object} event
   */
  toggleExpanded: function (event) {
    var isExpanded = event.context.get('isExpanded');
    event.contexts[1].filterProperty('isExpanded').forEach(function (item) {
      this.collapseLowerLevels(item);
      item.set('isExpanded', false);
    }, this);
    this.collapseLowerLevels(event.context);
    event.context.set('isExpanded', !isExpanded);
    if (!isExpanded && event.context.get('type') === 'ITEM') {
      event.context.set('isTasksLoaded', false);
      this.doPolling(event.context);
    }
  },

  /**
   * collapse sub-entities of current
   * @param {App.upgradeEntity} entity
   */
  collapseLowerLevels: function (entity) {
    if (entity.get('isExpanded')) {
      if (entity.type === 'ITEM') {
        entity.get('tasks').setEach('isExpanded', false);
      } else if (entity.type === 'GROUP') {
        entity.get('upgradeItems').forEach(function (item) {
          this.collapseLowerLevels(item);
          item.set('isExpanded', false);
        }, this);
      }
    }
  },

  /**
   * poll for tasks when item is expanded
   */
  doPolling: function (item) {
    var self = this;

    if (item && item.get('isExpanded')) {
      this.getTasks(item).complete(function () {
        self.set('timer', setTimeout(function () {
          self.doPolling(item);
        }, App.bgOperationsUpdateInterval));
      });
    } else {
      clearTimeout(this.get('timer'));
    }
  },

  /**
   * request tasks from server
   * @return {$.ajax}
   */
  getTasks: function (item) {
    return App.ajax.send({
      name: 'admin.upgrade.upgrade_item',
      sender: this,
      data: {
        upgradeId: item.get('request_id'),
        groupId: item.get('group_id'),
        stageId: item.get('stage_id')
      },
      success: 'getTasksSuccessCallback'
    });
  },

  /**
   * success callback of <code>getTasks</code>
   * @param {object} data
   */
  getTasksSuccessCallback: function (data) {
    this.get('controller.upgradeData.upgradeGroups').forEach(function (group) {
      if (group.get('group_id') === data.UpgradeItem.group_id) {
        group.get('upgradeItems').forEach(function (item) {
          if (item.get('stage_id') === data.UpgradeItem.stage_id) {
            if (item.get('tasks.length')) {
              item.set('isTasksLoaded', true);
              data.tasks.forEach(function (task) {
                var currentTask = item.get('tasks').findProperty('id', task.Tasks.id);
                this.get('taskDetailsProperties').forEach(function (property) {
                  currentTask.set(property, task.Tasks[property]);
                }, this);
              }, this);
            } else {
              var tasks = [];
              data.tasks.forEach(function (task) {
                tasks.pushObject(App.upgradeEntity.create({type: 'TASK'}, task.Tasks));
              });
              item.set('tasks', tasks);
            }
            item.set('isTasksLoaded', true);
          }
        }, this);
      }
    }, this);
  },

  willDestroyElement: function () {
    clearTimeout(this.get('timer'));
  }
});
