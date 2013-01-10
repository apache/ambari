/*
 * D3 Sankey diagram 
 * another type to display graph
 */
d3.sankey = function () {
  var sankey = {},
    nodeWidth = 24,
    nodePadding = 8,
    size = [1, 1],
    nodes = [],
    links = [],
    overlapLinksAtSources = false,
    overlapLinksAtTargets = false,
    minValue = 1;

  sankey.nodeWidth = function (_) {
    if (!arguments.length) return nodeWidth;
    nodeWidth = +_;
    return sankey;
  };

  sankey.nodePadding = function (_) {
    if (!arguments.length) return nodePadding;
    nodePadding = +_;
    return sankey;
  };

  sankey.nodes = function (_) {
    if (!arguments.length) return nodes;
    nodes = _;
    return sankey;
  };

  sankey.links = function (_) {
    if (!arguments.length) return links;
    links = _;
    return sankey;
  };

  sankey.size = function (_) {
    if (!arguments.length) return size;
    size = _;
    return sankey;
  };

  sankey.overlapLinksAtSources = function (_) {
    if (!arguments.length) return overlapLinksAtSources;
    overlapLinksAtSources = _;
    return sankey;
  };

  sankey.overlapLinksAtTargets = function (_) {
    if (!arguments.length) return overlapLinksAtTargets;
    overlapLinksAtTargets = _;
    return sankey;
  };

  sankey.minValue = function (_) {
    if (!arguments.length) return minValue;
    minValue = _;
    return sankey;
  };

  sankey.layout = function (iterations) {
    computeNodeLinks();
    computeNodeValues();
    computeNodeBreadths();
    computeNodeDepths(iterations);
    computeLinkDepths();
    return sankey;
  };

  sankey.relayout = function () {
    computeLinkDepths();
    return sankey;
  };

  sankey.link = function () {
    var curvature = .5;

    function link(d) {
      var x0 = d.source.x + d.source.dx,
        x1 = d.target.x,
        xi = d3.interpolateNumber(x0, x1),
        x2 = xi(curvature),
        x3 = xi(1 - curvature),
        y0 = d.source.y + (overlapLinksAtSources ? 0 : d.sy) + d.dy / 2,
        y1 = d.target.y + (overlapLinksAtTargets ? 0 : d.ty) + d.dy / 2;
      return "M" + x0 + "," + y0
        + "C" + x2 + "," + y0
        + " " + x3 + "," + y1
        + " " + x1 + "," + y1;
    }

    link.curvature = function (_) {
      if (!arguments.length) return curvature;
      curvature = +_;
      return link;
    };

    return link;
  };

  // Populate the sourceLinks and targetLinks for each node.
  // Also, if the source and target are not objects, assume they are indices.
  function computeNodeLinks() {
    nodes.forEach(function (node) {
      node.sourceLinks = [];
      node.targetLinks = [];
    });
    links.forEach(function (link) {
      var source = link.source,
        target = link.target;
      if (typeof source === "number") source = link.source = nodes[link.source];
      if (typeof target === "number") target = link.target = nodes[link.target];
      source.sourceLinks.push(link);
      target.targetLinks.push(link);
      if ("value" in link)
        link.value = Math.max(link.value, minValue);
      else
        link.value = minValue;
    });
  }

  // Compute the value (size) of each node by summing the associated links.
  function computeNodeValues() {
    nodes.forEach(function (node) {
      if ("value" in node)
        node.value = Math.max(node.value, minValue);
      else
        node.value = minValue;
      if (node.sourceLinks.length > 0) {
        if (overlapLinksAtSources)
          node.value = Math.max(node.value, d3.max(node.sourceLinks, value));
        else
          node.value = Math.max(node.value, d3.sum(node.sourceLinks, value));
      }
      if (node.targetLinks.length > 0) {
        if (overlapLinksAtTargets)
          node.value = Math.max(node.value, d3.max(node.targetLinks, value));
        else
          node.value = Math.max(node.value, d3.sum(node.targetLinks, value));
      }
    });
  }

  // Iteratively assign the breadth (x-position) for each node.
  // Nodes are assigned the maximum breadth of incoming neighbors plus one;
  // nodes with no incoming links are assigned breadth zero, while
  // nodes with no outgoing links are assigned the maximum breadth.
  function computeNodeBreadths() {
    var remainingNodes = nodes,
      nextNodes,
      x = 0;

    while (remainingNodes.length) {
      nextNodes = [];
      remainingNodes.forEach(function (node) {
        node.x = x;
        node.dx = nodeWidth;
        node.sourceLinks.forEach(function (link) {
          nextNodes.push(link.target);
        });
      });
      remainingNodes = nextNodes;
      ++x;
    }

    //
    moveSinksRight(x);
    scaleNodeBreadths((size[0] - nodeWidth) / (x - 1));
  }

  function moveSourcesRight() {
    nodes.forEach(function (node) {
      if (!node.targetLinks.length) {
        node.x = d3.min(node.sourceLinks, function (d) {
          return d.target.x;
        }) - 1;
      }
    });
  }

  function moveSinksRight(x) {
    nodes.forEach(function (node) {
      if (!node.sourceLinks.length) {
        node.x = x - 1;
      }
    });
  }

  function scaleNodeBreadths(kx) {
    nodes.forEach(function (node) {
      node.x *= kx;
    });
  }

  function computeNodeDepths(iterations) {
    var nodesByBreadth = d3.nest()
      .key(function (d) {
        return d.x;
      })
      .sortKeys(d3.ascending)
      .entries(nodes)
      .map(function (d) {
        return d.values;
      });

    //
    initializeNodeDepth();
    resolveCollisions();
    for (var alpha = 1; iterations > 0; --iterations) {
      relaxRightToLeft(alpha *= .99);
      resolveCollisions();
      relaxLeftToRight(alpha);
      resolveCollisions();
    }

    function initializeNodeDepth() {
      var ky = d3.min(nodesByBreadth, function (nodes) {
        return (size[1] - (nodes.length - 1) * nodePadding) / d3.sum(nodes, value);
      });

      nodesByBreadth.forEach(function (nodes) {
        nodes.forEach(function (node, i) {
          node.y = i;
          node.dy = node.value * ky;
        });
      });

      links.forEach(function (link) {
        link.dy = link.value * ky;
      });
    }

    function relaxLeftToRight(alpha) {
      nodesByBreadth.forEach(function (nodes, breadth) {
        nodes.forEach(function (node) {
          if (node.targetLinks.length) {
            var y = d3.sum(node.targetLinks, weightedSource) / d3.sum(node.targetLinks, value);
            node.y += (y - center(node)) * alpha;
          }
        });
      });

      function weightedSource(link) {
        return center(link.source) * link.value;
      }
    }

    function relaxRightToLeft(alpha) {
      nodesByBreadth.slice().reverse().forEach(function (nodes) {
        nodes.forEach(function (node) {
          if (node.sourceLinks.length) {
            var y = d3.sum(node.sourceLinks, weightedTarget) / d3.sum(node.sourceLinks, value);
            node.y += (y - center(node)) * alpha;
          }
        });
      });

      function weightedTarget(link) {
        return center(link.target) * link.value;
      }
    }

    function resolveCollisions() {
      nodesByBreadth.forEach(function (nodes) {
        var node,
          dy,
          y0 = 0,
          n = nodes.length,
          i;

        // Push any overlapping nodes down.
        nodes.sort(ascendingDepth);
        for (i = 0; i < n; ++i) {
          node = nodes[i];
          dy = y0 - node.y;
          if (dy > 0) node.y += dy;
          y0 = node.y + node.dy + nodePadding;
        }

        // If the bottommost node goes outside the bounds, push it back up.
        dy = y0 - nodePadding - size[1];
        if (dy > 0) {
          y0 = node.y -= dy;

          // Push any overlapping nodes back up.
          for (i = n - 2; i >= 0; --i) {
            node = nodes[i];
            dy = node.y + node.dy + nodePadding - y0;
            if (dy > 0) node.y -= dy;
            y0 = node.y;
          }
        }
      });
    }

    function ascendingDepth(a, b) {
      return a.y - b.y;
    }
  }

  function computeLinkDepths() {
    nodes.forEach(function (node) {
      node.sourceLinks.sort(ascendingTargetDepth);
      node.targetLinks.sort(ascendingSourceDepth);
    });
    nodes.forEach(function (node) {
      var sy = 0, ty = 0;
      node.sourceLinks.forEach(function (link) {
        link.sy = sy;
        sy += link.dy;
      });
      node.targetLinks.forEach(function (link) {
        link.ty = ty;
        ty += link.dy;
      });
    });

    function ascendingSourceDepth(a, b) {
      return a.source.y - b.source.y;
    }

    function ascendingTargetDepth(a, b) {
      return a.target.y - b.target.y;
    }
  }

  function center(node) {
    return node.y + node.dy / 2;
  }

  function value(link) {
    return link.value;
  }

  return sankey;
};
/*
 * Example usage:
 *
 *  var dv = new DagViewer(false,'pig_5')
 *  .setPhysicalParametrs(width,height[,charge,gravity])
 *  .setData(dagSchema [,jobsData])
 *  .drawDag([nodeSize,largeNodeSize,linkDistance]);
 */
function DagViewer(type, domId) {
  // initialize variables and force layout
  this._nodes = new Array();
  this._links = new Array();
  this._numNodes = 0;
  this._type = type;
  this._id = domId;
}
DagViewer.prototype.setPhysicalParametrs = function (w, h, charge, gravity) {
  this._w = w;
  this._h = h;
  this._gravity = gravity || 0.1;
  this._charge = charge || -1000;
  this._force = d3.layout.force()
    .size([w, h])
    .gravity(this._gravity)
    .charge(this._charge);
  return this;
}

//set workflow schema
DagViewer.prototype.setData = function (wfData, jobData) {
  // create map from entity names to nodes
  var existingNodes = new Array();
  var jobData = (jobData) ? jobData : new Array();
  // iterate through job data
  for (var i = 0; i < jobData.length; i++) {
    this._addNode(existingNodes, jobData[i].entityName, jobData[i]);
  }
  var dag = eval('(' + wfData + ')').dag;
  // for each source node in the context, create links between it and its target nodes
  for (var source in dag) {
    var sourceNode = this._getNode(source, existingNodes);
    for (var i = 0; i < dag[source].length; i++) {
      var targetNode = this._getNode(dag[source][i], existingNodes);
      this._addLink(sourceNode, targetNode);
    }
  }
  return this;
}
// add a node to the nodes array and to a provided map of entity names to nodes
DagViewer.prototype._addNode = function (existingNodes, entityName, node) {
  existingNodes[entityName] = node;
  this._nodes.push(node);
  this._numNodes++;
}

// add a link between sourceNode and targetNode
DagViewer.prototype._addLink = function (sourceNode, targetNode) {
  var status = false;
  if (sourceNode.status && targetNode.status)
    status = true;
  this._links.push({"source":sourceNode, "target":targetNode, "status":status, "value":sourceNode.output});
}

// get the node for an entity name, or add it if it doesn't exist
// called after job nodes have all been added
DagViewer.prototype._getNode = function (entityName, existingNodes) {
  if (!(entityName in existingNodes))
    this._addNode(existingNodes, entityName, { "name":entityName, "status":false, "input":1, "output":1});
  return existingNodes[entityName];
}
// display the graph
DagViewer.prototype.drawDag = function (nodeSize, largeNodeSize, linkDistance) {
  this._nodeSize = nodeSize || 18;
  this._largeNodeSize = largeNodeSize || 30;
  this._linkDistance = linkDistance || 100;
  // add new display to specified div
  this._svg = d3.select("div#" + this._id).append("svg:svg")
    .attr("width", this._w)
    .attr("height", this._h);
  // add sankey diagram or graph depending on type
  if (this._type)
    this._addSankey();
  else
    this._addDag();
  return this;
}
//draw the graph 
DagViewer.prototype._addDag = function () {
  var w = this._w;
  var h = this._h;
  var nodeSize = this._nodeSize;
  var largeNodeSize = this._largeNodeSize;
  var linkDistance = this._linkDistance;
  // add nodes and links to force layout
  this._force.nodes(this._nodes)
    .links(this._links)
    .linkDistance(this._linkDistance);

  // defs for arrowheads marked as to whether they link finished jobs or not
  this._svg.append("svg:defs").selectAll("marker")
    .data(["finished", "unfinished"])
    .enter().append("svg:marker")
    .attr("id", String)
    .attr("viewBox", "0 -5 10 10")
    .attr("refX", nodeSize + 10)
    .attr("refY", 0)
    .attr("markerWidth", 6)
    .attr("markerHeight", 6)
    .attr("orient", "auto")
    .append("svg:path")
    .attr("d", "M0,-5L10,0L0,5");

  // create links between the nodes
  var lines = this._svg.append("svg:g").selectAll("line")
    .data(this._links)
    .enter().append("svg:line")
    .attr("class", function (d) {
      return "link" + (d.status ? " finished" : "");
    })
    .attr("marker-end", function (d) {
      return "url(#" + (d.status ? "finished" : "unfinished") + ")";
    });

  // create a circle for each node
  var circles = this._svg.append("svg:g").selectAll("circle")
    .data(this._nodes)
    .enter().append("svg:circle")
    .attr("r", nodeSize)
    .attr("class", function (d) {
      return "node " + (d.status ? " finished" : "");
    })
    .attr("id", function (d) {
      return d.name;
    })
    .on("dblclick", click)
    .call(this._force.drag);

  // create text group for each node label
  var text = this._svg.append("svg:g").selectAll("g")
    .data(this._nodes)
    .enter().append("svg:g");

  // add a shadow copy of the node label (will have a lighter color and thicker
  // stroke for legibility
  text.append("svg:text")
    .attr("x", nodeSize + 3)
    .attr("y", ".31em")
    .attr("class", "shadow")
    .text(function (d) {
      return d.name;
    });

  // add the main node label
  text.append("svg:text")
    .attr("x", nodeSize + 3)
    .attr("y", ".31em")
    .text(function (d) {
      return d.name;
    });

  // add mouseover actions
  this._addMouseoverSelection(circles);

  // start the force layout
  this._force.on("tick", tick)
    .start();

  // on force tick, adjust positions of nodes, links, and text
  function tick() {
    circles.attr("transform", function (d) {
      if (d.x < largeNodeSize) d.x = largeNodeSize;
      if (d.y < largeNodeSize) d.y = largeNodeSize;
      if (d.x > w - largeNodeSize) d.x = w - largeNodeSize;
      if (d.y > h - largeNodeSize) d.y = h - largeNodeSize;
      return "translate(" + d.x + "," + d.y + ")";
    });

    lines.attr("x1", function (d) {
      return d.source.x
    })
      .attr("y1", function (d) {
        return d.source.y
      })
      .attr("x2", function (d) {
        return d.target.x
      })
      .attr("y2", function (d) {
        return d.target.y
      });

    text.attr("transform", function (d) {
      return "translate(" + d.x + "," + d.y + ")";
    });
  }

  // on double click, fix node in place or release it
  function click() {
    d3.select(this).attr("fixed", function (d) {
      if (d.fixed) {
        d.fixed = false
      } else {
        d.fixed = true
      }
      return d.fixed;
    });
  }
}
//define mouseover action on nodes
DagViewer.prototype._addMouseoverSelection = function (nodes) {
  var nodeSize = this._nodeSize;
  var largeNodeSize = this._largeNodeSize;
  // on mouseover, change size of node 
  nodes.on("mouseover", function (d) {
    d3.select(this).transition().attr("r", largeNodeSize);
  })
    .on("mouseout", function (d) {
      d3.select(this).transition().attr("r", nodeSize);
    });
}
//draw Sankey diagram
DagViewer.prototype._addSankey = function () {
  var w = this._w;
  var h = this._h;

  // add svg group
  var svgg = this._svg.append("g");

  var color = d3.scale.category20();

  // create sankey
  var sankey = d3.sankey()
    .nodeWidth(15)
    .nodePadding(10)
    .size([w, h * 0.67]);

  // get sankey links
  var spath = sankey.link();

  // set sankey nodes and links and calculate their positions and sizes
  sankey
    .nodes(this._nodes)
    .links(this._links)
    .overlapLinksAtSources(true)
    .layout(32);

  // create links and set their attributes
  var slink = svgg.append("g").selectAll(".link")
    .data(this._links)
    .enter().append("path")
    .attr("class", "slink")
    .attr("d", spath)
    .style("stroke-width", function (d) {
      return Math.max(1, d.dy);
    })
    .sort(function (a, b) {
      return b.dy - a.dy;
    });

  // add mouseover text to links
  slink.append("title")
    .text(function (d) {
      return d.source.name + " - " + d.target.name + ": " + d.value;
    });

  // create node groups, set their attributes, and enable vertical dragging
  var snode = svgg.append("g").selectAll(".node")
    .data(this._nodes)
    .enter().append("g")
    .attr("class", "snode")
    .attr("transform", function (d) {
      return "translate(" + d.x + "," + d.y + ")";
    })
    .call(d3.behavior.drag()
    .origin(function (d) {
      return d;
    })
    .on("dragstart", function () {
      this.parentNode.appendChild(this);
    })
    .on("drag", dragmove));

  // add rectangles to node groups
  snode.append("rect")
    .attr("height", function (d) {
      return d.dy;
    })
    .attr("width", sankey.nodeWidth())
    .style("fill", function (d) {
      return d.color = color(d.name.replace(/ .*/, ""));
    })
    .style("stroke", function (d) {
      return d3.rgb(d.color).darker(2);
    })
    .append("title")
    .text(function (d) {
      return "info" in d ? d.info.join("\n") : d.name;
    });

  // add node labels
  snode.append("text")
    .attr("x", -6)
    .attr("y", function (d) {
      return d.dy / 2;
    })
    .attr("dy", ".35em")
    .attr("text-anchor", "end")
    .attr("transform", null)
    .text(function (d) {
      return d.name;
    })
    .filter(function (d) {
      return d.x < w / 2;
    })
    .attr("x", 6 + sankey.nodeWidth())
    .attr("text-anchor", "start");

  // add mouseover actions
  this._addMouseoverSelection(snode);

  // enable vertical dragging with recalculation of link placement
  function dragmove(d) {
    d3.select(this).attr("transform", "translate(" + d.x + "," + (d.y = Math.max(0, Math.min(h - d.dy, d3.event.y))) + ")");
    sankey.relayout();
    slink.attr("d", spath);
  }
}
