package UIAssembler;
 
 /*
 This class creates the graphic user interface for the assembler. It provides the user with two canvas: The one on the 
 left for typing SIC/SICXE instructions, the one on the right for displaying output after assembly.
 On the upper left are two radio buttons for switching between SIC and SICXE. on the bottom is a submit button used to 
 submit code are assembly and a cancel button to clear the left canvas in order to type in new code.
 This code was created by my project partner, Vo Trang. 
 */
 
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.logging.*;


public class InputDialogue extends JPanel
                             implements ActionListener {
    
    static JRadioButton sicOption = new JRadioButton("SIC");
    static JRadioButton XEOption = new JRadioButton("SIC/XE");
    static JPanel optionLayout = new JPanel();
    static JLabel inputLabel = new JLabel("Please put in your code");
    static JLabel outputLabel = new JLabel("Output");
    static JPanel ipanelLayout = new JPanel();
    static JPanel opanelLayout = new JPanel();
    static JPanel textLayout = new JPanel();
    static JPanel buttonPanel = new JPanel();
    static JTextArea textInput= new JTextArea(30,50);
    static JTextArea textOutput = new JTextArea(30,50);
    JScrollPane scrollPane = new JScrollPane(textInput);
    JScrollPane scrollPane2 = new JScrollPane(textOutput);
    protected JButton submitBtn = new JButton("Submit");
    protected JButton cancelBtn = new JButton("Cancel");

    
    
    
    
    public InputDialogue(){
        super(new BorderLayout());
        
        sicOption.setSelected(true);
        //Radio Button Group
        ButtonGroup languageChoice = new ButtonGroup();
        languageChoice.add(sicOption);
        languageChoice.add(XEOption);
        
        //prevent text area from expanding
        textInput.setLineWrap(true);
        textInput.setWrapStyleWord(true);
        
        //prevent text area from expanding 
        //and uneditable for output text area
        textOutput.setLineWrap(true);
        textOutput.setWrapStyleWord(true);
        textOutput.setEditable(false);
        
        //Scroll bars
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        
        
        //submit button
        submitBtn.setVerticalTextPosition(AbstractButton.CENTER);
        submitBtn.setHorizontalTextPosition(AbstractButton.CENTER);
        submitBtn.setMnemonic(KeyEvent.VK_S);
        submitBtn.setActionCommand("submit");
        
        //cancel button
        cancelBtn.setVerticalTextPosition(AbstractButton.BOTTOM);
        cancelBtn.setHorizontalTextPosition(AbstractButton.CENTER);
        cancelBtn.setMnemonic(KeyEvent.VK_C);
        cancelBtn.setActionCommand("cancel");
        
        //listen for actions on buttons
        submitEvent e = new submitEvent();
        cancelEvent e1 = new cancelEvent();
        submitBtn.addActionListener(e);
        cancelBtn.addActionListener(e1);
        
        FlowLayout mainLayout= new FlowLayout();
        
        //option panel
        optionLayout.add(sicOption);
        optionLayout.add(XEOption);
        optionLayout.setLayout(new BoxLayout(optionLayout, BoxLayout.X_AXIS));
        
        //add elements to Input Panel
        ipanelLayout.add(inputLabel);
        ipanelLayout.add(scrollPane);
        ipanelLayout.setLayout(new BoxLayout(ipanelLayout, BoxLayout.Y_AXIS));
    
        //add elements to Output Panel
        opanelLayout.add(outputLabel);
        opanelLayout.add(scrollPane2);
        opanelLayout.setLayout(new BoxLayout(opanelLayout, BoxLayout.Y_AXIS));
       
        //to label panel
        textLayout.add(ipanelLayout);
        textLayout.add(opanelLayout);
        
        //Set Layout label panel
        textLayout.setLayout(mainLayout);
        textLayout.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        
        //button panel
        buttonPanel.add(submitBtn);
        buttonPanel.add(cancelBtn);
        
        //Layout and add elements to main panel
        add(optionLayout, BorderLayout.NORTH);
        add(textLayout, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        
        
        
    }

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); 
    //To change body of generated methods, choose Tools | Templates.
    }

    
    
    public class submitEvent implements ActionListener{
            public void actionPerformed(ActionEvent e){
                BufferedWriter fileOut;
                String text2 = textInput.getText();
                try{ 
                    fileOut = new BufferedWriter(new FileWriter("inputCode.txt", false));
                    textInput.write(fileOut);
                    fileOut.close();                
                } catch (IOException ex) {
                    Logger.getLogger(InputDialogue.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                Assembler codeAssem = new Assembler("inputCode.txt", sicOption.isSelected());
                codeAssem.assemble();
                
            if(codeAssem.isErrorFree())
                printOutputFile("finalAssembly.txt");
            else
                printOutputFile("errorMsg.txt");
            
        }
    }
            
    public class cancelEvent implements ActionListener{
            public void actionPerformed(ActionEvent e){
                textInput.setText("");
            }
    }
    
    
    private void printOutputFile (String fileName){
        FileReader reader = null;
            try {
            	reader = new FileReader(fileName);
            	textOutput.read(reader, fileName);
            	}catch(IOException exception){
            		System.err.println("Errors while printing out results");
            		exception.printStackTrace();
            	}finally{
            		if(reader !=null){
            			try{
            				reader.close();
            				}catch(IOException exception){
            				System.err.println("error closing reader");
            				exception.printStackTrace();
                        	}
                    	}
                	} 
    	}//END OF: printOutputFile
    
    
    private static void createAndShowGUI(){
        //create and set up the window
        JFrame frame = new JFrame("Input Code");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JComponent newContentPane = new InputDialogue();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

public static void main(String[] args){

    javax.swing.SwingUtilities.invokeLater(new Runnable(){
        public void run() {
            createAndShowGUI();
        }
    });
    }
}


