import { PowPipe } from './pow.pipe';
        


describe('PowPipe', () => {
    
    let pipe: PowPipe;
    
    beforeEach(() => {
       pipe = new PowPipe(); 
    });
    
    it('Should return 4', () => {
        
        expect(pipe.transform(2)).toEqual(4);
    });
    
    it('Should return 8', () => {
       
        expect(pipe.transform(2, 3)).toEqual(8); 
    });

    it('Should return NaN', () => {
        expect(pipe.transform('a')).toEqual('NaN');
    });
  
});