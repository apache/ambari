import { Pipe, PipeTransform } from '@angular/core';
import { isString } from '../utils/utils';

@Pipe({
  name: 'repeat'
})
export class RepeatPipe implements PipeTransform {
  
  transform (input: any, times: number = 1, characters: string = ''): any {
    
    if (!isString(input)) {
      return input;
    }
    
    if (times <= 0) {
      times = 1;
    }
    
    const repeated = [input];
    for (let i = 1; i < times; ++i) {
      repeated.push(input)
    }
    
    return repeated.join(characters);
  }
}