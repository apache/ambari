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
var date = require('utils/date');
var sort = require('views/common/sort_view');

App.MainDashboardServiceFlumeView = App.TableView.extend({
  templateName: require('templates/main/service/services/flume'),

  pagination: false,

  isActionsDisabled: true,

  isStartAgentDisabled: true,

  isStopAgentDisabled: true,

  content: function () {
    return this.get('service.agents');
  }.property('service.agents.length'),

  summaryHeader: function () {
    var agents = App.FlumeService.find().objectAt(0).get('agents');//this.get('service.agents');
    var agentCount = agents.get('length');
    var hostCount = agents.mapProperty('host').uniq().get('length');
    var prefix = agentCount == 1 ? this.t("dashboard.services.flume.summary.single") :
        this.t("dashboard.services.flume.summary.multiple").format(agentCount);
    var suffix = hostCount == 1 ? this.t("dashboard.services.flume.summary.hosts.single") :
        this.t("dashboard.services.flume.summary.hosts.multiple").format(hostCount);
    return prefix + suffix;
  }.property('service.agents'),

  flumeHandlerComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'FLUME_HANDLER');
  }.property(),

  agentView: Em.View.extend({
    content: null,
    tagName: 'tr',
    classNameBindings: ['selectedClass'],

    selectedClass: function () {
      return this.get('controller.selectedFlumeAgent.id') === this.get('content.id') ? 'highlight' : '';
    }.property('controller.selectedFlumeAgent'),

    click: function () {
      this.get('parentView').showAgentInfo(this.get('content'));
    }
  }),

  sortView: sort.wrapperView,

  statusSort: sort.fieldView.extend({
    column: '1',
    name: 'status',
    displayName: ''
  }),

  agentSort: sort.fieldView.extend({
    column: '2',
    name: 'name',
    displayName: Em.I18n.t('dashboard.services.flume.agent')
  }),

  hostSort: sort.fieldView.extend({
    column: '3',
    name: 'hostName',
    displayName: Em.I18n.t('common.host')
  }),

  sourceSort: sort.fieldView.extend({
    column: '4',
    name: 'sourcesCount',
    displayName: Em.I18n.t('dashboard.services.flume.sources')
  }),

  channelSort: sort.fieldView.extend({
    column: '5',
    name: 'channelsCount',
    displayName: Em.I18n.t('dashboard.services.flume.channels')
  }),

  sinkSort: sort.fieldView.extend({
    column: '6',
    name: 'sinksCount',
    displayName: Em.I18n.t('dashboard.services.flume.sinks')
  }),

  didInsertElement: function () {
    this.set('controller.selectedFlumeAgent', null);
    this.filter();
  },

  /**
   * Change classes for dropdown DOM elements after status change of selected agent
   */
  setActionsDropdownClasses: function () {
    var selectedFlumeAgent = this.get('controller.selectedFlumeAgent');
    this.set('isActionsDisabled', !selectedFlumeAgent);
    if (selectedFlumeAgent) {
      this.set('isStartAgentDisabled', selectedFlumeAgent.get('status') !== 'NOT_RUNNING');
      this.set('isStopAgentDisabled', selectedFlumeAgent.get('status') !== 'RUNNING');
    }
  }.observes('controller.selectedFlumeAgent', 'controller.selectedFlumeAgent.status'),

  /**
   * Action handler from flume tepmlate.
   * Highlight selected row and show metrics graphs of selected agent.
   *
   * @method showAgentInfo
   * @param {object} agent
   */
  showAgentInfo: function (agent) {
    this.set('controller.selectedFlumeAgent', agent);
    this.setAgentMetrics(agent);
  },
  /**
   * Show Flume agent metric.
   *
   * @method setFlumeAgentMetric
   * @param {object} agent - DS.model of agent
   */
  setAgentMetrics: function(agent) {
    var getMetricTitle = function(metricTypeKey, hostName) {
      var metricType = Em.I18n.t('services.service.info.metrics.flume.' + metricTypeKey).format(Em.I18n.t('common.metrics'));
      return  metricType + ' - ' + hostName;
    };
    var gangliaUrlTpl = App.router.get('clusterController.gangliaUrl') + '/?r=hour&cs=&ce=&m=load_one&s=by+name&c=HDPFlumeServer&h={0}&host_regex=&max_graphs=0&tab=m&vn=&sh=1&z=small&hc=4';
    var agentHostMock = agent.get('host.hostName');
    var mockMetricData = [
      {
        header: 'channelName',
        metricView: App.MainServiceInfoFlumeGraphsView.extend(),
        metricViewData: {
          agent: agent,
          metricType: 'CHANNEL'
        }
      },
      {
        header: 'sinkName',
        metricView: App.MainServiceInfoFlumeGraphsView.extend(),
        metricViewData: {
          agent: agent,
          metricType: 'SINK'
        }
      },
      {
        header: 'sourceName',
        metricView: App.MainServiceInfoFlumeGraphsView.extend(),
        metricViewData: {
          agent: agent,
          metricType: 'SOURCE'
        }
      }
    ];
    mockMetricData.forEach(function(mockData, index) {
      mockData.header = getMetricTitle(mockData.header, agentHostMock);
      mockData.url = gangliaUrlTpl.format(agentHostMock);
      mockData.id = 'metric' + index;
      mockData.toggleIndex = '#' + mockData.id;
    });
    this.set('parentView.collapsedSections', mockMetricData);
  }
});
