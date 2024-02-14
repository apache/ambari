"use strict";
var __extends = (this && this.__extends) || function (d, b) {
    for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
    function __() { this.constructor = d; }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
};
var tslint_1 = require("tslint");
var source_map_1 = require("source-map");
var findLineAndColumnNumber = function (pos, code) {
    code = code.replace('\r\n', '\n').replace('\r', '\n');
    var line = 1;
    var column = 0;
    for (var i = 0; i < pos; i += 1) {
        column += 1;
        if (code[i] === '\n') {
            line += 1;
            column = 0;
        }
    }
    return { line: line, column: column };
};
var findCharNumberFromLineAndColumn = function (_a, code) {
    var line = _a.line, column = _a.column;
    code = code.replace('\r\n', '\n').replace('\r', '\n');
    var char = 0;
    while (line) {
        if (code[char] === '\n') {
            line -= 1;
        }
        char += 1;
    }
    return char + column;
};
var SourceMappingVisitor = (function (_super) {
    __extends(SourceMappingVisitor, _super);
    function SourceMappingVisitor(sourceFile, options, codeWithMap, basePosition) {
        var _this = _super.call(this, sourceFile, options) || this;
        _this.codeWithMap = codeWithMap;
        _this.basePosition = basePosition;
        return _this;
    }
    SourceMappingVisitor.prototype.createFailure = function (start, length, message) {
        var end = start + length;
        if (this.codeWithMap.map) {
            var consumer = new source_map_1.SourceMapConsumer(this.codeWithMap.map);
            start = this.getMappedPosition(start, consumer);
            end = this.getMappedPosition(end, consumer);
        }
        else {
            start += this.basePosition;
            end = start + length;
        }
        return _super.prototype.createFailure.call(this, start, end - start, message);
    };
    SourceMappingVisitor.prototype.getMappedPosition = function (pos, consumer) {
        var absPos = findLineAndColumnNumber(pos, this.codeWithMap.code);
        var mappedPos = consumer.originalPositionFor(absPos);
        var char = findCharNumberFromLineAndColumn(mappedPos, this.codeWithMap.source);
        return char + this.basePosition;
    };
    return SourceMappingVisitor;
}(tslint_1.RuleWalker));
exports.SourceMappingVisitor = SourceMappingVisitor;
