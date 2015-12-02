package UIAssembler;

/*
This is the assembler, which is a two pass assmbler able to process both SIC and SICXE. The assembler recieves
as input a text file containing SIC or SICXE code. It then outputs two files: errorMsg.txt, and finalAssembly.txt
errorMsg.txt contains a complete copy of the input file along with any error messages, if there are any. finalAssembly.txt
contains a comlete copy of the input file along with two additional columns: a location counter column and an object
code column.
errorMsg.txt is created during pass one and finalAssembly.txt is created during pass two. If any errors are found in pass one,
only errorMsg.txt is created and not finalAssembly.txt. If no errors are found in pass one, then errorMsg.txt and 
finalAssembly.txt are created.
Instructions on how to invoke the assembler are found in the README.md file
*/

import java.util.Hashtable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;

public class Assembler {
	//public static void main(String[] args) {new Assembler("assemblyCode.txt", true);}								  
	
	private final char COMMENT = '.';
	private final int MAX_SYMBOL_LEN = 6;
	private final byte FORMAT_SIC = 0, FORMAT1 = 1, FORMAT2 = 2; 
	private final byte FORMAT3 = 3, FORMAT4 = 4;
	private final byte FORMATBYTE = 5, FORMATWORD = 6;
	private boolean isErrorFree;
	private boolean isSIC;
	private int programLength, startingAddress;
	private String LABEL, OPCODE, OPERAND;
	private String fileName;
	private Hashtable<String, Short> symbolTable;
	private Hashtable<String, OpcodeObj> opcodeTable;
	private Hashtable<String, Byte> registerTable;
	private BufferedReader reader;
	private PrintWriter errorMsgWriter;
	private PrintWriter objWriter;
	

 	public Assembler(String inputFile, boolean is_sic){
 		fileName = inputFile;
 		symbolTable = new Hashtable<String, Short>();
		opcodeTable = new Hashtable<String, OpcodeObj>();
		registerTable = new Hashtable<String, Byte>();
		LABEL = OPCODE = OPERAND = "";
		programLength = startingAddress = 0;
		isSIC = is_sic;
		setIsSIC(isSIC);
		isErrorFree = true;
 		}
 	
 	
 	
	public Assembler(){ this("", true); }
	
	
	
	public void assemble(){ 
		pass1(); 
		if(isErrorFree) pass2();
		}
	
	
	
	private void pass1(){
		symbolTable.clear();
		isErrorFree = true;
		int locCounter = 0;
		String line;
		boolean isValidOpcode = true, isValidSymbol = true;
		
		try {
			/**create reader and writer, write first lines**/
			reader = new BufferedReader(new FileReader(fileName));
			errorMsgWriter = new PrintWriter("errorMsg.txt", "UTF-8");
			errorMsgWriter.write(String.format("%-9s\t%-9s\t%-9s\t%-9s\t",
					"loc", "label", "opcode", "operand"));
			errorMsgWriter.println();  errorMsgWriter.println();
			line = reader.readLine();  setTokens(line);
			
			
			if(OPCODE.equals("START")){
				startingAddress = Integer.parseInt(OPERAND);
				locCounter = Integer.valueOf(
						String.valueOf(startingAddress),16);
				errorMsgWriter.write(String.format("%-9d\t%-9s\t%-9s\t%-9s\t",
						startingAddress, LABEL, OPCODE, OPERAND));
				errorMsgWriter.println();
				line = reader.readLine();	setTokens(line);
				}
			
			
			while(!OPCODE.equals("END") && line != null){
					
				if(line.trim().charAt(0) != COMMENT && !OPCODE.equals("BASE")){
						
					if(!LABEL.equals("")){
							
						/**identify the symbol**/
						if(symbolTable.containsKey(LABEL)){
							/**error: duplicate symbol**/
							isValidSymbol = false;
							isErrorFree = false;
							}
						else{
							symbolTable.put(LABEL, (short)locCounter);
							isValidSymbol = true;
							}
						
						if(!isValidSymbol){
							/**write error message**/
							errorMsgWriter.write(
							   ".!!!Error detected on next line. Duplicate symbol!!!");
							errorMsgWriter.println();
							isValidSymbol = true;//reset
							}
					
					}//END OF: LABEL != ""
						
						
					isValidOpcode = true;
					/**identify the opcode, increase location counter**/
					if(opcodeTable.containsKey(OPCODE))
						locCounter += opcodeTable.get(OPCODE).format;
					else if(OPCODE.charAt(0) == '+' 
							&& OPCODE.length() > 1 
							&& opcodeTable.containsKey(OPCODE.substring(1)))
								locCounter += FORMAT4;
					else if(OPCODE.equals("WORD"))		
						locCounter += 3;
					else if(OPCODE.equals("BYTE")){
						String constant = "";
						if(OPERAND.length() >= 4 
						 && OPERAND.substring(0, 2).equals("C'") 
						 && OPERAND.substring(OPERAND.length()-1).equals("'")){
							/**Byte is a constant**/
							constant = OPERAND.substring(2, OPERAND.length()-1);
							locCounter += constant.length();
							}
						else
							locCounter += 1;
						}
					else if(OPCODE.equals("RESW"))
						locCounter += 3 * Integer.parseInt(OPERAND);
					else if(OPCODE.equals("RESB"))
						locCounter += Integer.parseInt(OPERAND);
					else{
						isValidOpcode = false;
						isErrorFree = false;
						}
					
					if(!isValidOpcode){
						/**write error message to error message document**/
						errorMsgWriter.write(
						   ".!!!Error detected on next line. Invalid opcode!!!");
						errorMsgWriter.println();
						isValidOpcode = true;//reset boolean for next iteration
						}
					
					}//END OF: line != COMMENT

					
					/**write to file, read new line**/
					errorMsgWriter.write(String.format("%-9X\t%-9s\t%-9s\t%-9s\t",
							locCounter, LABEL, OPCODE, OPERAND));
					errorMsgWriter.println();
					line = reader.readLine();  setTokens(line);
					
					}//END OF: while opcode != END
				
				/**finish writing to file**/
				errorMsgWriter.write(String.format("%-9X\t%-9s\t%-9s\t%-9s\t",
						locCounter, LABEL, OPCODE, OPERAND));
				errorMsgWriter.println();
				programLength = locCounter - startingAddress;

				if(isErrorFree){
					errorMsgWriter.write("...No errors detected...");
					errorMsgWriter.println();
					}
				
				reader.close();
				errorMsgWriter.close();
		
		}//END OF: try
		
		catch (FileNotFoundException e1) { e1.printStackTrace();}	 
		catch (IOException e) { e.printStackTrace(); }
		
		}//END OF: pass1
	
	
	
	private void pass2(){
		
		int destAddress = 0, srcAddress = 0, base = 0;
		boolean isOperandValid = true;
		ObjcodeGenerator generator = new ObjcodeGenerator();
		String line, strObjCodeInstruction = "";
		
		
		try{
			reader = new BufferedReader(new FileReader("errorMsg.txt"));
			objWriter = new PrintWriter("finalAssembly.txt", "UTF-8");
			line = reader.readLine();
			objWriter.write(String.format("%-37s%-9s", line, "object code")); 
			objWriter.println();
			line = reader.readLine();   objWriter.println();
			line = reader.readLine();
			objWriter.write(line);   objWriter.println();
			setTokens(line.substring(9));
			
			
			if(OPCODE.equals("START")){
				line = reader.readLine();
				setTokens(line.substring(9));
				}
			

			while(!OPCODE.equals("END")){
				
				if(line.trim().charAt(0) != COMMENT){
					
					strObjCodeInstruction = "";
					/**evaluate opcode and operand**/
					if(opcodeTable.containsKey(OPCODE) 
						|| (OPCODE.charAt(0) == '+' 
							&& opcodeTable.containsKey(OPCODE.substring(1)))){
						
						if(!OPERAND.equals("")){
							if(symbolTable.containsKey(OPERAND))
								destAddress = symbolTable.get(OPERAND);
							else if((OPERAND.charAt(0) == '#' || OPERAND.charAt(0) == '@')
									 && OPERAND.length() > 1){
								if(symbolTable.containsKey(OPERAND.substring(1)))
									destAddress = symbolTable.get(OPERAND.substring(1));
								else if(isConstant(OPERAND.substring(1)))
									destAddress = 0;
								}
							else if(registerTable.containsKey(OPERAND))
								destAddress = registerTable.get(OPERAND);
							else if(registerTable.containsKey(OPERAND.substring(0, 1))
									&& registerTable.containsKey(OPERAND.substring(2)))
								destAddress = 0;
							else if(OPERAND.length() > 2 
								  && OPERAND.substring(OPERAND.length() - 2).equals(",X"))
								destAddress = symbolTable.get(OPERAND.substring(0, OPERAND.length() - 2));
							else {//undefined operand symbol
								destAddress = 0;
								isOperandValid = false;
								}
							}
						else{//operand field is empty
							destAddress = 0;
							}
						

						if(isOperandValid){
							/**generate object code**/
							byte opcodeByte;
							byte format;
							if(isSIC){ 
								format = FORMAT_SIC;
								opcodeByte = (byte)opcodeTable.get(OPCODE).hexRepresentation;
								}
							else if(OPCODE.charAt(0) == '+' 
								  && opcodeTable.get(OPCODE.substring(1)).format == FORMAT3){ 
								format = FORMAT4;
								opcodeByte = (byte)opcodeTable
										.get(OPCODE.substring(1)).hexRepresentation;
								}
							else{
								format = opcodeTable.get(OPCODE).format;
								opcodeByte = (byte)opcodeTable.get(OPCODE).hexRepresentation;
								}
							String str = line.substring(0, 7).trim();
							srcAddress = Integer.valueOf(str, 16);
							strObjCodeInstruction = generator.generateObjcode(OPCODE, opcodeByte, OPERAND,
									srcAddress, destAddress, base, 0, format);
							}
						else
							isOperandValid = true;//reset
						
						}//END OF: if opcode is in table
					else if(OPCODE.equals("BYTE")){
						strObjCodeInstruction = 
							generator.generateObjcode(OPCODE, (byte)-1, OPERAND,
								0, 0, 0, 0, FORMATBYTE);
							}
					else if(OPCODE.equals("WORD")){
						strObjCodeInstruction = 
							generator.generateObjcode(OPCODE, (byte)-1, OPERAND,
								0, 0, 0, 0, FORMATWORD);
							}
					else if(OPCODE.equals("BASE")){
						if(symbolTable.containsKey(OPERAND))
							base = symbolTable.get(OPERAND);
						}
					else if(OPCODE.equals("RESW") || OPCODE.equals("RESB")){
						strObjCodeInstruction = "";
						}
					
					
					objWriter.write(String.format("%-37s%-9s",
							line, strObjCodeInstruction));
					objWriter.println();
					
					}//END OF: this is not a comment line

				line = reader.readLine();
				setTokens(line.substring(9));
				
				}//END OF while != "END"
			
			
			objWriter.write(line);
			
			objWriter.close();
			reader.close();
			}
		
			catch (IOException e) { e.printStackTrace(); }		
		
		}//END OF pass2
	
	
	
	private void setTokens(String line){
		/**setTokens: sets label, opcode, operand**/
		if(null == line){
			LABEL = OPCODE = OPERAND = "";
			return;
			}
		String[] sArray = line.trim().split("\\s+");
		if(sArray.length == 1){		//0 label, 1 opcode, 0 operand
			LABEL = "";
			OPCODE = sArray[0];
			OPERAND = "";
			}
		else if(sArray.length == 2){//0 label, 1 opcode, 1 operand
			LABEL = "";
			OPCODE = sArray[0];
			OPERAND = sArray[1];
			}
		else if(sArray.length == 3){//1 label, 1 opcode, 1 operand
			LABEL = sArray[0];
			OPCODE = sArray[1];
			OPERAND = sArray[2];
			}
		//if label length exceeds 6 then truncate the label
		if(LABEL.length() > MAX_SYMBOL_LEN)
			LABEL = LABEL.substring(0, (MAX_SYMBOL_LEN - 1));		
		
		}//END OF: setTokens

	
	/**Helper methods**/
	
	public void setInputFile(String inputFile){ fileName = inputFile; }
	
	
	public void setIsSIC(boolean is_sic){
		isSIC = is_sic;
		if(isSIC) buildSICOpcodeTable();
		else buildSICXEOpcodeTable();
		}
	
	
	public boolean isSIC(){ return isSIC; }
	
	
	public boolean isErrorFree(){ return isErrorFree; }
	
	
	private boolean isConstant(String str){  
		try  {  Integer.parseInt(str);  }  
	  	catch(NumberFormatException e)  {  return false;  }  
	  	return true;  
		}//END OF isDigit
	
	
	public class OpcodeObj{
		int hexRepresentation;
		byte format;
		OpcodeObj(int hr, byte frmt){
			hexRepresentation = hr;
			format = frmt;
			}
 		}//END OF: class OpcodeObj
	
	
	private void buildSICXEOpcodeTable(){
		opcodeTable.clear();
		opcodeTable.put("ADD",new OpcodeObj(0x18, FORMAT3));
		opcodeTable.put("ADDF",new OpcodeObj(0x58, FORMAT3));
		opcodeTable.put("ADDR",new OpcodeObj(0x90, FORMAT2));
		opcodeTable.put("AND",new OpcodeObj(0x40, FORMAT3));
		opcodeTable.put("CLEAR",new OpcodeObj(0xB4, FORMAT2));
		opcodeTable.put("COMP",new OpcodeObj(0x28, FORMAT3));
		opcodeTable.put("COMPF",new OpcodeObj(0x88, FORMAT3));
		opcodeTable.put("COMPR",new OpcodeObj(0xA0, FORMAT2));
		opcodeTable.put("DIV",new OpcodeObj(0x24, FORMAT3));
		opcodeTable.put("DIVF",new OpcodeObj(0x64, FORMAT3));
		opcodeTable.put("DIVR",new OpcodeObj(0x9C, FORMAT2));
		opcodeTable.put("FIX",new OpcodeObj(0xC4, FORMAT1));
		opcodeTable.put("FLOAT",new OpcodeObj(0xC0, FORMAT1));
		opcodeTable.put("HIO",new OpcodeObj(0xF4, FORMAT1));
		
		opcodeTable.put("J",new OpcodeObj(0x3C, FORMAT3));
		opcodeTable.put("JEQ",new OpcodeObj(0x30, FORMAT3));
		opcodeTable.put("JGT",new OpcodeObj(0x34, FORMAT3));
		opcodeTable.put("JLT",new OpcodeObj(0x38, FORMAT3));
		opcodeTable.put("JSUB",new OpcodeObj(0x48, FORMAT3));
		
		opcodeTable.put("LDA",new OpcodeObj(0x00, FORMAT3));
		opcodeTable.put("LDB",new OpcodeObj(0x68, FORMAT3));
		opcodeTable.put("LDCH",new OpcodeObj(0x50, FORMAT3));
		opcodeTable.put("LDF",new OpcodeObj(0x70, FORMAT3));
		opcodeTable.put("LDL",new OpcodeObj(0x08, FORMAT3));
		opcodeTable.put("LDS",new OpcodeObj(0x6C, FORMAT3));
		opcodeTable.put("LDT",new OpcodeObj(0x74, FORMAT3));
		opcodeTable.put("LDX",new OpcodeObj(0x04, FORMAT3));
		opcodeTable.put("LPS",new OpcodeObj(0xD0, FORMAT3));
		
		opcodeTable.put("MUL",new OpcodeObj(0x20, FORMAT3));
		opcodeTable.put("MULF",new OpcodeObj(0x60, FORMAT3));
		opcodeTable.put("MULR",new OpcodeObj(0x98, FORMAT2));
		opcodeTable.put("NORM",new OpcodeObj(0xC8, FORMAT1));
		opcodeTable.put("OR",new OpcodeObj(0x44, FORMAT3));
		opcodeTable.put("RD",new OpcodeObj(0xD8, FORMAT3));
		opcodeTable.put("RMO",new OpcodeObj(0xAC, FORMAT2));
		opcodeTable.put("RSUB",new OpcodeObj(0x4C, FORMAT3));
		opcodeTable.put("SHIFTL",new OpcodeObj(0xA4, FORMAT2));
		opcodeTable.put("SHIFTR",new OpcodeObj(0xA8, FORMAT2));
		opcodeTable.put("SIO",new OpcodeObj(0xF0, FORMAT1));
		opcodeTable.put("SSK",new OpcodeObj(0xEC, FORMAT3));
		
		opcodeTable.put("STA",new OpcodeObj(0x0C, FORMAT3));
		opcodeTable.put("STB",new OpcodeObj(0x78, FORMAT3));
		opcodeTable.put("STCH",new OpcodeObj(0x54, FORMAT3));
		opcodeTable.put("STF",new OpcodeObj(0x80, FORMAT3));
		opcodeTable.put("STI",new OpcodeObj(0xD4, FORMAT3));
		opcodeTable.put("STL",new OpcodeObj(0x14, FORMAT3));
		opcodeTable.put("STS",new OpcodeObj(0x7C, FORMAT3));
		opcodeTable.put("STSW",new OpcodeObj(0xE8, FORMAT3));
		opcodeTable.put("STT",new OpcodeObj(0x84, FORMAT3));
		opcodeTable.put("STX",new OpcodeObj(0x10, FORMAT3));
		
		opcodeTable.put("SUB",new OpcodeObj(0x1C, FORMAT3));
		opcodeTable.put("SUBF",new OpcodeObj(0x5C, FORMAT3));
		opcodeTable.put("SUBR",new OpcodeObj(0x94, FORMAT2));
		opcodeTable.put("SVC",new OpcodeObj(0xB0, FORMAT2));
		opcodeTable.put("TD",new OpcodeObj(0xE0, FORMAT3));
		opcodeTable.put("TIO",new OpcodeObj(0xF8, FORMAT1));
		opcodeTable.put("TIX",new OpcodeObj(0x2C, FORMAT3));
		opcodeTable.put("TIXR",new OpcodeObj(0xB8, FORMAT2));
		opcodeTable.put("WD",new OpcodeObj(0xDC, FORMAT3));
		
		/**insert register table values**/
		registerTable.put("A", (byte)0);
		registerTable.put("X", (byte)1);
		registerTable.put("L", (byte)2);
		registerTable.put("B", (byte)3);
		registerTable.put("S", (byte)4);
		registerTable.put("T", (byte)5);
		registerTable.put("F", (byte)6);
		registerTable.put("PC", (byte)8);
		registerTable.put("SW", (byte)9);
		}
	
	
	private void buildSICOpcodeTable(){
		opcodeTable.clear();
		opcodeTable.put("ADD",new OpcodeObj(0x18, FORMAT3));
		opcodeTable.put("AND",new OpcodeObj(0x40, FORMAT3));
		opcodeTable.put("CLEAR",new OpcodeObj(0xB4, FORMAT2));
		opcodeTable.put("COMP",new OpcodeObj(0x28, FORMAT3));
		opcodeTable.put("DIV",new OpcodeObj(0x24, FORMAT3));
		
		opcodeTable.put("J",new OpcodeObj(0x3C, FORMAT3));
		opcodeTable.put("JEQ",new OpcodeObj(0x30, FORMAT3));
		opcodeTable.put("JGT",new OpcodeObj(0x34, FORMAT3));
		opcodeTable.put("JLT",new OpcodeObj(0x38, FORMAT3));
		opcodeTable.put("JSUB",new OpcodeObj(0x48, FORMAT3));
		
		opcodeTable.put("LDA",new OpcodeObj(0x00, FORMAT3));
		opcodeTable.put("LDCH",new OpcodeObj(0x50, FORMAT3));
		opcodeTable.put("LDL",new OpcodeObj(0x08, FORMAT3));
		opcodeTable.put("LDX",new OpcodeObj(0x04, FORMAT3));
		opcodeTable.put("LPS",new OpcodeObj(0xD0, FORMAT3));
		
		opcodeTable.put("MUL",new OpcodeObj(0x20, FORMAT3));
		opcodeTable.put("OR",new OpcodeObj(0x44, FORMAT3));
		opcodeTable.put("RD",new OpcodeObj(0xD8, FORMAT3));
		opcodeTable.put("RSUB",new OpcodeObj(0x4C, FORMAT3));
		opcodeTable.put("SHIFTL",new OpcodeObj(0xA4, FORMAT2));
		opcodeTable.put("SHIFTR",new OpcodeObj(0xA8, FORMAT2));
		opcodeTable.put("SSK",new OpcodeObj(0xEC, FORMAT3));
		
		opcodeTable.put("STA",new OpcodeObj(0x0C, FORMAT3));
		opcodeTable.put("STCH",new OpcodeObj(0x54, FORMAT3));
		opcodeTable.put("STI",new OpcodeObj(0xD4, FORMAT3));
		opcodeTable.put("STL",new OpcodeObj(0x14, FORMAT3));
		opcodeTable.put("STSW",new OpcodeObj(0xE8, FORMAT3));
		opcodeTable.put("STX",new OpcodeObj(0x10, FORMAT3));
		
		opcodeTable.put("SUB",new OpcodeObj(0x1C, FORMAT3));
		opcodeTable.put("SVC",new OpcodeObj(0xB0, FORMAT2));
		opcodeTable.put("TD",new OpcodeObj(0xE0, FORMAT3));
		opcodeTable.put("TIO",new OpcodeObj(0xF8, FORMAT1));
		opcodeTable.put("TIX",new OpcodeObj(0x2C, FORMAT3));
		opcodeTable.put("WD",new OpcodeObj(0xDC, FORMAT3));
		}
	
	}//END OF: class Assembler