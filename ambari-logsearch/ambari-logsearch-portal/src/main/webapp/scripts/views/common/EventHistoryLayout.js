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
    'utils/Utils',
    'collections/VEventHistoryList',
    'models/VCommonModel',
    'hbs!tmpl/common/EventHistoryLayout_tmpl',
    'hbs!tmpl/common/EventHistoryItemView_tmpl',
    'moment'
],function(require,Backbone,Globals,Utils,VEventHistoryList,VCommonModel,EventHistoryLayout_tmpl, EventHistoryItemView_tmpl,moment){
    'use strict';
	
    var EventView = Backbone.Marionette.ItemView.extend({
    	tagName:"li",
    	template : EventHistoryItemView_tmpl,
    	ui : {
    		content : "[data-id='content']",
    		flagWrap : ".flagWrap"
    	},
    	/** ui events hash */
		events: function() {
			var events = {};
			events["click "+this.ui.flagWrap] = 'onFlagClick';
			events["click .infoBtn"] = 'onInfoClick';
			events["click .removeFlag"] = 'onRemoveFlagClick';
			events["click .saveBtn"] = 'onSaveClick';
			return events;
		},
    	initialize: function(options) {
    		_.extend(this, _.pick(options,'eventVent','vent'));
    		this.bindEvents();
    	},
    	bindEvents : function(){
    		this.listenTo(this.model, "event:highlight", function($el) {
				this.highlightli($el);
			}, this);
    		this.listenTo(this.model, "event:fireReinitialize", function() {
				this.fireReinitialize();
			}, this);
    	},
    	onRender : function(){
    		this.ui.content.text(this.getLabel());
    	},
    	getLabel : function(){
    		var text = "",param = this.model.get("newParam"),key = _.keys(this.model.get("newParam"))[0];
    		if(param){
    			var preText = "Value:";
    			if(param.dateRangeLabel){
    				return param.dateRangeLabel;
    			}
    			if(key === "includeQuery"){
    				preText = "IColumn :";
    			}
    			if(key === "excludeQuery"){
    				preText = "EColumn :";
    			}
    			if(key === "level"){
    				preText = "Level :";
    			}
    			if(key === "mustBe"){
    				preText = "IC :";
    			}
    			if(key === "mustNot"){
    				preText = "EC :";
    			}
    			if(key === "mustNot"){
    				preText = "EC :";
    			}
    			if(key === "from" || key === "to"){
    				preText = param.dateRangeLabel;
    			}
    			if(key === "filtername"){
    				preText = "Filter :";
    			}
    			return preText + " "+this.model.get("newParam")[_.keys(this.model.get("newParam"))[0]];
    		}
    		return text;
    	},
    	onFlagClick : function(e){
    		var that = this;
    		this.fireReinitialize();
    		setTimeout(function(){
    			that.highlightli(that.$el.children("[data-id]"))
    		},10);
    	},
    	highlightli : function($el){
    		//$el is the flag container
    		$el.parents("li").siblings("li").find(".showFlag").removeClass("flagActive");
    		$el.addClass("flagActive");
    		this.eventVent.trigger("event:position:center",$el.parents("li"));
    	},
    	onInfoClick : function(e){
    		var html = "<table class='table eventTable table-hover'><thead><th>Name</th><th>Value</th></thead>",that=this;
            var customParam = {"mustNot":[],"mustBe":[],"includeQuery":[],"excludeQuery":[]};
            var paramNames = _.extend({},this.model.get("params"),customParam);
            _.each(paramNames, function(value, key) {
            	if ( (! _.isEmpty(value) || _.isArray(value)) && ( key != "bundleId" && key != "start_time" && 
                		key != "end_time" && key != "q" && key != "unit" && key != "query" && key != "type" && 
                		key != "time" && key != "dateRangeLabl" && key != "advanceSearch" && !_.isUndefined(Globals.paramsNameMapping[key]) )){
            		html += '<tr class="' + key + '"><td>' + Globals.paramsNameMapping[key].label + '</td><td>' + that.getHtmlForParam(key) + '</td><tr>'
    			}
    		});
    		html += "</table>";
    		Utils.alertPopup({msg : html,className:"bootBoxSmall"});
    	},
    	getHtmlForParam : function(key){
            var paramValue = this.model.get("params"),value=paramValue[key];

    		if(key === "from" || key === "to"){
    			value = moment(paramValue[key]).format('MM/DD/YYYY,HH:mm:ss,SSS');
    		}else{
                if(_.isUndefined(paramValue[key])){
                    value = "[]";
                }
            }
    		return value;
    	},
    	onRemoveFlagClick : function(e){
    		e.stopImmediatePropagation();e.stopPropagation();
        	var siblings = this.$el.siblings(),that=this;
        	if(siblings.length > 0){
        		var focusLi = $(siblings[siblings.length -1]).children("[data-id]");
        		this.collection.remove(this.model);
        		this.close();
        		focusLi.find(".flagWrap").click();
        	}
        	
        },
        fireReinitialize: function() {
            this.vent.trigger("reinitialize:filter:tree " +
            		"reinitialize:filter:include:exclude " +
            		"reinitialize:filter:bubbleTable " +
            		"reinitialize:filter:mustNot " +
            		"reinitialize:filter:mustBe " +
            		"reinitialize:filter:level " +
            		"reinitialize:filter:logtime", _.extend({
                mustNot: null,
                mustBe: null,
                query: null,
                includeQuery: null,
                excludeQuery: null
            }, this.model.get('params')));
        },
        onSaveClick : function(e){
        	var that = this;
            require(['views/dialog/SaveSearchFilterView'], function(SaveSearchFilterView) {
            	var view = new SaveSearchFilterView({
                    selectedCollectionObject: that.model
                });
            	that.setupDialog({
                    title: "Save Search Filter",
                    content: view,
                    viewType: 'Save',
                    width: 850,
                    height: 500,
                    buttons: [{
                        id: "okBtn",
                        text: "Save",
                        "class": "btn btn-primary",
                        click: function() {
                            that.onDialogSubmitted();
                        }
                    }, {
                        id: "cancelBtn",
                        text: "Close",
                        "class": "btn btn-default",
                        click: function() {
                            that.onDialogClosed();
                        }
                    }]
                });
            });
        },
        setupDialog: function(options) {
            var that = this;
            require(['views/common/JBDialog'], function(JBDialog) {
                var opts = _.extend({
                    appendTo: 'body',
                    modal: true,
                    resizable: false
                }, options);
                var dialog = that.dialog = new JBDialog(opts).render().open();
            })
        },
        onDialogSubmitted: function() {
            var content = this.dialog.options.content;
            if (content.$('form')[0].checkValidity && !content.$('form')[0].checkValidity()) {
                content.$('form').addClass('has-error');
                if (content.$('form')[0].reportValidity) {
                    if (!content.$('form')[0].reportValidity()) {
                        return;
                    }
                }
                return;
            } else {
                if(_.isEmpty(content.ui.filterName.val().trim())){
                    if(content.$('form')[0].reportValidity){
                        content.ui.filterName.val('')
                        content.$('form')[0].reportValidity();
                        return;
                    }
                    return;
                }else{
                    content.$('form').removeClass('has-error');  
                }
            }
            var timeType = content.$("input[name='radio']:checked").parents("[data-id]").data('id'),
            params = content.selectedCollectionObject.get("params");
            if(timeType === "absolute"){
                params["dateRangeLabel"] = "Custom Range";
            }
            params["time"] = timeType;
            var postObject = {
                filtername: content.ui.filterName.val().trim(),
                rowType: "history",
                values: JSON.stringify(params)
            }
            content.trigger("toggle:okBtn");
            this.saveEventHistory(postObject);
        },
        saveEventHistory: function(postObject) {
            var that = this
            this.collection.saveEventHistory(postObject, {
                success: function(data, textStatus, jqXHR) {
                    Utils.notifySuccess({
                        content: "Event History saved successfully."
                    });
                    that.onDialogClosed();
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    Utils.notifyError({
                        content: JSON.parse(jqXHR.responseText).msgDesc || "There is some problem in Event History, Please try again later."
                    });
                    that.dialog.options.content.trigger("toggle:okBtn",true);
                },
                complete: function() {
                }
            });
        },
        onDialogClosed: function() {
            if (this.dialog) {
                this.dialog.close && this.dialog.close();
                this.dialog.remove && this.dialog.remove();
                this.dialog = null;
            }
        }
    });
    
    
    
	return Backbone.Marionette.CompositeView.extend(
	/** @lends EventHistoryLayout */
	{
		_viewName : 'EventHistoryLayout_tmpl',

		template: EventHistoryLayout_tmpl,
		
		itemViewContainer : "#events",
		
		itemView : EventView,
		
		itemViewOptions : function(){
			return {
				collection : this.collection,
				eventVent : this.eventVent,
				vent : this.vent
			}
		},

		/** Layout sub regions */
		regions: {
		},

		/** ui selector cache */
		ui: {
			loaderEvent : "#loaderEvent",
			eventsCont : "#eventsCont",
			events : "#events"
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events["change "+this.ui.mainCheck] = 'onMainCheckboxClick';
			events["click .slideArrow a"] = 'slideArrowClick';
			events['click .apply-link'] = 'onEventHistoryLoadClick';
			events["click .collapse-link"] = 'onCollapseBoxClick';
			return events;
		},

		/**
		 * intialize a new EventHistoryLayout Layout
		 * @constructs
		 */
		initialize: function(options) {
			_.extend(this, _.pick(options,'vent','globalVent','params'));
			this.eventVent = new Backbone.Wreqr.EventAggregator();
			this.searchParmas = (this.params) ? this.params : {};
			this.collection = new VEventHistoryList();
			this.bindEvents();
		},
		bindEvents: function() {
			this.listenTo(this.vent, "level:filter type:mustNot type:mustBe search:include:exclude " +
                    "logtime:filter " + Globals.eventName.serviceLogsIncludeColumns + " " + Globals.eventName.serviceLogsExcludeColumns,
                    function(value) {
				if(this.collection.last()){
					var params = _.extend({},this.searchParmas, value);//,id=this.getNewIndex();
					this.addFlag(params,value);
					var leftOffset = this.ui.events.offset();
					(leftOffset.left < 0) ? this.$('.slideArrow').show() : this.$('.slideArrow').hide();
				}
				
			}, this);
			
			this.listenTo(this.collection, "add remove reset", function() {
                this.$("[data-id='count']").text(this.collection.length);
                this.$("[data-id='totalCount']").text(this.collection.totalCount);
                this.limitTheFlags();
            }, this);
			
			this.listenTo(this.eventVent, "event:position:center", function($li) {
				this.scrollToElement($li);
			}, this);
		},
		onRender : function(){
			if(this.params){
				this.collection.add(this.collection.model({id:this.getNewIndex(),
					params:_.extend({},this.params),
					newParam:{level:this.params.level
				}}));
				this.$(".removeFlag").remove();
			}
		},
		onShow : function(){
			this.flagDraggable();
		},
		addFlag : function(params, showParam){
			var id=this.getNewIndex();
			var model = new this.collection.model({
				id : id,
				params : params,
				newParam : showParam
			});
			this.collection.add(model);
			model.trigger("event:highlight",this.ui.events.find("[data-id='"+id+"']"));
			return model;
		},
		limitTheFlags : function(){
			if(this.collection.length == 26){
				this.collection.remove(this.collection.at(1));
			}
		},
		flagDraggable: function() {
			var that = this;
            this.ui.events.draggable({
                axis: "x",
                stop : function(){
                	that.toggleSlideArrow();
                }
            });
            
        },
        toggleSlideArrow : function(){
        	var that = this;
        	that.leftDistance = that.ui.events.offset();
            if(that.timeLineWidth <= that.leftDistance.left){
                //this.$('.slideArrow').hide();
            }else{
                that.$('.slideArrow').show();
            }
        },
		getNewIndex : function(){
			if(this.lastIndex){
				this.lastIndex++;
			}else{
				this.lastIndex = 1;
			}
			return this.lastIndex;
		},
		scrollToElement : function($li){
			var flagIndex = $li.index();
            var options = { duration: 200 };
            this.timeLineWidth = this.ui.eventsCont.width() / 2;
                
            this.ui.events.animate({ 'left': this.timeLineWidth - (flagIndex * $li.outerWidth()) + 'px' }, options);

		},
		slideArrowClick : function(e){
            e.preventDefault();
            var leftDistance = this.ui.events.offset();
            if($(e.currentTarget).hasClass('arrowLeft')){              
                this.ui.events.animate({ 'left': leftDistance.left - 140 + 'px' }, 200) ;
            }else{
                this.ui.events.animate({ 'left': leftDistance.left + 140 + 'px' }, 200);
            }                          
        },
        onEventHistoryLoadClick: function() {
            var that = this;
            require(['views/dialog/ApplySearchFilterView'], function(ApplySearchFilterView) {
                var view = new ApplySearchFilterView({
                    collection: new VEventHistoryList([], {
                        state: {
                            firstPage: 0,
                            pageSize: 10 // have to pass max pageSize value or
                                // else it will take default pageSize
                        }
                    })
                })
                that.setupDialog({
                    title: "Apply Filter",
                    content: view,
                    viewType: 'Save',
                    width: 850,
                    height: 500,
                    buttons: [{
                        id: "cancelBtn",
                        text: "Close",
                        "class": "btn btn-default",
                        click: function() {
                            that.onDialogClosed();
                        }
                    }]
                });
                view.on("apply:filter",function(model){
                	var params = JSON.parse(model.get("values"));
                    if (params.time === "relative") {
                        var rangeNew = Utils.dateUtil.getRelativeDateFromString(params.dateRangeLabel);
                        if (_.isArray(rangeNew)) {
                            params.from = rangeNew[0].toJSON();
                            params.to = rangeNew[1].toJSON();
                        }
                    }
                    var newModel = that.addFlag(params,{filtername:model.get("filtername")});
                    newModel.trigger("event:fireReinitialize");
                    that.onDialogClosed();
                })
            });
        },
        setupDialog: function(options) {
            var that = this;
            require(['views/common/JBDialog'], function(JBDialog) {
                var opts = _.extend({
                    appendTo: 'body',
                    modal: true,
                    resizable: false
                }, options);
                var dialog = that.dialog = new JBDialog(opts).render().open();
            })
        },
        onDialogClosed: function() {
            if (this.dialog) {
                this.dialog.close && this.dialog.close();
                this.dialog.remove && this.dialog.remove();
                this.dialog = null;
            }
        },
        onCollapseBoxClick : function(e){
        	if($(e.currentTarget).find("i").hasClass("fa-chevron-down")){
        		var $el = this.ui.events.find(".flagActive");
        		if($el.length){
        			var model = this.collection.get($el.data("id"));
        			setTimeout(function(){
        				model.trigger("event:highlight",$el);
        			},1000);
        		}
        	}
        }
	});
});