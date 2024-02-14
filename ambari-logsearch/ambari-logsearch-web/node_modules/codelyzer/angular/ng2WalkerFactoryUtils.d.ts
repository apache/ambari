import { Ng2WalkerConfig } from './ng2Walker';
import { MetadataReader } from './metadataReader';
import { BasicCssAstVisitor } from './styles/basicCssAstVisitor';
import { RecursiveAngularExpressionVisitor } from './templates/recursiveAngularExpressionVisitor';
import { BasicTemplateAstVisitor } from './templates/basicTemplateAstVisitor';
export declare const ng2WalkerFactoryUtils: {
    defaultConfig(): {
        templateVisitorCtrl: typeof BasicTemplateAstVisitor;
        expressionVisitorCtrl: typeof RecursiveAngularExpressionVisitor;
        cssVisitorCtrl: typeof BasicCssAstVisitor;
    };
    defaultMetadataReader(): MetadataReader;
    normalizeConfig(config: Ng2WalkerConfig): any;
};
