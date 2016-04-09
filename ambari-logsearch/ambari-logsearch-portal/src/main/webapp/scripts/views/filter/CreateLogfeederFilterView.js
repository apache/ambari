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
    'hbs!tmpl/filter/CreateLogfeederFilter_tmpl',
    'models/VUserFilter',
    'utils/Globals',
    'collections/VGroupList',
    'moment',
    'select2',
], function(require, Backbone, Utils, CreateLogfeederFilter_tmpl, VUserFilter, Globals, VGroupList, moment) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends TimeZoneChangeView */
        {
            _viewName: 'FilterView',

            template: CreateLogfeederFilter_tmpl,

            /** ui selector cache */
            ui: {
                'componentSelect2': "#components",
                'hostSelect2': "#hosts",
                // 'levelSelect2': "#levels",
                'filterInput': '#filter[input]',
                'loader'     : "[data-id='loader']",
                'filterContent' : '#filterContent',
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events["click [data-override]"] = 'onDataOverrideClick';
                return events;
            },

            /**
             * intialize a new Filter Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, ''));
                this.componentsList = new VGroupList([], {
                    state: {
                        firstPage: 0,
                        pageSize: 99999
                    }
                });

                this.hostList = new VGroupList([], {
                    state: {
                        firstPage: 0,
                        pageSize: 99999
                    }
                });

                this.componentsList.url = Globals.baseURL + "dashboard/components";
                this.hostList.url = Globals.baseURL + "dashboard/hosts";
                this.model = new VUserFilter();

                this.levelCollection = new Backbone.Collection();
                var levelArr = ["FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"];

                for (var i in levelArr) {
                    this.levelCollection.add(new Backbone.Model({ type: levelArr[i] }));
                }
                this.bindEvents();
            },
            bindEvents: function() {
                // this.listenTo(this.componentsList, "reset", function(col, abc) {
                //      this.setupSelect2Fields(col, "type", 'type', 'componentSelect2', 'Select Component');
                // }, this);

                this.listenTo(this.hostList, "reset", function(col, abc) {
                    //this.setupSelect2Fields(col, "host", 'host', 'hostSelect2', 'Select Host', 'hostBoolean');
                }, this);
            },
            onRender: function() {
                var that = this;
                // this.setupSelect2Fields(this.levelCollection, "type", "type", "levelSelect2", 'Select Level');
                
                $.when(this.hostList.fetch({ reset: true }), this.componentsList.fetch({ reset: true }), this.model.fetch({})).done(function(c1, c2, m1) {
                    
                    // if (!_.isUndefined(that.model.get('components'))) {
                    //     that.ui.componentSelect2.select2('val', that.model.get('components'));
                    // }
                    //if (!_.isUndefined(that.model.get('hosts'))) {
                    //    that.ui.hostSelect2.select2('val', that.model.get('hosts'));
                    //}
                    // if (!_.isUndefined(that.model.get('levels'))) {
                    //     that.ui.levelSelect2.select2('val', that.model.get('levels'));
                    // }
                    that.ui.loader.hide();
                    that.trigger("toggle:okBtn",true);

                    //that.dataLevels = [];
                    //that.dataLevels = _.pluck(that.levelCollection.models, 'attributes');
                    
                    //that.dataList = [];
                    //that.dataList = _.pluck(that.componentsList.models, 'attributes');
                    that.renderComponents();
                    that.populateValues();
                });
            },
            renderComponents : function(){
            	var that =this;
            	_.each(that.componentsList.models, function(model){
                    var levels='<td align="left">'+model.get("type")+'<span class="pull-right"><small><i>Override</i></small> <input data-override type="checkbox" data-name='+model.get("type")+'></span></td>';
                    levels += that.getLevelForComponent(model.get("type"),true);
                    var html = '<tr data-component="'+model.get("type")+'">'+levels+'</tr>';
                    that.ui.filterContent.append(html);
                });
            },
            populateValues : function(){
            	var that =this;
            	if(this.model.get("filter")){
            		var components = this.model.get("filter");
            		_.each(components,function(value,key){
            			var obj = components[key];
            			if((_.isArray(obj.overrideLevels) && obj.overrideLevels.length) || 
            					(_.isArray(obj.hosts) && obj.hosts.length) || obj.expiryTime){
            				var $el = that.$("input[data-name='"+key+"']").filter("[data-override]");
        					$el.click();
            			}
            			
            			//setting override data
            			if(_.isArray(obj.overrideLevels)){
            				if(obj.overrideLevels.length){
            					var $override = that.$("tr.overrideRow."+key);
            					if($override.length){
            						for(var z=0; z<obj.overrideLevels.length; z++){
            							var $checkbox = $override.find("input[data-id='"+obj.overrideLevels[z]+"']");
            							if(! $checkbox.is(":checked")){
            								$checkbox.prop("checked",true);
            							}
            						}
            					}
            				}
            			}
            			//setting expiry
            			if(obj.expiryTime && that.$("[data-date='"+key+"']").data('daterangepicker')){
            				var dateObj = Utils.dateUtil.getMomentObject(obj.expiryTime);
            				that.$("[data-date='"+key+"']").data('daterangepicker').setStartDate(dateObj);
            				that.$("[data-date='"+key+"']").val(dateObj.format("MM/DD/YYYY HH:mm"));
            			}
            			//setting hosts
            			if(_.isArray(obj.hosts)){
            				if(obj.hosts.length){
            					that.$("[data-host='"+key+"']").select2("val",obj.hosts);
            				}
            			}
            		});
            	}
            },
            getLevelForComponent : function(type,checked){
            	var html="";
            	for(var z=0;z<this.levelCollection.length;z++){
            		html += '<td><input '+((checked) ? "checked":"") +' type="checkbox" data-id='+this.levelCollection.models[z].get("type")+' data-name='+type+'></td>';
                }
            	return html;
            },
            onDataOverrideClick : function(e){
            	var $el = $(e.currentTarget);
            	if(e.currentTarget.checked){
            		this.addOverrideRow($el.data("name"));
            	}else{
            		this.removeOverrideRow($el.data("name"));
            	}
            },
            addOverrideRow : function(forComponent){
            	var $el = this.ui.filterContent.find("tr[data-component='"+forComponent+"']");
            	if($el.length){
            		var html = "<tr class='overrideRow "+forComponent+"'><td>&nbsp;</td>"+this.getLevelForComponent($el.data("component"),false)+"</tr>";
            		html += "<tr class='overrideRow "+forComponent+"'><td>&nbsp;</td><td colspan='3'><input class='datepickerFilter' data-date='"+forComponent+"'></td>" +
            				"<td colspan='3'><div ><input data-host='"+forComponent+"' type='hidden' /></div></td></tr>"
            		$el.after(html);
            		this.ui[forComponent] = this.$("[data-host='"+forComponent+"']");
            		this.setupSelect2Fields(this.hostList, "host", 'host', forComponent, 'Select Host', 'hostBoolean');
            		this.$("[data-date='"+forComponent+"']").daterangepicker({
            	        singleDatePicker: true,
            	        showDropdowns: true,
            	        parentEl : this.$el,
            	        minDate :moment().format('MM/DD/YYYY'),
            	        //timeZone: 0,
            	        locale: {
            	            format: 'MM/DD/YYYY HH:mm'
            	        },
            	        //timePickerSeconds: true,
            	        "timePicker": true,
//                        "timePickerIncrement": 1,
                        "timePicker24Hour": true,
            	    });
            	}
            },
            removeOverrideRow : function(foComponent){
            	this.ui.filterContent.find("tr.overrideRow."+foComponent).remove();
            },
            setupSelect2Fields: function(col, idKey, textKey, selectTagId, placeHolder) {
                var that = this,
                    data = [];
                data = _.pluck(col.models, 'attributes');

                for (var i = 0; i < data.length; i++) {
                    data[i].id = data[i][idKey];
                }

                this.ui[selectTagId].select2({
                    dropdownParent: that.$el,
                    placeholder: (placeHolder) ? placeHolder : 'Select',
                    tags: true,
                    allowClear: true,
                    width: '100%',
                    data: { results: data, text: textKey },
                    formatSelection: function(item) {
                        return item[textKey];
                    },
                    formatResult: function(item) {
                        return item[textKey];
                    }

                });

            },
            setValues : function(){
            	var obj = {filter: {}},that= this;
            	_.each(that.componentsList.models, function(model){
            		var comp = model.get("type"),date = that.$("[data-date='"+comp+"']").data("daterangepicker");
            		var host = (that.$("[data-host='"+comp+"']").length) ? that.$("[data-host='"+comp+"']").select2('val') : [];
            		obj.filter[comp] = {
            				label : comp,
            				hosts: host,
            				defaultLevels : that.getDefaultValues(comp),
            				overrideLevels : that.getOverideValues(comp),
            				expiryTime : (date && date.startDate) ? date.startDate.toJSON() : ""
            		};
            	});
            	console.log(obj);
            	return (obj);
            },
            getOverideValues : function(ofComponent){
            	var $els = this.$("tr.overrideRow."+ofComponent).find("input:checked"),values=[];
            	for(var i=0; i<$els.length; i++){
            		values.push($($els[i]).data("id"));
            	}
            	return values;
            },
            getDefaultValues : function(ofComponent){
            	var $els = this.$("tr[data-component='"+ofComponent+"']").find("input:checked"),values=[];
            	for(var i=0; i<$els.length; i++){
            		if($($els[i]).data("id"))
            			values.push($($els[i]).data("id"));
            	}
            	return values;
            }
        });

});