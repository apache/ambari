/*
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

var childProcess = require('child_process');
var net = require('net');
var path = require('path');

var projectDirectory = path.resolve(__dirname, '..');
var brunchExecutable = path.join(projectDirectory, 'node_modules', 'brunch', 'bin', 'brunch');
var child;
var childExited = false;
var closing = false;
var completed = false;
var output = '';
var timeout;

function finish(error) {
  if (completed) {
    return;
  }
  completed = true;
  clearTimeout(timeout);

  if (child && !childExited) {
    child.kill('SIGKILL');
  }

  if (error) {
    console.error(error.stack || error);
    if (output) {
      console.error(output);
    }
    process.exitCode = 1;
  } else {
    console.log('Brunch development server smoke test passed.');
  }
}

function request(port, requestPath, callback) {
  var callbackCalled = false;
  var rawResponse = '';
  function complete(error, response, body) {
    if (callbackCalled) {
      return;
    }
    callbackCalled = true;
    callback(error, response, body);
  }

  var socket = net.connect(port, '127.0.0.1', function() {
    socket.write(
      'GET ' + requestPath + ' HTTP/1.0\r\n' +
      'Host: 127.0.0.1:' + port + '\r\n' +
      'Connection: close\r\n\r\n'
    );
  });
  socket.setEncoding('utf8');
  socket.setTimeout(5000);
  socket.on('data', function(chunk) {
    rawResponse += chunk;
  });
  socket.on('end', function() {
    var separator = rawResponse.indexOf('\r\n\r\n');
    var headerLines;
    var statusMatch;
    var headers = {};
    var index;

    if (separator === -1) {
      complete(new Error('Request ' + requestPath + ' returned an invalid HTTP response'));
      return;
    }
    headerLines = rawResponse.slice(0, separator).split('\r\n');
    statusMatch = /^HTTP\/\d\.\d (\d{3})/.exec(headerLines.shift());
    if (!statusMatch) {
      complete(new Error('Request ' + requestPath + ' returned an invalid status line'));
      return;
    }
    headerLines.forEach(function(line) {
      index = line.indexOf(':');
      if (index !== -1) {
        headers[line.slice(0, index).toLowerCase()] = line.slice(index + 1).trim();
      }
    });
    complete(null, {
      statusCode: parseInt(statusMatch[1], 10),
      headers: headers
    }, rawResponse.slice(separator + 4));
  });
  socket.on('timeout', function() {
    socket.destroy();
    complete(new Error('Request ' + requestPath + ' timed out'));
  });
  socket.on('error', function(error) {
    complete(new Error('Request ' + requestPath + ' failed: ' + error.message));
  });
}

function waitForIndex(port, attempts, callback) {
  request(port, '/index.html', function(error, response, body) {
    if (!error && response.statusCode === 200 && body.indexOf('<title>Ambari</title>') !== -1) {
      callback(null, response, body);
      return;
    }
    if (attempts === 0) {
      callback(error || new Error('Brunch server did not publish index.html'));
      return;
    }
    setTimeout(function() {
      waitForIndex(port, attempts - 1, callback);
    }, 250);
  });
}

function waitForBuild(attempts, callback) {
  if (output.indexOf('compiled ') !== -1) {
    callback();
    return;
  }
  if (attempts === 0) {
    callback(new Error('Brunch did not complete its initial compilation'));
    return;
  }
  setTimeout(function() {
    waitForBuild(attempts - 1, callback);
  }, 250);
}

function verifyClosed(port, callback) {
  setTimeout(function() {
    request(port, '/index.html', function(error) {
      if (!error) {
        finish(new Error('Brunch server still accepts requests after shutdown'));
        return;
      }
      callback();
    });
  }, 100);
}

function stopServer(port, callback) {
  closing = true;
  child.once('exit', function() {
    childExited = true;
    verifyClosed(port, callback);
  });
  child.kill('SIGINT');
}

function verifyMissingFile(port) {
  request(port, '/missing-file.txt', function(error, response) {
    if (error) {
      finish(error);
      return;
    }
    if (response.statusCode !== 404) {
      finish(new Error('Expected missing file to return 404, got ' + response.statusCode));
      return;
    }
    stopServer(port, finish);
  });
}

function verifyHistoryFallback(port) {
  request(port, '/clusters/example', function(error, response, body) {
    if (error) {
      finish(error);
      return;
    }
    if (response.statusCode !== 200 || body.indexOf('<title>Ambari</title>') === -1) {
      finish(new Error(
        'History fallback did not return the application index: status=' +
        response.statusCode + ', body=' + body.slice(0, 120)
      ));
      return;
    }
    stopServer(port, function() {
      findAvailablePort(function(nextPort) {
        startServer(nextPort, true);
      });
    });
  });
}

function verifyStaticIndex(port, noPushState) {
  waitForBuild(240, function(buildError) {
    if (buildError) {
      finish(buildError);
      return;
    }
    waitForIndex(port, 40, function(error, response, body) {
      if (error) {
        finish(error);
        return;
      }
      if ((response.headers['content-type'] || '').indexOf('text/html') !== 0) {
        finish(new Error('Expected index.html to use a text/html content type'));
        return;
      }
      if (noPushState) {
        verifyMissingFile(port);
      } else {
        verifyHistoryFallback(port);
      }
    });
  });
}

function startServer(port, noPushState) {
  var environment = {};
  var key;
  for (key in process.env) {
    if (process.env.hasOwnProperty(key)) {
      environment[key] = process.env[key];
    }
  }
  if (noPushState) {
    environment.AMBARI_BRUNCH_NO_PUSH_STATE = 'true';
  }

  childExited = false;
  closing = false;
  output = '';
  child = childProcess.spawn(process.execPath, [
    brunchExecutable,
    'watch',
    '--server',
    '--port',
    String(port)
  ], {
    cwd: projectDirectory,
    env: environment,
    stdio: ['ignore', 'pipe', 'pipe']
  });

  child.stdout.on('data', function(chunk) {
    output += chunk.toString();
  });
  child.stderr.on('data', function(chunk) {
    output += chunk.toString();
  });
  child.once('error', finish);
  child.once('exit', function(code, signal) {
    childExited = true;
    if (!closing) {
      finish(new Error('Brunch server exited early: code=' + code + ', signal=' + signal));
    }
  });

  verifyStaticIndex(port, noPushState);
}

function findAvailablePort(callback) {
  var portFinder = net.createServer();
  portFinder.once('error', finish);
  portFinder.listen(0, '127.0.0.1', function() {
    var port = portFinder.address().port;
    portFinder.close(function() {
      callback(port);
    });
  });
}

findAvailablePort(function(port) {
  startServer(port, false);
});

timeout = setTimeout(function() {
  finish(new Error('Timed out waiting for Brunch development server verification'));
}, 120000);
