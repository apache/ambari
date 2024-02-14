"use strict";
var helpers_1 = require("./helpers");
var path = require("path");
describe('normal tests', function () {
    var driverFactory = helpers_1.initMockSeleniumStandaloneServerAndGetDriverFactory();
    it('should set/get device activity', function (done) {
        var driver = driverFactory();
        driver.startDeviceActivity('sjelin', '.is.cool').then(function () {
            return driver.getCurrentDeviceActivity();
        }).then(function (activity) {
            expect(activity).toBe('.is.cool');
            return driver.startDeviceActivity('sjelin', '.is.the.coolest');
        }).then(function () {
            return driver.getCurrentDeviceActivity();
        }).then(function (activity) {
            expect(activity).toBe('.is.the.coolest');
            done();
        });
    });
    it('should set/get appium settings', function (done) {
        var driver = driverFactory();
        driver.setAppiumSettings({ ignoreUnimportantViews: true }).then(function () {
            return driver.getAppiumSettings();
        }).then(function (settings) {
            expect(settings['ignoreUnimportantViews']).toBe(true);
            return driver.setAppiumSettings({ ignoreUnimportantViews: false });
        }).then(function () {
            return driver.getAppiumSettings();
        }).then(function (settings) {
            expect(settings['ignoreUnimportantViews']).toBe(false);
            done();
        });
    });
    it('should set/get the context', function (done) {
        var driver = driverFactory();
        driver.selectContext('NATIVE_APP').then(function () {
            return driver.getCurrentContext();
        }).then(function (context) {
            expect(context).toBe('NATIVE_APP');
            return driver.selectContext('WEBVIEW_1');
        }).then(function () {
            return driver.getCurrentContext();
        }).then(function (context) {
            expect(context).toBe('WEBVIEW_1');
            done();
        });
    });
    it('should set/get screen orientation', function (done) {
        var driver = driverFactory();
        driver.setScreenOrientation('landscape').then(function () {
            return driver.getScreenOrientation();
        }).then(function (orientation) {
            expect(orientation).toBe('LANDSCAPE');
            return driver.setScreenOrientation('portrait');
        }).then(function () {
            return driver.getScreenOrientation();
        }).then(function (orientation) {
            expect(orientation).toBe('PORTRAIT');
            done();
        });
    });
    it('should lock/unlcok the device', function (done) {
        var driver = driverFactory();
        driver.lockDevice().then(function () {
            return driver.isDeviceLocked();
        }).then(function (locked) {
            expect(locked).toBe(true);
            return driver.unlockDevice();
        }).then(function () {
            return driver.isDeviceLocked();
        }).then(function (locked) {
            expect(locked).toBe(false);
            done();
        });
    });
    it('should install/uninstall an app', function (done) {
        var driver = driverFactory();
        driver.installApp(path.resolve(__dirname, 'totally_real_apk.apk')).then(function () {
            return driver.isAppInstalled('sjelin.is.cool');
        }).then(function (isInstalled) {
            expect(isInstalled).toBe(true);
            return driver.removeApp('sjelin.is.cool');
        }).then(function () {
            return driver.isAppInstalled('sjelin.is.cool');
        }).then(function (isInstalled) {
            expect(isInstalled).toBe(false);
            done();
        });
    });
    it('should manipulate file system', function (done) {
        var driver = driverFactory();
        Promise.all([
            driver.pushFileToDevice('/tmp/wd_js_ext/foo.txt', 'bar'),
            driver.pushFileToDevice('/tmp/wd_js_ext/folder/a.txt', 'x'),
            driver.pushFileToDevice('/tmp/wd_js_ext/folder/b.txt', 'y'),
            driver.pushFileToDevice('/tmp/wd_js_ext/folder/c.txt', 'z'),
        ]).then(function () {
            return driver.pullFileFromDevice('/tmp/wd_js_ext/foo.txt');
        }).then(function (fileContents) {
            expect(fileContents).toBe('bar');
            return driver.pullFolderFromDevice('/tmp/wd_js_ext/folder');
        }).then(function (folderContents) {
            expect(folderContents['a.txt']).toBe('x');
            expect(folderContents['b.txt']).toBe('y');
            expect(folderContents['c.txt']).toBe('z');
            done();
        });
    });
    describe('network connection', function () {
        it('should get/set the network connection', function (done) {
            var driver = driverFactory();
            driver.setNetworkConnection(0).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(0);
                return driver.setNetworkConnection(6);
            }).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(6);
                done();
            });
        });
        it('should be able to toggle various settings', function (done) {
            var driver = driverFactory();
            driver.setNetworkConnection(0).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(0);
                return driver.toggleAirplaneMode();
            }).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(1);
                return driver.toggleWiFi();
            }).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(3);
                return driver.toggleData();
            }).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(7);
                return driver.toggleWiFi();
            }).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(5);
                return driver.toggleAirplaneMode();
            }).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(4);
                return driver.toggleWiFi();
            }).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(6);
                return driver.toggleData();
            }).then(function () {
                return driver.getNetworkConnection();
            }).then(function (networkConnection) {
                expect(networkConnection).toBe(2);
                done();
            });
        });
    });
    describe('geolocation', function () {
        it('should get/set the geolocation', function (done) {
            var driver = driverFactory();
            driver.setGeolocation(1, 2, 3).then(function () {
                return driver.getGeolocation();
            }).then(function (geolocation) {
                expect(geolocation).toEqual({ latitude: 1, longitude: 2, altitude: 3 });
                return driver.setGeolocation(0, 0, 0);
            }).then(function () {
                return driver.getGeolocation();
            }).then(function (geolocation) {
                expect(geolocation).toEqual({ latitude: 0, longitude: 0, altitude: 0 });
                done();
            });
        });
        it('should disable geolocation', function (done) {
            var driver = driverFactory();
            // Location should initially work
            driver.setGeolocation(1, 2, 3).then(function () {
                return driver.getGeolocation();
            }).then(function (geolocation) {
                expect(geolocation).toEqual({ latitude: 1, longitude: 2, altitude: 3 });
                // Double toggle should do nothing
                return driver.toggleLocationServices();
            }).then(function () {
                return driver.toggleLocationServices();
            }).then(function () {
                return driver.setGeolocation(0, 0, 0);
            }).then(function () {
                return driver.getGeolocation();
            }).then(function (geolocation) {
                expect(geolocation).toEqual({ latitude: 0, longitude: 0, altitude: 0 });
                // Single toggle should cause the command to fail
                return driver.toggleLocationServices();
            }).then(function () {
                return driver.getGeolocation().catch(function (error) {
                    expect(error.toString()).toContain('Location services disabled');
                    done();
                });
            });
        });
    });
});
//# sourceMappingURL=normal_spec.js.map