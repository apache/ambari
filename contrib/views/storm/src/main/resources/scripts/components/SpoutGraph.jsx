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

define(['react',
	'react-dom'
	], function(React, ReactDOM){
	'use strict';
	return React.createClass({
		displayName: 'SpoutGraph',
		getInitialState: function(){
			this.syncData();
			this.fields = ['', 'Acked', 'Failed', 'Emitted', 'Transferred'];
			this.colors = ['#0000b4','#0082ca','#0094ff','#0d4bcf'];
			this.grid = d3.range(10).map(function(i){
				return {'x1':0,'y1':0,'x2':0,'y2':240};
			});

			this.tickVals = this.grid.map(function(d,i){
				if(i>0){ return i*10; }
				else if(i===0){ return "100";}
			});

			this.xscale = d3.scale.linear()
						.domain([10,250])
						.range([0,350]);

			this.yscale = d3.scale.linear()
							.domain([0,this.fields.length])
							.range([0,600]);

			this.colorScale = d3.scale.quantize()
							.domain([0,this.fields.length])
							.range(this.colors);
			return null;
		},
		syncData: function(){
			this.values = [this.props.spout.acked, this.props.spout.failed,
							this.props.spout.emitted, this.props.spout.transferred];
			console.log(this.values);
		},
		componentDidMount: function(){
			this.renderGraph();
		},
		componentWillUpdate: function(){
			this.syncData();
		},
		componentDidUpdate: function(){

		},
		renderGraph: function(){
			this.canvas = d3.select(ReactDOM.findDOMNode(this))
							.attr({'width':640,'height':400});

			this.grids = this.canvas.append('g')
							  .attr('transform','translate(150,10)')
							  .selectAll('line')
							  .data(this.grid)
							  .enter()
							  .append('line')
							  .attr({'x1':function(d,i){ return i*30; },
									 'y1':function(d){ return d.y1; },
									 'x2':function(d,i){ return i*30; },
									 'y2':function(d){ return d.y2; },
								})
							  .style({'stroke':'#adadad','stroke-width':'1px'});
			var	xAxis = d3.svg.axis();
				xAxis
					.orient('bottom')
					.scale(this.xscale)
					.tickValues(this.tickVals);

			var	yAxis = d3.svg.axis();
				yAxis
					.orient('left')
					.scale(this.yscale)
					.tickSize(2)
					.tickFormat(function(d,i){ return this.fields[i]; }.bind(this))
					.tickValues(d3.range(this.fields.length));

			var y_xis = this.canvas.append('g')
							  .attr("transform", "translate(150,0)")
							  .call(yAxis);

			var x_xis = this.canvas.append('g')
							  .attr("transform", "translate(150,480)")
							  .call(xAxis);

			var chart = this.canvas.append('g')
								.attr("transform", "translate(150,0)")
								.attr('class','bars')
								.selectAll('rect')
								.data(this.values)
								.enter()
								.append('rect')
								.attr('height',19)
								.attr({'x':0,'y':function(d,i){ return this.yscale(i)+19; }.bind(this)})
								.style('fill',function(d,i){ return this.colorScale(i); }.bind(this))
								.attr('width',function(d){ return 0; });


			var transit = d3.select("svg").selectAll("rect")
							    .data(this.values)
							    .transition()
							    .duration(1000) 
							    .attr("width", function(d) {return this.xscale(d); }.bind(this));

			var transitext = this.canvas.select('.bars')
								.selectAll('text')
								.data(this.values)
								.enter()
								.append('text')
								.attr({'x':function(d) {return this.xscale(d)-200; }.bind(this),'y':function(d,i){ return this.yscale(i)+35; }.bind(this)})
								.text(function(d){ return d; }).style({'fill':'#fff','font-size':'14px'});
		},
		render: function(){
			return (
				<svg className="spout-chart"></svg>
			);
		}
	});
});