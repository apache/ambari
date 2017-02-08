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
import explain from '../utils/hive-explainer';

export default Ember.Component.extend({
  visualExplainJson:'',

  visualExplainInput: Ember.computed('visualExplainJson', function () {
    return this.get('visualExplainJson');
  }),

  isQueryRunning:false,

  didInsertElement(){
    this._super(...arguments);

    const width = '100vw', height = '100vh';

    d3.select('#explain-container').select('svg').remove();
    const svg = d3.select('#explain-container').append('svg')
      .attr('width', width)
      .attr('height', height);

    const container = svg.append('g');
    const zoom =
      d3.zoom()
        .scaleExtent([1 / 10, 4])
        .on('zoom', () => {
          container.attr('transform', d3.event.transform);
        });

      svg
        .call(zoom);

    const onRequestDetail = data => this.sendAction('showStepDetail', data);

    explain(JSON.parse(this.get('visualExplainInput')), svg, container, zoom, onRequestDetail);

  },

  actions:{
    expandQueryResultPanel(){
      this.sendAction('expandQueryResultPanel');
    }
  }

});

