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

import Ember from 'ember';

export default Ember.Component.extend({
  tagName: 'visualization-view',

  initDragAndDrop: function() {
    /*Ember.run.later(function(){
      self.$('.column-drag').draggable({
        helper: "clone",
        revert: "invalid"
      });
      self.$(".droppable.active").droppable({
        activeClass: "ui-state-default",
        hoverClass: "ui-state-hover",
        drop: function( event, ui ) {
          self.$( this )
            .addClass( "ui-state-highlight").html("Dropped");
          console.log(event);
          console.log(ui);

        }
      });
    });*/
  }.observes('json'),

  testing: function () {
    var self = this;

    var draggable = this.$('.column-drag').draggabilly({});
    draggable.on( 'dragEnd', function( event, pointer ) {
      console.log("Drag ended");
    });
  },

  initVisualization: function () {
    var vled = {
      version: 0.1,
      spec: {}
    };
    var self = this;
    vled.data = {
      values: [
        {"x":"A", "y":28}, {"x":"B", "y":55}, {"x":"C", "y":43},
        {"x":"D", "y":91}, {"x":"E", "y":81}, {"x":"F", "y":53},
        {"x":"G", "y":19}, {"x":"H", "y":87}, {"x":"I", "y":52}
      ]
      };
    vled.format = function() {
      var el = d3.select('#vlspec'),
        spec = JSON.parse(el.property('value')),
        text = JSON.stringify(spec, null, '  ', 60);
      el.property('value', text);
    };

    vled.parse = function() {
      var spec;
      try {
        spec = JSON.parse(d3.select('#vlspec').property('value'));
      } catch (e) {
        console.warn(e);
        return;
      }
      var done = function() {
        // only add url if data is not provided explicitly
        var theme = (spec.data && spec.data.values) ? {} : {
          data: {
            "values": vled.data.values
          }
        };
        vled.data.stats = vl.summary(vled.data.values).reduce(function(s, p) {
          s[p.field] = p;
          return s;
        },{});
        vled.loadSpec(spec, theme);
      };

      done();
    };


    vled.loadSpec = function(vlspec, theme) {

      var spec = vl.compile(vlspec, vled.data.stats, theme);

      self.$('textarea').trigger('autosize.resize');

      vled.vis = null; // DEBUG
      console.log("Inside loadSpec");
      console.log(spec);
      console.log(theme);
      vg.parse.spec(spec, function(chart) {
        vled.vis = chart({el:'#vis', renderer: 'svg'});
        console.log("Inside Vega Parse");
        vled.vis.update();
        vled.vis.on('mouseover', function(ev, item) {
          console.log(item);
        });
      });
    };


    vled.init = function() {

      // Initialize application
      d3.select('#btn_spec_parse').on('click', vled.parse);

      console.log("Inside init()");
      document.getElementById('vlspec').value = JSON.stringify({
        "data": {
          "values": [
            {"x":"A", "y":28}, {"x":"B", "y":55}, {"x":"C", "y":43},
            {"x":"D", "y":91}, {"x":"E", "y":81}, {"x":"F", "y":53},
            {"x":"G", "y":19}, {"x":"H", "y":87}, {"x":"I", "y":52}
          ]
        },
        "marktype": "bar",
        "encoding": {
          "y": {"type": "Q","name": "y"},
          "x": {"type": "O","name": "x"}
        }
      });

      vled.parse();
      vled.format();
    };

    vled.init();
    this.set('vled', vled);
    //this.testing();
  }.on('didInsertElement'),
});
