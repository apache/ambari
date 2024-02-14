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
import * as ts from "typescript";
import * as Lint from "../index";
export declare class Rule extends Lint.Rules.AbstractRule {
    static metadata: Lint.IRuleMetadata;
    static FAILURE_STRING: string;
    apply(sourceFile: ts.SourceFile): Lint.RuleFailure[];
}
export declare class NoUnusedExpressionWalker extends Lint.RuleWalker {
    protected expressionIsUnused: boolean;
    protected static isDirective(node: ts.Node, checkPreviousSiblings?: boolean): boolean;
    constructor(sourceFile: ts.SourceFile, options: Lint.IOptions);
    visitExpressionStatement(node: ts.ExpressionStatement): void;
    visitBinaryExpression(node: ts.BinaryExpression): void;
    visitPrefixUnaryExpression(node: ts.PrefixUnaryExpression): void;
    visitPostfixUnaryExpression(node: ts.PostfixUnaryExpression): void;
    visitBlock(node: ts.Block): void;
    visitArrowFunction(node: ts.ArrowFunction): void;
    visitCallExpression(node: ts.CallExpression): void;
    protected visitNewExpression(node: ts.NewExpression): void;
    visitConditionalExpression(node: ts.ConditionalExpression): void;
    protected checkExpressionUsage(node: ts.ExpressionStatement): void;
}
