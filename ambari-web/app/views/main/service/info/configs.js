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

App.MainServiceInfoConfigsView = Em.View.extend({
  templateName: require('templates/main/service/info/configs'),
  didInsertElement: function () {
    var controller = this.get('controller');
    controller.loadStep();
    var advanced = this.get('controller.selectedService.configCategories').filterProperty('name', 'Advanced');
    if(advanced.length) advanced.objectAt(0).set('isAdvanced', true);
  },
  onToggleBlock: function(event){
    $("#" + event.context).toggle('blind', 500);
  }
});

App.ServiceConfigsByCategoryView = Ember.View.extend({

  content: null,


  category: null,
  serviceConfigs: null, // General, Advanced, NameNode, SNameNode, DataNode, etc.

  categoryConfigs: function () {
    return this.get('serviceConfigs').filterProperty('category', this.get('category.name'))
  }.property('serviceConfigs.@each').cacheable(),
  didInsertElement: function(){
    $("#Advanced").hide();
  },
  layout: Ember.Handlebars.compile('<div {{bindAttr id="view.category.name"}} class="accordion-body collapse in"><div class="accordion-inner">{{yield}}</div></div>')
});