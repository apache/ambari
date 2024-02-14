import * as ts from 'typescript';
import { Ng2Walker } from '../angular/ng2Walker';
import { IOptions } from 'tslint';
import { ComponentMetadata } from '../angular/metadata';
import { F1, F2, Maybe } from '../util/function';
import { Failure } from './walkerFactory';
export declare type Validator = NodeValidator | ComponentValidator;
export declare type ValidateFn<T> = F2<T, IOptions, Maybe<Failure[]>>;
export declare type WalkerOptions = any;
export interface NodeValidator {
    kind: 'Node';
    validate: ValidateFn<ts.Node>;
}
export interface ComponentValidator {
    kind: 'Ng2Component';
    validate: ValidateFn<ComponentMetadata>;
}
export declare function validate(syntaxKind: ts.SyntaxKind): F1<ValidateFn<ts.Node>, NodeValidator>;
export declare function validateComponent(validate: F2<ComponentMetadata, WalkerOptions, Maybe<Failure[]>>): ComponentValidator;
export declare function all(...validators: Validator[]): F2<ts.SourceFile, IOptions, Ng2Walker>;
