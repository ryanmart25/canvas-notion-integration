package main.java;

import java.io.*;

public class EZFileReader {
    //fields
    private BufferedReader fileReader;
    //constructors
    public EZFileReader(String filename){
        try {
            this.fileReader = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            this.fileReader = null;
        }
    }
    public EZFileReader(File file){
        try{
            this.fileReader = new BufferedReader(new FileReader(file));
        }catch (FileNotFoundException e){
            System.out.println("File not found");
            this.fileReader = null;
        }
    }
    //methods
    public void close(){
        try {
            this.fileReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public String readLine(){
        if(this.fileReader != null){
            try{
                String line = "";
                if((line = this.fileReader.readLine()) != null){
                    return line;
                }
            }catch (IOException e){
                e.printStackTrace();
                return "[END]";
            }

        }
        return "void";
    }
    public void readToStdOut(){
        if(this.fileReader != null){
            String line = "";
            try{
                while((line = this.fileReader.readLine()) != null){
                    System.out.println(line);
                }
            }catch (IOException e){
                System.out.println("IO exception");

            }
        }

    }
}
