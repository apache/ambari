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

App.SliderAppSummaryView = Ember.View.extend({

  classNames: ['app_summary'],

  /**
   * List of graphs shown on page
   * Format:
   * <code>
   *   [
   *      {
   *        id: string,
   *        view: App.AppMetricView
   *      },
   *      {
   *        id: string,
   *        view: App.AppMetricView
   *      },
   *      ....
   *   ]
   * </code>
   * @type {{object}[][]}
   */
  graphs: [],
  
  /**
   * Update <code>graphs</code>-list when <code>model</code> is updated
   * New metrics are pushed to <code>graphs</code> (not set new list to <code>graphs</code>!) to prevent page flickering
   * @method updateGraphs
   */
  updateGraphs: function() {
    var model = this.get('controller.model'),
      existingGraphs = this.get('graphs'),
      graphsBeenChanged = false;

    if (model) {
      var currentGraphIds = [];
      var supportedMetrics = model.get('supportedMetricNames');
      if (supportedMetrics) {
        var appId = model.get('id');
        supportedMetrics.split(',').forEach(function(metricName) {
          var graphId = metricName + '_' + appId;
          currentGraphIds.push(graphId);
          if (!existingGraphs.isAny('id', graphId)) {
            graphsBeenChanged = true;
            var view = App.AppMetricView.extend({
              app: model,
              metricName: metricName
            });
            existingGraphs.push({
              id : graphId,
              view : view
            });
          }
        });
      }
      // Delete not existed graphs
      existingGraphs = existingGraphs.filter(function(existingGraph) {
        graphsBeenChanged = graphsBeenChanged || !currentGraphIds.contains(existingGraph.id);
        return currentGraphIds.contains(existingGraph.id);
      });
      if (graphsBeenChanged) {
        this.set('graphs', existingGraphs);
      }
    }
  }.observes('controller.model.supportedMetricNames'),

  /**
   * @type {string}
   */
  gangliaUrl: function () {
    return 'http://' + App.get('gangliaHost') + '/ganglia';
  }.property('App.gangliaHost'),

  /**
   * @type {string}
   */
  nagiosUrl: function () {
    return 'http://' + App.get('nagiosHost') + '/nagios';
  }.property('App.nagiosHost'),

  fitPanels: function () {
    var heightLeft = parseInt(this.$('.panel-summury').css('height'));
    this.$('.panel-components').css('height', ((heightLeft < 200) ? 200 : heightLeft - 20) / 2);
    this.$('.panel-alerts .app-alerts').css('height', ((heightLeft < 200) ? 200 : heightLeft - 106) / 2);
  }.on('didInsertElement'),

  AlertView: Em.View.extend({
    content: null,
    tagName: 'li',
    tooltip: function () {
      var self = this;
      return Ember.Object.create({
        trigger: 'hover',
        content: this.get('content.timeSinceAlertDetails'),
        placement: "bottom"
      });
    }.property('content')
  })

});
