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

  showDetailsModal: false,

  classNames:['visual-explain-container'],

  explainDetailData: '',

  visualExplainInput: Ember.computed('visualExplainJson', function () {
    return this.get('visualExplainJson');
  }),

  isQueryRunning:false,

  didInsertElement() {
    this._super(...arguments);

    const onRequestDetail = data => this.set('explainDetailData', JSON.stringify( data, null, '  ') );
    const explainData = JSON.parse(this.get('visualExplainInput'));
    // if(explainData) {
      explain(explainData, '#explain-container', onRequestDetail);
    // }

  },

  click(event){

    if(this.get('explainDetailData') === ''){
      return;
    }

    Ember.run.later(() => {
      this.set('showDetailsModal', true);
    }, 100);
  },

  actions:{
    expandQueryResultPanel(){
      this.sendAction('expandQueryResultPanel');
    },

    closeModal(){
      this.set('showDetailsModal', false);
      this.set('explainDetailData', '');
      return false;
    }

  }

});

