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

define(['require','intro'],function(require,IntroJs) {
    'use strict';
    var Intro = {};
    Intro.Start = function() {

        var intro = IntroJs();
        intro.setOptions({
            steps: [{
                element: $('#r_EventHistory').get(0),
                intro: "This section will track the ongoing filters applied to the query, you can navigate back and forth.",
                position: "top"
            }, {
                element: $('#r_Histogram').get(0),
                intro: "This section is basically time filter required for particular time related results.",
                position: "top"
            }, {
                element: document.querySelectorAll('#r_BubbleTable')[1],
                intro: "This is consolidated view for all host and components.",
                position: "top",
                child: 'li[data-parent="true"]'
            }, {
                element: document.querySelectorAll('#r_BubbleTable')[1],
                intro: "By clicking on host node the tree will expand to shows it underlying components.",
                position: "top",
                child: 'li[data-parent="true"]'
            }, {
                element: $(document.querySelectorAll('#r_BubbleTable')[1]).find('.box-content')[1],
                intro: "Blue icon will appear near every components to view its logs on new tab. You can simultaneously view components log file in adjacent tabs.",
                position: "right",
                child: 'li[data-parent="true"]'
            }, {
                element: document.querySelectorAll('#r_BubbleTable')[1],
                intro: " This view shows the actual log entries with segregated columns.",
                position: "top",
                child: 'li[data-parent="true"]'
            }, {
                element: $(document.querySelectorAll('#r_BubbleTable')[1]).find('td.logTime:first').get(0),
                intro: "This is quick menu for every log entry with has options for getting into the detail of that log.",
                position: "right"
            }].filter(function(obj) {
                if (obj.child)
                    return $(obj.element).find(obj.child).length
                return $(obj.element).length;
            }),
            'exitOnOverlayClick': false
        });
        Intro.bindEvent(intro)

        this.dataAttribute = $('div[role="tabpanel"]').find('.nav-tabs .active').attr('data-id');
        if (this.dataAttribute != "hierarchy") {
            $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
        }
        intro.start();
        if ($('#r_EventHistory').find(".fa-chevron-down").length > 0) {
            $('#r_EventHistory').find('a.collapse-link').click();
        }
        if ($('#r_Histogram').find(".fa-chevron-down").length > 0) {
            $('#r_Histogram').find('a.collapse-link').click();
            this.histoGram = true;
        }
        if ($(document.querySelectorAll('#r_BubbleTable')[1]).find(".fa-chevron-down").length > 0) {
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('a.collapse-link').click();
            this.bubbleGraph = true;
        }
        var overlay = document.createElement('div');
        overlay.setAttribute('class', 'overlayIntro');
        $('body').append(overlay);

    };

    Intro.bindEvent = function(intro) {

        var that = this;
        var options = {
            0: {
                css: {
                    'top': '38px',
                    'left': '98px'
                },
                handDirection: 'down',
                handClass: "up-down"
            },
            1: {
                css: {
                    'top': '38px',
                    'left': '98px'
                },
                handDirection: 'up',
                handClass: "up-down"
            },
            2: {
                css: {
                    'top': '38px',
                    'left': '98px'
                },
                appendIndex: 0,
                handDirection: 'down',
                handClass: "up-down"
            },
            3: {
                css: {
                    'top': '14px',
                    'left': '142px'
                },
                appendIndex: 1,
                handDirection: 'left',
                handText: "Components",
                handClass: "left-right"

            },
            4: {
                css: {
                    'top': '45px',
                    'right': '0'
                },
                appendIndex: 0,
                handDirection: 'left',
                handClass: "left-right"
            },
            5: {
                css: {
                    'top': '25px',
                    'left': '190px'
                },
                appendIndex: 0,
                handDirection: 'up',
                handClass: "up-down"
            },
            6: {
                css: {
                    'top': '50%',
                    'left': '0'
                },
                appendIndex: 0,
                handDirection: 'up',
                handClass: "up-down"
            }
        }


        //Onafter step Callback
        intro.onafterchange(function(targetElement) {
            if (this._currentStep == 2) {
                scroll(targetElement, 0);
            } else if (this._currentStep == 3) {
                scroll(targetElement, 0);
            } else if (this._currentStep == 4) {
                scroll(targetElement, 0);
            } else if (this._currentStep == 5) {
                scroll(targetElement, -200);
            } else if (this._currentStep == 6) {
                setTimeout(function() {
                    $(targetElement).mouseover();
                }, 100);
                setTimeout(function(){
                    $('#rLogTable').find('.btn-quickMenu').first().click();
                },800);

            }


        });
        //OnBefore step Callback
        intro.onbeforechange(function(targetElement) {
            removeFingerAndOverlayDiv();
            if (this._currentStep == 1) {
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep])
            } else if (this._currentStep == 2) {
                $(targetElement).find('input[value="H"]').click();
                $(targetElement).find('li[data-parent="true"]').first().find('span[data-state="expand"]').first().click();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep])
            } else if (this._currentStep == 3) {
                $(targetElement).find('li[data-parent="true"]').first().find('span[data-state="collapse"]').first().click();
                if ($(targetElement).find('li[data-parent="true"]').length == 0) {

                }
                $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="H"]').click();
                $(targetElement).find('li[data-parent="true"]').first().find('span[data-state="collapse"]').first().click();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep])
            } else if (this._currentStep == 4) {
                $(document.querySelectorAll('#r_BubbleTable')[1]).find('li[data-parent="true"]').first().find('span[data-state="collapse"]').first().click();
                $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="H"]').click();
                $(targetElement).find('ul[role="group"]').find('li').first().mouseover();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep])
            } else if (this._currentStep == 5) {
                $(targetElement).find('input[value="T"]').click();
                $(targetElement).find('[data-id="r_tableList"]').css('height','200px');
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep])
            } else if (this._currentStep == 6) {
                $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="T"]').click();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep])
            }

        });

        //OnDone Callback
        intro.oncomplete(function() {
            $($('#r_EventHistory').get(0)).find('a.collapse-link').click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="H"]').click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('li[data-parent="true"]').first().find('span[data-state="expand"]').first().click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('[data-id="r_tableList"]').css('height','');
            removeFingerAndOverlayDiv(true);
            tabClick();
        });

        //OnSkip Callback
        intro.onexit(function(targetElement) {
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="H"]').click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('li[data-parent="true"]').first().find('span[data-state="expand"]').first().click();
            $('#r_EventHistory').find('a.collapse-link').click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('[data-id="r_tableList"]').css('height','');
            if (that.histoGram) {
                $('#r_Histogram').find('a.collapse-link').click();
            }
            if (that.bubbleGraph) {
                $(document.querySelectorAll('#r_BubbleTable')[1]).find('a.collapse-link').click();
            }
            removeFingerAndOverlayDiv(true);
            tabClick();
        });

        //remove all animation and overlay div
        function removeFingerAndOverlayDiv(overlay) {
            if(overlay){
                 $('body').find('.overlayIntro').remove();
            }
            $('body').find('.box-content .finger').remove();

        }
        //add all animation and overlay div
        function appendFingerAndOverlayDiv(targetElementObject, options) {

            if (options.appendIndex != undefined) {
                if ($(targetElementObject).find('.box-content').length == 0) {
                    $(targetElementObject).append('<div class="animated infinite finger ' + options.handClass + '"><i class="fa fa-hand-o-' + options.handDirection + ' fa-2x"></i></div>');
                    $(targetElementObject).find('.finger').css(options.css);
                } else {
                    $($(targetElementObject).find('.box-content')[options.appendIndex]).append('<div class="animated infinite finger ' + options.handClass + '"><i class="fa fa-hand-o-' + options.handDirection + ' fa-2x"></i></div>');
                    $($(targetElementObject).find('.box-content')[options.appendIndex]).find('.finger').css(options.css);
                }
            } else {
                $(targetElementObject).find('.box-content').append('<div class="animated infinite finger ' + options.handClass + '"><i class="fa fa-hand-o-' + options.handDirection + ' fa-2x"></i></div>');
                $(targetElementObject).find('.box-content').find('.finger').css(options.css);
            }

        }

        function scroll(targetElement, offsetPlus) {
            $('html,body').animate({
                scrollTop: (($(targetElement).offset().top) + offsetPlus)
            });
        }

        function dispatchResizeEvent() {
            setTimeout(function() {
                window.dispatchEvent(new Event('resize'));
            }, 100);
        }

        function tabClick() {
            if (that.dataAttribute != "hierarchy") {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id=' + that.dataAttribute + '] a').click();
            }
            window.scrollTo(0, 0);
        }
    }
    return Intro;
});
