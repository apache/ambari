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
                events["click [data-value]"] = 'onLogLevelHeaderClick';
                events["click #filterContent input[type='checkbox']"] = 'onAnyCheckboxClick';
                events["click .overrideRow a"] = 'onEditHost';

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

                this.componentsList.url = Globals.baseURL + "service/logs/components";
                this.hostList.url = Globals.baseURL + "service/logs/hosts";
                this.model = new VUserFilter();

                this.levelCollection = new Backbone.Collection();
                var levelArr = ["FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "UNKNOWN"];

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

                $.when(this.hostList.fetch({ reset: true }), this.componentsList.fetch({ reset: true }), this.model.fetch({})).then(function(c1, c2, m1) {
                    // if (!_.isUndefined(that.model.get('components'))) {
                    //     that.ui.componentSelect2.select2('val', that.model.get('components'));
                    // }
                    //if (!_.isUndefined(that.model.get('hosts'))) {
                    //    that.ui.hostSelect2.select2('val', that.model.get('hosts'));
                    //}
                    // if (!_.isUndefined(that.model.get('levels'))) {
                    //     that.ui.levelSelect2.select2('val', that.model.get('levels'));
                    // }
                    that.hideLoading();
                    that.trigger("toggle:okBtn",true);

                    //that.dataLevels = [];
                    //that.dataLevels = _.pluck(that.levelCollection.models, 'attributes');

                    //that.dataList = [];
                    //that.dataList = _.pluck(that.componentsList.models, 'attributes');
                    that.renderComponents();
                    that.populateValues();
                },function(error){
                	that.hideLoading();
                	Utils.notifyError({
                        content: "There is some issues on server, Please try again later."
                    });
                	that.trigger("closeDialog");
                });
            },
            hideLoading : function(){
            	this.ui.loader.hide();
            },
            renderComponents : function(){
            	var that =this;
            	_.each(that.componentsList.models, function(model){
                    var levels='<td align="left">'+model.get("type")+'</td>';
                    var override = '<td class="text-left"><span class="pull-left"><!--small><i>Override</i></small--> <input data-override type="checkbox" data-name='+model.get("type")+'></span></td>';
                    levels +=  override + that.getLevelForComponent(model.get("type"),false);
                    var html = '<tr class="overrideSpacer"></tr><tr class="componentRow borderShow" data-component="'+model.get("type")+'">'+levels+'</tr><tr></tr>';
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
            								// that.showHostSelect2(key);
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
            				that.showExpiry(key)
            			}
            			//setting hosts
            			if(_.isArray(obj.hosts)){
            				if(obj.hosts.length){
            					that.$("[data-host='"+key+"']").select2("val",obj.hosts);
                                that.showHostSelect2(key);
            				}
            			}
            			//setting default values
            			if(obj.defaultLevels && _.isArray(obj.defaultLevels) && obj.defaultLevels.length){
            				var $default = that.$("tr[data-component='"+key+"']");
        					if($default.length){
        						for(var z=0; z<obj.defaultLevels.length; z++){
        							var $checkbox = $default.find("input[data-id='"+obj.defaultLevels[z]+"']");
        							if(! $checkbox.is(":checked")){
        								$checkbox.prop("checked",true);
        							}
        						}
        					}
            			}
            		});
            	}
            	//set check all value
            	_.each(this.levelCollection.models,function(model){
            		that.setCheckAllValue(model.get("type"));
            	});

            },
            onAnyCheckboxClick : function(e){
            	var $el = $(e.currentTarget);
            	this.setCheckAllValue($el.data("id"));
            },
            onEditHost : function(e){
            	var $el = $(e.currentTarget);
            	$el.hide();
            	if($el.data("type") == "host"){
            		this.showHostSelect2($el.data("component"));
                }else{
            		this.showExpiry($el.data("component"));
                }
            },
            hideHostSelect2 : function(forComponent){
            	this.ui[forComponent].siblings(".select2-container").hide();
            	this.$("a[data-component='"+forComponent+"'][data-type='host']").show();
                this.$('i.hostDown[data-component="'+forComponent+'"]').show();
            },
            showHostSelect2 : function(forComponent){
            	this.ui[forComponent].siblings(".select2-container").show();
            	this.$("a[data-component='"+forComponent+"'][data-type='host']").hide();
                this.$('i.hostDown[data-component="'+forComponent+'"]').hide();
            },
            showExpiry : function(forComponent){
            	this.$("[data-date='"+forComponent+"']").show();
            	this.$("a[data-component='"+forComponent+"'][data-type='expiry']").hide();
            },
            hideExpiry : function(forComponent){
            	this.$("[data-date='"+forComponent+"']").hide();
            	this.$("a[data-component='"+forComponent+"'][data-type='expiry']").show();
            },
            setCheckAllValue : function(type){
            	var that = this;
            	if(! type)
            		return
            	if(that.$("[data-id='"+type+"']:checked").length == that.$("[data-id='"+type+"']").length){
        			that.$("[data-value='"+type+"']").prop("checked",true);
        		}else{
        			that.$("[data-value='"+type+"']").prop("checked",false);
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
                    this.$('tr[data-component="'+$el.data("name")+'"]').removeClass('borderShow ');
                    this.$('tr[data-component="'+$el.data("name")+'"]').addClass('bgHighlight ');
            	}else{
            		this.removeOverrideRow($el.data("name"));
                    this.$('tr[data-component="'+$el.data("name")+'"]').addClass('bgHighlight borderShow ');
                    this.$('tr[data-component="'+$el.data("name")+'"]').removeClass('bgHighlight ');
            	}
            },
            onLogLevelHeaderClick : function(e){
            	var $el = $(e.currentTarget);
            	if(e.currentTarget.checked){
            		this.$("[data-id='"+$el.data("value")+"']").prop("checked",true);
            	}else{
            		this.$("[data-id='"+$el.data("value")+"']").prop("checked",false);
            	}
            },
            addOverrideRow : function(forComponent){
            	var $el = this.ui.filterContent.find("tr[data-component='"+forComponent+"']"),textForHost = "Click here to apply on specific host",
            	textForExpiry="Select Expiry Date";
            	if($el.length){
            		var html = "<tr class='overrideRow bgHighlight "+forComponent+"'><td class='text-left'><i data-component='"+forComponent+"' class='fa fa-level-down hostDown' aria-hidden='true'></i><a href='javascript:void(0);' data-type='host' data-component='"+forComponent+"'>"+textForHost+"</a><input data-host='"+forComponent+"' type='hidden' /></td>" +
            				"<td  class='text-left'><a href='javascript:void(0);' data-type='expiry' data-component='"+forComponent+"'>"+textForExpiry+"</a>" +
            				"<input class='datepickerFilter' data-date='"+forComponent+"'></td>"+this.getLevelForComponent($el.data("component"),false)+"</tr>";
            		//html += "<tr class='overrideRow "+forComponent+"'><td>&nbsp;</td><td>&nbsp;</td><td colspan='3'><input class='datepickerFilter' data-date='"+forComponent+"'></td>" +
            			//	"<td colspan='3'><div ><input data-host='"+forComponent+"' type='hidden' /></div></td></tr>"
            		$el.after(html);
            		this.ui[forComponent] = this.$("[data-host='"+forComponent+"']");
            		this.setupSelect2Fields(this.hostList, "host", 'host', forComponent, 'Select Host', 'hostBoolean');
            		this.hideHostSelect2(forComponent);
            		this.$("[data-date='"+forComponent+"']").daterangepicker({
            	        singleDatePicker: true,
            	        showDropdowns: true,
            	        parentEl : this.$el,
                       'startDate':moment().add(1,'hours').format('MM/DD/YYYY HH:mm'),
            	        //timeZone: 0,
            	        locale: {
            	            format: 'MM/DD/YYYY HH:mm'
            	        },
            	        //timePickerSeconds: true,
            	        "timePicker": true,
                        "timePicker24Hour": true,
            	    });
                    this.$("[data-date='"+forComponent+"']").val(moment().add(1,'hours').format("MM/DD/YYYY HH:mm"));
            		this.hideExpiry(forComponent);
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
