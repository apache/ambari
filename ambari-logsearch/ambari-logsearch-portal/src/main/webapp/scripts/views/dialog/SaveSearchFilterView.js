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
    'hbs!tmpl/dialog/SaveSearchFilterView_tmpl'
], function(require, Backbone, Utils, Globals, SaveSearchFilterViewTmpl) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends SaveSearchFilterView */
        {
            _viewName: 'SaveSearchFilterView',

            template: SaveSearchFilterViewTmpl,


            /** ui selector cache */
            ui: {
                radioAbsolute: "[data-id = 'absolute']",
                radioRelative: "[data-id='relative']",
                paramsPanelBody: "[data-id='panelBody']",
                panelHeading: "[data-id='panelHeading']",
                filterName: "[data-id='name']"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['change ' + this.ui.viewType] = 'onViewTypeChange';
                return events;
            },

            /**
             * intialize a new SaveSearchFilterView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'selectedCollectionObject'));
                this.dateUtil = Utils.dateUtil;


            },
            bindEvents: function() {
                this.listenTo(this, "dialog:rendered", function(value) {
                    this.popoverForTd();
                }, this);
            },
            onRender: function() {
                this.params = this.selectedCollectionObject.get('params');
                //this.ui.panelHeading.html("Filter Parameter From : <strong>"+ this.dateUtil.getTimeZone(this.params.from) +"</strong> To <strong>" +this.dateUtil.getTimeZone(this.params.from)+"</strong>" )
                this.ui.radioAbsolute.find('label').html(' <input type="radio" name="radio" checked>' + this.dateUtil.getTimeZone(this.params.from) + '&emsp;&emsp;&emsp;TO&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;' + this.dateUtil.getTimeZone(this.params.to) + '<i class="fa fa-circle-o small"></i>');
                if (this.params.dateRangeLabel == "Custom Range") {
                    this.ui.radioRelative.find('label').html(' <input type="radio" name="radio" disabled="disabled">' + this.params.dateRangeLabel + '<i class="fa fa-circle-o small"></i>');
                    this.ui.radioRelative.css('color', '#8C8C8C');
                } else {
                    this.ui.radioRelative.find('label').html(' <input type="radio" name="radio">' + this.params.dateRangeLabel + '<i class="fa fa-circle-o small"></i>');
                }
                this.showParams();
            },
            showParams: function() {
                var tableSting = "",
                    that = this;
                var customParam = {"mustNot":[],"mustBe":[],"includeQuery":[],"excludeQuery":[]};
                var paramNames = _.extend({},this.params,customParam);
                _.each(paramNames, function(value, key) {
                    if ((key != "from" && (! _.isEmpty(value) || _.isArray(value)) && key != "to" && key != "bundleId" && key != "start_time" && 
                    		key != "end_time" && key != "q" && key != "unit" && key != "query" && key != "type" && 
                    		key != "time" && key != "dateRangeLabel" && key != "advanceSearch" && !_.isUndefined(Globals.paramsNameMapping[key]) )) {
                        tableSting += '<tr class="' + key + '"><td>' + Globals.paramsNameMapping[key].label + '</td><td>' + (that.createInnerSpan(key)) + '</td><tr>'
                    }
                });
                this.ui.paramsPanelBody.html(tableSting);
            },
            createInnerSpan: function(type) {
                var typeString = "",
                    that = this;
                if (this.params[type]) {
                    Utils.encodeIncludeExcludeStr(this.params[type], false, ((type == "iMessage" || type == "eMessage") ? ("|i::e|") : (","))).map(function(typeName) {
                        typeString += '<span class="' + ((type != "level") ? (type) : (typeName)) + '">' +
                            ((type == "from" || type == "to") ? (that.dateUtil.getTimeZone(that.params[type])) : (Utils.escapeHtmlChar(typeName))) + '</span>' +
                            ((type == "level") ? (",") : (""));
                    });
                }
                return ((typeString.length == 0) ? ("[ ]") : ((type == "level") ? ((typeString).slice(0, -1)) : (typeString)))
            },
            popoverForTd: function() {
                this.ui.paramsPanelBody.find('td:nth-child(2) span').map(function() {
                    if (this.offsetWidth < this.scrollWidth) {
                        $(this).popover({
                            html: true,
                            content: function() {
                                return this.textContent
                            },
                            placement: 'left',
                            container: 'body',
                            trigger: 'hover'
                        });
                    }
                });
            }
        });
});
