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
    'utils/Globals',
    'hbs!tmpl/common/AdvanceSearchLayout_tmpl'
], function(require, backbone, Utils, Globals, AdvanceSearchLayoutTmpl) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends AdvanceSearchLayout */
        {
            _viewName: 'AdvanceSearchLayout',

            template: AdvanceSearchLayoutTmpl,


            /** ui selector cache */
            ui: {
                searchArea : '[data-id="searchArea"]',
                searchInput: '[data-id="searchInput"]',
                suggesterBox: '[data-id="suggesterBox"]',
                suggesterList: '[data-id="suggesterList"]'
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.startStop] = 'onStartStopClick';
                events["change " + this.ui.searchArea] = 'advancedSearch';
                /*  events["click " + this.ui.start] = 'onStartClick';
                  events["click " + this.ui.stop] = 'onStopClick';*/
                return events;
            },

            /**
             * intialize a new AdvanceSearchLayout Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'vent', 'globalVent', 'params'));
                this.dateUtil = Utils.dateUtil;
            },
            bindEvents: function() {},
            onRender: function() {
                var that = this;

                var li = this.elementGenerator(Globals.serviceLogsColumns)
                that.ui.suggesterList.html(li);
                this.ui.searchInput.on('focusin', function() {
                    that.ui.suggesterBox.show();
                    that.ui.suggesterBox.addClass('advanceSearchActive');
                });

                this.$('.advanceSearchList').on('click', 'li', function(e) {
                    var value = that.ui.searchInput.val();
                    that.ui.searchInput.val(value + $(this).data().value);
                });
                this.ui.searchInput.on('focusout', function() {
                    that.ui.suggesterBox.hide();
                    that.ui.suggesterBox.removeClass('advanceSearchActive')
                });
            },
            elementGenerator: function(serviceLogsColumns) {
                var li = "";
                _.keys(serviceLogsColumns).map(function(object) {
                    li += '<li data-value="' + object + '">' + serviceLogsColumns[object] + '(' + object + ')</li>'
                });
                return li;
            },
            advancedSearch : function(){
               var that = this,textareaValue = '';

               if(that.ui.searchArea.val() != ""){
                textareaValue = that.ui.searchArea.val();
               }
               textareaValue = textareaValue.replace(/\&/g,'}{');
              // that.vent.trigger('main:search',{advanceSearch : textareaValue});
            }
            /* suggester*/
            /*elementGenerator: function(text) {
                this.$('.advanceSearchList').on('click keydown', 'li', function(e) {
                var value = that.ui.searchInput.val();
                if (that.lastSpchar) {
                    var splitArray = value.split(that.lastSpchar[0])
                    splitArray[splitArray.length - 1] = $(this).data().value;
                    that.ui.searchInput.val(splitArray.join(that.lastSpchar));
                } else {
                    that.ui.searchInput.val($(this).data().value);
                }

            })
                var checkLastLatter = text.slice(-1).match(/^[ :~?\}\{\[\]!@#\$%\^\&*\)\(+=._-]+$/g);
                if (checkLastLatter) {
                    this.lastSpchar = checkLastLatter;
                    var splitArray = text.split(this.lastSpchar[0])
                    text = splitArray[splitArray.length - 1];
                } else {
                    if (this.lastSpchar) {
                        var splitArray = text.split(this.lastSpchar[0])
                        text = splitArray[splitArray.length - 1];
                    }
                }
                var li = "";
                if (text != '') {
                    _.each(this.availableTags, function(object) {
                        var regex = new RegExp(text.replace(/(\S+)/g, function(s) {
                            return "\\b(" + s + ")(.*)"
                        }).replace(/\s+/g, ''), "gi");
                        var matches = regex.exec(object);
                        var result = '';
                        if (matches && matches.length) {
                            for (var i = 1; i < matches.length; i++) {
                                if (i % 2 == 1)
                                    result += '<b>' + matches[i] + '</b>';
                                else
                                    result += matches[i];
                            }
                            li += '<li data-value="' + object + '">' + result + '</li>'
                        }
                    });
                    return li
                }

            },*/


        });
});
