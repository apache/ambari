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

App.MainChartsHeatmapHostView = Em.View.extend({
  templateName: require('templates/main/charts/heatmap/heatmap_host'),
  /** @private */ hostClass: 'hostBlock',

  /**
   * show Host details block and move it near the cursor
   * @param {Object} e
   * @this App.MainChartsHeatmapHostView
   */
  mouseEnter:function (e) {
    var host = this.get('content');
    var view = App.MainChartsHeatmapHostDetailView.create();
    $.each(view.get('details'), function (i) {
      view.set('details.' + i, host.get(i));
    });
    $("#heatmapDetailsBlock").css('top', e.pageY + 10);
    $("#heatmapDetailsBlock").css('left', e.pageX + 10);

    $("#heatmapDetailsBlock").show();
  },

  /**
   * hide Host details block
   * @param {Object} e
   * @this App.MainChartsHeatmapHostView
   */
  mouseLeave:function (e) {
    $("#heatmapDetailsBlock").hide();
  }
});