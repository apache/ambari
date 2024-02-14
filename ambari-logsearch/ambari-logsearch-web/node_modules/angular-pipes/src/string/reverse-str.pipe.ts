import { Pipe, PipeTransform } from '@angular/core';
import { isString } from '../utils/utils';

@Pipe({
  name: 'reverseStr'
})
export class ReverseStrPipe implements PipeTransform {
  
  transform(input: string): string {
    
    if (!isString(input)) {
      return input;
    }
    
    return Array.from(input).reverse().join('');
  }
}
