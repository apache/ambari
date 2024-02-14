/**
 * @license
 * Copyright 2014 Palantir Technologies, Inc.
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
var ts = require("typescript");
var utils_1 = require("./utils");
function wrapProgram(program) {
    var files = new Map(); // file name -> content
    var fileVersions = new Map();
    var host = {
        getCompilationSettings: function () { return program.getCompilerOptions(); },
        getCurrentDirectory: function () { return program.getCurrentDirectory(); },
        getDefaultLibFileName: function () { return "lib.d.ts"; },
        getScriptFileNames: function () { return program.getSourceFiles().map(function (sf) { return sf.fileName; }); },
        getScriptSnapshot: function (name) {
            var file = files.get(name);
            if (file !== undefined) {
                return ts.ScriptSnapshot.fromString(file);
            }
            if (!program.getSourceFile(name)) {
                return undefined;
            }
            return ts.ScriptSnapshot.fromString(program.getSourceFile(name).getFullText());
        },
        getScriptVersion: function (name) {
            var version = fileVersions.get(name);
            return version === undefined ? "1" : String(version);
        },
        log: function () { },
        editFile: function (fileName, newContent) {
            files.set(fileName, newContent);
            var prevVersion = fileVersions.get(fileName);
            fileVersions.set(fileName, prevVersion === undefined ? 0 : prevVersion + 1);
        },
    };
    var langSvc = ts.createLanguageService(host, ts.createDocumentRegistry());
    langSvc.editFile = host.editFile;
    return langSvc;
}
exports.wrapProgram = wrapProgram;
function checkEdit(ls, sf, newText) {
    if (ls.hasOwnProperty("editFile")) {
        var host = ls;
        host.editFile(sf.fileName, newText);
        var newProgram = ls.getProgram();
        var newSf = newProgram.getSourceFile(sf.fileName);
        var newDiags = ts.getPreEmitDiagnostics(newProgram, newSf);
        // revert
        host.editFile(sf.fileName, sf.getFullText());
        return newDiags;
    }
    return [];
}
exports.checkEdit = checkEdit;
function createLanguageServiceHost(fileName, source) {
    return {
        getCompilationSettings: function () { return utils_1.createCompilerOptions(); },
        getCurrentDirectory: function () { return ""; },
        getDefaultLibFileName: function () { return "lib.d.ts"; },
        getScriptFileNames: function () { return [fileName]; },
        getScriptSnapshot: function (name) { return ts.ScriptSnapshot.fromString(name === fileName ? source : ""); },
        getScriptVersion: function () { return "1"; },
        log: function () { },
    };
}
exports.createLanguageServiceHost = createLanguageServiceHost;
function createLanguageService(fileName, source) {
    var languageServiceHost = createLanguageServiceHost(fileName, source);
    return ts.createLanguageService(languageServiceHost);
}
exports.createLanguageService = createLanguageService;
