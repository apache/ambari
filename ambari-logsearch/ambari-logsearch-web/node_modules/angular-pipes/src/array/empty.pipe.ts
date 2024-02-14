import { Pipe, PipeTransform  } from '@angular/core';
import { empty } from '../utils/utils';

@Pipe({
  name: 'empty'
})
export class EmptyPipe implements PipeTransform {
  
  transform (input: any): any {
    
    return empty(input);
  }
}
