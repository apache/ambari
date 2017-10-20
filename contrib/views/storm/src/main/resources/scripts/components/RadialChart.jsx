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
		displayName: 'RadialChart',
		propTypes: {
			data: React.PropTypes.array.isRequired,
			labels: React.PropTypes.array.isRequired,
			width: React.PropTypes.string,
			height: React.PropTypes.string,
			innerRadius: React.PropTypes.string.isRequired,
			outerRadius: React.PropTypes.string.isRequired,
			color: React.PropTypes.array
		},
		getInitialState: function(){
			this.const = {
				tau: 2 * Math.PI,
				width: this.props.width || "44",
				height: this.props.height || "52",
				innerRadius: parseInt(this.props.innerRadius, 10) || 20,
				outerRadius: parseInt(this.props.outerRadius, 10) || 25,
				color: this.props.color || d3.scale.category20()
			};
			this.arc = d3.svg.arc()
		    .innerRadius(this.const.innerRadius)
		    .outerRadius(this.const.outerRadius)
		    .startAngle(0);

			return null;
		},
		componentDidUpdate: function(){
			this.animateGraph();
		},
		componentDidMount: function(){
			this.tip = d3.tip()
				.attr('class', 'd3-tip')
				.offset([-10, 0])
				.html(function() {
					var text = "<div class='summary'>"+this.props.labels[0]+": "+this.props.data[0]+"</div>";
						text += "<div class='summary'>Free: "+(parseInt(this.props.data[1],10) - parseInt(this.props.data[0],10))+"</div>";
						text += "<div class='summary'>"+this.props.labels[1]+": "+this.props.data[1]+"</div>";
					return text;
				}.bind(this));
			var svg = this.svg = d3.select(ReactDOM.findDOMNode(this))
				.attr('width', this.const.width+"px")
				.attr('height', this.const.height+"px")
				.append('g').attr('transform', 'translate('+(this.const.width/2)+', '+(this.const.height/2)+')');
			
			this.text = svg.append("text")
				.attr("y", "0.3em")
				.attr("class","graphVal")
			    .attr("text-anchor", "middle")
			    .attr("font-size", this.const.fontSize)
			    .on("mouseover", function(d){
			    	this.tip.show();
			    }.bind(this))
			    .on("mouseout", function(d){
			    	this.tip.hide();
			    }.bind(this))
			    .text("0");
			
			var background = svg.append("path")
			    .datum({endAngle: this.const.tau})
			    .style("fill", this.const.color[0])
			    .attr("d", this.arc);
			
			this.foreground = svg.append("path")
			    .datum({endAngle: 0})
			    .style("fill", function(d, i) { return this.const.color[1]; }.bind(this))
			    .attr("d", this.arc);
			this.svg.call(this.tip);
			$('#container').append($('body > .d3-tip'));
			this.animateGraph();
		},
		animateGraph: function(){
			var percent = (parseInt(this.props.data[0],10)/parseInt(this.props.data[1],10)*100);
			if(percent){
				percent = percent.toFixed(0)+' %';
			} else {
				percent = '0 %';
			}

			d3.select(ReactDOM.findDOMNode(this)).select('.graphVal').text(percent);

			var newValue = this.props.data[0] / this.props.data[1] * 100;
			this.foreground.transition()
			    .duration(750)
			    .call(this._arcTween, this.const.tau * (newValue/100));
		},
		_arcTween: function(transition, newAngle) {
		  var arc = this.arc;
		  transition.attrTween("d", function(d) {
		    var interpolate = d3.interpolate(d.endAngle, newAngle);
		    return function(t) {
		      d.endAngle = interpolate(t);
		      if(!d.endAngle){
		      	d.endAngle = 0;
		      }
		      return arc(d);
		    };
		     
		  });
		},
		render: function() {
			return (
				<svg className="radial-chart"></svg>
			);
		},
	});
});