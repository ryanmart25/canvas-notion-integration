package main.java;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Scanner;
import main.java.EZFileReader;
public class Main {


    public static void main(String[] args) {
        Main main = new Main();
        main.launch();
        main.makeRequest();
        //System.out.println(main.getToken());
        main.exit();
    }
    private void launch(){
        System.out.println("Launching...");
    }
    private String[] getParameters(){
        System.out.println("input parameters:");
        short numparams = 3;
        String[] params = new String[numparams];
        Scanner scan = new Scanner(System.in);
        for(int i = 0; i < numparams;i++){
            params[i] = scan.nextLine();

        }
        scan.close();
        return params;
    }
    private URL buildurl(){
        URL url;
        try {
            url = new URL("https://canvas.instructure.com/api/v1/courses");
        } catch (MalformedURLException e) {
            System.out.println("malformed url");
            throw new RuntimeException(e);
        }
        return url;

    }
    private String getToken(){
        EZFileReader reader = new EZFileReader(new File("src\\main\\resources\\sensitives.txt"));
        String line ;
        while(true){
            line = reader.readLine();
            if(line.equals("[START]")){
                continue;
            }
            else if(line.equals("[END]")){
                return "void";
            }
            else{
                reader.close();
                return line.substring(13);
            }
        }

    }
    private void makeRequest(){
        URL url = buildurl();
        if(url!= null){
            try{
                HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Authorization", "Bearer " + this.getToken());
                if(con.getResponseCode() == 200){
                    print_cert_info(con);
                    print_content(con);
                }
            } catch (IOException e) {
                System.out.println("Error: Server responded with: " + e);
            }
        }

    }
    private void print_cert_info(HttpsURLConnection con){

        if(con!=null){
            try{
               System.out.println("Response Code: "+ con.getResponseCode());
               System.out.println("Authorization Header: " + con.getHeaderField("Authorization"));
               System.out.println("Cipher Suite: " + con.getCipherSuite());
               System.out.println("\n");

                Certificate[] certs = con.getServerCertificates();
                for (Certificate cert :
                        certs) {
                    System.out.println("Cert Type: " + cert.getType());
                    System.out.println("Cert Hash Code: " + cert.hashCode());
                    System.out.println("Cert Public Key Algorithm: " + cert.getPublicKey().getAlgorithm());
                    System.out.println("Cert Public Key Format: " + cert.getPublicKey().getFormat());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
    private void print_content(HttpsURLConnection con){
        if(con!=null){
            try {
                System.out.println("*****Content*****");
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String input;
                while((input = reader.readLine()) != null){
                    System.out.println(input);
                }
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void exit(){
        System.out.println("Tearing down.");
    }
}