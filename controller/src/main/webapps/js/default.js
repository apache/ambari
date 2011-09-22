function renderCluster(cluster) {
  var buffer = [];
  var i=0;
  buffer[i++]='<a href="/status-cluster.html?cluster='+cluster['clusterName']+'">'+cluster['clusterName']+'</a>';
  buffer[i++]='<a href="'+cluster['nodes']['url']+'">Nodes List</a>';
  buffer[i++]='<a href="'+cluster['software']['url']+'">Software Stack</a>';
  buffer[i++]='<a href="'+cluster['config']['url']+'">Configuration</a>';
  return buffer;
}

function renderCommand(command) {
  var buffer = [];
  var i=0;
  buffer[i++]='<a href="/status-command.html?cmd='+command['id']+'">'+command['id']+'</a>';
  buffer[i++]=command['cmd'];
  return buffer;
}

function basename(path) {
    return path.replace(/\\/g,'/').replace( /.*\//, '' );
}

function dirname(path) {
    return path.replace(/\\/g,'/').replace(/\/[^\/]*$/, '');;
}

