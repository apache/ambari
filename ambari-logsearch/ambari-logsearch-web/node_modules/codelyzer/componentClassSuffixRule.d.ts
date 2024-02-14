import * as Lint from 'tslint';
import * as ts from 'typescript';
import { F2 } from './util/function';
import { IOptions } from 'tslint';
import { Ng2Walker } from './angular/ng2Walker';
export declare class Rule extends Lint.Rules.AbstractRule {
    static FAILURE: string;
    static validate(className: string, suffixList: string[]): boolean;
    static walkerBuilder: F2<ts.SourceFile, IOptions, Ng2Walker>;
    apply(sourceFile: ts.SourceFile): Lint.RuleFailure[];
}
