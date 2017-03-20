/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 
define(['require','backbone','hbs!tmpl/common/TimerView_tmpl'],function(require,Backbone,TimerViewTmpl){
    'use strict';
	
	var TimerView = Backbone.Marionette.ItemView.extend(
	/** @lends TimerView */
	{
		_viewName : "TimerView",
		
    	template: TimerViewTmpl,
    	/** ui selector cache */
    	ui: {
    		container : "#container",
    		timerBtn : "[data-id='timerBtn']"
    	},
    	//tagName : "span",
    	className : "timer",
		/** ui events hash */
		events: function() {
			var events = {};
			//events['change ' + this.ui.input]  = 'onInputChange';
			events['click ' + this.ui.timerBtn]  = 'onTimerBtnClick';
			return events;
		},

    	/**
		* intialize a new TimerView ItemView 
		* @constructs
		*/
		initialize: function(options) {
			_.extend(this, _.pick(options, 'vent', 'globalVent'));
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			this.listenTo(this.vent,'start:timer', this.startTimer, this);
		},

		/** on render callback */
		onRender: function() {
			//this.startTimer();
		},
		onTimerBtnClick : function(e){
			var $el = $(e.currentTarget).find("i");
			if($el.hasClass("fa-play")){
				$el.removeClass().addClass("fa fa-stop").attr("title","Stop auto-refresh");
				this.startTimer();
			}else{
				$el.removeClass().addClass("fa fa-play").attr("title","Start auto-refresh");;
				this.clearTimerInterval();
			}
		},
		startTimer : function(){
//			var today = new Date(),that=this;
//		    today.setHours(0, 0, 10, 0);
//		    var startTime = function(){
//		    	var h = today.getHours();
//			    var m = today.getMinutes();
//			    today.setSeconds(today.getSeconds() - 1);
//			    var s = today.getSeconds();
//			    m = that.checkTime(m);
//			    s = that.checkTime(s);
//			    that.ui.container.text(m + ":" + s);
//			    var t = setTimeout(startTime, 1000);
//		    }
//		    startTime();
			var time = 10,that=this;
		    var initialOffset = '56';
		    var i = 1;
		    clearInterval(that.timerInterval);
		    this.$('h6').show();
		    this.timerInterval = setInterval(function() {
		        that.$('.circle_animation').css('stroke-dashoffset', initialOffset-(i*(initialOffset/time)));
		        that.$('h6').text(i);
		        if (i == time) {
		        	clearInterval(that.timerInterval);
		            setTimeout(function(){
		            	if(! that.isTimerManuallyStopped()){
		            		that.timerCallBack();
		            	}
		            },1000);
		        }
		        i++;  
		    }, 1000);
		},
		timerCallBack : function(){
			this.vent.trigger("timer:end");
			//this.startTimer();
		},
		clearTimerInterval : function(){
			this.$('h6').text(0).hide();
			this.$('.circle_animation').css('stroke-dashoffset', "56.2px");
			clearInterval(this.timerInterval);
		},
		isTimerManuallyStopped : function(){
			return this.ui.timerBtn.find('i').hasClass("fa-play");
		},
		/** on close */
		onClose: function(){
		}

	});

	return TimerView;
});
