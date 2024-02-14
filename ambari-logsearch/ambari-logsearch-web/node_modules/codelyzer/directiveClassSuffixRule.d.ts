import * as Lint from 'tslint';
import * as ts from 'typescript';
import { Ng2Walker } from './angular/ng2Walker';
import { DirectiveMetadata } from './angular/metadata';
export declare class Rule extends Lint.Rules.AbstractRule {
    static FAILURE: string;
    static validate(className: string, suffix: string): boolean;
    apply(sourceFile: ts.SourceFile): Lint.RuleFailure[];
}
export declare class ClassMetadataWalker extends Ng2Walker {
    visitNg2Directive(meta: DirectiveMetadata): void;
}
