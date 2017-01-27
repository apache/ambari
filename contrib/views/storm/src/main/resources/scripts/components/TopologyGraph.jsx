/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
*
     http://www.apache.org/licenses/LICENSE-2.0
*
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

define(['react', 'react-dom', 'd3', 'dagreD3', 'd3.tip'], function(React, ReactDOM, d3, dagreD3) {
	'use strict';
	return React.createClass({
		displayName: 'TopologyGraph',
        propTypes: {
            data: React.PropTypes.object.isRequired,
            width: React.PropTypes.string,
            height: React.PropTypes.string
        },
		getInitialState: function(){
			this.syncData(this.props.data);
			return null;
		},
		componentDidUpdate: function(){
			this.syncData(this.props.data);
            this.updateGraph();
		},
		componentDidMount: function(){
            var that = this;
            this.svg = d3.select(ReactDOM.findDOMNode(this))
            //Set up tooltip
            this.tooltip = d3.tip()
                .attr('class', function() {
                    return 'd3-tip testing';
                })
                .offset([-10, 0])
                .html(function(data) {
                    var d = that.g.node(data);
                    var tip = "<ul>";
                    if (d[":capacity"] !== null) tip += "<li>Capacity: " + d[":capacity"].toFixed(2) + "</li>";
                    if (d[":latency"] !== null) tip += "<li>Latency: " + d[":latency"].toFixed(2) + "</li>";
                    if (d[":transferred"] !== null) tip += "<li>Transferred: " + d[":transferred"].toFixed(2) + "</li>";
                    tip += "</ul>";
                    return tip;
                });
            //Set up zoom
            this.zoom = d3.behavior.zoom()
                .scaleExtent([0, 8])
                .on("zoom", this.zoomed);
        },
        zoomed: function(){
            this.inner.attr("transform",
                "translate(" + this.zoom.translate() + ")" +
                "scale(" + this.zoom.scale() + ")"
            );
        },
		// update graph (called when needed)
		updateGraph: function(){
            var that = this;
            var g = ReactDOM.findDOMNode(this).children[0];
            if(g){
                g.remove();
            }
            var inner = this.inner = this.svg.append("g");
            // Create the renderer
            var render = new dagreD3.render();
            
            render.arrows().arrowPoint = function normal(parent, id, edge, type) {
                var marker = parent.append("marker")
                    .attr("id", id)
                    .attr("viewBox", "0 0 10 10")
                    .attr("refX", 5)
                    .attr("refY", 5)
                    .attr("markerUnits", "strokeWidth")
                    .attr("markerWidth", 6)
                    .attr("markerHeight", 6.5)
                    .attr("orient", "auto");

                var path = marker.append("path")
                    .attr("d", "M 0 0 L 10 5 L 0 10 z")
                    .style("stroke-width", 1)
                    .style("stroke-dasharray", "1,0")
                    .style("fill", "grey")
                    .style("stroke", "grey");
                dagreD3.util.applyStyle(path, edge[type + "Style"]);
            };

            render.shapes().img = function circle(parent, bbox, node) {
                var shapeSvg = parent.insert("image")
                    .attr("class", "nodeImage")
                    .attr("xlink:href", function(d) {
                        if (node) {
                            if(node.type === 'spout'){
                                return "images/icon-spout.png";
                            } else if(node.type === 'bolt'){
                                return "images/icon-bolt.png";
                            }
                        }
                    }).attr("x", "-12px")
                    .attr("y", "-12px")
                    .attr("width", "30px")
                    .attr("height", "30px");
                node.intersect = function(point) {
                    return dagreD3.intersect.circle(node, 20, point);
                };
                return shapeSvg;
            }
            this.svg.call(this.zoom).call(this.tooltip);
            // Run the renderer. This is what draws the final graph.
            render(inner, this.g);

            inner.selectAll("g.nodes image")
                .on('mouseover', function(d) {
                    that.tooltip.show(d);
                })
                .on('mouseout', function(d) {
                    that.tooltip.hide();
                });

            inner.selectAll("g.nodes g.label")
                    .attr("transform", "translate(2,-30)");
            // Center the graph
            var initialScale = 1;
            var svgWidth = this.svg[0][0].parentNode.clientWidth;
            var svgHeight = this.svg[0][0].parentNode.clientHeight;
            if(this.linkArray.length > 0){
                this.zoom.translate([(svgWidth - this.g.graph().width * initialScale) / 2, (svgHeight - this.g.graph().height * initialScale) / 2])
                    .scale(initialScale)
                    .event(this.svg);
            }
        },
		syncData: function(data){
			this.nodeArray = [];
            this.linkArray = [];
            this.g = new dagreD3.graphlib.Graph().setGraph({
                nodesep: 50,
                ranksep: 190,
                rankdir: "LR",
                marginx: 20,
                marginy: 20,
                // transition: function transition(selection) {
                //     return selection.transition().duration(500);
                // }
            });
            if(data){
            	var keys = _.keys(data);
            	keys.map(function(key){
            		if(!key.startsWith('__')){
            			data[key].id = key;
                    	data[key].type = data[key][":type"];
                    	this.nodeArray.push(data[key]);
            		}
            	}.bind(this));

            	var spoutObjArr = _.where(this.nodeArray, { "type": "spout" });
	            if (spoutObjArr.length > 1) {
                    for(var i = 0; i < spoutObjArr.length; i++){
                        spoutObjArr[i].x = 50;
                        spoutObjArr[i].y = parseInt(i+'10', 10);
                        spoutObjArr[i].fixed = true;
                    }
	            } else if (spoutObjArr.length == 1) {
	                spoutObjArr[0].x = 50;
	                spoutObjArr[0].y = 100;
	                spoutObjArr[0].fixed = true;
	            }

	            this.nodeArray.map(function(node){
	            	var inputArr = node[":inputs"] || [];
	            	inputArr.map(function(input){
	            		if(!input[":component"].startsWith("__")){
	            			var sourceNode = _.findWhere(this.nodeArray, {id: input[":component"]});
                            this.linkArray.push({
	            				source: sourceNode,
	            				target: node
	            			});
                            this.g.setNode(sourceNode.id, _.extend(sourceNode, {label: sourceNode.id, shape: 'img'}));
                            this.g.setNode(node.id, _.extend(node, {label: node.id, shape: 'img'}));
                            this.g.setEdge(sourceNode.id, node.id, {"arrowhead": 'arrowPoint'});
	            		}
	            	}.bind(this));
	            }.bind(this));
            }
		},
		render: function() {
			return (
				<svg className="topology-graph" width="100%" height="300"></svg>
			);
		},
	});
});