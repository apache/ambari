import { Pipe, PipeTransform  } from '@angular/core';
import { isString } from '../utils/utils';

@Pipe({
  name: 'template'
})
export class TemplatePipe implements PipeTransform {
  
  transform (input: any, ...args: any[]): any {
    
    if (!isString(input) || args.length === 0) {
      return input;
    }
    
    let template = input;
    for (let i = 0; i < args.length; ++i) {
      template = template.replace( `$${i + 1}`, args[i]);
    }
    
    return template;   
  }
}