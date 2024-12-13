package main.java;

public class NotionInterface {
    private final String databaseID = "2bb7271bc23b47c880fdedea1aee5dc6";
    private String baseURL = "https://api.notion.com";
    private EZFileReader reader;

    private String[] endpoints;
    public static void main(String[] args) {
        Main main = new Main();

    }
    private NotionInterface(EZFileReader reader){
        this.endpoints = new String[8];
        this.reader = reader;
    }
    private void readEndpoints(){

    }
    private void buildGETRequestURL(){
       // final String
    }
    private void sendGETRequest(){

    }
}
