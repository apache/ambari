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
**/

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';
import d3 from 'd3';
import d3Tip from 'd3-tip';

export default class BarChart extends Component{
  static propTypes = {
    data: PropTypes.array.isRequired,
    width: PropTypes.number,
    height: PropTypes.number
  }

  constructor(props) {
    super(props);
  }

  componentDidMount(){
    this.setUpSVG();
    this.initToolTip();
    this.setLayout();
    this.initSets();
    this.barTypeTransition = this.transitionGrouped;
    this.hiddenLayers = [];
    this.drawBars();
    this.drawXAxis();
    this.drawYAxis();
    this.drawTooltip();
    this.drawLegends();
  }

  initSets(){
    this.layers = this.dataMapY(this.props.data);
    // this.setMax();
    this.setX();
    this.setY();
    this.colorDomain();
    this.setXAxis();
    this.setYAxis();
  }

  setUpSVG(){
    this.svg = d3.select(ReactDOM.findDOMNode(this))
      .attr('width', this.props.width+"px")
      .attr('height', this.props.height+50+"px");
      // .attr("viewBox", "-46 -5 " + (this.props.width+82) + " " + (this.props.height+28) );

    this.container = this.svg.append("g")
      .attr('class', 'svg-container')
      .attr("transform", "translate(50,10)");

    this.tipcontainer = this.svg.append('g').classed('tip-g', true)
      .attr("transform", "translate(" + 40 + "," + 0 + ")");

    this.tipcontainer.append('g').classed('tipLine-g', true).append('line').classed('tipline', true)
      .style('stroke', '#aaa')
      .style('visibility', 'hidden')
      // .style('shape-rendering', 'crispEdges')
      .attr('x1', 0).attr('x2', 0).attr('y1', 0).attr('y2', this.props.height);
  }

  initToolTip() {
    let self = this;
    this.tip = d3Tip()
      .attr('class', 'd3-tip')
      .offset([-10, 0])
      .html(function(d) {
        return self.toolTipHtml.call(self, d);
      });
    this.svg.call(this.tip);
    // const container = document.getElementById('app_container');
    // container.append($('body > .d3-tip'));
  }

  setMax() {
    this.yGroupMax = d3.max(this.layers, function(layer) {
      return d3.max(layer, function(d) {
        return d.y;
      });
    });
    this.yGroupMin = d3.min(this.layers, function(layer) {
      return d3.min(layer, function(d) {
        return d.y;
      });
    });
    this.yStackMax = d3.max(this.layers, function(layer) {
      return d3.max(layer, function(d) {
        return d.y0 + d.y;
      });
    });
    this.yStackMin = d3.min(this.layers, function(layer) {
      return d3.min(layer, function(d) {
        return d3.min([d.y0, d.y]);
      });
    });
  }

  setX() {
    let self = this;
    this.x = d3.scale.ordinal()
      .domain(self.layers[0].map(function(d) {
        return d.x;
      }))
      .rangeRoundBands([0, this.props.width], 0.08);
  }

  setY() {
    this.y = d3.scale.linear()
      .domain([this.yStackMin, this.yStackMax])
      .range([this.props.height, 0]);
  }

  setXAxis() {
    this.xAxis = d3.svg.axis().scale(this.x).orient("bottom");
  }

  setYAxis() {
    let formatValue = d3.format('.2s');
    this.yAxis = d3.svg
      .axis()
      .scale(this.y)
      .orient("left")
      .tickFormat(function(d){return formatValue(d);});
  }

  drawXAxis(xAxis, container, height) {
    let xA = xAxis || this.xAxis,
      containor = container || this.container,
      hght = height || this.props.height;

    this.xAxisGrp = containor['xAxisEl'] = containor.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + hght + ")")
      .call(xA)
      .selectAll(".tick text")
      .call(this.wrap, this.x.rangeBand());
  }

  wrap(text, width) {
    text.each(function() {
      let text = d3.select(this),
        words = text.text().split(/-+/).reverse(),
        word,
        line = [],
        lineNumber = 0,
        lineHeight = 1.1, // ems
        y = text.attr("y"),
        dy = parseFloat(text.attr("dy")),
        tspan = text.text(null).append("tspan").attr("x", 0).attr("y", y).attr("dy", dy + "em");

      //Hack to show hidden div to find getComputedTextLength
      // $('#lag-graph').css({visibility: 'hidden', display: 'block', position: 'absolute'});

      while (word = words.pop()) {
        line.push(word);
        tspan.text(line.join(" "));
        if (tspan.node().getComputedTextLength() > width) {
          line.pop();
          tspan.text(line.join(" "));
          line = [word];
          tspan = text.append("tspan").attr("x", 0).attr("y", y).attr("dy", ++lineNumber * lineHeight + dy + "em").text(word);
        }
      }
      // $('#lag-graph').css({visibility: '', display: 'none', position: ''});
    });
  }

  drawYAxis(x) {
    let yAxis = this.yAxis;
    this.yAxisGrp = this.container.append("g")
      .attr("class", "y axis");
    this.yAxisGrp.ticks = this.yAxisGrp.call(yAxis);
    this.yAxisGrp.append('text')
      .text(this.props.yAttr[0].toUpperCase() + this.props.yAttr.substr(1,this.props.yAttr.length)).attr("text-anchor", "end")
      .attr("y", 6)
      .attr("dy", ".75em")
      .attr("transform", "rotate(-90)");
  }

  dataMapY(data) {
    let self = this;
    let keys = d3.keys(data[0]).filter(function(key) {
      return key !== self.props.xAttr;
    });
    let layers = this.stack(keys.map(function(yAttr) {
      return data.map(function(d) {
        return {
          x: d[self.props.xAttr],
          y: d[yAttr],
          type: yAttr
        };
      });
    }));
    let allLayers = layers.allLayers = [];
    layers.forEach(function(d) {
      allLayers.push(d);
    });
    return layers;
  }

  setLayout() {
    let self = this;
    this.stack = d3.layout.stack();
  }

  colorDomain() {
    let self = this;
    this.color = d3.scale.ordinal()
      .range(["#b9cde5", "#1B76BB"]);
    // this.color = d3.scale.category20c();
    // this.color.domain(d3.keys(this.props.data[0]).filter(function(key) {
    // 	return key !== self.props.xAttr;
    // }));
  }

  drawBars() {
    let self = this;

    this.layers_g = this.container.selectAll(".barLayer")
      .data(this.layers);

    this.layers_g
      .exit()
      .remove();

    this.layers_g
      .enter().append("g")
      .attr("class", "barLayer")
      .style("fill", function(d, i) {
        return self.color(d[0].type);
      });

    this.rect = this.layers_g.selectAll("rect")
      .data(function(d) {
        return d;
      });

    this.rect
      .exit()
      .remove();

    this.rect
      .enter().append("rect")
      .attr("x", function(d) {
        return self.x(d.x);
      })
      .attr("y", function(d) {
        return self.props.height;
      })
      .attr("width", function(d) {
        return self.x.rangeBand();
      })
      .classed("visible", true)
      .attr("height", function(d) {
        return 0;
      });

    this.barTypeTransition();
  }

  transitionGrouped() {
    let x = this.x,
      y = this.y,
      height = this.props.height,
      n = this.layers.length;
    this.setMax();
    let yMin = this.yGroupMin < 0 ? this.yGroupMin : 0;
    this.y.domain([yMin, this.yGroupMax]);

    let barWidth = (x.rangeBand() / n > 25) ? 25 : x.rangeBand() / n;
    let xArr = new Array(n);
    this.layers_g.selectAll('rect.visible')
      .attr("x", function(d, i, j) {
        if (xArr[i] == undefined) {
          xArr[i] = x(d.x) + (x.rangeBand() / 2) - (n / 2 * barWidth);
        } else {
          xArr[i] += barWidth;
        }
        return xArr[i];
      })
      .attr("width", barWidth)
      .transition().duration(500)
      .attr("y", function(d) {
        let _y = y(d.y);
        if (d.y < 0){
          _y = y(d.y) - (height - y(0));
        }
        return _y;
      })
      .attr("height", function(d) {
        return (height - y(Math.abs(d.y))) - (height - y(0));
      });
    this.container.select(".y.axis").transition().duration(500).call(this.yAxis);
  }

  transitionStacked() {
    this.stack(this.layers);
    let x = this.x,
      y = this.y,
      height = this.props.height,
      self = this,
      n = this.layers.length;
    this.setMax();
    this.y.domain([this.yStackMin, this.yStackMax]);

    let barWidth = (x.rangeBand() / n > 25) ? 25 : x.rangeBand() / n;
    let xArr = new Array(n);
    this.layers_g.selectAll('rect.visible').transition().duration(500)
      .attr("y", function(d) {
        let _y = y(d.y0 + d.y);
        if (d.y < 0){
          _y = y(d.y) - Math.abs(y(d.y0) - y(d.y0 + d.y));
        }
        return _y;
      })
      .attr("height", function(d) {
        return Math.abs(y(d.y0) - y(d.y0 + d.y));
      })
      .attr("x", function(d, i, j) {
        xArr[i] = x(d.x) + (x.rangeBand() / 2) - (barWidth / 2);
        return xArr[i];
      })
      .attr("width", barWidth);
    this.container.select(".y.axis").transition().duration(500).call(this.yAxis);
  }

  drawTooltip() {
    let self = this;
    let x = this.x.rangeBand ? this.x : d3.scale.ordinal()
      .domain(self.data.map(function(d) {
        return d[self.props.xAttr];
      }))
      .rangeRoundBands([0, this.props.width]);

    let tipline = this.tipcontainer.select('.tipline');

    this.tipcontainer.append('g').classed('tipRect-g', true).selectAll(".tipRect")
      .data(this.props.data)
      .enter().append("rect")
      .attr("class", "tipRect")
      .style('opacity', '0')
      .attr("x", function(d) {
        return self.x(d[self.props.xAttr]);
      })
      .attr("width", function() {
        return x.rangeBand();
      })
      .attr("y", function(d) {
        return 0;
      })
      .attr("height", function(d) {
        return self.props.height;
      })
      .on('mouseover', function(d) {
        let x1 = parseInt(d3.select(this).attr('x')) + parseInt((x.rangeBand() / 2));
        tipline.attr('x1', x1).attr('x2', x1);
        tipline.style('visibility', 'visible');
        return self.tip.show(d,this);
      })
      .on('mouseout', function(d) {
        tipline.style('visibility', 'hidden');
        return self.tip.hide(d,this);
      });
  }

  toolTipHtml(d) {
    let self = this;
    let html = d[self.props.xAttr] + '<table><tbody>';
    _.each(d, function(val, key) {
      if (key != self.props.xAttr){
        html += '<tr><td>' + key + ' </td><td> ' + val + '</td></tr>';
      }
    });
    html += '</tbody></table>';
    return html;
  }

  drawLegends() {
    let self = this;
    let legends = this.legendsEl = document.createElement('ul');
    legends = d3.select(legends)
      .attr('class', 'legends')
      .style('list-style', 'none');

    let legend = legends.selectAll('.legend')
      .data(this.color.domain())
      .enter()
      .append('li')
      .attr('class', 'legend');

    legend.append('div')
      .style('width', '10px')
      .style('height', '10px')
      .style('display', 'inline-block')
      .style('background-color', function(d) {
        return self.color(d);
      });

    legend.append('span')
      .style('padding', '4px 0 4px 4px')
      .text(function(d) {
        return d;
      });
  }

  render() {
    return (
      <svg></svg>
    );
  }
}
