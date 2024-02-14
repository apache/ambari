/**
 * @license
 * Copyright 2013 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var findup = require("findup-sync");
var fs = require("fs");
var path = require("path");
var resolve = require("resolve");
var error_1 = require("./error");
var utils_1 = require("./utils");
exports.CONFIG_FILENAME = "tslint.json";
/* tslint:disable:object-literal-key-quotes */
exports.DEFAULT_CONFIG = {
    "extends": "tslint:recommended",
};
/* tslint:enable:object-literal-key-quotes */
var BUILT_IN_CONFIG = /^tslint:(.*)$/;
/**
 * Searches for a TSLint configuration and returns the data from the config.
 * @param configFile A path to a config file, this can be null if the location of a config is not known
 * @param inputFileLocation A path to the current file being linted. This is the starting location
 * of the search for a configuration.
 * @returns Load status for a TSLint configuration object
 */
function findConfiguration(configFile, inputFilePath) {
    var path = findConfigurationPath(configFile, inputFilePath);
    var loadResult = { path: path };
    try {
        loadResult.results = loadConfigurationFromPath(path);
        return loadResult;
    }
    catch (error) {
        throw new error_1.FatalError("Failed to load " + path + ": " + error.message, error);
    }
}
exports.findConfiguration = findConfiguration;
/**
 * Searches for a TSLint configuration and returns the path to it.
 * Could return undefined if not configuration is found.
 * @param suppliedConfigFilePath A path to an known config file supplied by a user. Pass null here if
 * the location of the config file is not known and you want to search for one.
 * @param inputFilePath A path to the current file being linted. This is the starting location
 * of the search for a configuration.
 * @returns An absolute path to a tslint.json file
 * or undefined if neither can be found.
 */
function findConfigurationPath(suppliedConfigFilePath, inputFilePath) {
    if (suppliedConfigFilePath != null) {
        if (!fs.existsSync(suppliedConfigFilePath)) {
            throw new Error("Could not find config file at: " + path.resolve(suppliedConfigFilePath));
        }
        else {
            return path.resolve(suppliedConfigFilePath);
        }
    }
    else {
        // search for tslint.json from input file location
        var configFilePath = findup(exports.CONFIG_FILENAME, { cwd: inputFilePath, nocase: true });
        if (configFilePath != null && fs.existsSync(configFilePath)) {
            return path.resolve(configFilePath);
        }
        // search for tslint.json in home directory
        var homeDir = getHomeDir();
        if (homeDir != null) {
            configFilePath = path.join(homeDir, exports.CONFIG_FILENAME);
            if (fs.existsSync(configFilePath)) {
                return path.resolve(configFilePath);
            }
        }
        // no path could be found
        return undefined;
    }
}
exports.findConfigurationPath = findConfigurationPath;
/**
 * Used Node semantics to load a configuration file given configFilePath.
 * For example:
 * '/path/to/config' will be treated as an absolute path
 * './path/to/config' will be treated as a relative path
 * 'path/to/config' will attempt to load a to/config file inside a node module named path
 * @returns a configuration object for TSLint loaded from the file at configFilePath
 */
function loadConfigurationFromPath(configFilePath) {
    if (configFilePath == null) {
        return exports.DEFAULT_CONFIG;
    }
    else {
        var resolvedConfigFilePath = resolveConfigurationPath(configFilePath);
        var configFile = void 0;
        if (path.extname(resolvedConfigFilePath) === ".json") {
            var fileContent = utils_1.stripComments(fs.readFileSync(resolvedConfigFilePath)
                .toString()
                .replace(/^\uFEFF/, ""));
            configFile = JSON.parse(fileContent);
        }
        else {
            configFile = require(resolvedConfigFilePath);
            delete require.cache[resolvedConfigFilePath];
        }
        var configFileDir_1 = path.dirname(resolvedConfigFilePath);
        configFile.rulesDirectory = getRulesDirectories(configFile.rulesDirectory, configFileDir_1);
        // load configurations, in order, using their identifiers or relative paths
        // apply the current configuration last by placing it last in this array
        var configs = utils_1.arrayify(configFile.extends).map(function (name) {
            var nextConfigFilePath = resolveConfigurationPath(name, configFileDir_1);
            return loadConfigurationFromPath(nextConfigFilePath);
        }).concat([configFile]);
        return configs.reduce(extendConfigurationFile, {});
    }
}
exports.loadConfigurationFromPath = loadConfigurationFromPath;
/**
 * Resolve configuration file path or node_module reference
 * @param filePath Relative ("./path"), absolute ("/path"), node module ("path"), or built-in ("tslint:path")
 */
function resolveConfigurationPath(filePath, relativeTo) {
    var matches = filePath.match(BUILT_IN_CONFIG);
    var isBuiltInConfig = matches != null && matches.length > 0;
    if (isBuiltInConfig) {
        var configName = matches[1];
        try {
            return require.resolve("./configs/" + configName);
        }
        catch (err) {
            throw new Error(filePath + " is not a built-in config, try \"tslint:recommended\" instead.");
        }
    }
    var basedir = relativeTo || process.cwd();
    try {
        return resolve.sync(filePath, { basedir: basedir });
    }
    catch (err) {
        try {
            return require.resolve(filePath);
        }
        catch (err) {
            throw new Error("Invalid \"extends\" configuration value - could not require \"" + filePath + "\". " +
                "Review the Node lookup algorithm (https://nodejs.org/api/modules.html#modules_all_together) " +
                "for the approximate method TSLint uses to find the referenced configuration file.");
        }
    }
}
function extendConfigurationFile(targetConfig, nextConfigSource) {
    var combinedConfig = {};
    var configRulesDirectory = utils_1.arrayify(targetConfig.rulesDirectory);
    var nextConfigRulesDirectory = utils_1.arrayify(nextConfigSource.rulesDirectory);
    combinedConfig.rulesDirectory = configRulesDirectory.concat(nextConfigRulesDirectory);
    var combineProperties = function (targetProperty, nextProperty) {
        var combinedProperty = {};
        for (var _i = 0, _a = Object.keys(utils_1.objectify(targetProperty)); _i < _a.length; _i++) {
            var name = _a[_i];
            combinedProperty[name] = targetProperty[name];
        }
        // next config source overwrites the target config object
        for (var _b = 0, _c = Object.keys(utils_1.objectify(nextProperty)); _b < _c.length; _b++) {
            var name = _c[_b];
            combinedProperty[name] = nextProperty[name];
        }
        return combinedProperty;
    };
    combinedConfig.rules = combineProperties(targetConfig.rules, nextConfigSource.rules);
    combinedConfig.jsRules = combineProperties(targetConfig.jsRules, nextConfigSource.jsRules);
    combinedConfig.linterOptions = combineProperties(targetConfig.linterOptions, nextConfigSource.linterOptions);
    return combinedConfig;
}
exports.extendConfigurationFile = extendConfigurationFile;
function getHomeDir() {
    var environment = global.process.env;
    var paths = [
        environment.USERPROFILE,
        environment.HOME,
        environment.HOMEPATH,
        environment.HOMEDRIVE + environment.HOMEPATH,
    ];
    for (var _i = 0, paths_1 = paths; _i < paths_1.length; _i++) {
        var homePath = paths_1[_i];
        if (homePath != null && fs.existsSync(homePath)) {
            return homePath;
        }
    }
}
// returns the absolute path (contrary to what the name implies)
function getRelativePath(directory, relativeTo) {
    if (directory != null) {
        var basePath = relativeTo || process.cwd();
        return path.resolve(basePath, directory);
    }
    return undefined;
}
exports.getRelativePath = getRelativePath;
/**
 * @param directories A path(s) to a directory of custom rules
 * @param relativeTo A path that directories provided are relative to.
 * For example, if the directories come from a tslint.json file, this path
 * should be the path to the tslint.json file.
 * @return An array of absolute paths to directories potentially containing rules
 */
function getRulesDirectories(directories, relativeTo) {
    var rulesDirectories = utils_1.arrayify(directories)
        .map(function (dir) { return getRelativePath(dir, relativeTo); })
        .filter(function (dir) { return dir !== undefined; });
    for (var _i = 0, rulesDirectories_1 = rulesDirectories; _i < rulesDirectories_1.length; _i++) {
        var directory = rulesDirectories_1[_i];
        if (directory != null && !fs.existsSync(directory)) {
            throw new Error("Could not find custom rule directory: " + directory);
        }
    }
    return rulesDirectories;
}
exports.getRulesDirectories = getRulesDirectories;
