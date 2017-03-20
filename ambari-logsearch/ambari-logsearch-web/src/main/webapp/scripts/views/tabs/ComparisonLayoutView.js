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
    'backbone',
    'utils/Utils',
    'hbs!tmpl/tabs/ComparisonLayoutView_tmpl'
],function(require,Backbone,Utils,ComparisonLayoutViewTmpl) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends ComparisonLayoutView */
        {
            _viewName: 'ComparisonLayoutView',

            template: ComparisonLayoutViewTmpl,


            /** ui selector cache */
            ui: {
                comparisonTab: '.comparisonTab'
            },

            /** ui events hash */
            events: function() {
                var events = {};
                return events;
            },
            regions: { },

            /**
             * intialize a new ComparisonLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'globalVent', 'params', 'componetList'));
            },
            bindEvents: function() {},
            onRender: function() {
                this.createRegion()
            },
            createRegion: function() {
                var that = this;
                require(['views/tabs/ComparisonView'],function(ComparisonView){

                    if (that.componetList) {
                        var $parent = that.ui.comparisonTab;
                        _.each(that.componetList, function(object,i) {
                            var id = (object.host_name + '_' + object.component_name).replace(/\./g, "_");
                            if(i % 2 == 0 && i > 0){
                                var $div = $("<div class='row comparisonTab'></div>");
                                that.ui.comparisonTab.parent().append($div);
                                $parent = $div;
                            }
                            $parent.append('<div id="' + id + '" style="position: relative;"></div>');
                            
                            var region = {};
                            region[id] = '#' + id;
                            that.addRegions(region);
                            var region = that.getRegion(id);
                            region.show(new ComparisonView({
                                globalVent: that.globalVent,
                                params: _.extend({},that.params, {
                                    'host_name': object.host_name,
                                    'component_name': object.component_name
                                }),
                                datePickerPosition:(((i+1) % 2 == 0)?("left"):("right"))
                            }));
                        })
                    }
                })
            }


        });


});
