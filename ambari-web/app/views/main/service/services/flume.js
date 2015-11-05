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
var date = require('utils/date/date');
var sort = require('views/common/sort_view');
require('views/main/service/info/metrics/flume/flume_agent_metrics_section');

App.MainDashboardServiceFlumeView = App.TableView.extend(App.MainDashboardServiceViewWrapper, {
  templateName: require('templates/main/service/services/flume'),

  isFullWidth: true,

  pagination: false,

  selectedHost: null,

  flumeAgentsCount:0,

  content: function () {
    var flumeAgents = this.get('service.agents'),
    hostNames = flumeAgents.mapProperty('hostName').uniq(),
    content = [],
    self = this;
    hostNames.forEach(function(hostName) {
      var agents = flumeAgents.filterProperty('hostName', hostName);
      content.push(
        Em.Object.create({
          hostName: hostName,
          agents: agents,
          rowspan: agents.length,
          firtstAgent: agents[0],
          otherAgents: agents.without(agents[0])
        })
      );
    });
    return content;
  }.property('App.FlumeAgent.@each.id', 'flumeAgentsCount'),



  summaryHeader: function () {
    var agents = App.FlumeService.find().objectAt(0).get('agents'),
    agentCount = agents.get('length'),
    hostCount = this.get('service.flumeHandlersTotal');
    return this.t("dashboard.services.flume.summary.title").format(hostCount, (hostCount > 1 ? "s" : ""), agentCount,  (agentCount > 1 ? "s" : ""));
  }.property('service.agents', 'service.hostComponents.length'),

  flumeHandlerComponent: function () {
    return Em.Object.create({
      componentName: 'FLUME_HANDLER'
    });
    //return App.HostComponent.find().findProperty('componentName', 'FLUME_HANDLER');
  }.property(),

  agentView: Em.View.extend({
    content: null,
    tagName: 'tr',

    click: function (e) {
      var numberOfAgents = this.get('content.agents').length;
      if($(e.target).attr('class') == "agent-host-name" || $(e.target).attr('class') == "agent-host-link"){
        var currentTargetRow = $(e.currentTarget);
        if ($(e.target).attr('class') == "agent-host-link") {
          currentTargetRow = currentTargetRow.parent(".agent-host-name").parent();
        }
        currentTargetRow.parents("table:first").find('tr').removeClass('highlight');
        currentTargetRow.addClass('highlight').nextAll("tr").slice(0,numberOfAgents - 1).addClass('highlight');
        this.get('parentView').showAgentInfo(this.get('content'));
      }
    }
  }),

  sortView: sort.wrapperView,

  hostSort: sort.fieldView.extend({
    column: '1',
    name: 'hostName',
    displayName: Em.I18n.t('common.host')
  }),

  didInsertElement: function () {
    var self = this;
    this.filter();
    this.$().on('click','.flume-agents-actions .dropdown-toggle', function (e){
      self.setDropdownPosition(this);
    });
  },

  /**
   * Locate dropdown menu absolutely outside of scrollable block
   * @param element
   */
  setDropdownPosition: function (element) {
    var button = $(element);
    var dropdown = button.parent().find('.dropdown-menu');
    dropdown.css('top', button.offset().top + button.outerHeight() + "px");
    dropdown.css('left', button.offset().left + "px");
  },

  willDestroyElement: function () {
    this.$().off();
  },

  /**
   * Change classes for dropdown DOM elements after status change of selected agent
   */
  setActionsDropdownClasses: function () {
    this.get('content').forEach(function(hosts){
      hosts.agents.forEach(function (agent) {
        agent.set('isStartAgentDisabled', agent.get('status') !== 'NOT_RUNNING');
        agent.set('isStopAgentDisabled', agent.get('status') !== 'RUNNING');
      });
    });
  }.observes('service.agents.@each.status'),

  updateFlumeAgentsCount: function () {
    this.set('flumeAgentsCount', this.get('service.agents.length'));
  }.observes('service.agents.length'),

  /**
   * Action handler from flume tepmlate.
   * Highlight selected row and show metrics graphs of selected agent.
   *
   * @method showAgentInfo
   * @param {object} agent
   */
  showAgentInfo: function (host) {
    this.set('selectedHost', host);
    this.setAgentMetrics(host);
  },
  /**
   * Show Flume agent metric.
   *
   * @method setFlumeAgentMetric
   * @param {object} agent - DS.model of agent
   */
  setAgentMetrics: function (host) {
    var mockMetricData = [
      {
        header: 'channelName',
        metricType: 'CHANNEL'
      },
      {
        header: 'sinkName',
        metricType: 'SINK'
      },
      {
        header: 'sourceName',
        metricType: 'SOURCE'
      }
    ];
    var metricViews = mockMetricData.map(function (mockData, index) {
      return App.FlumeAgentMetricsSectionView.extend({
        index: index,
        metricTypeKey: mockData.header,
        metricView: App.MainServiceInfoFlumeGraphsView.extend(),
        metricViewData: {
          agent: host,
          metricType: mockData.metricType
        }
      });
    });
    this.set('parentView.collapsedSections', metricViews);
  }
});
