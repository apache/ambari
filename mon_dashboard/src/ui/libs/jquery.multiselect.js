/*
 * jQuery MultiSelect Plugin 0.6
 * Copyright (c) 2010 Eric Hynds
 *
 * http://www.erichynds.com/jquery/jquery-multiselect-plugin-with-themeroller-support/
 * Inspired by Cory S.N. LaViska's implementation, A Beautiful Site (http://abeautifulsite.net/) 2009
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
*/
(function($){
	
	$.fn.multiSelect = function(opts){
		opts = $.extend({}, $.fn.multiSelect.defaults, opts);

		return this.each(function(){
			return new MultiSelect(this, opts);
		});
	};
	
	// counter to dynamically generate label/option IDs if they don't exist
	var multiselectID = 0;
	
	var MultiSelect = function(select,o){
		var $select = $original = $(select), 
			$options, $header, $labels, 
			html = [], 
			optgroups = [], 
			isDisabled = $select.is(':disabled'), 
			id = select.id || 'ui-multiselect-'+multiselectID++; // unique ID for the label & option tags
		
		html.push('<a id="'+ id +'" class="ui-multiselect ui-widget ui-state-default ui-corner-all' + (isDisabled || o.disabled ? ' ui-state-disabled' : '') + '">');
		html.push('<input readonly="readonly" type="text" class="ui-state-default" value="'+ o.noneSelectedText +'" title="'+ select.title +'" /><span class="ui-icon ui-icon-triangle-1-s"></span></a>');
		html.push('<div class="ui-multiselect-options' + (o.shadow ? ' ui-multiselect-shadow' : '') + ' ui-widget ui-widget-content ui-corner-all">');
	
		if(o.showHeader){
			html.push('<div class="ui-widget-header ui-helper-clearfix ui-corner-all ui-multiselect-header">');
			html.push('<ul class="ui-helper-reset">');
			html.push('<li><a class="ui-multiselect-all" href=""><span class="ui-icon ui-icon-check"></span>' + o.checkAllText + '</a></li>');
			html.push('<li><a class="ui-multiselect-none" href=""><span class="ui-icon ui-icon-closethick"></span>' + o.unCheckAllText + '</a></li>');
			html.push('<li class="ui-multiselect-close"><a href="" class="ui-multiselect-close ui-icon ui-icon-circle-close"></a></li>');
			html.push('</ul>');
			html.push('</div>');
		}
		
		// build options
		html.push('<ul class="ui-multiselect-checkboxes ui-helper-reset">');
		
		/* 
			If this select is disabled, remove the actual disabled attribute and let themeroller's .ui-state-disabled class handle it.
			This is a workaround for jQuery bug #6211 where options in webkit inherit the select's disabled property.  This
			won't achieve the same level of 'disabled' behavior (the checkboxes will still be present in form submission), 
			but it at least gives you a way to emulate the UI. 
		*/
		if(isDisabled){
			$select.removeAttr("disabled");
		}
		
		$select.find('option').each(function(i){
			var $this = $(this), 
				title = $this.html(), 
				value = this.value, 
				inputID = this.id || "ui-multiselect-"+id+"-option-"+i, 
				$parent = $this.parent(), 
				hasOptGroup = $parent.is('optgroup'), 
				isDisabled = $this.is(':disabled'), 
				labelClasses = ['ui-corner-all'];
			
			if(hasOptGroup){
				var label = $parent.attr('label');
				
				if($.inArray(label,optgroups) === -1){
					html.push('<li class="ui-multiselect-optgroup-label"><a href="#">' + label + '</a></li>');
					optgroups.push(label);
				}
			}
		
			if(value.length > 0){
				if(isDisabled){
					labelClasses.push('ui-state-disabled');
				}
				
				html.push('<li class="' + (isDisabled ? 'ui-multiselect-disabled' : '') +'">');
				html.push('<label for="'+inputID+'" class="'+labelClasses.join(' ')+'"><input id="'+inputID+'" type="'+(o.multiple ? 'checkbox' : 'radio')+'" name="'+select.name+'" value="'+value+'" title="'+title+'"');
				if($this.is(':selected')){
					html.push(' checked="checked"');
				}
				if(isDisabled){
					html.push(' disabled="disabled"');
				}
				html.push(' />'+title+'</label></li>');
			}
		});
		html.push('</ul></div>');

		// append the new control to the DOM; cache queries
		$select  = $select.after( html.join('') ).next('a.ui-multiselect');
		$options = $select.next('div.ui-multiselect-options');
		$header  = $options.find('div.ui-multiselect-header');
		$labels  = $options.find('label').not('.ui-state-disabled');
		
		// calculate widths
		var iconWidth = $select.find('span.ui-icon').outerWidth(), inputWidth = $original.outerWidth(), totalWidth = inputWidth+iconWidth;
		if( /\d/.test(o.minWidth) && totalWidth < o.minWidth){
			inputWidth = o.minWidth-iconWidth;
			totalWidth = o.minWidth;
		}
		
		// set widths
		$select.width(totalWidth).find('input').width(inputWidth);
		
		// build header links
		if(o.showHeader){
			$header.find('a').click(function(e){
				var $this = $(this);
			
				// close link
				if($this.hasClass('ui-multiselect-close')){
					$options.trigger('close');
			
				// check all / uncheck all
				} else {
					var checkAll = $this.hasClass('ui-multiselect-all');
					$options.trigger('toggleChecked', [(checkAll ? true : false)]);
					o[ checkAll ? 'onCheckAll' : 'onUncheckAll']['call'](this);
				}
			
				e.preventDefault();
			});
		}
		
		var updateSelected = function(){
			var $inputs = $labels.find('input'),
				$checked = $inputs.filter(':checked'),
				value = '',
				numChecked = $checked.length;
			
			if(numChecked === 0){
				value = o.noneSelectedText;
			} else {
				if($.isFunction(o.selectedText)){
					value = o.selectedText.call(this, numChecked, $inputs.length, $checked.get());
				} else if( /\d/.test(o.selectedList) && o.selectedList > 0 && numChecked <= o.selectedList){
					value = $checked.map(function(){ return this.title; }).get().join(', ');
				} else {
					value = o.selectedText.replace('#', numChecked).replace('#', $inputs.length);
				}
			}
			
			$select.find('input').val(value);
			return value;
		};
		
		// the select box events
		$select.bind({
			click: function(){
				$options.trigger('toggle');
			},
			keypress: function(e){
				switch(e.keyCode){
					case 27: // esc
					case 38: // up
						$options.trigger('close');
						break;
					case 40: // down
					case 0: // space
						$options.trigger('toggle');
						break;
				}
			},
			mouseenter: function(){
				if(!$select.hasClass('ui-state-disabled')){
					$(this).addClass('ui-state-hover');
				}
			},
			mouseleave: function(){
				$(this).removeClass('ui-state-hover');
			},
			focus: function(){
				if(!$select.hasClass('ui-state-disabled')){
					$(this).addClass('ui-state-focus');
				}
			},
			blur: function(){
				$(this).removeClass('ui-state-focus');
			}
		});
		
		// bind custom events to the options div
		$options.bind({
			'close': function(e, others){
				others = others || false;
			
				// hides all other options but the one clicked
				if(others === true){
					$('div.ui-multiselect-options')
					.filter(':visible')
					.fadeOut(o.fadeSpeed)
					.prev('a.ui-multiselect')
					.removeClass('ui-state-active')
					.trigger('mouseout');
					
				// hides the clicked options
				} else {
					$select.removeClass('ui-state-active').trigger('mouseout');
					$options.fadeOut(o.fadeSpeed);
				}
			},
			'open': function(e, closeOthers){
				
				// bail if this widget is disabled
				if($select.hasClass('ui-state-disabled')){
					return;
				}
				
				// use position() if inside ui-widget-content, because offset() won't cut it.
				var offset = $select.position(),
					$container = $options.find('ul:last'), 
					top, width;
				
				// calling select is active
				$select.addClass('ui-state-active');
				
				// hide all other options
				if(closeOthers || typeof closeOthers === 'undefined'){
					$options.trigger('close', [true]);
				}
				
				// calculate positioning
				if(o.position === 'middle'){
					top = ( offset.top+($select.height()/2)-($options.outerHeight()/2) );
				} else if(o.position === 'top'){
					top = (offset.top-$options.outerHeight());
				} else {
					top = (offset.top+$select.outerHeight());
				}
				
				// calculate the width of the options menu
				width = $select.width()-parseInt($options.css('padding-left'),10)-parseInt($options.css('padding-right'),10);
				
				// select the first option
				$labels.filter('label:first').trigger('mouseenter').trigger('focus');
				
				// show the options div + position it
				$options.css({ 
					position: 'absolute',
					top: top+'px',
					left: offset.left+'px',
					width: width+'px'
				}).show();
				
				// set the scroll of the checkbox container
				$container.scrollTop(0);
				
				// set the height of the checkbox container
				if(o.maxHeight){
					$container.css('height', o.maxHeight);
				}
				
				o.onOpen.call($options[0]);
			},
			'toggle': function(){
				$options.trigger( $(this).is(':hidden') ? 'open' : 'close' );
			},
			'traverse': function(e, start, keycode){
				var $start = $(start), 
					moveToLast = (keycode === 38 || keycode === 37) ? true : false,
					
					// select the first li that isn't an optgroup label / disabled
					$next = $start.parent()[moveToLast ? 'prevAll' : 'nextAll']('li:not(.ui-multiselect-disabled, .ui-multiselect-optgroup-label)')[ moveToLast ? 'last' : 'first']();

				// if at the first/last element
				if(!$next.length){
					var $container = $options.find("ul:last");
					
					// move to the first/last
					$options.find('label')[ moveToLast ? 'last' : 'first' ]().trigger('mouseover');
					
					// set scroll position
					$container.scrollTop( moveToLast ? $container.height() : 0 );
					
				} else {
					$next.find('label').trigger('mouseenter');
				}
			},
			'toggleChecked': function(e, flag, group){
				var $inputs = (group && group.length) ? group : $labels.find('input');
				$inputs.not(':disabled').attr('checked', (flag ? 'checked' : '')); 
				updateSelected();
			}
		})
		.find('li.ui-multiselect-optgroup-label a')
		.click(function(e){
			// optgroup label toggle support
			var $checkboxes = $(this).parent().nextUntil('li.ui-multiselect-optgroup-label').find('input');
			
			$options.trigger('toggleChecked', [ ($checkboxes.filter(':checked').length === $checkboxes.length) ? false : true, $checkboxes]);
			o.onOptgroupToggle.call(this, $checkboxes.get());
			e.preventDefault();
		});
		
		// labels/checkbox events
		$labels.bind({
			mouseenter: function(){
				$labels.removeClass('ui-state-hover');
				$(this).addClass('ui-state-hover').find('input').focus();
			},
			keyup: function(e){
				switch(e.keyCode){
					case 27: // esc
						$options.trigger('close');
						break;
			
					case 38: // up
					case 40: // down
					case 37: // left
					case 39: // right
						$options.trigger('traverse', [this, e.keyCode]);
						break;
				
					case 13: // enter
						e.preventDefault();
						$(this).click();
						break;
				}
			}
		})
		.find('input')
		.bind('click', function(e){
			o.onCheck.call(this);
			updateSelected();
		});

		// remove the original input element
		$original.remove();
		
		// apply bgiframe if available
		if($.fn.bgiframe){
			$options.bgiframe();
		}
		
		// open by default?
		if(o.state === 'open'){
			$options.trigger('open', [false]);
		}
		
		// update the number of selected elements when the page initially loads, and use that as the defaultValue.  necessary for form resets when options are pre-selected.
		$select.find('input')[0].defaultValue = updateSelected();

		return $select;
	};
	
	// close each select when clicking on any other element/anywhere else on the page
	$(document).bind('click', function(e){
		var $target = $(e.target);

		if(!$target.closest('div.ui-multiselect-options').length && !$target.parent().hasClass('ui-multiselect')){
			$('div.ui-multiselect-options').trigger('close', [true]);
		}
	});

	// default options
	$.fn.multiSelect.defaults = {
		showHeader: true,
		maxHeight: 175, /* max height of the checkbox container (scroll) in pixels */
		minWidth: 215, /* min width of the entire widget in pixels. setting to 'auto' will disable */
		checkAllText: 'Check all',
		unCheckAllText: 'Uncheck all',
		noneSelectedText: 'Select options',
		selectedText: '# selected',
		selectedList: 0,
		position: 'bottom', /* top|middle|bottom */
		shadow: false,
		fadeSpeed: 200,
		disabled: false,
		state: 'closed',
		multiple: true, 
		onCheck: function(){}, /* when an individual checkbox is clicked */
		onOpen: function(){}, /* when the select menu is opened */
		onCheckAll: function(){}, /* when the check all link is clicked */
		onUncheckAll: function(){}, /* when the uncheck all link is clicked */
		onOptgroupToggle: function(){} /* when the optgroup heading is clicked */
	};

})(jQuery);