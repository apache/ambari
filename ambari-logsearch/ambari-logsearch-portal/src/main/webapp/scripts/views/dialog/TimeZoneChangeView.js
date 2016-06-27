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
    'hbs!tmpl/dialog/TimeZoneChangeView_tmpl'
],function(require,Backbone,Utils,TimeZoneChangeViewTmpl) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends TimeZoneChangeView */
        {
            _viewName: 'TimeZoneChangeView',

            template: TimeZoneChangeViewTmpl,


            /** ui selector cache */
            ui: {
                map: "#timezone-picker"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                return events;
            },

            /**
             * intialize a new TimeZoneChangeView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'currentTime'));
                this.dateUtil = Utils.dateUtil;
                this.changedTimeZone = false;
                var storeTimezone = '';
                if(!_.isUndefined(this.currentTime)){
                    storeTimezone = this.currentTime;
                }
                if (storeTimezone && storeTimezone != "undefined") {
                    this.selectedtimeZone = storeTimezone.value
                }
            },
            bindEvents: function() {
                var that = this;
                this.ui.map.on('map:clicked', function(e) {
                    var valueArray = $(this).data('WorldMapGenerator').getValue();
                    if (valueArray.length) {
                        if (that.selectedtimeZone != valueArray[0].zonename) {
                            that.selectedtimeZone = valueArray[0].timezone + "," + valueArray[0].zonename + "," + valueArray.length
                            that.changedTimeZone = true;
                        }
                    }

                    that.enabledButton()
                })
                this.ui.map.on('map:loaded', function(e) {
                    var selectedtimeZone = that.selectedtimeZone.split(',');
                    if (selectedtimeZone.length <= 1) {
                        $(this).data('WorldMapGenerator').setValue(that.selectedtimeZone.split(',')[0], 'timezone');
                    } else {
                        if (selectedtimeZone[2] && parseInt(selectedtimeZone[2]) <= 1 ) {
                            $(this).data('WorldMapGenerator').setValue(that.selectedtimeZone.split(',')[0], 'timezone');
                        } else {
                            $(this).data('WorldMapGenerator').setValue(that.selectedtimeZone.split(',')[1], 'zonename');
                        }
                    }

                    that.trigger('toggle:btn', false, 'reloadBtn');
                    that.trigger('toggle:btn', false, 'reloadNewBtn');
                })
            },
            onRender: function() {
                this.bindEvents();
            },
            enabledButton: function() {
                this.trigger('toggle:btn', true, 'reloadBtn');
                this.trigger('toggle:btn', true, 'reloadNewBtn');
            }

        });


});
