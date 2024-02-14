WebDriver JS Extender
=====================

This tools extends [Selenium's javascript implementation](
https://www.npmjs.com/package/selenium-webdriver) of the WebDriver API
to include additional commands (e.g. commands required for [appium](
https://github.com/appium/appium)).

Currently, few commands are implemented.  But the groundwork has been laid,
future commands should be easy to add, and PRs are very welcome!  See
[CONTRIBUTING.md](CONTRIBUTING.md) for details.

Usage
-----

If you are using a versoin of `selenium-webdriver` below `3.0.0-beta-1`, you
must use the `patch()` function before you create your webdriver instance:

```js
require('webdriver-js-extender').patch(
    require('selenium-webdriver/lib/command'),
    require('selenium-webdriver/executors'),
    require('selenium-webdriver/http'));
```

Once you've patched `selenium-webdriver` (or if you're using version `3.x`), all
you need to do is run the `extend` function on your webdriver instance:

```js
  var extendedWebdriver = require('webdriver-js-extender').extend(webdriver);

  extendedWebdriver.setNetworkConnection(5);
```
