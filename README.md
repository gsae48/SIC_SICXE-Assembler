# SIC_SICXE-Assembler
This is an assembler for the sic and sicxe assembly language.

Assembler has two invokable constructors:

Assembler(String inputFile, boolean is_Sic){...}

Assembler(){...}//No input file, is_Sic is set to true by default

To invoke the assembler:

method 1)
Assembler myAssem = new Assembler("input.txt", true);

myAssem.assemble();

method 2)
Assembler myAssem = new Assembler();

myAssem.setInputFile("input.txt");

myAssem.setIsSic(false); //set it to SICXE

myAssem.assemble();
