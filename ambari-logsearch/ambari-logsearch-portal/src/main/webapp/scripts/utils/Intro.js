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

define(['require', 'intro'], function(require, IntroJs) {
    'use strict';
    var Intro = {};
    Intro.Start = function() {

        var intro = IntroJs();
        intro.setOptions({

            skipLabel: "Close",

            showBullets : false,

            steps: [{
                element: $('[data-id="troubleshoot"]').get(0),
                intro: "Welcome, This is the landing page of the LogSearch app, its displays high-level info about services and its components along with graph.",
                position: "right"
            }, {
                element: $('#troubleShootHeader').get(0),
                intro: "In this section you can choose specific service to focus the issue along with their dependency.",
                position: "bottom"
            }, {
                element: $('div[data-id="dateRange"] .selectDateRange').get(0),
                intro: "This is date time picker by clicking on it you can select time frame and also there are pre-defined time slots.",
                position: "top"
            }, {
                element: $("#showServicelog").get(0),
                intro: " After selecting the service and time frame you can navigate to the logs detail page [Service log tab].",
                position: "top"
            }, {
                element: $('[data-id="hierarchy"]').get(0),
                intro: " This tab holds the logs of all services and its components with different views and aggregation and you can also apply filters for getting into the issues in detail.",
                position: "right"
            }, {
                element: $("#searchIncludeExclude").get(0),
                intro: "This filter allows you to query the log data column wise(log_message, level, host etc). Include Search is basically \"or\" condition and Exclude Search is \"and\" condition between multiple input.",
                position: "top"
            }, {
                element: $("#compInculdeExculde").get(0),
                intro: "This filter allows you to filter the log data depending upon the component selection. Include Component is again \"or\" condition and Exclude Component is \"and\" condition between multiple selection.",
                position: "top"
            }, {
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
                intro: "This is quick menu for every log entry which has options for getting into the detail of that log.",
                position: "right"
            }, {
                element: $('li[data-id="audit"]').get(0),
                intro: "This tab holds the access information across services and its components with different views and aggregation and you can also apply filters for viewing access info in detail.",
                position: "right"
            }, {
                element: $('#r_AuditLine').get(0),
                intro: "This section shows the component which are accessed related to time.",
                position: "bottom"
            }, {
                element: $('#AuditSearch').get(0),
                intro: "This filter allows you to query the data column wise(Access Enforcer, Access type etc). Include Search is basically \"or\" condition and Exclude Search is \"and\" condition between multiple input.",
                position: "top"
            }, {
                element: $('li[data-id="createFilters"]').get(0),
                intro: "This is the feature to filter out the data that is handled by LogSearch. For ex if you are only interested in logs with level ERROR,FATAL etc to be tracked by LogSearch.",
                position: "left"
            }].filter(function(obj) {
                if (obj.child)
                    return $(obj.element).find(obj.child).length
                return $(obj.element).length;
            }),
            'exitOnOverlayClick': false
        });
        Intro.bindEvent(intro)

        this.dataAttribute = $('div[role="tabpanel"]').find('.nav-tabs .active').attr('data-id');
        if (this.dataAttribute != "troubleshoot") {
            $('div[role="tabpanel"]').find('.nav-tabs [data-id="troubleshoot"] a').click();
        }
        intro.start();

        //Hiding Back button
        $('.introjs-prevbutton').hide();

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
                    'top': '0px',
                    'left': '98px'
                },
                handDirection: 'down',
                handClass: "up-down"
            },
            1: {
                css: {
                    'top': '0px',
                    'left': '98px'
                },
                handDirection: 'down',
                handClass: "up-down"
            },
            2: {
                css: {
                    'top': '0',
                    'right': '95px'
                },
                handDirection: 'left',
                handClass: "left-right"
            },
            3: {
                css: {
                    'top': '0',
                    'left': '40px'
                },
                handDirection: 'right',
                handClass: "left-right"
            },
            4: {
                css: {
                    'top': '0px',
                    'left': '98px'
                },
                handDirection: 'down',
                handClass: "up-down"
            },
            5: {
                css: {
                    'top': '10px',
                    'left': '10px'
                },
                handDirection: 'right',
                handClass: "right-left"
            },
            6: {
                css: {
                    'top': '10px',
                    'left': '10px'
                },
                handDirection: 'right',
                handClass: "right-left"
            },
            7: {
                css: {
                    'top': '38px',
                    'left': '98px'
                },
                handDirection: 'down',
                handClass: "up-down"
            },
            8: {
                css: {
                    'top': '38px',
                    'left': '98px'
                },
                handDirection: 'up',
                handClass: "up-down"
            },
            9: {
                css: {
                    'top': '38px',
                    'left': '98px'
                },
                appendIndex: 0,
                handDirection: 'down',
                handClass: "up-down"
            },
            10: {
                css: {
                    'top': '14px',
                    'left': '142px'
                },
                appendIndex: 1,
                handDirection: 'left',
                handText: "Components",
                handClass: "left-right"

            },
            11: {
                css: {
                    'top': '45px',
                    'right': '0'
                },
                appendIndex: 0,
                handDirection: 'left',
                handClass: "left-right"
            },
            12: {
                css: {
                    'top': '25px',
                    'left': '190px'
                },
                appendIndex: 0,
                handDirection: 'up',
                handClass: "up-down"
            },
            13: {
                css: {
                    'top': '50%',
                    'left': '0'
                },
                appendIndex: 0,
                handDirection: 'up',
                handClass: "up-down"
            },
            14: {
                css: {
                    'top': '0',
                    'left': '0'
                },
                handDirection: 'right',
                handClass: "left-right"
            },
            15: {
                css: {
                    'top': '0',
                    'left': '0'
                },
                handDirection: 'right',
                handClass: "left-right"
            },
            16: {
                css: {
                    'top': '0',
                    'left': '0'
                },
                handDirection: 'right',
                handClass: "left-right"
            },
            17: {
                css: {
                    'top': '0',
                    'left': '0'
                },
                handDirection: 'right',
                handClass: "left-right"
            },
            18: {
                css: {
                    'top': '0',
                    'left': '0'
                },
                handDirection: 'right',
                handClass: "left-right"
            }
        }


        //Onafter step Callback
        intro.onafterchange(function(targetElement) {
            if (this._currentStep == 0) {

            } else if (this._currentStep == 1) {
                scroll(targetElement, -200);
            } else if (this._currentStep == 2) {
                // scroll(targetElement, 0);
            }else if (this._currentStep == 3) {
                // scroll(targetElement, 0);
            } else if (this._currentStep == 4) {
                // scroll(targetElement, 0);
            } else if (this._currentStep == 5) {
                // scroll(targetElement, 0);
            } else if (this._currentStep == 6) {
               
            } else if (this._currentStep == 7) {
                
            } else if (this._currentStep == 8) {
                scroll(targetElement, -200);
            } else if (this._currentStep == 9) {
                scroll(targetElement, -150);
            } else if (this._currentStep == 10) {
                scroll(targetElement, 0);
            } else if (this._currentStep == 1) {
                scroll(targetElement, -250);
            } else if (this._currentStep == 12) {
                scroll(targetElement, -150);
            } else if (this._currentStep == 13) {
                setTimeout(function() {
                    $(targetElement).mouseover();
                }, 100);
                setTimeout(function() {
                    $('#rLogTable').find('.btn-quickMenu').first().click();
                }, 800);
            } else if (this._currentStep == 14) {

            } else if (this._currentStep == 15) {

            }else if (this._currentStep == 16) {

            }


        });

        //OnBefore step Callback
        intro.onbeforechange(function(targetElement) {
            removeFingerAndOverlayDiv();
            if (this._currentStep == 0) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="troubleshoot"] a').click();
                dispatchResizeEvent();
            } else if (this._currentStep == 1) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="troubleshoot"] a').click();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep]);
            } else if (this._currentStep == 2) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="troubleshoot"] a').click();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep]);
            } else if (this._currentStep == 3) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="troubleshoot"] a').click();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep]);
            } else if (this._currentStep == 4) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
                dispatchResizeEvent();
            } else if (this._currentStep == 5) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
                dispatchResizeEvent();
            } else if (this._currentStep == 6) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
                dispatchResizeEvent();
            } else if (this._currentStep == 7) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep]);
            } else if (this._currentStep == 8) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
                $(targetElement).find('input[value="H"]').click();
                $(targetElement).find('li[data-parent="true"]').first().find('span[data-state="expand"]').first().click();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep]);
            } else if (this._currentStep == 9) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();               
                $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="H"]').click();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep]);
            } else if (this._currentStep == 10) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
                $(document.querySelectorAll('#r_BubbleTable')[1]).find('li[data-parent="true"]').first().find('span[data-state="collapse"]').first().click();
                $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="H"]').click();
                $(targetElement).find('ul[role="group"]').find('li').first().mouseover();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep]);
            } else if (this._currentStep == 11) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
                $(targetElement).find('input[value="T"]').click();
                $(targetElement).find('[data-id="r_tableList"]').css('height', '200px');
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep]);
            } else if (this._currentStep == 12) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
                $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="T"]').click();
                dispatchResizeEvent();
                appendFingerAndOverlayDiv(targetElement, options[this._currentStep]);
            } else if (this._currentStep == 13) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="hierarchy"] a').click();
            } else if (this._currentStep == 14) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="audit"] a').click();
                dispatchResizeEvent();
            }else if (this._currentStep == 15) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="audit"] a').click();
                dispatchResizeEvent();
            }else if (this._currentStep == 15) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="audit"] a').click();
                dispatchResizeEvent();
            }else if (this._currentStep == 16) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="audit"] a').click();
                dispatchResizeEvent();
            }
        });

        //OnDone Callback
        intro.oncomplete(function() {
            $($('#r_EventHistory').get(0)).find('a.collapse-link').click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="H"]').click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('li[data-parent="true"]').first().find('span[data-state="expand"]').first().click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('[data-id="r_tableList"]').css('height', '');
            removeFingerAndOverlayDiv(true);
            tabClick();
        });

        //OnSkip Callback
        intro.onexit(function(targetElement) {
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('input[value="H"]').click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('li[data-parent="true"]').first().find('span[data-state="expand"]').first().click();
            $('#r_EventHistory').find('a.collapse-link').click();
            $(document.querySelectorAll('#r_BubbleTable')[1]).find('[data-id="r_tableList"]').css('height', '');
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
            if (overlay) {
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
                var flag = $(targetElementObject).find('.box-content');
                if (flag.length != 0) {
                    $(targetElementObject).find('.box-content').append('<div class="animated infinite finger ' + options.handClass + '"><i class="fa fa-hand-o-' + options.handDirection + ' fa-2x"></i></div>');
                    $(targetElementObject).find('.box-content').find('.finger').css(options.css);
                } else {
                    $(targetElementObject).append('<div class="animated infinite finger ' + options.handClass + '"><i class="fa fa-hand-o-' + options.handDirection + ' fa-2x"></i></div>');
                    $(targetElementObject).find('.finger').css(options.css);
                }
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
            if (!that.dataAttribute) {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id="troubleshoot"] a').click();
            } else {
                $('div[role="tabpanel"]').find('.nav-tabs [data-id=' + that.dataAttribute + '] a').click();
            }
            window.scrollTo(0, 0);
        }
    }
    return Intro;
});