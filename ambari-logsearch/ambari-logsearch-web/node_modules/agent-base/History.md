
2.1.0 / 2017-05-26
==================

  * unref is not supported for node < 0.9.1 (@pi0)
  * add tests to dangling socket (@pi0)
  * check unref() is supported (@pi0)
  * fix dangling sockets problem (@pi0)
  * add basic "ws" module tests
  * make `Agent` be subclassable
  * turn `addRequest()` into a named function
  * test: Node.js v4 likes to call `cork` on the stream (#3, @tomhughes)
  * travis: test node v4, v5, v6 and v7

2.0.1 / 2015-09-10
==================

  * package: update "semver" to v5.0.1 for WebPack (#1, @vhpoet)

2.0.0 / 2015-07-10
==================

  * refactor to patch Node.js core for more consistent `opts` values
  * ensure that HTTP(s) default port numbers are always given
  * test: use ssl-cert-snakeoil SSL certs
  * test: add tests for arbitrary options
  * README: add API section
  * README: make the Agent HTTP/HTTPS generic in the example
  * README: use SVG for Travis-CI badge

1.0.2 / 2015-06-27
==================

  * agent: set `req._hadError` to true after emitting "error"
  * package: update "mocha" to v2
  * test: add artificial HTTP GET request test
  * test: add artificial data events test
  * test: fix artifical GET response test on node > v0.11.3
  * test: use a real timeout for the async error test

1.0.1 / 2013-09-09
==================

  * Fix passing an "error" object to the callback function on the first tick

1.0.0 / 2013-09-09
==================

  * New API: now you pass a callback function directly

0.0.1 / 2013-07-09
==================

  * Initial release
