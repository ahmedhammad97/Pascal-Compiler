
package compiler;

import static compiler.Compiler.programName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Compiler {
    
    protected static ArrayList inp = new ArrayList<String>();
    protected static Map tokens = new HashMap<String,Integer>();
    protected static HashMap reversedTokens = new HashMap<Integer,String>();
    protected static String strInp = new String();
    protected static String fullCheckRegex = new String();
    protected static String programName;
    protected static ArrayList variables = new ArrayList<String>();
    protected static String program;
    protected static ArrayList<String> commandsList = new ArrayList<String>();
    protected static String[] commands;
    protected static List tokenStream = new ArrayList<Token>();
    
    
    public void loadInput(String s) throws FileNotFoundException{
        Scanner scan = new Scanner(new File("./"+s));
        while(scan.hasNextLine()){
            inp.add(scan.nextLine());
        }
    }
    
    public void loadRegex() throws FileNotFoundException{
        Scanner scan = new Scanner(new File("./fullCheckRegex.txt"));
        fullCheckRegex = scan.nextLine();
    }
    
    public void getInputAsString() throws FileNotFoundException{
        for(int i=0;i<inp.size();i++){strInp = strInp + " " + inp.get(i);}
        //System.out.println(strInp);
        strInp = strInp.trim();
    }
    
    public boolean checkProgramValidity(){
        if(!strInp.matches(fullCheckRegex)){System.out.println("Compilation Error!");return false;}
        return true;
    }
    
    public void initializeTokenMap() throws FileNotFoundException{
        int i=1;
        Scanner scan = new Scanner(new File("./tokens.txt"));
        while(scan.hasNextLine()){tokens.put(scan.nextLine(), i);i++;}
        
        for (Iterator it = tokens.entrySet().iterator(); it.hasNext();) {
            HashMap.Entry me = (HashMap.Entry) it.next();
            reversedTokens.put(me.getValue(), me.getKey());
        }
    }
    
    public int getTokenNum(String s){
        if(tokens.containsKey(s)){return (int) tokens.get(s);}
        else{return Integer.MAX_VALUE;}
    }
    
    public String getTokenType(int num){
        if(reversedTokens.containsKey(num)){return (String) reversedTokens.get(num);}
        else{return "ERROR!";}
    }
    
    public boolean splitCode(){
        int s = strInp.indexOf("PROGRAM");
        int f = strInp.indexOf("VAR");
        if(s==-1 || f==-1){System.out.println("Compilation Error1");return false;}
        s+=7;
        programName = strInp.substring(s, f);
        programName = programName.trim();
        tokenStream.add(new Token("PROGRAM",1));
        tokenStream.add(new Token(programName,16));
        tokenStream.add(new Token("VAR",2));
        
        f+=3;
        s = strInp.indexOf("BEGIN");
        if(s==-1){System.out.println("Compilation Error2");return false;}
        String temp = strInp.substring(f, s);
        temp = temp.trim();
        String x[] = temp.split("\\s*,\\s*");
        ArrayList sdf = new ArrayList<Token>();
        for(int i=0;i<x.length;i++){
            if(!(x[i].equals(",")||x[i].equals(" "))){
                x[i] = x[i].trim();
                if(!x[i].matches("[A-Za-z][A-Za-z0-9|_]*")){System.out.println("Compliation Error3");return false;}
                variables.add(x[i]);
                sdf.add(new Token(x[i],16));
            }
        }
        tokenStream.addAll(sdf);
        tokenStream.add(new Token("BEGIN",3));
        
        s+=5;
        f = strInp.indexOf("END.");
        if(f==-1){System.out.println("Compilation Error4");return false;}
        program = strInp.substring(s, f);
        program = removeSpaces(program);
        
        String reg = "([a-zA-Z][a-zA-Z0-9|_]*\\s*:=\\s*[a-zA-Z,\\(,\\)]\\s*[a-zA-Z0-9|_]*\\s*(?:[\\(,\\)]*\\s*[\\+,\\*,\\(]\\s*[\\(,\\)]*\\s*(?:[a-zA-Z][a-zA-Z0-9|_]*|[0-9]+))*\\s*[\\)]*\\s*;)|(READ\\s*\\(\\s*[a-zA-Z][a-zA-Z0-9|_]*(?:\\s*,\\s*[a-zA-Z_][a-zA-Z0-9_]*)*\\)[;]*)\\s*|(WRITE\\([a-zA-Z_][a-zA-Z0-9_]*(?:\\s*,\\s*[a-zA-Z_][a-zA-Z0-9_]*)*\\)[;]*)";
        final Pattern pattern = Pattern.compile(reg);
        final Matcher matcher = pattern.matcher(program);

        while (matcher.find()) {
            //System.out.println("Full match: " + matcher.group(0));
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if(!(matcher.group(i)==null)){
                    commandsList.add(matcher.group(i));
                    //System.out.println("Group " + i + ": " + matcher.group(i));
                }
            }
        }
        
        int progCount = removeSpaces(new String(program)).length();
        int count = countArray(commandsList);
        if(count!=progCount){System.out.println("Compilation Error " + progCount + " " + count);return false;}
        
        return true;
        
    }
    
    public boolean splitCommandsToTokens(){
        
        for(String temp : commandsList){
            //System.out.println(temp);
            if(temp.contains("READ")||temp.contains("WRITE")){
                String[] x = temp.split("READ\\s*\\(|WRITE\\s*\\(|,|\\)[;]*");
                String qwerty;
                if(temp.contains("READ")){qwerty="READ";}else{qwerty="WRITE";}
                tokenStream.add(new Token(qwerty,getTokenNum(qwerty)));
                tokenStream.add(new Token("(",14));
                for(int k=0;k<x.length;k++){
                    x[k] = x[k].trim();
                    if(!x[k].isEmpty()){
                        if(variables.contains(x[k])){
                            tokenStream.add(new Token(x[k],16));
                        }
                        else{System.out.println("Compilation Error: Invalid Variable " + x[k]);System.out.println(Arrays.asList(x));return false;}
                    }
                }
                tokenStream.add(new Token(")",15));
            }
            else{
                //String[] y = temp.split("\\s*([A-Z][A-Z0-9]*)\\s*=\\s*([A-Z][A-Z0-9]*)\\s*([\\+|\\*]\\s*([A-Z][A-Z0-9]*))*\\s*;");
                String[] operands = temp.split("[+*[:=];-]");
                String[] operations = temp.split("[a-zA-Z0-9_,\\(,\\)]");
                ArrayList sum1 = new ArrayList<String>();
                ArrayList sum2 = new ArrayList<String>();
                for(int k=0;k<operands.length;k++){if(!(operands[k].isEmpty()||operands[k].equals("\\s+"))){sum1.add(operands[k]);}}
                for(int k=0;k<operations.length;k++){if(!(operations[k].isEmpty()||operations[k].equals("\\s+"))){sum2.add(operations[k]);}}
                if(sum1.size()!=sum2.size()){System.out.println("Error, not matching arrays");}
                ArrayList ttt = new ArrayList<Token>();
                boolean close = false;
                for(int k=0;k<sum1.size();k++){
                    String tempp = (String) sum1.get(k);
                    tempp = tempp.trim();
                    if(tempp.charAt(0)=='('){tempp = tempp.substring(1); ttt.add(new Token("(",14));sum1.remove(k);sum1.add(k,tempp);}
                    int last = tempp.length()-1;
                    if(tempp.charAt(last)==')'){tempp = tempp.substring(0,last);sum1.remove(k);sum1.add(k,tempp);close=true;}
                    if(!variables.contains(sum1.get(k))){System.out.println("Compilation Error : Invalid Variable " + sum1.get(k));return false;}
                    ttt.add(new Token((String) sum1.get(k),16));
                    if(close){ttt.add(new Token(")",15));close=false;}
                    String sdfs = sum2.get(k).toString();
                    sdfs = sdfs.trim();
                    ttt.add(new Token((String) sum2.get(k), getTokenNum(sdfs)));
                }
                tokenStream.addAll(ttt);
                
                
            }
        }
        tokenStream.add(new Token("END.",5));
        return true;
    }
    
    public String removeSpaces(String s){
        String temp="";
        for(int i=0;i<s.length();i++){
            if(s.charAt(i)!=' '){temp = temp + s.charAt(i);}
        }
        //System.out.println(temp);
        return temp;
    }
    
    public int countArray(ArrayList<String> s){
        int sum = 0;
        for(int i=0;i<s.size();i++){
            sum+= removeSpaces(s.get(i)).length();
        }
        return sum;
    }
    
    public void printTokens(){
        for(int i=0;i<tokenStream.size();i++){
            Token temp = (Token) tokenStream.get(i);
            temp.printThis();
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        Compiler x = new Compiler();
        x.loadInput(args[0]);
        x.getInputAsString();
        x.initializeTokenMap();
        if(!x.splitCode()){return;}
        if(!x.splitCommandsToTokens()){return;}
        CodeGenerator bla = new CodeGenerator((ArrayList<Token>) tokenStream);
        bla.doTheJob();
        bla.writeFile();
                
        
        
    }
    
    
}



class Token{
    private String value;
    private int mapType;
    
    public Token(String value, int type){
        value = value.trim();
        this.value = value;
        this.mapType = type;
    }
    
    public void printThis(){
        System.out.println(this.value + " :" + new Compiler().getTokenType(this.mapType) + "\n");
    }
    
    public String getType(){
        return new Compiler().getTokenType(this.mapType);
    }
    
    public String getValue(){
        return this.value;
    }
   
}




    class CodeGenerator {
    protected ArrayList tokens = new ArrayList<Token>();
    protected ArrayList arr = new ArrayList<String>();
    private Stack theStack = new Stack<Character>();
    private String input="";
    private String output = "";
    
    public CodeGenerator(ArrayList<Token> tokens){
        this.tokens = tokens;
    }
    
    public void printTokens(){
        for(int i=0;i<tokens.size();i++){
            Token temp = (Token) tokens.get(i);
            temp.printThis();
        }
    }
    
    public void writeFile() throws IOException{
        for(int i=0;i<arr.size();i++){
            System.out.println(arr.get(i));
        }
        FileWriter f = new FileWriter("./" + programName +".asm");
        for(Object x : arr){
            String temp = (String) x;
            f.write(temp);
            f.write("\n");
        }
        f.close();
    }
    
    public void doTheJob(){
        boolean commands=false;
        for(int i=0;i<tokens.size();i++){
            Token temp = (Token) tokens.get(i);
            switch(temp.getType()){
                
                case "PROGRAM" : 
                    Token name = (Token) tokens.get(i+1);
                    arr.add(name.getValue() + "\t\t" + "START\t\t" + "0");
                    i++;
                break;
                    
                case "VAR" : 
                    arr.add("\t\tEXTREF\t\tXREAD,XWRITE");
                    arr.add("\t\tSTL\t\tRETADR");
                    arr.add("\t\tJ\t\t{EXTADAR}");
                    i++;
                    while(true){
                        Token x = (Token) tokens.get(i);
                        if(x.getType().equals("id")){
                            arr.add(x.getValue()+"\t\t"+"RESW\t\t1");
                            i++;
                        }else{i--;break;}
                    }
                    arr.add("EXTADAR\t\tLDA\t\t#0");
                break;
                
                case "BEGIN" : commands=true; break;
                
                case "END." :
                    arr.add("\t\tEND\t\t0");
                break;
                
                case "READ" : 
                    arr.add("\t\t+JSUB\t\tXREAD");
                    int count=0;
                    i++;
                    while(true){
                        Token x = (Token) tokens.get(i);
                        if(x.getValue().equals("(")){i++;continue;}
                        if(x.getValue().equals(")")){break;}
                        else{
                            arr.add("\t\tWORD\t\t"+x.getValue());
                            i++;
                            count++;
                        }
                    }
                    arr.add(arr.size()-count,"\t\tWORD\t\t"+count);
                break;
                    
                case "WRITE" : 
                arr.add("\t\t+JSUB\t\tXWRITE");
                    int countt=0;
                    i++;
                    while(true){
                        Token x = (Token) tokens.get(i);
                        if(x.getValue().equals("(")){i++;continue;}
                        if(x.getValue().equals(")")){break;}
                        else{
                            arr.add("\t\tWORD\t\t"+x.getValue());
                            i++;
                            countt++;
                        }
                    }
                    arr.add(arr.size()-countt,"\t\tWORD\t\t"+countt);    
                break;
                
                case "id":
                    String res = temp.getValue();
                    i+=2;
                    while(true){
                        Token y = (Token) tokens.get(i);
                        if(y.getValue().equals(";")){break;}
                        else{
                            input = input + " " + y.getValue() + " ";
                            i++;
                        }
                    }
                    doTrans();
                    String[] xx = output.split("\\s+");
                    //System.out.println(Arrays.asList(xx));
                    Stack<String> s = new Stack<String>();
                    boolean stored=false;
                    for(int k=0;k<xx.length;k++){
                        xx[k] = xx[k].trim();
                        if(xx[k].isEmpty()){continue;}
                        if(xx.length==1){
                            arr.add("\t\tLDA\t\t"+xx[0]);continue;
                        }
                        if(xx[k].equals("+") || xx[k].equals("-") || xx[k].equals("*") || xx[k].equals("/")){
                            if(stored){
                                String bs = s.pop();
                                switch(xx[k]){
                                    case "+" : arr.add("\t\tADD\t\t"+bs);break;
                                    case "-" : arr.add("\t\tSUB\t\t"+bs);break;
                                    case "*" : arr.add("\t\tMUL\t\t"+bs);break;
                                    case "/" : arr.add("\t\tDIV\t\t"+bs);break;
                                }
                                
                            }
                            else{
                                arr.add("\t\tLDA\t\t"+s.pop());
                                String bs = s.pop();
                                switch(xx[k]){
                                    case "+" : arr.add("\t\tADD\t\t"+bs);break;
                                    case "-" : arr.add("\t\tSUB\t\t"+bs);break;
                                    case "*" : arr.add("\t\tMUL\t\t"+bs);break;
                                    case "/" : arr.add("\t\tDIV\t\t"+bs);break;
                                }
                                stored=true;
                            }
                        }
                        else{
                            s.push(xx[k]);
                        }
                        
                        
                        
                    }
                    
                    
                    input="";
                    output="";
                    arr.add("\t\tSTA\t\t"+res);
                break;    
                
            }
        }
    }
    
    
    public String doTrans() {
      for (int j = 0; j < input.length(); j++) {
         char ch = input.charAt(j);
         switch (ch) {
            case '+': 
            case '-':
               gotOper(ch, 1); 
               break; 
            case '*': 
            case '/':
               gotOper(ch, 2); 
               break; 
            case '(': 
               theStack.push(ch);
               break;
            case ')': 
               gotParen(ch); 
               break;
            default: 
               output = output + ch; 
               break;
         }
      }
      while (!theStack.isEmpty()) {
         output = output + theStack.pop();
      }
      //System.out.println(output);
      return output; 
   }
   public void gotOper(char opThis, int prec1) {
      while (!theStack.isEmpty()) {
         char opTop = (char) theStack.pop();
         if (opTop == '(') {
            theStack.push(opTop);
            break;
         } else {
            int prec2;
            if (opTop == '+' || opTop == '-')
            prec2 = 1;
            else
            prec2 = 2;
            if (prec2 < prec1) { 
               theStack.push(opTop);
               break;
            } 
            else output = output + " " + opTop;
         }
      }
      theStack.push(opThis);
   }
   public void gotParen(char ch) { 
      while (!theStack.isEmpty()) {
         char chx = (char) theStack.pop();
         if (chx == '(') 
         break; 
         else output = output + " " + chx; 
      }
   }
    
}