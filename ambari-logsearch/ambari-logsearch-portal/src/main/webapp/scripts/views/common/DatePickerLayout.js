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
    'hbs!tmpl/common/DatePickerLayout_tmpl',
    'bootstrap-daterangepicker'
], function(require, backbone, Utils, DatePickerLayoutTmpl, daterangepicker) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends DatePickerLayout */
        {
            _viewName: 'DatePickerLayout',

            template: DatePickerLayoutTmpl,


            /** ui selector cache */
            ui: {
                dateRange: "#dateRange",
                selectDateRange: ".selectDateRange",
                dateRangeTitle: "span[data-id='dateRangeTitle']",

            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['change ' + this.ui.viewType] = 'onViewTypeChange';
                return events;
            },

            /**
             * intialize a new DatePickerLayout Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'vent', 'globalVent', 'params', 'viewType', 'datePickerPosition','parentEl', 'fetch', 'rangeLabel', 'width', 'hideFireButton','buttonLabel'));
                this.dateUtil = Utils.dateUtil;
                this.dateRangeLabel = new String();

                this.bindEvents();
                this.graphParams = {};
                this.unit = this.params.unit ? this.params.unit : "+1HOUR";
                this.isEventTriggerdFromVent = false;
            },
            bindEvents: function() {
                this.listenTo(this.vent, "tab:refresh", function(params) {
                    this.reRenderView(params);
                }, this);
                this.listenTo(this.vent, "date:setDate", function(options) {
                    this.setValues(options);
                }, this);
                this.listenTo(this.vent, "date:click", function(options) {
                	this.isEventTriggerdFromVent = true;
                    this.setValues(options);
                    this.ui.dateRange.data('daterangepicker').clickApply();
                }, this);
                this.listenTo(this.vent, "date:getValues", function(obj) {
                	var dates = this.getValues();
                	obj.dates = [dates[0], dates[1]];
                	obj.dateRangeLabel = this.dateRangeLabel;
                	obj.unit = this.unit;
                }, this);

            },
            onRender: function() {
                var that = this;
                if(this.hideFireButton){
                	this.$(".goBtn").hide();
                }
                if(this.buttonLabel){
                	this.$(".goBtn").text(this.buttonLabel);
                }
                if (!this.params.dateRangeLabel) {
                    this.params['dateRangeLabel'] = "Today";
                }
                this.initializeDateRangePicker();
                this.setValues(this.params);
                this.unit = that.checkDateRange(that.ui.dateRange.data("daterangepicker"));
                if (this.fetch) {
                    that.vent.trigger("logtime:filter", _.extend({
                        q: "*:*"
                    }, this.params, {
                        unit: this.unit
                    }));
                }
                if (this.rangeLabel) {
                    this.ui.dateRangeTitle.show();
                }else{
                    this.ui.dateRangeTitle.hide();
                }
                if (this.width) {
                    this.ui.selectDateRange.css('width',this.width);
                }

            },
            setValues: function(val) {
                var startDate, endDate;
                if (val.from) {
                    startDate = this.dateUtil.getMomentObject(val.from)
                    this.ui.dateRange.data('daterangepicker').setStartDate(startDate);
                }
                if (val.to) {
                    endDate = this.dateUtil.getMomentObject(val.to)
                    this.ui.dateRange.data('daterangepicker').setEndDate(endDate);
                }
                if (startDate && endDate)
                    this.setDateText(startDate, endDate);
                if (val.dateRangeLabel) this.ui.dateRangeTitle.html(val.dateRangeLabel);
                this.dateRangeLabel = val.dateRangeLabel;
            },
            getValues : function(){
            	var obj = this.ui.dateRange.data("daterangepicker");
            	if(obj){
            		return [obj.startDate, obj.endDate];
            	}
            },
            initializeDateRangePicker: function() {
                var that = this,
                    ranges = {};
                //Apply moments for all ranges separately if you pass single instance then it will run into problem.
                _.each(Utils.relativeDates, function(k) {
                    ranges[k.text] = [];
                })
                this.ui.dateRange.daterangepicker(_.extend({
                    'ranges': ranges
                }, {
                    "timePicker": true,
                    "timePickerIncrement": 1,
                    "timePicker24Hour": true,
                    "opens": (that.datePickerPosition) ? (that.datePickerPosition) : (undefined),
                    timePickerSeconds: true,
                    showWeekNumbers: true,
                    timeZone: 0,
                    locale: {
                        format: 'MM/DD/YYYY H:mm:ss,SSS'
                    },
                    parentEl: (that.parentEl) ? (that.parentEl) : (that.$el),
                }));
                this.bindDateRangePicker();
            },
            bindDateRangePicker: function() {
                var that = this;
                if (this.parentEl) {
                    var elem = this.parentEl.find('.daterangepicker');
                } else {
                    var elem = this.$('.daterangepicker');
                }


                this.ui.dateRange.on('apply.daterangepicker ', function(ev, picker) {
                	if(! that.isEventTriggerdFromVent && !(_.isUndefined(picker.chosenLabel)) ){
                		that.dateRangeLabel = picker.chosenLabel;
                	}else{
                		that.isEventTriggerdFromVent = false;
                	}
                	if (that.dateRangeLabel !== "Custom Range") {
                		var last1Hour = that.dateUtil.getRelativeDateFromString(that.dateRangeLabel);
                		that.setDateText(last1Hour[0], last1Hour[1]);  
                	}
                    that.ui.dateRangeTitle.html(that.dateRangeLabel);
                    that.unit = that.checkDateRange(picker);
                    var options = {
                        'from': (picker.startDate).toJSON(),
                        'to': (picker.endDate).toJSON(),
                        'unit': that.unit,
                        'dateRangeLabel': that.dateRangeLabel
                    }
                    that.vent.trigger("logtime:filter", options);
                    that.pickerOpend = false
                });
                this.ui.dateRange.on('show.daterangepicker', function(ev, picker) {
                    elem.find('li').removeClass('active');
                    elem.find('li:contains(' + that.dateRangeLabel + ')').addClass('active');
                       picker.chosenLabel = that.dateRangeLabel; 
                });
                this.ui.dateRange.on('hide.daterangepicker', function(ev, picker) {
                    that.pickerOpend = true
                });

                this.ui.selectDateRange.on("click", 'button.goBtn', function() {
                    if (that.pickerOpend) {
                        var textRange = elem.find('li.active').text();
                        if (textRange == "Custom Range") that.dateRangeLabel = elem.find('li.active').text();
                    }
                    if (that.dateRangeLabel == "Last 1 Hour") {
                        var last1Hour = that.dateUtil.getRelativeDateFromString(that.dateRangeLabel);
                        that.setDateText(last1Hour[0], last1Hour[1]);
                    }
                    that.ui.dateRange.data('daterangepicker').clickApply();

                });
            },
            checkDateRange: function(picker) {
                return this.dateUtil.calculateUnit(picker)
            },
            setDateText: function(start, end) {

                this.ui.dateRange.val(this.dateUtil.getTimeZone(start, "MM/DD/YYYY H:mm:ss,SSS") + ' - ' + this.dateUtil.getTimeZone(end, "MM/DD/YYYY H:mm:ss,SSS"));
                this.ui.dateRange.data('daterangepicker').setStartDate(start);
                this.ui.dateRange.data('daterangepicker').setEndDate(end);

            },
            reRenderView: function(params) {
                this.setValues(_.extend(this.params, params));
            }
        });


});
