/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import { FileOperator, Rule } from '../engine/interface';
import { BaseException } from '../exception/exception';
export declare class OptionIsNotDefinedException extends BaseException {
    constructor(name: string);
}
export declare class UnknownPipeException extends BaseException {
    constructor(name: string);
}
export declare class InvalidPipeException extends BaseException {
    constructor(name: string);
}
export declare const kPathTemplateComponentRE: RegExp;
export declare const kPathTemplatePipeRE: RegExp;
export declare type TemplateValue = boolean | string | number;
export declare type TemplatePipeFunction = (x: string) => TemplateValue;
export declare type TemplateOptions = {
    [key: string]: TemplateValue | TemplateOptions | TemplatePipeFunction;
};
export declare function applyContentTemplate<T extends TemplateOptions>(options: T): FileOperator;
export declare function contentTemplate<T extends TemplateOptions>(options: T): Rule;
export declare function applyPathTemplate<T extends TemplateOptions>(options: T): FileOperator;
export declare function pathTemplate<T extends TemplateOptions>(options: T): Rule;
export declare function template<T extends TemplateOptions>(options: T): Rule;
