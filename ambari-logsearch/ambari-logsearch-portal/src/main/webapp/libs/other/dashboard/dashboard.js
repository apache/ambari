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
'use strict';
(function(root, factory) {

    if (typeof define === 'function' && define.amd) {
        define(['jquery', 'gridster'], function($, gridster) {
            factory($, gridster);
        });
    } else {
        if ((root.jQuery != "undefined" || root.Zepto != "undefined" || root.ender != "undefined" || root.$ != "undefined")) {
            /*if (root.gridster != "undefined" || gridster != "undefined") {
                console.log("dsdsds")
                factory(root.gridster || gridster, (root.jQuery || root.Zepto || root.ender || root.$));
            } else {
                throw new Error('gridster dependnecy not found');
            }*/
            factory((root.jQuery || root.Zepto || root.ender || root.$));
        } else {
            throw new Error('jQuery dependnecy not found');
        }

    }

}(this, function($, gridster) {

    var dashboard = function(element, options) {
        this.$el = element;
        this.options = options;
        this.generateDashboard();
    }

    var gridsterPrototypeFn = {};

    dashboard.VERSION = '1.0.0';

    dashboard.DEFAULTS = {

    };

    dashboard.prototype = {

        constructor: dashboard,


        importData: function(data) {
            for (var i = 0; i < data.length; i++) {
                this.addWidget('<li class="new"></li>', data[i].size_x, data[i].size_y, data[i].col, data[i].row,data[i]);
            }
        },

        exportData: function(el) {

            this.data = gridsterPrototypeFn.serialize((el) ? (el) : (undefined));
            return this.data
        },
        addWidget: function(html, size_x, size_y, col, row,data) {
            if (this.options.onAdded) {
                var el = gridsterPrototypeFn.add_widget(html, size_x, size_y, col, row);
                this.options.onAdded(el,data);
            } else {
                var el = gridsterPrototypeFn.add_widget(html, size_x, size_y, col, row);

            }
        },
        deleteWidget: function(el, callback) {
            var el = gridsterPrototypeFn.remove_widget(el, function() {
                this.options.onDeleted(el);
            });
        },
        deleteAll: function(callback) {
            var data = this.$el.children();
            for (var i = 0; i < data.length; i++) {
                if ((data.length - 1) == i) {
                    var el = gridsterPrototypeFn.remove_widget(data[i], function() {
                        this.options.onAllDeleted(el);
                    });
                } else {
                    gridsterPrototypeFn.remove_widget(data[i]);
                    //this.options.onDeleted(el);
                }

            }
        },
        /**
         * [generateMap create element dynamically]
         * @param  {[type]} options [depanding on option it will create e]
         * @return {[type]}         [description]
         */
        generateDashboard: function() {
            var dockLi = [];
            if (this.options.dock && this.options.dock.dockIcon && this.options.dock.dockIcon.length > 0) {
                var dockList = this.options.dock.dockIcon
                for (var index in dockList) {
                    var dockIcon = (this.genrateElement('i', {
                        'class': (dockList[index].iClass) ? (dockList[index].iClass) : ("")
                    }));
                    var attr = {};
                    if (dockList[index].parentAttr) {
                        attr = dockList[index].parentAttr
                        if (attr.class && attr.class != "iconG") {
                            attr.class += " iconG"
                        }
                    } else {
                        attr.class = 'iconG'

                    }

                    var li = this.genrateElement('li',
                        attr, dockIcon);
                    dockLi.push(li);
                }
            }

            var dockUl = this.genrateElement('ul', {}, dockLi);



            var dock = this.genrateElement('div', {
                'class': 'slideMenu dragArea'
            }, dockUl);

            var gridBoxUl = this.genrateElement('ul', {});

            var gridBox = this.genrateElement('div', {
                'class': 'grid-container',
            }, gridBoxUl);


            if (this.options.dock && this.options.dock.position) {
                if (this.options.dock.position == "right") {
                    this.$el.append(gridBox);
                    this.$el.append(dock);
                    this.$el.addClass("right");
                } else {
                    this.$el.append(dock);
                    this.$el.append(gridBox);
                    this.$el.addClass("left");
                }

            } else {
                this.$el.append(dock);
                this.$el.append(gridBox);
            }
            this.$el.addClass("dashbord")

            this.bindEvent();
            this.createCss();

        },
        /**
         * [bindEvent bind all event i.e click,mouseenter,mouseleave,change(select)]
         * @return {[type]} [description]
         */
        bindEvent: function() {
            var that = this;
            var dockObject = this.options.dock;

            if (dockObject && dockObject.draggable) {
                if (dockObject.draggable.el) {
                    if (dockObject.draggable.el instanceof jQuery) {
                        dockObject.draggable.el.draggable(dockObject.draggable);
                    } else if (typeof c == "string") {
                        $(dockObject.draggable.el).draggable(dockObject.draggable);
                    }
                } else {
                    this.$el.find('.slideMenu li.iconG').draggable(dockObject.draggable);
                }
            }
            if (dockObject && dockObject.droppable) {
                //dockObject.droppable.appendTo:
                dockObject.droppable['drop'] = function(event, ui) {

                    that.createGrid(ui.draggable);
                }
                if (dockObject.droppable.el) {
                    if (dockObject.droppable.el instanceof jQuery) {
                        dockObject.droppable.el.droppable(dockObject.droppable);
                    } else if (typeof c == "string") {
                        $(dockObject.droppable.el).droppable(dockObject.droppable);
                    }

                } else {
                    this.$el.find('.grid-container').droppable(dockObject.droppable);
                }
            }
            if (dockObject && dockObject.dockClick) {
                if (dockObject.draggable && dockObject.draggable.el) {
                    if (dockObject.droppable.el instanceof jQuery) {
                        dockObject.droppable.el.click(function() {
                            that.createGrid($(this));
                        });
                    } else if (typeof c == "string") {
                        $(dockObject.droppable.el).click(function() {
                            that.createGrid($(this));
                        });
                    }

                } else {
                    this.$el.find('.slideMenu li.iconG').click(function() {
                        that.createGrid($(this));
                    })
                }
            }
            this.initializeGridster();
        },
        createGrid: function(el) {
            var that = this;
            if (el && el.data()) {
                var attr = {};
                attr = $.extend({}, el.data());
                if (attr.uiDraggable) {
                    delete attr.uiDraggable
                }
                for (var index in attr) {
                    attr["data-" + index] = attr[index]
                    delete attr[index]
                }

                var li = that.genrateElement('li',
                    attr);
                that.addWidget(li, 3, 2);
            } else {
                that.addWidget('<li/>', 3, 2);
            }

        },
        initializeGridster: function() {
            gridsterPrototypeFn = this.$el.find('.grid-container ul').gridster(this.options).data('gridster');
            $.extend(dashboard.prototype, gridsterPrototypeFn);
            if (this.options.onLoaded) {
                this.options.onLoaded();
            } else {
                this.el.trigger("dashboard:loaded");
            }
        },
        /**
         * [genrateElement description]
         * @param  {[Jquery Object]}  element     [selector]
         * @param  {[type]}  elementAttr [description]
         * @param  {[javascript Object or text]}  chilled      [If we pass javascript object or  array it will append all chilled and if you pass string it will add string(value) inside element ]
         * @param  {Boolean} isSvg       [If it is svg then it will create svg element]
         * @return {[type]}              [description]
         */
        genrateElement: function(element, elementAttr, chilled, isSvg) {


            if (isSvg) {
                var elementObject = document.createElementNS('http://www.w3.org/2000/svg', element);
            } else {
                var elementObject = document.createElement(element);
            }
            if (elementAttr) {
                for (var key in elementAttr) {
                    elementObject.setAttribute(key, elementAttr[key]);
                }
            }
            if (chilled) {
                if (chilled instanceof Array) {
                    for (var chilleds in chilled) {
                        elementObject.appendChild(chilled[chilleds]);
                    }
                } else if (typeof chilled == 'string') {
                    elementObject.innerHTML = chilled;
                } else {
                    elementObject.appendChild(chilled);
                }

            }

            return elementObject;

        },
        /**
         * [createCss function will create css dynamically it is insert style attribute in  in head ]
         * @param  {[type]} options [options has mapColor,selectedColor,hoverColor ]
         * @return {[type]}         [description]
         */
        createCss: function(options) {
            var style = document.createElement('style');
            style.type = 'text/css';
            var strVar = "";
            strVar += ".grid-container {";
            strVar += "    position: relative;";
            strVar += "    list-style: none;";
            strVar += "    min-height: 250px !important;";
            strVar += "    width: 92%;";
            strVar += "    float: left;";
            strVar += "    border: 2px #ddd dotted;";
            strVar += "    margin-left: 1%;";
            strVar += "}";
            strVar += ".slideMenu{";
            strVar += "    width: 7%;";
            strVar += "    float: left;";
            strVar += "    min-height: 250px;";
            strVar += "}";
            strVar += ".slideMenu .iconG i {";
            strVar += "    font-size: 33px;";
            strVar += "    text-shadow: 4px 4px 1px #DADADA;";
            strVar += "}";
            strVar += ".dashbord .gs-w {";
            strVar += "    background: #FFF;";
            strVar += "    cursor: pointer;";
            strVar += "    -webkit-box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);";
            strVar += "    box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);";
            strVar += "}";
            strVar += "";
            strVar += ".dashbord {";
            strVar += "    padding: 3px;";
            strVar += "}";
            strVar += "";
            strVar += ".dashbord ul,";
            strVar += "ol {";
            strVar += "    list-style: none;";
            strVar += "}";
            strVar += ".dashbord > * {";
            strVar += "    margin: 0;";
            strVar += "}";
            strVar += ".dashbord .preview-holder {";
            strVar += "    background-color: #999;";
            strVar += "}";
            strVar += "";
            strVar += ".slideMenu ul{";
            strVar += "    list-style: none;";
            strVar += "    margin: 0;";
            strVar += "    padding: 0px;";
            strVar += "}";
            strVar += ".slideMenu li i {";
            strVar += "  height: 64px;";
            strVar += "  -webkit-transition: all 0.3s;";
            strVar += "  -webkit-transform-origin: 50% 100%;";
            strVar += "}";
            strVar += ".dashbord.left .slideMenu li:hover i {";
            strVar += "     cursor: pointer;";
            strVar += "    -webkit-transform: scale(2);";
            strVar += "    margin-top: 59px;";
            strVar += "    margin-left: 34px;";
            strVar += "    margin-bottom: -47px;";
            strVar += "}";
            strVar += ".dashbord.right .slideMenu li:hover i {";
            strVar += "     cursor: pointer;";
            strVar += "    -webkit-transform: scale(2);";
            strVar += "    margin-top: 59px;";
            strVar += "    margin-right: 34px;";
            strVar += "    margin-bottom: -47px;";
            strVar += "}";
            strVar += ".dashbord.right .slideMenu li{";
            strVar += "     text-align: right;";
            strVar += "}";
            strVar += ".dashbord.left .slideMenu li{";
            strVar += "     text-align: left;";
            strVar += "}";
            style.innerHTML = strVar;
            document.getElementsByTagName('head')[0].appendChild(style);

        }
    };
    /**
     * [Plugin Staring point for plugin]
     * @param {[type]} option [user options which can be override the default options]
     */
    var that = this

    function Plugin(option) {
        return this.each(function() {
            var $el = $(this)
            var options = $.extend({}, dashboard.DEFAULTS, $el.data(), typeof option == 'object' && option);
            $el.data('dashboard', new dashboard($el, options));



        });
    };

    $.fn.dashboard = Plugin;


}));
