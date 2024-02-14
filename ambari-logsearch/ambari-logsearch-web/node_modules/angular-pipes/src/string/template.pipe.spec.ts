import { TemplatePipe } from './template.pipe';
        


describe('TemplatePipe', () => {
    
    let pipe: TemplatePipe;
    
    beforeEach(() => {
       pipe = new TemplatePipe(); 
    });
    
    it ('Should replace the parameters', () => {
       
       expect(pipe.transform('Hello $1', 'World')).toEqual('Hello World'); 
    });
    
    
    it ('Should replace the parameters #2', () => {
       
       expect(pipe.transform('Hello $1, how is it $2', 'World', 'going?')).toEqual('Hello World, how is it going?'); 
    });
   
   
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1, [null])).toEqual(1); 
    });
   
});