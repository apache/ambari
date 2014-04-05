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
var date = require('utils/date');

App.WizardStep9View = App.TableView.extend({

  templateName: require('templates/wizard/step9'),
  barColor: '',
  resultMsg: '',
  resultMsgColor: '',
  isStepCompleted: function() {
   return (this.get('controller.progress') === '100');
  }.property('controller.progress'),

  displayLength: "25",

  content: function () {
    return this.get('controller.hosts');
  }.property('controller.hosts'),

  filteredContent: [],

  selectedCategory: function() {
    return this.get('categories').findProperty('isActive');
  }.property('categories.@each.isActive'),

  /**
   * Ember Object category. This object also contains
   * <code>
   *   hostStatus: {String} A valid status of a host.
   *               Used to filter hosts in that status.
   *   hostsCount: {Int} Dynamic count of hosts displayed in the category label
   *   label : {String} status and hosts in that status displayed which consists as a category on the page
   *   isActive: {boolean} Gets set when the category is selected/clicked by the user
   *   itemClass: {computed property} Binds the category link to active class when user clicks on the link
   * </code>
   */
  categoryObject: Em.Object.extend({
    hostsCount: 0,
    label: function () {
      return "%@ (%@)".fmt(this.get('value'), this.get('hostsCount'));
    }.property('value', 'hostsCount'),
    isActive: false,
    itemClass: function () {
      return this.get('isActive') ? 'active' : '';
    }.property('isActive')
  }),
  /**
   * computed property creates the category objects on the load of the page and sets 'All' as the active category
   * @Returns: All created categories which are binded and iterated in the template
   */
  categories: function () {
    return [
      this.categoryObject.create({value: Em.I18n.t('common.all'), hostStatus: 'all', isActive: true}),
      this.categoryObject.create({value: Em.I18n.t('installer.step9.hosts.status.label.inProgress'), hostStatus: 'inProgress'}),
      this.categoryObject.create({value: Em.I18n.t('installer.step9.hosts.status.label.warning'), hostStatus: 'warning'}),
      this.categoryObject.create({value: Em.I18n.t('common.success'), hostStatus: 'success'}),
      this.categoryObject.create({value: Em.I18n.t('common.fail'), hostStatus: 'failed', last: true })
    ];
  }.property(),

  hostStatusObserver: function(){
    Ember.run.once(this, 'countCategoryHosts');
    Ember.run.once(this, 'filter');
  }.observes('content.@each.status'),

  /**
   * count each category hosts to update label
   */
  countCategoryHosts: function () {
    var counters = {
      "info": 0,
      "pending": 0,
      "in_progress": 0,
      "heartbeat_lost": 0,
      "warning": 0,
      "success": 0,
      "failed": 0
    };

    this.get('content').forEach(function (host) {
      if (counters[host.get('status')] !== undefined) {
        counters[host.get('status')]++;
      }
    }, this);
    counters["all"] = this.get('content.length');
    counters["inProgress"] = counters["info"] + counters["pending"] + counters["in_progress"];
    counters["failed"] += counters["heartbeat_lost"];
    this.get('categories').forEach(function(category) {
      category.set('hostsCount', counters[category.get('hostStatus')]);
    }, this);
  },

  /**
   * filter hosts by category
   */
  filter: function () {
    var self = this;
    Em.run.next(function () {
      var result = [];
      var content = self.get('content');
      var selectedCategory = self.get('selectedCategory');
      if (!selectedCategory || selectedCategory.get('hostStatus') === 'all') {
        result = content;
      } else if (selectedCategory.get('hostStatus') == 'inProgress') {
        result = content.filter(function (_host) {
          return (_host.get('status') == 'info' || _host.get('status') == 'pending' || _host.get('status') == 'in_progress');
        });
      } else if (selectedCategory.get('hostStatus') == 'failed') {
        result = content.filter(function (_host) {
          return (_host.get('status') == 'failed' || _host.get('status') == 'heartbeat_lost');
        });
      } else {
        result = content.filterProperty('status', selectedCategory.get('hostStatus'));
      }
      self.set('filteredContent', result);
      self.set('filteredHostsInfo', Em.I18n.t('installer.step9.hosts.filteredHostsInfo').format(result.get('length'), content.get('length')));
    });
  }.observes('selectedCategory'),

  /**
   * filter hosts info shown up on bottom of the box. Set by filter function, when 'seletedCategory' changed
   */
  filteredHostsInfo: '',

  /**
   * On click handler for 'show all' link
   */
  showAllHosts: function () {
    this.get('categories').forEach(function (category) {
      category.set('isActive', (category.get('hostStatus') === 'all'));
    });
  },

  /**
   * Trigger on Category click
   * @param {Object} event
   */
  selectCategory: function (event) {
    var categoryStatus = event.context.get('hostStatus');
    this.get('categories').forEach(function (category) {
      category.set('isActive', (category.get('hostStatus') === categoryStatus));
    });
  },

  didInsertElement: function () {
    var controller = this.get('controller');
    this.onStatus();
    controller.navigateStep();
  },

  isHostHeartbeatLost: function () {
    return (this.get('controller.hostsWithHeartbeatLost').length > 0);
  }.property('controller.hostsWithHeartbeatLost.@each'),

  barWidth: function () {
    var controller = this.get('controller');
    return 'width: ' + controller.get('progress') + '%;';
  }.property('controller.progress'),

  progressMessage: function () {
    return Em.I18n.t('installer.step9.overallProgress').format(this.get('controller.progress'));
  }.property('controller.progress'),

  onStatus: function () {
    if (this.get('controller.status') === 'info') {
      this.set('resultMsg', '');
      this.set('barColor', 'progress-info');
    } else if (this.get('controller.status') === 'warning') {
      this.set('barColor', 'progress-warning');
      this.set('resultMsg', Em.I18n.t('installer.step9.status.warning'));
      this.set('resultMsgColor', 'alert-warning');
    } else if (this.get('controller.status') === 'failed') {
      this.set('barColor', 'progress-danger');
      this.set('resultMsgColor', 'alert-error');
      console.log('TRACE: Inside error view step9');
      if (this.get('isHostHeartbeatLost')) {
        // When present requests succeeds but some host components are in UNKNOWN or INSTALL_FAILED state and
        // hosts are in HEARTBEAT_LOST state
        this.set('resultMsg', Em.I18n.t('installer.step9.status.hosts.heartbeat_lost').format(this.get('controller.hostsWithHeartbeatLost').length));
      } else if (this.get('controller.startCallFailed')) {
        this.set('resultMsg', Em.I18n.t('installer.step9.status.start.services.failed'));
      } else {
        this.set('resultMsg', Em.I18n.t('installer.step9.status.failed'));
      }
    } else if (this.get('controller.status') === 'success') {
      console.log('TRACE: Inside success view step9');
      this.set('barColor', 'progress-success');
      this.set('resultMsg', Em.I18n.t('installer.step9.status.success'));
      this.set('resultMsgColor', 'alert-success');
    }
  }.observes('controller.status', 'controller.startCallFailed','isHostHeartbeatLost'),


  hostWithInstallFailed: function (event, context) {
    var controller = this.get('controller');
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step9.host.heartbeat_lost.header'),
      classNames: ['sixty-percent-width-modal'],
      autoHeight: false,
      secondary: null,

      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step9_install_host_popup'),
        c: controller,
        failedHosts: function () {
          return controller.get('hostsWithHeartbeatLost');
        }.property()
      })
    });
  }
});

App.HostStatusView = Em.View.extend({
  tagName: 'tr',
  /**
   * Current host
   */
  obj: null,
  /**
   * wizardStep9Controller
   */
  controller: null,
  barColor: '',

  didInsertElement: function () {
    this.onStatus();
  },

  barWidth: function () {
    return 'width: ' + this.get('obj.progress') + '%;';
  }.property('obj.progress'),

  onStatus: function () {
    if (this.get('obj.status') === 'info') {
      this.set('barColor', 'progress-info');
    } else if (this.get('obj.status') === 'warning') {
      this.set('barColor', 'progress-warning');
      if (this.get('obj.progress') === '100') {
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.warning'));
      }
    } else if (this.get('obj.status') === 'failed') {
      this.set('barColor', 'progress-danger');
      if (this.get('obj.progress') === '100') {
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.failed'));
      }
    } else if (this.get('obj.status') === 'heartbeat_lost') {
      this.set('barColor', 'progress-danger');
      if (this.get('obj.progress') === '100') {
        this.set('obj.message', Em.I18n.t('installer.step9.host.heartbeat_lost'));
      }
    } else if (this.get('obj.status') === 'success' && this.get('isHostCompleted') && parseInt(this.get('controller.progress')) > 34) {
        this.set('barColor', 'progress-success');
        this.set('obj.message', Em.I18n.t('installer.step9.host.status.success'));
    }
  }.observes('obj.status', 'obj.progress', 'controller.progress'),

  isFailed: function () {
    return (this.get('isHostCompleted') && (this.get('obj.status') === 'failed' || this.get('obj.status') === 'heartbeat_lost'));
  }.property('obj.status', 'isHostCompleted'),

  isSuccess: function () {
    return (this.get('isHostCompleted') && this.get('obj.status') === 'success');
  }.property('obj.status', 'isHostCompleted'),

  isWarning: function () {
    return(this.get('isHostCompleted') && this.get('obj.status') === 'warning');
  }.property('obj.status', 'isHostCompleted'),

  isHostCompleted: function () {
    return this.get('obj.progress') == 100;
  }.property('obj.progress'),

  hostLogPopup: function (event, context) {
    var controller = this.get('controller');
    var host = this.get('obj');

    App.ModalPopup.show({
      header: host.get('name'),
      classNames: ['sixty-percent-width-modal'],
      autoHeight: false,
      secondary: null,
      /**
       * Current host
       */
      host: host,
      /**
       * wizardStep9Controller
       */
      c: controller,

      onClose: function () {
        this.set('c.currentOpenTaskId', 0);
        this.hide();
      },

      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step9HostTasksLogPopup'),
        isHeartbeatLost: function() {
          return (host.get('status') === 'heartbeat_lost');
        }.property('host.status'),
        isNoTasksScheduled: function() {
          return host.get('isNoTasksForInstall');
        }.property('host.isNoTasksForInstall'),

        isLogWrapHidden: true,

        showTextArea: false,

        isEmptyList: true,

        visibleTasks: function () {
          this.set("isEmptyList", true);
          if (this.get('category.value')) {
            var filter = this.get('category.value');
            var tasks = this.get('tasks');
            tasks.setEach("isVisible", false);

            if (filter == "all") {
              tasks.setEach("isVisible", true);
            }
            else if (filter == "pending") {
              tasks.filterProperty("status", "pending").setEach("isVisible", true);
              tasks.filterProperty("status", "queued").setEach("isVisible", true);
            }
            else if (filter == "in_progress") {
              tasks.filterProperty("status", "in_progress").setEach("isVisible", true);
            }
            else if (filter == "failed") {
              tasks.filterProperty("status", "failed").setEach("isVisible", true);
            }
            else if (filter == "completed") {
              tasks.filterProperty("status", "completed").setEach("isVisible", true);
            }
            else if (filter == "aborted") {
              tasks.filterProperty("status", "aborted").setEach("isVisible", true);
            }
            else if (filter == "timedout") {
              tasks.filterProperty("status", "timedout").setEach("isVisible", true);
            }

            if (tasks.filterProperty("isVisible", true).length > 0) {
              this.set("isEmptyList", false);
            }
          }
        }.observes('category', 'tasks'),

        categories: [
          Ember.Object.create({value: 'all', label: Em.I18n.t('installer.step9.hostLog.popup.categories.all') }),
          Ember.Object.create({value: 'pending', label: Em.I18n.t('installer.step9.hostLog.popup.categories.pending')}),
          Ember.Object.create({value: 'in_progress', label: Em.I18n.t('installer.step9.hostLog.popup.categories.in_progress')}),
          Ember.Object.create({value: 'failed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.failed') }),
          Ember.Object.create({value: 'completed', label: Em.I18n.t('installer.step9.hostLog.popup.categories.completed') }),
          Ember.Object.create({value: 'aborted', label: Em.I18n.t('installer.step9.hostLog.popup.categories.aborted') }),
          Ember.Object.create({value: 'timedout', label: Em.I18n.t('installer.step9.hostLog.popup.categories.timedout') })
        ],

        category: null,

        tasks: function () {
          var tasksArr = [];
          var host = this.get('parentView.host');
          var tasks = this.getStartedTasks(host);
          tasks = tasks.sortProperty('Tasks.id');
          if (tasks.length) {
            tasks.forEach(function (_task) {
              var taskInfo = Ember.Object.create({});
              taskInfo.set('id', _task.Tasks.id);
              taskInfo.set('requestId', _task.Tasks.request_id);
              taskInfo.set('command', _task.Tasks.command.toLowerCase() === 'service_check' ? '' : _task.Tasks.command.toLowerCase());
              taskInfo.set('status', App.format.taskStatus(_task.Tasks.status));
              taskInfo.set('role', App.format.role(_task.Tasks.role));
              taskInfo.set('stderr', _task.Tasks.stderr);
              taskInfo.set('stdout', _task.Tasks.stdout);
              taskInfo.set('startTime',  date.startTime(_task.Tasks.start_time));
              taskInfo.set('duration', date.durationSummary(_task.Tasks.start_time, _task.Tasks.end_time));
              taskInfo.set('isVisible', true);
              taskInfo.set('icon', '');
              taskInfo.set('hostName', _task.Tasks.host_name);
              taskInfo.set('outputLog', Em.I18n.t('common.hostLog.popup.logDir.path') + Em.I18n.t('common.hostLog.popup.outputLog.value').format(_task.Tasks.id));
              taskInfo.set('errorLog', Em.I18n.t('common.hostLog.popup.logDir.path') + Em.I18n.t('common.hostLog.popup.errorLog.value').format(_task.Tasks.id));
              if (taskInfo.get('status') == 'pending' || taskInfo.get('status') == 'queued') {
                taskInfo.set('icon', 'icon-cog');
              } else if (taskInfo.get('status') == 'in_progress') {
                taskInfo.set('icon', 'icon-cogs');
              } else if (taskInfo.get('status') == 'completed') {
                taskInfo.set('icon', ' icon-ok');
              } else if (taskInfo.get('status') == 'failed') {
                taskInfo.set('icon', 'icon-exclamation-sign');
              } else if (taskInfo.get('status') == 'aborted') {
                taskInfo.set('icon', 'icon-minus');
              } else if (taskInfo.get('status') == 'timedout') {
                taskInfo.set('icon', 'icon-time');
              }
              tasksArr.push(taskInfo);
            }, this);
          }
          return tasksArr;
        }.property('parentView.c.logTasksChangesCounter'),

        backToTaskList: function (event, context) {
          this.destroyClipBoard();
          this.set("isLogWrapHidden", true);
        },

        getStartedTasks: function (host) {
          return host.logTasks.filter(function (task) {
            return task.Tasks.status;
          });
        },

        openTaskLogInDialog: function () {
          var newwindow = window.open();
          var newdocument = newwindow.document;
          newdocument.write($(".task-detail-log-info").html());
          newdocument.close();
        },

        openedTask: function () {
          return this.get('tasks').findProperty('id', this.get('parentView.c.currentOpenTaskId'))
        }.property('parentView.c.currentOpenTaskId', 'tasks.@each'),

        toggleTaskLog: function (event, context) {
          if (this.get('isLogWrapHidden')) {
            var taskInfo = event.context;
            this.set("isLogWrapHidden", false);
            this.set('parentView.c.currentOpenTaskId', taskInfo.id);
            this.set('parentView.c.currentOpenTaskRequestId', taskInfo.requestId);
            this.get('parentView.c').loadCurrentTaskLog();
            $(".modal").scrollTop(0);
            $(".modal-body").scrollTop(0);
          }
          else {
            this.set("isLogWrapHidden", true);
            this.set('parentView.c.currentOpenTaskId', 0);
            this.set('parentView.c.currentOpenTaskRequestId', 0);
          }
        },

        textTrigger: function (event) {
          if ($(".task-detail-log-clipboard").length > 0) {
            this.destroyClipBoard();
          }
          else {
            this.createClipBoard();
          }
        },

        createClipBoard: function () {
          var log = $(".task-detail-log-maintext");
          $(".task-detail-log-clipboard-wrap").html('<textarea class="task-detail-log-clipboard"></textarea>');
          $(".task-detail-log-clipboard")
            .html("stderr: \n" + $(".stderr").html() + "\n stdout:\n" + $(".stdout").html())
            .css("display", "block")
            .width(log.width())
            .height(log.height())
            .select();
          log.css("display", "none")
        },

        destroyClipBoard: function () {
          $(".task-detail-log-clipboard").remove();
          $(".task-detail-log-maintext").css("display", "block");
        }
      })
    });
  }

});