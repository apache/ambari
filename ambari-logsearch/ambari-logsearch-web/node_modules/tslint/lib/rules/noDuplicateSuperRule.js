/**
 * @license
 * Copyright 2017 Palantir Technologies, Inc.
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
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __assign = (this && this.__assign) || Object.assign || function(t) {
    for (var s, i = 1, n = arguments.length; i < n; i++) {
        s = arguments[i];
        for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
            t[p] = s[p];
    }
    return t;
};
Object.defineProperty(exports, "__esModule", { value: true });
var ts = require("typescript");
var Lint = require("../index");
var Rule = (function (_super) {
    __extends(Rule, _super);
    function Rule() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    Rule.prototype.apply = function (sourceFile) {
        return this.applyWithWalker(new Walker(sourceFile, this.getOptions()));
    };
    return Rule;
}(Lint.Rules.AbstractRule));
/* tslint:disable:object-literal-sort-keys */
Rule.metadata = {
    ruleName: "no-duplicate-super",
    description: "Warns if 'super()' appears twice in a constructor.",
    rationale: "The second call to 'super()' will fail at runtime.",
    optionsDescription: "Not configurable.",
    options: null,
    optionExamples: ["true"],
    type: "functionality",
    typescriptOnly: false,
};
/* tslint:enable:object-literal-sort-keys */
Rule.FAILURE_STRING_DUPLICATE = "Multiple calls to 'super()' found. It must be called only once.";
Rule.FAILURE_STRING_LOOP = "'super()' called in a loop. It must be called only once.";
exports.Rule = Rule;
var Walker = (function (_super) {
    __extends(Walker, _super);
    function Walker() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    /** Whether we've seen 'super()' yet in the current constructor. */
    Walker.prototype.visitConstructorDeclaration = function (node) {
        if (!node.body) {
            return;
        }
        this.getSuperForNode(node.body);
        _super.prototype.visitConstructorDeclaration.call(this, node);
    };
    Walker.prototype.getSuperForNode = function (node) {
        if (Lint.isLoop(node)) {
            var bodySuper = this.combineSequentialChildren(node);
            if (typeof bodySuper === "number") {
                return 0 /* NoSuper */;
            }
            if (!bodySuper.break) {
                this.addFailureAtNode(bodySuper.node, Rule.FAILURE_STRING_LOOP);
            }
            return __assign({}, bodySuper, { break: false });
        }
        switch (node.kind) {
            case ts.SyntaxKind.ReturnStatement:
            case ts.SyntaxKind.ThrowStatement:
                return 1 /* Return */;
            case ts.SyntaxKind.BreakStatement:
                return 2 /* Break */;
            case ts.SyntaxKind.ClassDeclaration:
            case ts.SyntaxKind.ClassExpression:
                // 'super()' is bound differently inside, so ignore.
                return 0 /* NoSuper */;
            case ts.SyntaxKind.SuperKeyword:
                return node.parent.kind === ts.SyntaxKind.CallExpression && node.parent.expression === node
                    ? { node: node.parent, break: false }
                    : 0 /* NoSuper */;
            case ts.SyntaxKind.IfStatement: {
                var _a = node, thenStatement = _a.thenStatement, elseStatement = _a.elseStatement;
                return worse(this.getSuperForNode(thenStatement), elseStatement ? this.getSuperForNode(elseStatement) : 0 /* NoSuper */);
            }
            case ts.SyntaxKind.SwitchStatement:
                return this.getSuperForSwitch(node);
            default:
                return this.combineSequentialChildren(node);
        }
    };
    Walker.prototype.getSuperForSwitch = function (node) {
        // 'super()' from any clause. Used to track whether 'super()' happens in the switch at all.
        var foundSingle;
        // 'super()' from the previous clause if it did not 'break;'.
        var fallthroughSingle;
        for (var _i = 0, _a = node.caseBlock.clauses; _i < _a.length; _i++) {
            var clause = _a[_i];
            var clauseSuper = this.combineSequentialChildren(clause);
            switch (clauseSuper) {
                case 0 /* NoSuper */:
                    break;
                case 2 /* Break */:
                    fallthroughSingle = undefined;
                    break;
                case 1 /* Return */:
                    return 0 /* NoSuper */;
                default:
                    if (fallthroughSingle) {
                        this.addDuplicateFailure(fallthroughSingle, clauseSuper.node);
                    }
                    if (!clauseSuper.break) {
                        fallthroughSingle = clauseSuper.node;
                    }
                    foundSingle = clauseSuper.node;
                    break;
            }
        }
        return foundSingle ? { node: foundSingle, break: false } : 0 /* NoSuper */;
    };
    /**
     * Combines children that come one after another.
     * (As opposed to if/else, switch, or loops, which need their own handling.)
     */
    Walker.prototype.combineSequentialChildren = function (node) {
        var _this = this;
        var seenSingle;
        var res = ts.forEachChild(node, function (child) {
            var childSuper = _this.getSuperForNode(child);
            switch (childSuper) {
                case 0 /* NoSuper */:
                    return;
                case 2 /* Break */:
                    if (seenSingle) {
                        return __assign({}, seenSingle, { break: true });
                    }
                    return childSuper;
                case 1 /* Return */:
                    return childSuper;
                default:
                    if (seenSingle && !seenSingle.break) {
                        _this.addDuplicateFailure(seenSingle.node, childSuper.node);
                    }
                    seenSingle = childSuper;
                    return;
            }
        });
        return res || seenSingle || 0 /* NoSuper */;
    };
    Walker.prototype.addDuplicateFailure = function (a, b) {
        this.addFailureFromStartToEnd(a.getStart(), b.end, Rule.FAILURE_STRING_DUPLICATE);
    };
    return Walker;
}(Lint.RuleWalker));
;
// If/else run separately, so return the branch more likely to result in eventual errors.
function worse(a, b) {
    return typeof a === "number"
        ? typeof b === "number" ? (a < b ? b : a) : b
        : typeof b === "number" ? a : a.break ? b : a;
}
