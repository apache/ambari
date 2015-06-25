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

define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'hbs!tmpl/site/header'], function(require, vent, localization, headerTmpl){
  'use strict';

  var HeaderView = Marionette.LayoutView.extend({
    _viewNmae: 'Header',

    template: headerTmpl,

    templateHelpers: function() {},

    regions: {

    },

    ui: {
      toplogyLink: '[data-id="topology"]',
      clusterLink: '[data-id="cluster"]'
    },

    events: {
      'click [data-id="topology"]': 'showTopologySection',
      'click [data-id="cluster"]': 'showClusterSection',
      'click #refresh' : 'evRefresh'
    },

    initialize: function (options) {
      this.clusterTabFlag = false;
      this.bindEvent();
    },

    bindEvent: function() {
      var that = this;
      vent.on('Breadcrumb:Show', function(name){
        that.$('.breadcrumb').removeClass('displayNone');
        that.$('#breadcrumbName').html(name);
      });
      vent.on('Breadcrumb:Hide', function(){
        that.$('.breadcrumb').addClass('displayNone');
      });
      vent.on('LastUpdateRefresh', function(flag){
        if(flag)
          that.$('.last-refreshed').css("margin-top","0px");
        else
          that.$('.last-refreshed').css("margin-top","35px");
        that.$('#refreshTime').html(new Date().toLocaleString());
      });
    },

    onRender: function () {},

    showTopologySection: function () {
      if(!this.ui.toplogyLink.parent().hasClass('active')){
        this.ui.clusterLink.parent().removeClass('active');
        this.ui.toplogyLink.parent().addClass('active');
        vent.trigger('Region:showTopologySection');
      }
    },

    showClusterSection: function () {
      if(!this.ui.clusterLink.parent().hasClass('active')){
        this.ui.toplogyLink.parent().removeClass('active');
        this.ui.clusterLink.parent().addClass('active');
        vent.trigger('Region:showClusterSection');
      }
    },

    evRefresh: function(){
      if(this.ui.toplogyLink.parent().hasClass('active')){
        vent.trigger('Region:showTopologySection');
      } else {
        vent.trigger('Region:showClusterSection');
      }
    }

  });

  return HeaderView;
});