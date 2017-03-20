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
 	'App',
 	'backgrid',
 ],function(require,App){
	
	/**********************************************************************
	 *                      Backgrid related                              *
	 **********************************************************************/
	/*
	 * HtmlCell renders any html code
	 * @class Backgrid.HtmlCell
	 * @extends Backgrid.Cell
	*/
	var HtmlCell = Backgrid.HtmlCell = Backgrid.Cell.extend({
    /** @lends Backgrid.HtmlCell */
		 className: "html-cell",

		 render: function () {
		     this.$el.empty();
		     var rawValue = this.model.get(this.column.get("name"));
		     var formattedValue = this.formatter.fromRaw(rawValue, this.model);
		     this.$el.append(formattedValue);
		     this.delegateEvents();
		     return this;
		 }
	});
	/*
	 * Overriding Cell for adding custom className to Cell i.e <td>
	 */
	var cellInit = Backgrid.Cell.prototype.initialize;
	Backgrid.Cell.prototype.initialize = function () {
	  cellInit.apply(this, arguments);
	  var className = this.column.get('className');
	  if (className) this.$el.addClass(className);
	}
	
	Backgrid.HeaderRow = Backgrid.HeaderRow.extend({
	    render: function() {
	      var that = this;
	      Backgrid.HeaderRow.__super__.render.apply(this, arguments);
	      _.each(this.columns.models, function(modelValue) {
	        if (modelValue.get('width')) that.$el.find('.' + modelValue.get('name')).css('width', modelValue.get('width') + '%')
	        if (modelValue.get('toolTip')) that.$el.find('.' + modelValue.get('name')).attr('title', modelValue.get('toolTip'))
	      });
	      return this;
	    }
	  });
	
	
	$(function(){
		$("#content").on('click', '.expand-link', function (e) {
			var body = $('body');
			e.preventDefault();
			var box = $(this).closest('div.box');
			var button = $(this).find('i');
			button.toggleClass('fa-expand').toggleClass('fa-compress');
			box.toggleClass('expanded');
			body.toggleClass('body-expanded');
			var timeout = 0;
			if (body.hasClass('body-expanded')) {
				timeout = 100;
			}
			setTimeout(function () {
				box.toggleClass('expanded-padding');
			}, timeout);
			setTimeout(function () {
				box.resize();
				box.find('[id^=map-]').resize();
			}, timeout + 50);
		})
		.on('click', '.collapse-link', function (e) {
			e.preventDefault();
			var box = $(this).closest('div.box');
			var button = $(this).find('i');
			var content = box.find('div.box-content');
			content.slideToggle('fast');
			button.toggleClass('fa-chevron-up').toggleClass('fa-chevron-down');
			setTimeout(function () {
				box.resize();
				box.find('[id^=map-]').resize();
			}, 50);
		});
		
		//
		// Swap 2 elements on page. Used by WinMove function
		//
		jQuery.fn.swap = function(b){
			b = jQuery(b)[0];
			var a = this[0];
			var t = a.parentNode.insertBefore(document.createTextNode(''), a);
			b.parentNode.insertBefore(a, b);
			t.parentNode.insertBefore(b, t);
			t.parentNode.removeChild(t);
			return this;
		}; 
	})
	/**
	 * Highlighting text in a particular div
	 */

		jQuery.fn.highlight = function(pat,semi_highlight,elem) {
		var i=1;
		function innerHighlight(node, pat) {
			var skip = 0;
			if (node.nodeType == 3) {
				var pos = node.data.toUpperCase().indexOf(pat);
				if (pos >= 0) {
					var spannode = document.createElement('span');
					if(semi_highlight)
						spannode.className = 'semi-highlight';
					else
						spannode.className = 'highlight';
					spannode.setAttribute("data-count",i);
					i++;
					var middlebit = node.splitText(pos);
					var endbit = middlebit.splitText(pat.length);
					var middleclone = middlebit.cloneNode(true);
					spannode.appendChild(middleclone);
					middlebit.parentNode.replaceChild(spannode, middlebit);
					skip = 1;
				}
			} else if (node.nodeType == 1 && node.childNodes
					&& !/(script|style)/i.test(node.tagName)) {
				for (var i = 0; i < node.childNodes.length; ++i) {
					i += innerHighlight(node.childNodes[i], pat);
				}
			}
			return skip;
		}
		return this.length && pat && pat.length ? this.each(function(i) {
			/*
			 * Excluding the clicked element for just highlighting the selection
			 */
			if(semi_highlight){
				if(elem && (! $(this).is($(elem))) ){
					innerHighlight(this, pat.toUpperCase());
				}
			}else
				innerHighlight(this, pat.toUpperCase());
				
		}) : this;
	};

	jQuery.fn.removeHighlight = function(isSemiHighlight) {
		if(isSemiHighlight){
			return this.find("span.semi-highlight").each(function() {		
			this.parentNode.firstChild.nodeName;
			with (this.parentNode) {
				replaceChild(this.firstChild, this);
				normalize();
			}
		}).end();
		}else{
			return this.find("span.highlight").each(function() {		
			this.parentNode.firstChild.nodeName;
			with (this.parentNode) {
				replaceChild(this.firstChild, this);
				normalize();
			}
		}).end();
		}
		
	};
	
	
	jQuery.fn.visible = function() {
	    return this.css('visibility', 'visible');
	};

	jQuery.fn.invisible = function() {
	    return this.css('visibility', 'hidden');
	};

	jQuery.fn.visibilityToggle = function() {
	    return this.css('visibility', function(i, visibility) {
	        return (visibility == 'visible') ? 'hidden' : 'visible';
	    });
	};
	//hide the context menu when clicked on window's element
	$('body').on("mouseup",function(e){
		if(! $(".contextMenuBody").is(":hidden")){
			if(! $(e.target).parents(".contextMenuBody").length > 0){
				$(".contextMenuBody").hide();
			}
		}
		if(! $(".contextMenu").is(":hidden")){
			if(! $(e.target).parents(".contextMenu").length > 0){
				$(".contextMenu").hide();
				//Remove highlighted text when user clicks somewhere on the screen
				$('body').removeHighlight(true);
			}
		}
	})
	
//	String.prototype.capitalizeFirstLetter = function() {
//	    return this.charAt(0).toUpperCase() + this.slice(1);
//	}
	$('body').on("mouseenter",'.topLevelFilter.fixed',function(e){
		$(this).find('.fixedSearchBox').removeClass('hiddeBox')
	});
	$('body').on("mouseleave",'.topLevelFilter.fixed',function(e){
		if($(this).find('.fixedSearchBox .select2-container.select2-dropdown-open').length 
			|| $(this).find('.VS-focus').length
			|| $(this).find('.advanceSearchActive').length){
			$(this).find('.fixedSearchBox').removeClass('hiddeBox')
		}else{
			$(this).find('.fixedSearchBox').addClass('hiddeBox')
		}	
	});

	require(["nv"],function(){
    	nv.dispatch.on("render_end",function(){
    		$('.nvtooltip').css('opacity', '0');
    	});
    });
	
});
