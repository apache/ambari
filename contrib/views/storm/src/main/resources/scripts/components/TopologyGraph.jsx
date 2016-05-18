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

define(['react', 'react-dom', 'd3', 'd3.tip'], function(React, ReactDOM, d3) {
	'use strict';
	return React.createClass({
		displayName: 'TopologyGraph',
		getInitialState: function(){
			this.width = this.props.width || '1100';
			this.height = this.props.height || '260';
			this.syncData(this.props.data);
			return null;
		},
        componentWillUpdate: function(){

        },
		componentDidUpdate: function(){
			this.syncData(this.props.data);
            this.updateGraph();
            this.force.start();
            for (var i = 300; i > 0; --i) this.force.tick();
            this.force.stop();
		},
		componentDidMount: function(){
			var width = this.width,
                height = this.height,
                nodes = this.nodeArray,
                links = this.linkArray,
                radius = this.radius = 20.75;

            var svg = this.svg = d3.select(ReactDOM.findDOMNode(this))
                .attr('width', width)
                .attr('height', height);

            //Set up tooltip
            this.tip = d3.tip()
                .attr('class', function() {
                    return 'd3-tip';
                })
                .offset([-10, 0])
                .html(function(d) {
                    var tip = "<ul>";
                    if (d[":capacity"] !== null) tip += "<li>Capacity: " + d[":capacity"].toFixed(2) + "</li>";
                    if (d[":latency"] !== null) tip += "<li>Latency: " + d[":latency"].toFixed(2) + "</li>";
                    if (d[":transferred"] !== null) tip += "<li>Transferred: " + d[":transferred"].toFixed(2) + "</li>";
                    tip += "</ul>";
                    return tip;
                });
            svg.call(this.tip);

            // define arrow markers for graph links
            svg.append('svg:defs').append('svg:marker')
                .attr('id', 'end-arrow')
                .attr('viewBox', '0 -5 10 10')
                .attr('refX', 6)
                .attr('markerWidth', 6)
                .attr('markerHeight', 6.5)
                .attr('orient', 'auto')
                .append('svg:path')
                .attr('d', 'M0,-5L10,0L0,5');

            svg.append('svg:defs').append('svg:marker')
                .attr('id', 'start-arrow')
                .attr('viewBox', '0 -5 10 10')
                .attr('refX', 4)
                .attr('markerWidth', 3)
                .attr('markerHeight', 3)
                .attr('orient', 'auto')
                .append('svg:path')
                .attr('d', 'M10,-5L0,0L10,5');

            // handles to link and node element groups
            this.path = svg.append('svg:g').selectAll('path');
            this.image = svg.append('svg:g').selectAll('g');

            this.selected_node = null;
			
			// only respond once per keydown
            this.lastKeyDown = -1;
            d3.select(window)
                .on('keydown', this.keydown)
                .on('keyup', this.keyup);
            this.updateGraph();
            this.force.start();
            this.force.tick();
            this.force.stop();
		},
		// update graph (called when needed)
		updateGraph: function(){
			// init D3 force layout
            this.force = d3.layout.force()
                .nodes(this.nodeArray)
                .links(this.linkArray)
                .size([this.width, this.height])
                .linkDistance(150)
                .charge(-500)
                .on('tick', this.tick);

			// path (link) group
            this.path = this.path.data(this.linkArray);

            // update existing links
            this.path.style('marker-start', function(d) {
                    return ''; })
                .style('marker-end', function(d) {
                    return 'url(#end-arrow)'; });


            // add new links
            this.path.enter().append('svg:path')
                .attr('class', 'link')
                .style('marker-start', function(d) {
                    return ''; })
                .attr("stroke-dasharray", "5, 5")
                .attr('stroke-width', '2')
                .style('marker-end', function(d) {
                    return 'url(#end-arrow)'; });

            // remove old links
            this.path.exit().remove();


            // image (node) group
            // NB: the function arg is crucial here! nodes are known by id, not by index!
            this.image = this.image.data(this.nodeArray, function(d) {
                return d.id; 
            });

            //update old nodes
            this.image
                .on('mouseover', function(d) {
                    this.tip.show(d);
                }.bind(this))
                .on('mouseout', function(d) {
                    this.tip.hide();
                }.bind(this));

            // add new nodes
            var g = this.image.enter().append('svg:g');

            g.append('svg:image')
            	.attr("xlink:href", function(d){
					if(d.type === 'spout'){
						return "images/icon-spout.png";
					} else if(d.type === 'bolt'){
						return "images/icon-bolt.png";
					}
				})
				.attr("width", "68px")
				.attr("height", "68px")
                .on('mouseover', function(d) {
                    this.tip.show(d);
                }.bind(this))
                .on('mouseout', function(d) {
                    this.tip.hide();
                }.bind(this));

            g.append("svg:text")
                .attr("dx", 18)
                .attr("dy", 78)
                .text(function(d) {
                    return d.id; });

            // remove old nodes
            this.image.exit().remove();
		},
		// update force layout (called automatically each iteration)
		tick: function(){
			// draw directed edges with proper padding from node centers
            this.path.attr('d', function(d) {
                var deltaX = d.target.x - d.source.x,
                    deltaY = d.target.y - d.source.y,
                    dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
                    normX = deltaX / dist,
                    normY = deltaY / dist,
                    sourcePadding = 68,
                    targetPadding = 5,
                    sourceX = d.source.x + (sourcePadding * normX),
                    sourceY = d.source.y + (sourcePadding * normY) + 34,
                    targetX = d.target.x - (targetPadding * normX),
                    targetY = d.target.y - (targetPadding * normY) + 34;
                return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
            });
            
            this.image.attr('transform', function(d) {
                return 'translate(' + Math.max(this.radius, Math.min(this.width - this.radius, d.x)) + ',' + Math.max(this.radius, Math.min(this.height - this.radius, d.y)) + ')';
            }.bind(this));
		},
		keydown: function(){
			if (this.lastKeyDown !== -1) return;
            this.lastKeyDown = d3.event.keyCode;

            // ctrl
            if (d3.event.keyCode === 17) {
                d3.event.preventDefault();
                this.tip.hide();
                this.image.call(this.force.drag);
                this.svg.classed('ctrl', true);
            }
		},
		keyup: function(){
			this.lastKeyDown = -1;
            // ctrl
            if (d3.event.keyCode === 17) {
                this.image
                    .on('mousedown.drag', null)
                    .on('touchstart.drag', null);
                this.svg.classed('ctrl', false);
            }
		},
		syncData: function(data){
			this.nodeArray = [];
            this.linkArray = [];
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
	            	var index = this.nodeArray.length - 1;
	                this.nodeArray[index].x = 50;
	                this.nodeArray[index].y = 100;
	                this.nodeArray[index].fixed = true;
	            } else if (spoutObjArr.length == 1) {
	                spoutObjArr[0].x = 50;
	                spoutObjArr[0].y = 100;
	                spoutObjArr[0].fixed = true;
	            }

	            this.nodeArray.map(function(node){
	            	var inputArr = node[":inputs"] || [];
	            	inputArr.map(function(input){
	            		if(!input[":component"].startsWith("__")){
	            			this.linkArray.push({
	            				source: _.findWhere(this.nodeArray, {id: input[":component"]}),
	            				target: node
	            			});
	            		}
	            	}.bind(this));
	            }.bind(this));
            }
		},
		render: function() {
			return (
				<svg className="topology-graph"></svg>
			);
		},
	});
});