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
var template = '' +
        '<div class="arrow"></div>' +
        '{{#if title}}' +
        '    <h3 class="popover-title">{{title}}</h3>' +
        '{{/if}}' +
        '<div class="popover-content">' +
        '{{#if content}}' +
        '        {{content}}' +
        '{{else}}' +
        '{{yield}}' +
        '{{/if}}' +
        '    </div>';
 
Ember.TEMPLATES["components/bs-popover"] = Ember.Handlebars.compile(template);
 
Ember.BsPopoverComponent = Ember.Component.extend({
    classNames: 'popover',
    classNameBindings:  ['fade', 'in', 'top', 'left', 'right', 'bottom'],
 
    top: function(){
        return this.get('realPlacement')=='top';
    }.property('realPlacement'),
    left: function(){
        return this.get('realPlacement')=='left';
    }.property('realPlacement'),
    right: function(){
        return this.get('realPlacement')=='right';
    }.property('realPlacement'),
    bottom: function(){
        return this.get('realPlacement')=='bottom';
    }.property('realPlacement'),
 
    title: '',
    content: '',
    html: false,
    delay: 0,
    isVisible: false,
    animation: true,
    fade: function(){
        return this.get('animation');
    }.property('animation'),
    in: function(){
        return this.get('isVisible');
    }.property('isVisible'),
    triggers: 'hover focus',
    placement: 'top',
    onElement: null,
    $element: null,
    $tip: null,
    inserted: false,
 
    styleUpdater: function(){
        if( !this.$tip || !this.get('isVisible')){
            return;
        }
        this.$tip.css('display','block');
        var placement = this.get('realPlacement');
        var pos = this.getPosition();
        var actualWidth = this.$tip[0].offsetWidth;
        var actualHeight = this.$tip[0].offsetHeight;
        var calculatedOffset = this.getCalculatedOffset(placement, pos, actualWidth, actualHeight);
 
        this.$tip.css('top',calculatedOffset.top);
        this.$tip.css('left',calculatedOffset.left);
        if(this.firstTime){
            this.firstTime = false;
            this.styleUpdater();
            this.firstTime = true;
        }
    }.observes('content','realPlacement','inserted', 'isVisible'),
 
 
    didInsertElement: function(){
 
        this.$tip = this.$();
        if(this.get('onElement')){
            this.$element=$('#'+this.get('onElement'));
        }else if(this.$tip.prev(':not(script)').length){
            this.$element = this.$tip.prev(':not(script)');
        }else{
            this.$element = this.$tip.parent(':not(script)');
        }
 
 
        var triggers = this.triggers.split(' ');
 
        for (var i = triggers.length; i--;) {
            var trigger = triggers[i];
 
            if (trigger == 'click') {
                this.$element.on('click',$.proxy(this.toggle, this));
            } else if (trigger != 'manual') {
                var eventIn  = trigger == 'hover' ? 'mouseenter' : 'focus';
                var eventOut = trigger == 'hover' ? 'mouseleave' : 'blur';
 
                this.$element.on(eventIn, $.proxy(this.enter, this));
                this.$element.on(eventOut, $.proxy(this.leave, this));
            }
        }
        this.set('inserted',true);
    },
 
 
    toggle: function(){
        this.toggleProperty('isVisible');
    },
 
    enter: function(){
        this.set('isVisible',true);
    },
 
    leave: function(){
        this.set('isVisible',false);
    },
 
    afterRender: function(){
        this.notifyPropertyChange('content');
    },
 
 
    realPlacement: function(){
 
        if(!this.$tip) return null;
        var placement = this.get('placement') || '';
        var autoToken = /\s?auto?\s?/i;
        var autoPlace = autoToken.test(placement);
        if (autoPlace)
            placement = placement.replace(autoToken, '') || 'top';
 
        var pos = this.getPosition();
        var actualWidth = this.$tip[0].offsetWidth;
        var actualHeight = this.$tip[0].offsetHeight;
 
        if (autoPlace) {
            var $parent = this.$element.parent();
 
            var orgPlacement = placement;
            var docScroll = document.documentElement.scrollTop || document.body.scrollTop;
            var parentWidth = $parent.outerWidth();
            var parentHeight = $parent.outerHeight();
            var parentLeft = $parent.offset().left;
 
            placement = placement == 'bottom' && pos.top + pos.height + actualHeight - docScroll > parentHeight ? 'top' :
                    placement == 'top' && pos.top - docScroll - actualHeight < 0 ? 'bottom' :
                            placement == 'right' && pos.right + actualWidth > parentWidth ? 'left' :
                                    placement == 'left' && pos.left - actualWidth < parentLeft ? 'right' :
                                            placement;
        }
        return placement;
 
    }.property('placement','inserted'),
 
 
    hasContent: function () {
        return this.get('title');
    },
 
    getPosition: function () {
        var el = this.$element[0];
        return $.extend({}, (typeof el.getBoundingClientRect == 'function') ? el.getBoundingClientRect() : {
            width: el.offsetWidth, height: el.offsetHeight
        }, this.$element.offset());
    },
 
 
    getCalculatedOffset: function (placement, pos, actualWidth, actualHeight) {
        return placement == 'bottom' ? { top: pos.top + pos.height, left: pos.left + pos.width / 2 - actualWidth / 2  } :
                placement == 'top' ? { top: pos.top - actualHeight, left: pos.left + pos.width / 2 - actualWidth / 2  } :
                        placement == 'left' ? { top: pos.top + pos.height / 2 - actualHeight / 2, left: pos.left - actualWidth } :
                            /* placement == 'right' */ { top: pos.top + pos.height / 2 - actualHeight / 2, left: pos.left + pos.width   }
    }
 
});
