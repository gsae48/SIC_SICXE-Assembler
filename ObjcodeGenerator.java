package UIAssembler;

/*
This class is used to generate SIC/SICXE hexidecimal object code for the assembler on pass two.
It is invoked as so:
ObjcodeGenerator generator = new ObjcodeGenerator();
String myObjcode = generator.generateObjcode("JEQ", 0x30, "CLOOP", 0x0021, 0x00F, 0, 0, 3);

the method generateObjcode, takes in these parameters inorder: The OPCODE, the OPCODE numeric representation, the OPERAND,
the source address, the destination address, the contents of the base register, the contents of the index register,
and the format.
It returns a string.
*/

import java.util.Hashtable;

public class ObjcodeGenerator {

	//public static void main(String[] args) {new ObjcodeGenerator();}
	
	private final byte FORMATSIC = 0, FORMAT1 = 1; 
	private final byte FORMAT2 = 2, FORMAT3 = 3, FORMAT4 = 4;
	private final byte FORMATBYTE = 5, FORMATWORD = 6;
	private boolean setBflag; 
	private boolean	offPflag;
	private boolean setPflag;
	private Hashtable<String, Byte> registerTable;
	
	public ObjcodeGenerator(){
		setPflag = offPflag = setBflag = false;
		buildRegisterTable();
		}
	
	public String generateObjcode(String OPCODE, byte opHexRep, String OPERAND, int srcAddress,
	int destAddress, int baseRegister, int xRegister, byte FORMAT){
		byte nixbpe;
		byte[] objcode;
		int disp_TA;
		
		nixbpe = setNixbpe(OPCODE, OPERAND, FORMAT);//sets nixbpe flags
		//calculates the displacement and sets the b flag boolean
		disp_TA = calcDisp_TA(srcAddress, destAddress, baseRegister, xRegister, OPERAND, FORMAT, nixbpe);
		
		if(setBflag){//if b flag variable was set?
			nixbpe |= 0x04;// set nixbpe b flag
			nixbpe &= 0xFD;// turn nixbpe p flag off
			setBflag = false; //set to false for next iteration
			}
		else if(setPflag){
			nixbpe |= 0x02;//set nixbpe p flag
			setPflag = false;
			}
		
		if((nixbpe & 0x01) == 1){// if format 4 truncate the '+' off of OPCODE string
			OPCODE = OPCODE.substring(1);
			}
	
		objcode = encodeObjcodeToByte(opHexRep, OPERAND, disp_TA, (short)disp_TA, 
				(short)disp_TA, FORMAT, nixbpe);//generate the objcode
		
		StringBuilder strBuilder = new StringBuilder();//turn byte array into a string
		for(int i =0; i < objcode.length; i++)
			strBuilder.append(String.format("%02X", objcode[i]));
		
		return strBuilder.toString();
		}
	
	private byte[] encodeObjcodeToByte(byte opcode, String operand, int extAddr, short addr, short disp, byte format, byte nixbpe){
		byte[] objcode = null;
		if(format == 1){
			/**format1(8 bits): |opcode(8)|**/
			/**[opcode(8)]**/
			objcode = new byte[1];
			objcode[0] = opcode;
			}
		else if(format == 2){
			/**format2(16 bits): |opcode(8)|register1(4)|register2(4)|**/
			/**[opcode(8)], [register1(4 lo) | register2(4 lo)]**/
			objcode = new byte[2];
			byte reg1 = 0, reg2 = 0;
			reg1 = registerTable.get(operand.charAt(0)+"");
			if(operand.length() > 2)//find second register
				reg2 = registerTable.get(operand.charAt(2)+"");
			objcode[0] = opcode;
			objcode[1] = (byte) ( (reg1 << 4) | (reg2) );
			}
		else if(format == 3){
			/**format3(24 bits): |opcode(6)|nixbbe(6)|displacement(12)|**/
			/**[opcode(6) | nixbpe(2 hi)], [nixbpe(4 lo) | displacement(4 hi)], [displacement(8 lo)]**/
			objcode = new byte[3];
			objcode[0] = (byte) (opcode | ( (nixbpe & 0x30) >> 4) );
			objcode[1] = (byte) ( ( (nixbpe & 0x0F) << 4) | ( (disp & 0x0F00) >> 8) );
			objcode[2] = (byte) (disp & 0x00FF);
			}
		else if(format == FORMATSIC){
			/**SIC format(24 bits): |opcode(8)|x(1)|address(15)|**/
			/**[opcode(8)], [nixbpe(1, 4th bit) | address(7 hi)], [address(8lo)]**/
			objcode = new byte[3];
			objcode[0] = opcode;
			objcode[1] = (byte) (((addr & 0x7F00) >> 8) | ((nixbpe & 0x08) << 4));
			objcode[2] = (byte)   (addr & 0x00FF);
			}
		else if(format == 4){
			/**format4(32 bits): |opcode(6)|nixbpe(6)|address(20)|**/
			/**[opcode(6 hi) | nixbpe(2 hi)], [nixbpe(4 lo) | address(4 hi)], [address(8 mid)], [address(8 lo)]**/
			objcode = new byte[4];
			objcode[0] = (byte) (opcode | ((nixbpe & 0x30) >> 4));
			objcode[1] = (byte) ( ( (nixbpe & 0x0F) << 4) | ( (extAddr & 0x000F0000) >> 16) );
			objcode[2] = (byte) ((extAddr & 0x0000FF00) >> 8);//0x105D
			objcode[3] = (byte) (extAddr & 0x000000FF);
			}
		else if((format == FORMATWORD)){
			objcode = new byte[3];
			objcode[0] = (byte) ((extAddr & 0x00FF0000) >> 16);
			objcode[1] = (byte) ((extAddr & 0x0000FF00) >> 8);
			objcode[2] = (byte) (extAddr & 0x000000FF);
			}
		else if(format == FORMATBYTE){
			if((extAddr & 0x0000FF) != extAddr){
				objcode = new byte[3];
				objcode[0] = (byte) ((extAddr & 0x00FF0000) >> 16);
				objcode[1] = (byte) ((extAddr & 0x0000FF00) >> 8);
				objcode[2] = (byte) (extAddr & 0x000000FF);
				}
			else{
				objcode = new byte[1];
				objcode[0] = (byte)disp;
				}
			}
		return objcode;
		}//END OF encode objcode to byte
	
	private byte setNixbpe(String OPCODE, String OPERAND, byte FORMAT){
		byte nixbpe = 0x00;
		boolean isSICXE = FORMATSIC != FORMAT;
		int len = OPERAND.length();
		if((FORMATBYTE == FORMAT) || (FORMATWORD == FORMAT) 
				|| (FORMAT2 == FORMAT) || (FORMAT1 == FORMAT))
			nixbpe = 0x00;
		else if(!isSICXE){
			/**SIC**/
			if(len > 2 && OPERAND.substring(OPERAND.length() - 2).equals(",X")){
				/**test: is indexed?**/
				nixbpe |= 0x08;//set x flag
				}
			}
		else if(isSICXE){
			/**SICXE**/
			if(len > 1 && OPCODE.charAt(0) == '+'){
				/**test: is extended format?**/
				nixbpe |= 0x01;//set e flag
				nixbpe &= 0x39;//turn off b and p flags
				}
			if(len > 1){
				char firstC = OPERAND.charAt(0);
				if(firstC == '@'){
					/**test: is indirect mode?**/
					nixbpe |= 0x20;//set n flag
					}
				else if(firstC == '#'){
					/**test: is immediate mode?**/
					nixbpe |= 0x10;//set i flag
					}
				else if(OPERAND.substring(OPERAND.length() - 2).equals(",X")){
					/**test: is indexed?**/
					nixbpe |= 0x08;//set x flag
					}
				
				if(firstC != '@' && firstC != '#'){
					/**test: is simple mode?**/
					nixbpe |= 0x30;//set n and i flags for simple mode
					}
				}
			if(len == 0){//non operand instructions
				nixbpe |= 0x30;//set to simple mode SICXE
				nixbpe &= 0x30;//turn off all other flags
				}
			}//END if isSICXE
		
		return nixbpe;
	}// END OF setNixbpe
	
	private int calcDisp_TA(int pc, int targetAddr, int baseReg, int xReg, String OPERAND, byte FORMAT, byte nixbpe){
		int disp_addr = 0;
		nixbpe &= 0x3F;//extract the first 6 bits
		boolean isIndirect = (nixbpe & 0x30) == 0x20;
		boolean isImmediate = (nixbpe & 0x30) == 0x10;
		boolean isSimpleSIC = (nixbpe & 0x30) == 0x00;
		boolean isSimpleSICXE = (nixbpe & 0x30) == 0x30;
		boolean isExtFormat = (nixbpe & 0x01) == 0x01;
		boolean isIndexed = (nixbpe & 0x08) == 0x08;
		boolean isWord = FORMATWORD == FORMAT;
		boolean isByte = FORMATBYTE == FORMAT;
		boolean isConst = false;
		String potentialConst = "";
		
		if(OPERAND.equals("") || FORMAT2 == FORMAT) return 0;//if no operand 
		
		if(isWord || isByte){//if it's a BYTE or WORD constant
			if(isConstant(OPERAND))
				return Integer.parseInt(OPERAND);
			
			String substrTwo = OPERAND.substring(0, 2);
			char last = OPERAND.charAt(OPERAND.length()-1);
			String substrMid = OPERAND.substring(2, OPERAND.length()-1);
			if(substrTwo.equals("X'") && last == '\''){//X'F1'
				disp_addr = Integer.valueOf(substrMid, 16);
				}
			else if(substrTwo.equals("C'") && last == '\''){//C'EOF'
				char[] constArray = substrMid.toCharArray();
				for(int i=0; i < constArray.length; i++){
					disp_addr |= (constArray[i]);
					disp_addr <<= 8;
					}
				disp_addr >>= 8;
				}
			return (disp_addr & 0x00FFFFFF);//return 3 bytes
			}//END if isConst
		
		if(isImmediate || isIndirect){//if true then pass operand minus '#' or '@'
			isConst = isConstant(OPERAND.substring(1));
			potentialConst = OPERAND.substring(1);
			}
		else if(!isIndexed && isSimpleSICXE){//if true pass whole operand
			isConst = isConstant(OPERAND);
			potentialConst = OPERAND;
			}
		else if(isIndexed && isSimpleSICXE){//if true pass operand minus ",X"
			isConst = isConstant(OPERAND.substring(0, OPERAND.length()-2));
			potentialConst = OPERAND.substring(0, OPERAND.length()-2);
			}
		
		if(isConst)//if true set displacement as constant
			disp_addr = Integer.parseInt(potentialConst);
		else
			disp_addr = targetAddr;
		
		if(isConst && (isImmediate || isIndirect)) return (disp_addr & 0x00000FFF);//extract 12 bits
		if(isIndexed) disp_addr += xReg; //add x register to target address
		if(isConst && isSimpleSICXE) return (disp_addr & 0x00000FFF);//extract 12 bits
		if(isExtFormat) return (disp_addr & 0x000FFFFF); //extract 20 bits
		if(isSimpleSIC) return (disp_addr & 0x00007FFF); //extract 15 bits
			
		disp_addr = targetAddr - pc;//calculate the displacement
 		if(disp_addr > 0 && (disp_addr != (disp_addr & 0x00000FFF))){//determine is target address is too far
			disp_addr = targetAddr - baseReg;//if true use base register instead
			setBflag = true;
			}
 		else if(disp_addr < 0 && (-disp_addr != (-disp_addr & 0x00000FFF))){
 			disp_addr = targetAddr - baseReg;//if true use base register instead
			setBflag = true;
 			}
 		else{
 			setPflag = true;
 			}
		
		if(isSimpleSICXE) return (disp_addr & 0x00000FFF); //extract 12 bits
		if(!isIndexed && (isIndirect || isImmediate)) return (disp_addr & 0x00000FFF);//extract 12 bits
		
		return 0;//maybe an error
		}//END of calcTargetAddress
	
	private boolean isConstant(String str){  
		try  {  Integer.parseInt(str);  }  
	  	catch(NumberFormatException e)  {  return false;  }  
	  	return true;  
		}//END OF isDigit
	
	private void buildRegisterTable(){
		registerTable = new Hashtable<String, Byte>();
		registerTable.put("A", (byte)0);
		registerTable.put("X", (byte)1);
		registerTable.put("L", (byte)2);
		registerTable.put("B", (byte)3);
		registerTable.put("S", (byte)4);
		registerTable.put("T", (byte)5);
		registerTable.put("F", (byte)6);
		registerTable.put("PC", (byte)8);
		registerTable.put("SW", (byte)9);
		}//END OF buildRegisterTable
	
}//END of class ObjcodeGenerator
