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
import { IWalker } from "../walker";
export interface IRuleMetadata {
    /**
     * The kebab-case name of the rule.
     */
    ruleName: string;
    /**
     * The type of the rule - its overall purpose
     */
    type: RuleType;
    /**
     * A rule deprecation message, if applicable.
     */
    deprecationMessage?: string;
    /**
     * A short, one line description of what the rule does.
     */
    description: string;
    /**
     * More elaborate details about the rule.
     */
    descriptionDetails?: string;
    /**
     * Whether or not the rule will provide fix suggestions.
     */
    hasFix?: boolean;
    /**
     * An explanation of the available options for the rule.
     */
    optionsDescription: string;
    /**
     * Schema of the options the rule accepts.
     * The first boolean for whether the rule is enabled or not is already implied.
     * This field describes the options after that boolean.
     * If null, this rule has no options and is not configurable.
     */
    options: any;
    /**
     * Examples of what a standard config for the rule might look like.
     */
    optionExamples?: string[];
    /**
     * An explanation of why the rule is useful.
     */
    rationale?: string;
    /**
     * Whether or not the rule requires type info to run.
     */
    requiresTypeInfo?: boolean;
    /**
     * Whether or not the rule use for TypeScript only. If `false`, this rule may be used with .js files.
     */
    typescriptOnly: boolean;
}
export declare type RuleType = "functionality" | "maintainability" | "style" | "typescript";
export interface IOptions {
    ruleArguments: any[];
    ruleName: string;
    disabledIntervals: IDisabledInterval[];
}
export interface IDisabledInterval {
    startPosition: number;
    endPosition: number;
}
export interface IRule {
    getOptions(): IOptions;
    isEnabled(): boolean;
    apply(sourceFile: ts.SourceFile, languageService: ts.LanguageService): RuleFailure[];
    applyWithWalker(walker: IWalker): RuleFailure[];
}
export interface IRuleFailureJson {
    endPosition: IRuleFailurePositionJson;
    failure: string;
    fix?: Fix;
    name: string;
    ruleName: string;
    startPosition: IRuleFailurePositionJson;
}
export interface IRuleFailurePositionJson {
    character: number;
    line: number;
    position: number;
}
export declare class Replacement {
    private innerStart;
    private innerLength;
    private innerText;
    static applyAll(content: string, replacements: Replacement[]): string;
    static replaceFromTo(start: number, end: number, text: string): Replacement;
    static deleteText(start: number, length: number): Replacement;
    static deleteFromTo(start: number, end: number): Replacement;
    static appendText(start: number, text: string): Replacement;
    constructor(innerStart: number, innerLength: number, innerText: string);
    readonly start: number;
    readonly length: number;
    readonly end: number;
    readonly text: string;
    apply(content: string): string;
}
export declare class Fix {
    private innerRuleName;
    private innerReplacements;
    static applyAll(content: string, fixes: Fix[]): string;
    constructor(innerRuleName: string, innerReplacements: Replacement[]);
    readonly ruleName: string;
    readonly replacements: Replacement[];
    apply(content: string): string;
}
export declare class RuleFailurePosition {
    private position;
    private lineAndCharacter;
    constructor(position: number, lineAndCharacter: ts.LineAndCharacter);
    getPosition(): number;
    getLineAndCharacter(): ts.LineAndCharacter;
    toJson(): IRuleFailurePositionJson;
    equals(ruleFailurePosition: RuleFailurePosition): boolean;
}
export declare class RuleFailure {
    private sourceFile;
    private failure;
    private ruleName;
    private fix;
    private fileName;
    private startPosition;
    private endPosition;
    private rawLines;
    constructor(sourceFile: ts.SourceFile, start: number, end: number, failure: string, ruleName: string, fix?: Fix);
    getFileName(): string;
    getRuleName(): string;
    getStartPosition(): RuleFailurePosition;
    getEndPosition(): RuleFailurePosition;
    getFailure(): string;
    hasFix(): boolean;
    getFix(): Fix | undefined;
    getRawLines(): string;
    toJson(): IRuleFailureJson;
    equals(ruleFailure: RuleFailure): boolean;
    private createFailurePosition(position);
}
