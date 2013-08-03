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
var numberUtils = require('utils/number_utils');

App.NameNodeHeapPieChartView = App.DashboardWidgetView.extend({

  templateName: require('templates/main/dashboard/widgets/pie_chart'),
  title: Em.I18n.t('dashboard.widgets.NameNodeHeap'),
  id: '1',

  isPieChart: true,
  isText: false,
  isProgressBar: false,
  model_type: 'hdfs',

  hiddenInfo: function () {
  var memUsed = this.get('model').get('jvmMemoryHeapUsed');
  var memCommitted = this.get('model').get('jvmMemoryHeapCommitted');
  var percent = memCommitted > 0 ? ((100 * memUsed) / memCommitted) : 0;
  var result = [];
  result.pushObject(percent.toFixed(1) + '% used');
  result.pushObject(numberUtils.bytesToSize(memUsed, 1, 'parseFloat', 1000000) + ' of ' + numberUtils.bytesToSize(memCommitted, 1, 'parseFloat', 1000000));
  return result;
  }.property('model.jvmMemoryHeapUsed', 'model.jvmMemoryHeapCommitted'),

  thresh1: null,
  thresh2: null,
  maxValue: 100,

  isPieExist: function () {
    var total = this.get('model.jvmMemoryHeapCommitted') * 1000000;
    return total > 0 ;
  }.property('model.jvmMemoryHeapCommitted'),

  content: App.ChartPieView.extend({

    model: null,  //data bind here
    id: 'widget-nn-heap', // html id
    stroke: '#D6DDDF', //light grey
    thresh1: null, //bind from parent
    thresh2: null,
    innerR: 25,

    existCenterText: true,
    centerTextColor: function () {
      return this.get('contentColor');
    }.property('contentColor'),

    palette: new Rickshaw.Color.Palette({
      scheme: [ '#FFFFFF', '#D6DDDF'].reverse()
    }),

    data: function () {
      var used = this.get('model.jvmMemoryHeapUsed') * 1000000;
      var total = this.get('model.jvmMemoryHeapCommitted') * 1000000;
      var percent = total > 0 ? ((used)*100 / total).toFixed() : 0;
      return [ percent, 100 - percent];
    }.property('model.jvmMemoryHeapUsed', 'model.jvmMemoryHeapCommitted'),

    contentColor: function () {
      var used = parseFloat(this.get('data')[0]);
      var thresh1 = parseFloat(this.get('thresh1'));
      var thresh2 = parseFloat(this.get('thresh2'));
      var color_green = '#95A800';
      var color_red = '#B80000';
      var color_orange = '#FF8E00';
      if (used <= thresh1) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: [ '#FFFFFF', color_green  ].reverse()
        }))
        return color_green;
      } else if (used <= thresh2) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: [ '#FFFFFF', color_orange  ].reverse()
        }))
        return color_orange;
      } else {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: [ '#FFFFFF', color_red  ].reverse()
        }))
        return color_red;
      }
    }.property('data', 'this.thresh1', 'this.thresh2'),

    // refresh text and color when data in model changed
    refreshSvg: function () {
      // remove old svg
      var old_svg =  $("#" + this.id);
      if(old_svg){
        old_svg.remove();
      }
      // draw new svg
      this.appendSvg();
    }.observes('this.data', 'this.thresh1', 'this.thresh2')
  })

})


