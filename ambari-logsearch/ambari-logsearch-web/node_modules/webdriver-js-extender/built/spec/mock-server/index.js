"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var commands_1 = require("./commands");
var selenium_mock_1 = require("selenium-mock");
var MockAppium = (function (_super) {
    __extends(MockAppium, _super);
    function MockAppium(port) {
        var _this = _super.call(this, port, function (basicSession) {
            var session = basicSession;
            session.currentContext = 'WEBVIEW_1';
            session.installedApps = [];
            session.locked = false;
            session.localStorage = {};
            session.location = { latitude: 0, longitude: 0, altitude: 0 };
            session.locationEnabled = true;
            session.orientation = 'PORTRAIT';
            session.files = {};
            session.sessionStorage = {};
            session.settings = { ignoreUnimportantViews: false };
            session.activity = null;
            session.networkConnection = 6;
            return session;
        }) || this;
        var addCommands = function (commandList) {
            for (var commandName in commandList) {
                var command = commandList[commandName];
                if (command instanceof selenium_mock_1.Command) {
                    _this.addCommand(command);
                }
                else {
                    addCommands(command);
                }
            }
        };
        addCommands(commands_1.session);
        return _this;
    }
    return MockAppium;
}(selenium_mock_1.Server));
exports.MockAppium = MockAppium;
//# sourceMappingURL=index.js.map