import * as ts from 'typescript';
import { Maybe } from './function';
import { WithStringInitializer } from './astQuery';
export declare function getInlineStyle(dec: ts.Decorator): Maybe<ts.ArrayLiteralExpression>;
export declare function getTemplateUrl(dec: ts.Decorator): Maybe<WithStringInitializer>;
export declare function getTemplate(dec: ts.Decorator): Maybe<WithStringInitializer>;
