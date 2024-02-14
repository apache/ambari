import * as ts from 'typescript';
export interface MetadataUrls {
    templateUrl: string;
    styleUrls: string[];
}
export declare abstract class AbstractResolver {
    abstract resolve(decorator: ts.Decorator): MetadataUrls;
    protected getTemplateUrl(decorator: ts.Decorator): string;
    protected getStyleUrls(decorator: ts.Decorator): string[];
    protected getDecoratorArgument(decorator: ts.Decorator): ts.ObjectLiteralExpression;
}
