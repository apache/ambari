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
    'hbs!tmpl/dialog/GlobalExclusionItemView_tmpl'
],function(require,Backbone,Globals,VGroupList,GlobalExclusionItemViewTmpl) {
    'use strict';

    return Backbone.Marionette.ItemView.extend(
        /** @lends GlobalExclusionListView */
        {

            template: GlobalExclusionItemViewTmpl,

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                selectionTypeLOV: "select[data-id='selectionTypeLOV']",
                /* select2Input: ".select2IComponents",*/
                textArea: "div[data-id='L']",
                closeButton: "div[data-id='closeButton']"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['change ' + this.ui.selectionTypeLOV] = 'onSelectionTypeLOVClicked';
                events['click ' + this.ui.closeButton] = 'onCloseButton';
                return events;
            },

            /**
             * intialize a new GlobalExclusionComponentView Layout
             * @constructs
             */
            initialize: function(options) {
                this.componentsList = options.col
                this.bindEvents();
            },
            onRender: function() {
                var that = this;
                this.$('textarea').text(that.model.get('message'));
            },
            bindEvents: function() {},
            /*changeDisplayType: function() {
                if (this.ui.selectionTypeLOV.val() == "C") {
                    this.ui.textArea.hide();
                    this.$('.select2IComponents').show();
                } else if (this.ui.selectionTypeLOV.val() == "L") {
                    this.$('.select2IComponents').hide();
                    this.ui.textArea.show();

                }
            },*/
            onSelectionTypeLOVClicked: function() {
                this.changeDisplayType();
            },

            onCloseButton: function() {
                this.model.destroy();
            }

        });
});
