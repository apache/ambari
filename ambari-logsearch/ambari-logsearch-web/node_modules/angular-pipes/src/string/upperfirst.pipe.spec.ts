import { UpperFirstPipe } from './upperfirst.pipe';
        


describe('UpperFirstPipe', () => {
    
    let pipe: UpperFirstPipe;
    
    beforeEach(() => {
       pipe = new UpperFirstPipe(); 
    });
    
    it('Should returned the string with first letter uppercase', () => {
       
       expect(pipe.transform('abcd')).toEqual('Abcd');
    });
    
    it('Should returned the string with first letter uppercase #2', () => {
       
       expect(pipe.transform('abcd AA')).toEqual('Abcd AA');
    });

    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1)).toEqual(1); 
    });
   
});