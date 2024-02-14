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
import * as ts from "typescript";
import { IWalker, WalkContext } from "../walker";
import { IDisabledInterval, IOptions, IRule, IRuleMetadata, RuleFailure } from "./rule";
export declare abstract class AbstractRule implements IRule {
    readonly ruleName: string;
    private value;
    private disabledIntervals;
    static metadata: IRuleMetadata;
    protected readonly ruleArguments: any[];
    static isRuleEnabled(ruleConfigValue: any): boolean;
    constructor(ruleName: string, value: any, disabledIntervals: IDisabledInterval[]);
    getOptions(): IOptions;
    abstract apply(sourceFile: ts.SourceFile, languageService: ts.LanguageService): RuleFailure[];
    applyWithWalker(walker: IWalker): RuleFailure[];
    isEnabled(): boolean;
    protected applyWithFunction(sourceFile: ts.SourceFile, walkFn: (ctx: WalkContext<void>) => void): RuleFailure[];
    protected applyWithFunction<T>(sourceFile: ts.SourceFile, walkFn: (ctx: WalkContext<T>) => void, options: T): RuleFailure[];
    protected filterFailures(failures: RuleFailure[]): RuleFailure[];
}
