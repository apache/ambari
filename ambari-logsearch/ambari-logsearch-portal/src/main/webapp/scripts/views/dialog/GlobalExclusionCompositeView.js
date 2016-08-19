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
    'utils/Globals',
    'collections/VGroupList',
    'hbs!tmpl/dialog/GlobalExclusionCompositeView_tmpl',
    'views/dialog/GlobalExclusionItemView'
],function(require,Backbone,Globals,VGroupList,GlobalExclusionCompositeView,GlobalExclusionItemView) {
    'use strict';

    return Backbone.Marionette.CompositeView.extend(
        /** @lends GlobalExclusionListView */
        {
            _viewName: 'GlobalExclusionListView',

            template: GlobalExclusionCompositeView,

            itemView: GlobalExclusionItemView,

            itemViewContainer: "div[data-id='addRowDiv']",

            itemViewOptions: function() {
                return {
                    col: this.componentsList
                }
            },

            initialize: function(options) {

                _.extend(this, _.pick(options, 'exclusionObj'));
                this.componentsList = new VGroupList([], {
                    state: {
                        pageSize: 1000
                    }
                });
                this.componentsList.url = Globals.baseURL + "service/logs/components";

                this.collection = this.exclusionObj.logMessageCollection;
                if (this.collection.length == 0) {
                    this.collection.add(new this.collection.model());
                }

                this.bindEvents();
            },
            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                addRow: "button[data-id='addRowButton']",
                addRowDiv: "div[data-id='addRowDiv']",
                getRow: "div[data-id='getRow']",
                select2Input: ".select2IComponents",
                select2Load:".select2Load"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['click ' + this.ui.addRow] = 'onAddRow';
                return events;
            },

            /**
             * intialize a new GlobalExclusionLView Layout
             * @constructs
             */

            onRender: function() {
                this.componentsList.fetch({
                    reset: true
                });
                this.ui.select2Input.select2({
                    placeholder: 'Exclude Components',
                    data: [],
                    width: '75%',
                });
                this.ui.select2Input.select2("disable");
            },
            onAddRow: function() {
                this.collection.add(new this.collection.model());
            },
            bindEvents: function() {
                this.listenTo(this.componentsList, "reset", function(col, abc) {
                    this.setupSelect2Fields(col, "type", 'type', 'excludeComponents', 'Exclude Components');
                    this.setComponentsList(this.exclusionObj.components)
                        // this.setupSelect2Fields(col, "type", 'type', 'includeComponents', 'Include Components');
                }, this);
            },
            setupSelect2Fields: function(col, idKey, textKey, selectTagId, placeHolder) {
                var that = this,
                    data = [];
                data = _.pluck(col.models, 'attributes');
                for (var i = 0; i < data.length; i++) {
                    data[i].id = data[i].type;
                }
                this.ui.select2Input.select2({
                    placeholder: (placeHolder) ? placeHolder : 'Select',
                    tags: true,
                    allowClear: true,
                    width: '75%',
                    data: {
                        results: data,
                        text: textKey
                    },
                    formatSelection: function(item) {
                        return item[textKey];
                    },
                    formatResult: function(item) {
                        return item[textKey];
                    }
                })
                this.ui.select2Input.select2("enable");
                this.ui.select2Load.hide();
            },
            setComponentsList: function(values) {
                if (values)
                    this.ui.select2Input.select2('val', values);
                else
                    this.ui.select2Input.select2('val', []);
            }

        });
});
