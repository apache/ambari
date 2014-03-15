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
var serviceComponents = require('data/service_components');

App.WizardStep9Controller = Em.Controller.extend({
  name: 'wizardStep9Controller',

  /**
   *  Array of host Objects that are successfully registered on "Confirm Host Options" page
   *  <code>
   *    {
   *      name: {String} host name.
   *      status: {String} Current status of the host. This field is used in the step 9 view to set the css class of the
   *             host progress bar and set the appropriate message. Possible values: info, warning, failed, heartbeat_lost and success
   *      logTasks: {Array} Tasks that are scheduled on the host for the current request.
   *      message: {String} Message to be shown in front of the host name.
   *      progress: {Int} Progress of the tasks on the host. Amount of tasks completed for all the tasks scheduled on the host.
   *      isNoTasksForInstall: {Boolean} Gets set when no task is scheduled for the host on install phase.
   *    }
   *  </code>
   */

  hosts: [],

  /*
   * Array of above hosts that should be made visible depending upon "Host State Filter" chosen <Possible filter values:
   * All, In Progress, Warning, Success and Fail>
   */
  visibleHosts: [],

  /**
   * overall progress of <Install,Start and Test> page shown as progress bar on the top of the page
   */
  progress: '0',

  /*
   * json file for the mock data to be used in mock mode
   */
  mockDataPrefix: '/data/wizard/deploy/5_hosts',

  /*
   * Current Request data polled from the API: api/v1/clusters/{clusterName}/requests/{RequestId}?fields=tasks/Tasks/command,
   * tasks/Tasks/exit_code,tasks/Tasks/start_time,tasks/Tasks/end_time,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,
   * tasks/Tasks/status&minimal_response=true
   */
  polledData: [],

  /*
   * This flag is only used in UI mock mode as a counter for number of polls.
   */
  numPolls: 1,

  // Interval in milliseconds between API calls While polling the request status for Install and, Start and Test tasks
  POLL_INTERVAL: 4000,

  /**
   * Array of objects
   * <code>
   *   {
   *     hostName: {String} Name of host that has stopped heartbeating to ambari-server
   *     componentNames: {Sting} Name of all components that are on the host
   *   }
   * </code>
   */
  hostsWithHeartbeatLost: [],

   // Flag is set in the start all services error callback function
  startCallFailed: false,

  /*
   * Status of the page. Possible values: <info, warning, failed and success>.
   * This property is used in the step-9 view for displaying the appropriate color of the overall progress bar and
   * the appropriate result message at the bottom of the page
   */
  status: 'info',

  /**
   * This computed property is used to determine if the Next button and back button on the page should be disabled. The property is
   * used in the template to grey out Next and Back buttons. Although clicking on the greyed out button do trigger the event and
   * calls submit and back function of the controller.
   */
  isSubmitDisabled: function () {
    var validStates = ['STARTED', 'START FAILED'];
    var controllerName = this.get('content.controllerName');
    if (controllerName == 'addHostController' || controllerName == 'addServiceController') {
      validStates.push('INSTALL FAILED');
    }
    return !validStates.contains(this.get('content.cluster.status'));
  }.property('content.cluster.status'),

  /**
   * This function is called when a click event happens on Next button of <Install, Start and Test> page
   */
  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  },

  /**
   * This function is called when a click event happens on back button of <Install, Start and Test> page
   */
  back: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('back');
    }
  },

  /**
   * Observer function: Enables previous steps link if install task failed in installer wizard.
   */
  togglePreviousSteps: function () {
    if (App.testMode) {
      return;
    } else if ('INSTALL FAILED' === this.get('content.cluster.status') && this.get('content.controllerName') == 'installerController') {
      App.router.get('installerController').setStepsEnable();
    } else {
      App.router.get('installerController').setLowerStepsDisable(9);
    }
  }.observes('content.cluster.status', 'content.controllerName'),

  /*
   * Computed property to determine if the Retry button should be made visible on the page.
   */
  showRetry: function () {
    return this.get('content.cluster.status') == 'INSTALL FAILED';
  }.property('content.cluster.status'),

  /**
   * Ember Object with the controllerbinding as this controller. This object also contains
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
    hostsCount: function () {
      var category = this;
      var hosts = this.get('controller.hosts').filter(function (_host) {
        if (category.get('hostStatus') == 'inProgress') {
          return (_host.get('status') == 'info' || _host.get('status') == 'pending' || _host.get('status') == 'in_progress');
        } else if (category.get('hostStatus') == 'failed') {
          return (_host.get('status') == 'failed' || _host.get('status') == 'heartbeat_lost');
        } else {
          return (_host.get('status') == category.get('hostStatus'));
        }
      }, this);
      return hosts.get('length');
    }.property('controller.hosts.@each.status'),
    label: function () {
      return "%@ (%@)".fmt(this.get('value'), this.get('hostsCount'));
    }.property('value', 'hostsCount')
  }),

  /**
   * computed property creates the category objects on the load of the page and sets 'All' as the active category
   * @Returns: All created categories which are binded and iterated in the template
   */
  categories: function () {
    var self = this;
    self.categoryObject.reopen({
      controller: self,
      isActive: function () {
        return this.get('controller.category') == this;
      }.property('controller.category'),
      itemClass: function () {
        return this.get('isActive') ? 'active' : '';
      }.property('isActive')
    });

    var categories = [
      self.categoryObject.create({value: Em.I18n.t('common.all'), hostStatus: 'all', hostsCount: function () {
        return this.get('controller.hosts.length');
      }.property('controller.hosts.length') }),
      self.categoryObject.create({value: Em.I18n.t('installer.step9.hosts.status.label.inProgress'), hostStatus: 'inProgress'}),
      self.categoryObject.create({value: Em.I18n.t('installer.step9.hosts.status.label.warning'), hostStatus: 'warning'}),
      self.categoryObject.create({value: Em.I18n.t('common.success'), hostStatus: 'success'}),
      self.categoryObject.create({value: Em.I18n.t('common.fail'), hostStatus: 'failed', last: true })
    ];

    this.set('category', categories.get('firstObject'));
    return categories;
  }.property(),

  /**
   * Registered as an handlebar action in step-9 template. Invoked whenever click event occurs on a category label
   * @param event: {categoryObject}
   */
  selectCategory: function (event) {
    this.set('category', event.context);
  },

  /**
   * Present clicked/selected {categoryObject} on the page
   */
  category: false,

  /**
   * Observer function: Calls {hostStatusUpdates} function once with change in a host status from any registered hosts.
   */
  hostStatusObserver: function () {
    Ember.run.once(this, 'hostStatusUpdates');
  }.observes('hosts.@each.status'),


  /**
   * Updates the hosts mapping to the category selected and updates entire page status
   */
  hostStatusUpdates: function () {
    this.updateVisibleHosts();
    this.updateStatus();
  },

  /**
   * Observer function: Updates {visibleHosts Array} of this controller whenever category is changed
   */
  updateVisibleHosts: function () {
    var targetStatus = this.get('category.hostStatus');
    var visibleHosts = (targetStatus === 'all') ? this.get('hosts') : this.get('hosts').filter(function (_host) {
      if (targetStatus == 'inProgress') {
        return (_host.get('status') == 'info' || _host.get('status') == 'pending' || _host.get('status') == 'in_progress');
      } else if (targetStatus === 'failed') {
        return (_host.get('status') === 'failed' || _host.get('status') === 'heartbeat_lost');
      } else {
        return (_host.get('status') == targetStatus);
      }
    }, this);
    this.set('visibleHosts', visibleHosts);
  }.observes('category'),

  /**
   * A flag that gets set with installation failure.
   */
  installFailed: false,

  /**
   * Observer function: Updates {status} field of the controller.
   */
  updateStatus: function () {
    var status = 'info';
    if (this.get('hosts').someProperty('status', 'failed')
      || this.get('hosts').someProperty('status', 'heartbeat_lost')
      || this.get('startCallFailed')) {
      status = 'failed';
    } else if (this.get('hosts').someProperty('status', 'warning')) {
      if (this.isStepFailed()) {
        status = 'failed';
      } else {
        status = 'warning';
      }
    } else if (this.get('progress') == '100' && this.get('content.cluster.status') !== 'INSTALL FAILED') {
      status = 'success';
    }
    this.set('status', status);
  }.observes('progress'),

  /**
   * Incremental flag that triggers an event in step 9 view to change the tasks related data and icons of hosts.
   */
  logTasksChangesCounter: 0,

  /**
   * navigateStep is called by App.WizardStep9View's didInsertElement and "retry" from router.
   * content.cluster.status can be:
   * PENDING: set upon successful transition from step 1 to step 2
   * INSTALLED: set upon successful completion of install phase as well as successful invocation of start services API
   * STARTED: set up on successful completion of start phase
   * INSTALL FAILED: set up upon encountering a failure in install phase
   * START FAILED: set upon unsuccessful invocation of start services API and also upon encountering a failure
   * during start phase

   * content.cluster.isCompleted
   * set to false upon successful transition from step 1 to step 2
   * set to true upon successful start of services in this step
   * note: looks like this is the same thing as checking content.cluster.status == 'STARTED'
   */
  navigateStep: function () {
    if (App.testMode) {
      // this is for repeatedly testing out installs in test mode
      this.set('content.cluster.status', 'PENDING');
      this.set('content.cluster.isCompleted', false);
      this.set('content.cluster.requestId', 1);
    }
    var clusterStatus = this.get('content.cluster.status');
    console.log('navigateStep: clusterStatus = ' + clusterStatus);
    if (this.get('content.cluster.isCompleted') === false) {
      // the cluster has not yet successfully installed and started
      if (clusterStatus === 'INSTALL FAILED') {
        this.loadStep();
        this.loadLogData(this.get('content.cluster.requestId'));
      } else if (clusterStatus === 'START FAILED') {
        this.loadStep();
        this.loadLogData(this.get('content.cluster.requestId'));
      } else {
        // handle PENDING, INSTALLED
        this.loadStep();
        this.loadLogData(this.get('content.cluster.requestId'));
        this.startPolling();
      }
    } else {
      // handle STARTED
      // the cluster has successfully installed and started
      this.loadStep();
      this.loadLogData(this.get('content.cluster.requestId'));
      this.set('progress', '100');
    }
  },

  /**
   * This is called on initial page load, refreshes and retry event.
   * clears all in memory stale data for retry event.
   */
  clearStep: function () {
    this.get('hosts').clear();
    this.set('hostsWithHeartbeatLost', []);
    this.set('startCallFailed',false);
    this.set('status', 'info');
    this.set('progress', '0');
    this.set('numPolls', 1);
  },

  /**
   * This is called on initial page load, refreshes and retry event.
   */
  loadStep: function () {
    console.log("TRACE: Loading step9: Install, Start and Test");
    this.clearStep();
    this.loadHosts();
  },


  /**
   * Reset status and message of all hosts when retry install
   */
  resetHostsForRetry: function () {
    var hosts = this.get('content.hosts');
    for (var name in hosts) {
      hosts[name].status = "pending";
      hosts[name].message = 'Waiting';
      hosts[name].isNoTasksForInstall = false;
    }
    this.set('content.hosts', hosts);
  },

  /**
   * Sets the {hosts} array for the controller
   */
  loadHosts: function () {
    var hosts = this.get('content.hosts');
    for (var index in hosts) {
      if (hosts[index].bootStatus === 'REGISTERED') {
        var hostInfo = App.HostInfo.create({
          name: hosts[index].name,
          status: (hosts[index].status) ? hosts[index].status : 'info',
          logTasks: [],
          message: (hosts[index].message) ? hosts[index].message : 'Waiting',
          progress: 0,
          isNoTasksForInstall: false
        });
        this.get('hosts').pushObject(hostInfo);
      }
    }
  },

  /**
   *
   * @param polledData: sets the {polledData} object of the controller
   */
  replacePolledData: function (polledData) {
    this.polledData.clear();
    this.set('polledData', polledData);
  },

  /**
   *
   * @param task
   * @returns {String} The appropriate message for the host as per the running task.
   */
  displayMessage: function (task) {
    var role = App.format.role(task.role);
    switch (task.command) {
      case 'INSTALL':
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.install.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.install.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.install.inProgress') + role;
          case 'COMPLETED' :
            return Em.I18n.t('installer.step9.serviceStatus.install.completed') + role;
          case 'FAILED':
            return Em.I18n.t('installer.step9.serviceStatus.install.failed') + role;
        }
      case 'UNINSTALL':
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.inProgress') + role;
          case 'COMPLETED' :
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.completed') + role;
          case 'FAILED':
            return Em.I18n.t('installer.step9.serviceStatus.uninstall.failed') + role;
        }
      case 'START' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.start.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.start.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.start.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.start.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.start.failed');
        }
      case 'STOP' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.stop.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.stop.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.stop.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.stop.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.stop.failed');
        }
      case 'EXECUTE' :
      case 'SERVICE_CHECK' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.execute.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.execute.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.execute.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.execute.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.execute.failed');
        }
      case 'ABORT' :
        switch (task.status) {
          case 'PENDING':
            return Em.I18n.t('installer.step9.serviceStatus.abort.pending') + role;
          case 'QUEUED' :
            return Em.I18n.t('installer.step9.serviceStatus.abort.queued') + role;
          case 'IN_PROGRESS':
            return Em.I18n.t('installer.step9.serviceStatus.abort.inProgress') + role;
          case 'COMPLETED' :
            return role + Em.I18n.t('installer.step9.serviceStatus.abort.completed');
          case 'FAILED':
            return role + Em.I18n.t('installer.step9.serviceStatus.abort.failed');
        }
    }
    return '';
  },

  /**
   * Run start/check services after installation phase.
   * Does Ajax call to start all services
   */
  launchStartServices: function () {
    var data = {
      "RequestInfo": {
        "context": Em.I18n.t("requestInfo.startServices")
      },
      "Body": {
        "ServiceInfo": { "state": "STARTED" }
      }
    };
    var name = 'wizard.step9.installer.launch_start_services';

    if (this.get('content.controllerName') === 'addHostController') {
      var hostnames = [];
      for (var hostname in this.get('wizardController').getDBProperty('hosts')) {
        hostnames.push(hostname);
      }
      data = {
        "RequestInfo": {
          "context": Em.I18n.t("requestInfo.startHostComponents"),
          "query": "HostRoles/component_name.in(" + App.get('components.slaves').join(',') + ")&HostRoles/state=INSTALLED&HostRoles/host_name.in(" + hostnames.join(',') + ")"
        },
        "Body": {
          "HostRoles": { "state": "STARTED" }
        }
      };
      name = 'wizard.step9.add_host.launch_start_services';
    }
    data = JSON.stringify(data);
    if (App.testMode) {
      this.set('numPolls', 6);
    }

    App.ajax.send({
      name: name,
      sender: this,
      data: {
        data: data,
        cluster: this.get('content.cluster.name')
      },
      success: 'launchStartServicesSuccessCallback',
      error: 'launchStartServicesErrorCallback'
    });
  },

  /**
   * Success callback function for start services task.
   * @param jsonData: {json object} Contains Request id to poll.
   */
  launchStartServicesSuccessCallback: function (jsonData) {
    var clusterStatus = {};
    if (jsonData) {
      console.log("TRACE: Step9 -> In success function for the startService call");
      console.log("TRACE: Step9 -> value of the received data is: " + jsonData);
      var requestId = jsonData.Requests.id;
      console.log('requestId is: ' + requestId);
      clusterStatus = {
        status: 'INSTALLED',
        requestId: requestId,
        isStartError: false,
        isCompleted: false
      };
      this.hostHasClientsOnly(false);
      this.saveClusterStatus(clusterStatus);
    } else {
      console.log('ERROR: Error occurred in parsing JSON data');
      this.hostHasClientsOnly(true);
      clusterStatus = {
        status: 'STARTED',
        isStartError: false,
        isCompleted: true
      };
      this.saveClusterStatus(clusterStatus);
      this.set('status', 'success');
      this.set('progress', '100');
    }
    // We need to do recovery if there is a browser crash
    App.clusterStatus.setClusterStatus({
      clusterState: 'SERVICE_STARTING_3',
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });

    if (jsonData) {
      this.startPolling();
    }
  },

  /**
   * This function will be called for Add host wizard only.
   * @param jsonError: {boolean} Boolean is true when API to start services returns 200 ok and no json data
   */
  hostHasClientsOnly: function (jsonError) {
    this.get('hosts').forEach(function (host) {
      var OnlyClients = true;
      var tasks = host.get('logTasks');
      tasks.forEach(function (task) {
        var component = serviceComponents.findProperty('component_name', task.Tasks.role);
        if (!(component && component.isClient)) {
          OnlyClients = false;
        }
      });
      if (OnlyClients || jsonError) {
        host.set('status', 'success');
        host.set('progress', '100');
      }
    });
  },

  /**
   * Error callback function for start services task.
   */
  launchStartServicesErrorCallback: function (jqXHR) {
    console.log("ERROR");
    this.set('startCallFailed',true);
    var clusterStatus = {
      status: 'INSTALL FAILED',
      isStartError: false,
      isCompleted: false
    };
    this.saveClusterStatus(clusterStatus);
    this.get('hosts').forEach(function (host) {
      host.set('progress', '100');
    });
    this.set('progress','100');

    var params = {
      cluster: this.get('content.cluster.name')
    };

    if (this.get('content.controllerName') === 'addHostController') {
      params.name = 'wizard.step9.add_host.launch_start_services';
    } else {
      params.name = 'wizard.step9.installer.launch_start_services';
    }

    var opt = App.formatRequest.call(App.urls[params.name], params);
    App.ajax.defaultErrorHandler(jqXHR,opt.url,opt.type);
  },

  /**
   * marks a host's status as "success" if all tasks are in COMPLETED state
   */
  onSuccessPerHost: function (actions, contentHost) {
    if (actions.everyProperty('Tasks.status', 'COMPLETED') && this.get('content.cluster.status') === 'INSTALLED') {
      contentHost.set('status', 'success');
    }
  },

  /**
   * marks a host's status as "warning" if at least one of the tasks is FAILED, ABORTED, or TIMEDOUT and marks host's status as "failed" if at least one master component install task is FAILED.
   * note that if the master failed to install because of ABORTED or TIMEDOUT, we don't mark it as failed, because this would mark all hosts as "failed" and makes it difficult for the user
   * to find which host FAILED occurred on, if any
   * @param actions {Array} of tasks retrieved from polled data
   * @param contentHost {Object} A host object
   */

  onErrorPerHost: function (actions, contentHost) {
    if (!actions) return;
    if (actions.someProperty('Tasks.status', 'FAILED') || actions.someProperty('Tasks.status', 'ABORTED') || actions.someProperty('Tasks.status', 'TIMEDOUT')) {
      contentHost.set('status', 'warning');
    }
    if ((this.get('content.cluster.status') === 'PENDING' && actions.someProperty('Tasks.status', 'FAILED')) || (this.isMasterFailed(actions))) {
      contentHost.get('status') !== 'heartbeat_lost' ? contentHost.set('status', 'failed') : '';
    }
  },

  /**
   *
   * @param polledData : Json data polled from API.
   * @returns {boolean}  true if there is at least one FAILED task of master component install
   */
  isMasterFailed: function (polledData) {
    var result = false;
    polledData.filterProperty('Tasks.command', 'INSTALL').filterProperty('Tasks.status', 'FAILED').mapProperty('Tasks.role').forEach(
      function (role) {
        if (!App.get('components.slaves').contains(role)) {
          result = true;
        }
      }
    );
    return result;
  },

  /**
   * Mark a host status as in_progress if the any task on the host if either in IN_PROGRESS, QUEUED or PENDONG state.
   * @param actions {Array} of tasks retrieved from polled data
   * @param contentHost {Object} A host object
   */
  onInProgressPerHost: function (actions, contentHost) {
    var runningAction = actions.findProperty('Tasks.status', 'IN_PROGRESS');
    if (runningAction === undefined || runningAction === null) {
      runningAction = actions.findProperty('Tasks.status', 'QUEUED');
    }
    if (runningAction === undefined || runningAction === null) {
      runningAction = actions.findProperty('Tasks.status', 'PENDING');
    }
    if (runningAction !== null && runningAction !== undefined) {
      contentHost.set('status', 'in_progress');
      contentHost.set('message', this.displayMessage(runningAction.Tasks));
    }
  },

  /**
   * calculate progress of tasks per host
   * @param actions
   * @param contentHost
   * @return {Number}
   */
  progressPerHost: function (actions, contentHost) {
    var progress = 0;
    var actionsPerHost = actions.length;
    var completedActions = 0;
    var queuedActions = 0;
    var inProgressActions = 0;
    actions.forEach(function (action) {
      completedActions += +(['COMPLETED', 'FAILED', 'ABORTED', 'TIMEDOUT'].contains(action.Tasks.status));
      queuedActions += +(action.Tasks.status === 'QUEUED');
      inProgressActions += +(action.Tasks.status === 'IN_PROGRESS');
    }, this);
    /** for the install phase (PENDING), % completed per host goes up to 33%; floor(100 / 3)
     * for the start phase (INSTALLED), % completed starts from 34%
     * when task in queued state means it's completed on 9%
     * in progress - 35%
     * completed - 100%
     */
    switch (this.get('content.cluster.status')) {
      case 'PENDING':
        progress = actionsPerHost ? (Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsPerHost * 33)) : 33;
        break;
      case 'INSTALLED':
        progress = actionsPerHost ? (34 + Math.ceil(((queuedActions * 0.09) + (inProgressActions * 0.35) + completedActions ) / actionsPerHost * 66)) : 100;
        break;
      default:
        progress = 100;
        break;
    }
    contentHost.set('progress', progress.toString());
    return progress;
  },

  /**
   *
   * @param polledData : Josn data retrieved from API
   * @returns {Boolean} : Has step completed successfully
   */
  isSuccess: function (polledData) {
    return polledData.everyProperty('Tasks.status', 'COMPLETED');
  },

  /**
   * return true if:
   *  1. any of the master/client components failed to install
   *  OR
   *  2. at least 50% of the slave host components for the particular service component fails to install
   */
  isStepFailed: function () {
    var failed = false;
    var polledData = this.get('polledData');
    polledData.filterProperty('Tasks.command', 'INSTALL').mapProperty('Tasks.role').uniq().forEach(function (role) {
      if (failed) {
        return;
      }
      var actionsPerRole = polledData.filterProperty('Tasks.role', role);
      if (App.get('components.slaves').contains(role)) {
        // check slave components for success factor.
        // partial failure for slave components are allowed.
        var actionsFailed = actionsPerRole.filterProperty('Tasks.status', 'FAILED');
        var actionsAborted = actionsPerRole.filterProperty('Tasks.status', 'ABORTED');
        var actionsTimedOut = actionsPerRole.filterProperty('Tasks.status', 'TIMEDOUT');
        if ((((actionsFailed.length + actionsAborted.length + actionsTimedOut.length) / actionsPerRole.length) * 100) > 50) {
          failed = true;
        }
      } else if (actionsPerRole.someProperty('Tasks.status', 'FAILED') || actionsPerRole.someProperty('Tasks.status', 'ABORTED') ||
        actionsPerRole.someProperty('Tasks.status', 'TIMEDOUT')) {
        // check non-salve components (i.e., masters and clients).  all of these must be successfully installed.
        failed = true;
      }
    }, this);
    return failed;
  },

  /**
   * polling from ui stops only when no action has 'PENDING', 'QUEUED' or 'IN_PROGRESS' status
   * Makes a state transition
   * PENDING -> INSTALLED
   * PENDING -> INSTALL FAILED
   * INSTALLED -> STARTED
   * INSTALLED -> START_FAILED
   * @param polledData json data retrieved from API
   * @returns {Boolean} true if polling should stop; false otherwise
   */
  finishState: function (polledData) {
    if (this.get('content.cluster.status') === 'INSTALLED') {
      return this.isServicesStarted(polledData);
    } else if (this.get('content.cluster.status') === 'PENDING') {
      return this.isServicesInstalled(polledData);
    } else if (this.get('content.cluster.status') === 'INSTALL FAILED' || this.get('content.cluster.status') === 'START FAILED'
      || this.get('content.cluster.status') === 'STARTED') {
      this.set('progress', '100');
      return true;
    }
    return false;
  },

  /**
   * @param polledData Josn data retrieved from API
   * @returns {boolean} Has "Start All Services" request completed successfully
   */
  isServicesStarted: function (polledData) {
    var clusterStatus = {};
    if (!polledData.someProperty('Tasks.status', 'PENDING') && !polledData.someProperty('Tasks.status', 'QUEUED') && !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
      this.set('progress', '100');
      clusterStatus = {
        status: 'INSTALLED',
        requestId: this.get('content.cluster.requestId'),
        isCompleted: true
      };
      if (this.isSuccess(polledData)) {
        clusterStatus.status = 'STARTED';
        var serviceStartTime = App.dateTime();
        clusterStatus.installTime = ((parseInt(serviceStartTime) - parseInt(this.get('content.cluster.installStartTime'))) / 60000).toFixed(2);
      } else {
        clusterStatus.status = 'START FAILED'; // 'START FAILED' implies to step10 that installation was successful but start failed
      }
      this.saveClusterStatus(clusterStatus);
      this.saveInstalledHosts(this);
      return true;
    }
    return false;
  },

  /**
   * @param polledData Josn data retrieved from API
   * @returns {boolean} Has "Install All Services" request completed successfully
   */
  isServicesInstalled: function (polledData) {
    var clusterStatus = {};
    if (!polledData.someProperty('Tasks.status', 'PENDING') && !polledData.someProperty('Tasks.status', 'QUEUED') && !polledData.someProperty('Tasks.status', 'IN_PROGRESS')) {
      clusterStatus = {
        status: 'PENDING',
        requestId: this.get('content.cluster.requestId'),
        isCompleted: false
      };
      if (this.get('status') === 'failed') {
        clusterStatus.status = 'INSTALL FAILED';
        this.saveClusterStatus(clusterStatus);
        this.set('progress', '100');
        this.get('hosts').forEach(function (host) {
          host.set('progress', '100');
        });
        this.isAllComponentsInstalled();
      } else {
        this.set('progress', '34');
        if (this.get('content.controllerName') === 'installerController') {
          this.isAllComponentsInstalled();
        } else {
          this.launchStartServices();
        }
      }
      this.saveInstalledHosts(this);
      return true;
    }
    return false;
  },

  /**
   * This is done at HostRole level.
   * @param tasksPerHost {Array}
   * @param host {Object}
   */
  setLogTasksStatePerHost: function (tasksPerHost, host) {
    tasksPerHost.forEach(function (_task) {
      var task = host.logTasks.findProperty('Tasks.id', _task.Tasks.id);
      if (task) {
        task.Tasks.status = _task.Tasks.status;
        task.Tasks.exit_code = _task.Tasks.exit_code;
      } else {
        host.logTasks.pushObject(_task);
      }
    }, this);
  },

  /**
   * Parses the Json data retrieved from API and sets the task on the host of {hosts} array binded to template
   * @param polledData Json data retrieved from API
   * @returns {Boolean} True if stage transition is completed.
   * On true, polling will be stopped.
   */
  parseHostInfo: function (polledData) {
    console.log('TRACE: Entering host info function');
    var self = this;
    var totalProgress = 0;
    var tasksData = polledData.tasks;
    console.log("The value of tasksData is: ", tasksData);
    if (!tasksData) {
      console.log("Step9: ERROR: NO tasks available to process");
    }
    var requestId = this.get('content.cluster.requestId');
    tasksData.setEach('Tasks.request_id', requestId);
    if (polledData.Requests && polledData.Requests.id && polledData.Requests.id != requestId) {
      // We don't want to use non-current requestId's tasks data to
      // determine the current install status.
      // Also, we don't want to keep polling if it is not the
      // current requestId.
      return false;
    }
    this.replacePolledData(tasksData);
    var tasksHostMap = {};
    tasksData.forEach(function (task) {
      if (tasksHostMap[task.Tasks.host_name]) {
        tasksHostMap[task.Tasks.host_name].push(task);
      } else {
        tasksHostMap[task.Tasks.host_name] = [task];
      }
    });

    this.get('hosts').forEach(function (_host) {
      var actionsPerHost = tasksHostMap[_host.name] || []; // retrieved from polled Data
      if (actionsPerHost.length === 0) {
        if (this.get('content.cluster.status') === 'PENDING' || this.get('content.cluster.status') === 'INSTALL FAILED') {
          _host.set('progress', '33');
          _host.set('isNoTasksForInstall', true);
          _host.set('status', 'pending');
        }
        if (this.get('content.cluster.status') === 'INSTALLED' || this.get('content.cluster.status') === 'FAILED') {
          _host.set('progress', '100');
          _host.set('status', 'success');
        }
        console.log("INFO: No task is hosted on the host");
      } else {
        _host.set('isNoTasksForInstall', false);
      }
      this.setLogTasksStatePerHost(actionsPerHost, _host);
      this.onSuccessPerHost(actionsPerHost, _host);     // every action should be a success
      this.onErrorPerHost(actionsPerHost, _host);     // any action should be a failure
      this.onInProgressPerHost(actionsPerHost, _host);  // current running action for a host
      totalProgress += self.progressPerHost(actionsPerHost, _host);
      if (_host.get('progress') == '33' && _host.get('status') != 'failed' && _host.get('status') != 'warning') {
        _host.set('message', this.t('installer.step9.host.status.nothingToInstall'));
        _host.set('status', 'pending');
      }
    }, this);
    this.set('logTasksChangesCounter', this.get('logTasksChangesCounter') + 1);
    totalProgress = Math.floor(totalProgress / this.get('hosts.length'));
    this.set('progress', totalProgress.toString());
    console.log("INFO: right now the progress is: " + this.get('progress'));
    return this.finishState(tasksData);
  },

  /**
   * starts polling to the API.
   */
  startPolling: function () {
    this.set('isSubmitDisabled', true);
    this.doPolling();
  },

  /**
   *
   * @param requestId {Int} Request Id received on triggering install/start command successfully
   * @returns {string} URL to poll to track the result of the triggered command
   */
  getUrl: function (requestId) {
    var clusterName = this.get('content.cluster.name');
    var requestId = requestId || this.get('content.cluster.requestId');
    var url = App.apiPrefix + '/clusters/' + clusterName + '/requests/' + requestId + '?fields=tasks/Tasks/command,tasks/Tasks/exit_code,tasks/Tasks/start_time,tasks/Tasks/end_time,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,tasks/Tasks/status&minimal_response=true';
    console.log("URL for step9 is: " + url);
    return url;
  },

  /**
   * This function calls API just once to fetch log data of all tasks.
   * @param requestId {Int} Request Id received on triggering install/start command successfully
   */
  loadLogData: function (requestId) {
    var url = this.getUrl(requestId);
    var requestsId = this.get('wizardController').getDBProperty('cluster').oldRequestsId;
    if (App.testMode) {
      this.POLL_INTERVAL = 1;
    }

    requestsId.forEach(function (requestId) {
      url = this.getUrl(requestId);
      if (App.testMode) {
        this.POLL_INTERVAL = 1;
        url = this.get('mockDataPrefix') + '/poll_' + this.numPolls + '.json';
      }
      this.getLogsByRequest(url, false);
    }, this);
  },
  /**
   * {Number}
   * <code>taskId</code> of current open task
   */
  currentOpenTaskId: 0,

  /**
   * {Number}
   * <code>requestId</code> of current open task
   */
  currentOpenTaskRequestId: 0,

  /**
   * Load form server <code>stderr, stdout</code> of current open task
   */
  loadCurrentTaskLog: function () {
    var taskId = this.get('currentOpenTaskId');
    var requestId = this.get('currentOpenTaskRequestId');
    var clusterName = this.get('content.cluster.name');
    if (!taskId) {
      console.log('taskId is null.');
      return;
    }
    App.ajax.send({
      name: 'background_operations.get_by_task',
      sender: this,
      data: {
        'taskId': taskId,
        'requestId': requestId,
        'clusterName': clusterName,
        'sync': true
      },
      success: 'loadCurrentTaskLogSuccessCallback',
      error: 'loadCurrentTaskLogErrorCallback'
    });
  },

  /**
   * success callback function for getting log data of the opened task
   * @param data json object
   */
  loadCurrentTaskLogSuccessCallback: function (data) {
    var taskId = this.get('currentOpenTaskId');
    if (taskId) {
      var currentTask = this.get('hosts').findProperty('name', data.Tasks.host_name).get('logTasks').findProperty('Tasks.id', data.Tasks.id);
      if (currentTask) {
        currentTask.Tasks.stderr = data.Tasks.stderr;
        currentTask.Tasks.stdout = data.Tasks.stdout;
      }
    }
    this.set('logTasksChangesCounter', this.get('logTasksChangesCounter') + 1);
  },

  /**
   * Error callback function for getting log data of the opened task
   */
  loadCurrentTaskLogErrorCallback: function () {
    this.set('currentOpenTaskId', 0);
  },

  /**
   * Function polls the API to retrieve data for the request.
   * @param url {string} url to poll
   * @param polling  {Boolean} whether to continue polling for status or not
   */
  getLogsByRequest: function (url, polling) {
    var self = this;
    $.ajax({
      type: 'GET',
      url: url,
      async: true,
      timeout: App.timeout,
      dataType: 'text',
      success: function (data) {
        var parsedData = jQuery.parseJSON(data);
        console.log("TRACE: In success function for the GET logs data");
        console.log("TRACE: Step9 -> The value is: ", parsedData);
        var result = self.parseHostInfo(parsedData);
        if (!polling) {
          if (self.get('content.cluster.status') === 'INSTALL FAILED') {
            self.isAllComponentsInstalled();
          }
          return;
        }
        if (result !== true) {
          window.setTimeout(function () {
            if (self.get('currentOpenTaskId')) {
              self.loadCurrentTaskLog();
            }
            self.doPolling();
          }, self.POLL_INTERVAL);
        }
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: STep9 -> In error function for the GET logs data");
        console.log("TRACE: STep9 -> value of the url is: " + url);
        console.log("TRACE: STep9 -> error code status is: " + request.status);
      },

      statusCode: require('data/statusCodes')
    }).retry({times: App.maxRetries, timeout: App.timeout}).then(null,
      function () {
        App.showReloadPopup();
        console.log('Install services all retries failed');
      }
    );
  },

  /**
   * Delegates the function call to {getLogsByRequest} with appropriate params
   */
  doPolling: function () {
    var url = this.getUrl();

    if (App.testMode) {
      this.numPolls++;
      url = this.get('mockDataPrefix') + '/poll_' + this.get('numPolls') + '.json';

    }
    this.getLogsByRequest(url, true);
  },

  /**
   * Check that all components are in INSTALLED state before issuing start command
   */
  isAllComponentsInstalled: function () {
    if (this.get('content.controllerName') !== 'installerController') {
      return;
    }
    var name = 'wizard.step9.installer.get_host_status';
    App.ajax.send({
      name: name,
      sender: this,
      data: {
        cluster: this.get('content.cluster.name')
      },
      success: 'isAllComponentsInstalledSuccessCallback',
      error: 'isAllComponentsInstalledErrorCallback'
    });
  },

  /**
   * Success callback function for API checking host state and host_components state.
   * @param jsonData {Object}
   */
  isAllComponentsInstalledSuccessCallback: function (jsonData) {
    var clusterStatus = {
      status: 'INSTALL FAILED',
      isStartError: true,
      isCompleted: false
    };
    var hostsWithHeartbeatLost = [];
    jsonData.items.filterProperty('Hosts.host_state', 'HEARTBEAT_LOST').forEach(function (host) {
      var hostComponentObj = {hostName: host.Hosts.host_name};
      var componentArr = [];
      host.host_components.forEach(function (_hostComponent) {
        var componentName = App.format.role(_hostComponent.HostRoles.component_name);
        componentArr.pushObject(componentName);
      }, this);
      hostComponentObj.componentNames = this.getComponentMessage(componentArr);
      hostsWithHeartbeatLost.pushObject(hostComponentObj);
    }, this);
    this.set('hostsWithHeartbeatLost', hostsWithHeartbeatLost);
    if (hostsWithHeartbeatLost.length) {
      this.get('hosts').forEach(function (host) {
        if (hostsWithHeartbeatLost.someProperty(('hostName'), host.get('name'))) {
          host.set('status', 'heartbeat_lost');
        } else if (host.get('status') !== 'failed' && host.get('status') !== 'warning') {
          host.set('message', Em.I18n.t('installer.step9.host.status.startAborted'));
        }
        host.set('progress', '100');
      });
      this.set('progress', '100');
      this.saveClusterStatus(clusterStatus);
    } else if (this.get('content.cluster.status') === 'PENDING') {
      this.launchStartServices();
    }

  },

  /**
   * Error callback function for API checking host state and host_components state
   */
  isAllComponentsInstalledErrorCallback: function () {
    console.log("ERROR");
    var clusterStatus = {
      status: 'INSTALL FAILED',
      isStartError: true,
      isCompleted: false
    };
    this.set('progress', '100');
    this.get('hosts').forEach(function (host) {
      if (host.get('status') !== 'failed' && host.get('status') !== 'warning') {
        host.set('message', Em.I18n.t('installer.step9.host.status.startAborted'));
        host.set('progress', '100');
      }
    });
    this.saveClusterStatus(clusterStatus);
  },

  /**
   * @param componentArr {Array}  Array of components
   * @returns {String} Formatted string of components to display on the UI.
   */
  getComponentMessage: function (componentArr) {
    var label;
    componentArr.forEach(function (_component) {
      if (_component === componentArr[0]) {
        label = _component;
      } else if (_component !== componentArr[componentArr.length - 1]) {           // [clients.length - 1]
        label = label + ' ' + _component;
        if (_component !== componentArr[componentArr.length - 2]) {
          label = label + ',';
        }
      } else {
        label = label + ' ' + Em.I18n.t('and') + ' ' + _component;
      }
    }, this);
    return label;
  },

  /**
   * save cluster status in the parentController and localdb
   * @param clusterStatus {Object}
   */
  saveClusterStatus: function(clusterStatus) {
    if (!App.testMode) {
      App.router.get(this.get('content.controllerName')).saveClusterStatus(clusterStatus);
    } else {
      this.set('content.cluster',clusterStatus);
    }
  },

  /**
   * save cluster status in the parentController and localdb
   * @param context
   */
  saveInstalledHosts: function(context) {
    if (!App.testMode) {
      App.router.get(this.get('content.controllerName')).saveInstalledHosts(context)
    }
  }
});
