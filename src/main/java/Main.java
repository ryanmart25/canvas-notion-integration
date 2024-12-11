package main.java;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {
private final String[] parameters = {};
private final String canvasToken = "11299~nX3JnXvhLLVCMYPXWEMuQrP92MneCJea9f34D8FE8XcJWtJCnVG97kYMQMUByCFU";
private final String databaseID = "516c52da34584026a3c4b785e954349d";
private final String notionToken = "secret_P1xfxva0V71KkN3SMvzwBGuai79BPMZGD6JqImcWeZ4";
private static final String[] courseIDs= {"112990000000124161", "112990000000093656", "112990000000114806", "112990000000104413", "112990000000113372", "112990000000106387", "112990000000084604", "112990000000112434", "112990000000089826"};
    public static void main(String[] args) {
        Main main = new Main();
        //Scanner scanner = new Scanner(System.in);
        //Specifies what the user would like to do.
        // 1. "All Courses" prints a list of currently enrolled courses
        // 2. "<name of course>" prints a list of assignments for a specific course
        //String which = scanner.nextLine();
        main.makeCoursesRequest();
        for (int i = 0; i < courseIDs.length; i++) {
            ArrayList<String> compiledAssignments = main.makeAssignmentsRequest(courseIDs[i]);
            System.out.println(compiledAssignments);
        }

        main.exit();
    }
    private URL buildAssignmentRequestURL(String courseid){
        URL url;
        try{
            url = new URL("https://canvas.instructure.com/api/v1/courses/"+courseid+"/assignments");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }
    private ArrayList<String> makeAssignmentsRequest(String courseID) { // todo extract notion request to a seperate method | extract for-each out of method. this method should only make 1 request per course
        // this should be archived.
        ArrayList<String> compiledAssignments = new ArrayList<>();
        URL url = buildAssignmentRequestURL(courseID);
        if(url != null){
            try{
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + canvasToken);
                //System.out.println("Headers: " + connection.getHeaderFields());
                System.out.println("Assignment request Server Response Code: "+connection.getResponseCode());
                //print_assignments(connection);
                compiledAssignments = compileassignments(connection);
                if(compiledAssignments.get(0).equals("failure")){
                    System.out.println("failed to compile assignments, skipping\nLikely reason: Course is not published, or this user does not have access to this course. ");

                }
                else {
                    return compiledAssignments;
                }
            } catch (IOException e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }

        return compiledAssignments;
    }

    private ArrayList<String> compileassignments(HttpsURLConnection con){
        ArrayList<String> failure = new ArrayList<>();
        failure.add("failed");
        if(con!=null){
            try {
                if(con.getResponseCode() == 200){
                    System.out.println("*****Assignments*****");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String input;
                    StringBuilder fullinput = new StringBuilder();
                    JSONParser parser = new JSONParser();
                    while((input = reader.readLine()) != null){
                        fullinput.append(input);
                    }
                    reader.close();
                    JSONArray array = (JSONArray) parser.parse(fullinput.toString());
                    Iterator<JSONObject> iterator = array.iterator();
                    ArrayList<String> assignments_out = new ArrayList<>();

                    while(iterator.hasNext()){ //put assignments into one JSON payload
                        JSONObject assignment = iterator.next();
                        String duedate;
                        String notes;
                        if(assignment.get("due_at") == null){
                            duedate = "2024-10-5";
                        }
                        else{
                            duedate = assignment.get("due_at").toString();
                        }
                        if(assignment.get("description") == null){
                            notes = "no notes";
                        }
                        else{
                            notes = assignment.get("description").toString();
                        }
                        String currentAssignment = "{\n" +
                                "            \"parent\": {\n" +
                                "                \"type\": \"database_id\",\n" +
                                "                \"database_id\": \"516c52da-3458-4026-a3c4-b785e954349d\"\n" +//hardcoded database id
                                "            },\n" +
                                "            \"properties\": {\n" +
                                "                \"Dates\": {\n" +
                                "                    \"id\": \"0%23%7Dp\",\n" +
                                "                    \"type\": \"date\",\n" +
                                "                    \"date\": {\n" +
                                "                        \"start\": \""+duedate+"\",\n" +
                                "                        \"end\": null,\n" +
                                "                        \"time_zone\": null\n" +
                                "                    }\n" +
                                "                },\n" +
                                "                \"Task\": {\n" +
                                "                    \"id\": \"2%3DL6\",\n" +
                                "                    \"type\": \"multi_select\",\n" +
                                "                    \"multi_select\": [\n" +
                                "                        {\n" +
                                "                            \"name\": \"Assignment\",\n" +
                                "                            \"color\": \"brown\"\n" +
                                "                        }\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                \"Status\": {\n" +
                                "                    \"type\": \"checkbox\",\n" +
                                "                    \"checkbox\": false\n" +
                                "                },\n" +
                                "                \"Notes\": {\n" +
                                "                    \"type\": \"rich_text\",\n" +
                                "                    \"rich_text\": [\n" +
                                "                        {\n" +
                                "                            \"type\": \"text\",\n" +
                                "                            \"text\": {\n" +
                                "                                \"content\": \"sample text\",\n" +
                                "                                \"link\": null\n" +
                                "                            },\n" +
                                "                            \"annotations\": {\n" +
                                "                                \"bold\": false,\n" +
                                "                                \"italic\": false,\n" +
                                "                                \"strikethrough\": false,\n" +
                                "                                \"underline\": false,\n" +
                                "                                \"code\": false,\n" +
                                "                                \"color\": \"default\"\n" +
                                "                            },\n" +
                                "                            \"plain_text\": \""+notes+"\",\n" +
                                "                            \"href\": null\n" +
                                "                        }\n" +
                                "                    ]\n" +
                                "                },\n" +
                                "                \"Course\": {\n" +
                                "                    \"id\": \"ysX%5E\",\n" +
                                "                    \"type\": \"select\",\n" +
                                "                    \"select\": {\n" +
                                "                        \"name\": \"Applied Linear Algrebra\",\n" +
                                "                        \"color\": \"red\"\n" +
                                "                    }\n" +
                                "                },\n" +
                                "                \"Name\": {\n" +
                                "                    \"id\": \"title\",\n" +
                                "                    \"type\": \"title\",\n" +
                                "                    \"title\": [\n" +
                                "                        {\n" +
                                "                            \"type\": \"text\",\n" +
                                "                            \"text\": {\n" +
                                "                                \"content\": \"\",\n" +
                                "                                \"link\": null\n" +
                                "                            },\n" +
                                "                            \"annotations\": {\n" +
                                "                                \"bold\": false,\n" +
                                "                                \"italic\": false,\n" +
                                "                                \"strikethrough\": false,\n" +
                                "                                \"underline\": false,\n" +
                                "                                \"code\": false,\n" +
                                "                                \"color\": \"default\"\n" +
                                "                            },\n" +
                                "                            \"plain_text\": \"\"+assignment.get(\"name\")+\"\",\n" +
                                "                            \"href\": null\n" +
                                "                        }\n" +
                                "                    ]\n" +
                                "                }\n" +
                                "            },\n" +
                                "        },";
                        assignments_out.add(currentAssignment);

                        System.out.println("~~~~~~~ Loaded assignment");
                    }
                    System.out.println("End of list");
                    return assignments_out;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return failure;
    }
    private URL buildURLNotionPostRequest(String databaseID){
        URL url;
        try{
            url = new URL("https://api.notion.com/v1/databases/"+databaseID+"/query");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }
    private void makeNotionPostRequest(String payload){
        ////queries a database to find out information on pages
        URL url =buildURLNotionPostRequest(databaseID);
        if(url != null){
            try{
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Authorization", "Bearer " + notionToken);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Notion-Version", "2022-06-28");
                con.setDoOutput(true);
                writePayload(payload, con);
                if(con.getResponseCode() == 200){
                    System.out.println("~~~ POST request returned 200 ~~~");
                    recieveResponseFromPOST(con);
                }else{
                    System.out.println("Server responded with: " + con.getResponseCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            System.out.println(" -- Notion Post Request was null");
        }
    }
    private void writePayload(String payload, HttpsURLConnection connection){
        try(DataOutputStream out = new DataOutputStream(connection.getOutputStream())){
            out.writeBytes(payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void makeNotionRequests(ArrayList<String> list){
            for(int j = 0; j < list.size(); j++){
                System.out.println("Attempting to make POST request for assignment: " + j);
                makeNotionPostRequest(list.get(j));
            }
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
    private URL buildurl(boolean application, boolean sub){
        //application is false for canvas, true for notion
        URL url;
        if(application){
            try{
                url = new URL("https://api.notion.com/v1/databases/"+databaseID+"/query");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            if(sub){
                try{
                    url = new URL("https://canvas.instructure.com/api/v1/courses/");
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            else{
                try {
                    url = new URL("https://canvas.instructure.com/api/v1/courses/112990000000112434/assignments");
                } catch (MalformedURLException e) {
                    System.out.println("malformed url");
                    throw new RuntimeException(e);
                }
            }
        }
        return url;

    }
    private String getToken(){
        //TODO fix behavior: grabbing second token only
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
    private String getEndpoint(String which){

        EZFileReader reader = new EZFileReader((new File("src\\main\\resources\\endpoints.txt")));
        String line;
        which = which.toLowerCase();
        which = which.trim();
        while(true){
            line = reader.readLine();
            if(line.equals("[START]")){
                continue;
            }
            else if(line.equals("[END]")){
                return "void";
            }
            else{
                if(line.contains(which)){
                    reader.close();
                    return line.substring(which.length() + 1);
                }
            }
        }
    }
    private void makeNotionGetRequest(){
        URL url = buildurl(true, false);
        if(url!=null){
            try{
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Authorization", "Bearer " + notionToken);

                    System.out.println("*****Content*****");
                    if(con.getResponseCode() == 200){
                        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String input;

                        while((input = reader.readLine()) != null){
                            System.out.println(input);
                            //   parser.parse(input);
                        }
                        reader.close();
                    }else{
                        System.out.println("Server responded with: " + con.getResponseCode());
                    }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private String formatAssignments(HttpsURLConnection con){
        // connection should be from a request to view assignments under a particular course. This method will format the data received from the canvas into an array of Strings that will be passed to writePayload
        if(con != null){
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))){
                String input;
                StringBuilder builder = new StringBuilder();

                while((input = reader.readLine()) != null){
                    builder.append(input);
                }
                JSONParser parser = new JSONParser();
                JSONArray assignments = (JSONArray) parser.parse(builder.toString());
                Iterator<JSONObject> iterator= assignments.iterator();

                StringBuilder payload = new StringBuilder();
                while(iterator.hasNext()){
                    JSONObject assignment = iterator.next();

                    //insert assignment details into payload.


                    builder.append("{\n" +
                            "            \"parent\": {\n" +
                            "                \"type\": \"database_id\",\n" +
                            "                \"database_id\": \"516c52da-3458-4026-a3c4-b785e954349d\"\n" +//hardcoded database id
                            "            },\n" +
                            "            \"properties\": {\n" +
                            "                \"Dates\": {\n" +
                            "                    \"id\": \"0%23%7Dp\",\n" +
                            "                    \"type\": \"date\",\n" +
                            "                    \"date\": {\n" +
                            "                        \"start\": \"2024-08-23\",\n" +
                            "                        \"end\": null,\n" +
                            "                        \"time_zone\": null\n" +
                            "                    }\n" +
                            "                },\n" +
                            "                \"Task\": {\n" +
                            "                    \"id\": \"2%3DL6\",\n" +
                            "                    \"type\": \"multi_select\",\n" +
                            "                    \"multi_select\": [\n" +
                            "                        {\n" +
                            "                            \"id\": \"e841a454-3e7c-496b-89ae-bacf2f489030\",\n" +
                            "                            \"name\": \"Assignment\",\n" +
                            "                            \"color\": \"brown\"\n" +
                            "                        }\n" +
                            "                    ]\n" +
                            "                },\n" +
                            "                \"Status\": {\n" +
                            "                    \"id\": \"u%5E%60%40\",\n" +
                            "                    \"type\": \"checkbox\",\n" +
                            "                    \"checkbox\": false\n" +
                            "                },\n" +
                            "                \"Notes\": {\n" +
                            "                    \"id\": \"v%3CK%5D\",\n" +
                            "                    \"type\": \"rich_text\",\n" +
                            "                    \"rich_text\": [\n" +
                            "                        {\n" +
                            "                            \"type\": \"text\",\n" +
                            "                            \"text\": {\n" +
                            "                                \"content\": \"sample text\",\n" +
                            "                                \"link\": null\n" +
                            "                            },\n" +
                            "                            \"annotations\": {\n" +
                            "                                \"bold\": false,\n" +
                            "                                \"italic\": false,\n" +
                            "                                \"strikethrough\": false,\n" +
                            "                                \"underline\": false,\n" +
                            "                                \"code\": false,\n" +
                            "                                \"color\": \"default\"\n" +
                            "                            },\n" +
                            "                            \"plain_text\": \"see pages 214-213\",\n" +
                            "                            \"href\": null\n" +
                            "                        }\n" +
                            "                    ]\n" +
                            "                },\n" +
                            "                \"Course\": {\n" +
                            "                    \"id\": \"ysX%5E\",\n" +
                            "                    \"type\": \"select\",\n" +
                            "                    \"select\": {\n" +
                            "                        \"id\": \"a8ab63c8-ae8a-4723-b983-9703856fb94f\",\n" +
                            "                        \"name\": \"Applied Linear Algrebra\",\n" +
                            "                        \"color\": \"red\"\n" +
                            "                    }\n" +
                            "                },\n" +
                            "                \"Name\": {\n" +
                            "                    \"id\": \"title\",\n" +
                            "                    \"type\": \"title\",\n" +
                            "                    \"title\": [\n" +
                            "                        {\n" +
                            "                            \"type\": \"text\",\n" +
                            "                            \"text\": {\n" +
                            "                                \"content\": \""+assignment.get("name")+"\",\n" +
                            "                                \"link\": null\n" +
                            "                            },\n" +
                            "                            \"annotations\": {\n" +
                            "                                \"bold\": false,\n" +
                            "                                \"italic\": false,\n" +
                            "                                \"strikethrough\": false,\n" +
                            "                                \"underline\": false,\n" +
                            "                                \"code\": false,\n" +
                            "                                \"color\": \"default\"\n" +
                            "                            },\n" +
                            "                            \"plain_text\": \"Problem Set 1\",\n" +
                            "                            \"href\": null\n" +
                            "                        }\n" +
                            "                    ]\n" +
                            "                }\n" +
                            "            },\n" +
                            "            \"url\": \"https://www.notion.so/Problem-Set-1-86e95c6d602d444c8a8415e66d731d61\",\n" +
                            "            \"public_url\": null\n" +
                            "        },");
                }
                return "";
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return "";
    }
    private void recieveResponseFromPOST(HttpsURLConnection httpsURLConnection){
        System.out.println("~~~ Attempting to read Server's Response to POST request~~~");
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()))){
            String line;
            while((line = reader.readLine()) != null){
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private URL buildCoursesRequestURL() {
        URL url;
        try{
            url = new URL("https://canvas.instructure.com/api/v1/courses");

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }
    private void makeCoursesRequest(){ // get a list of courses
        URL url = buildCoursesRequestURL();
            if(url != null){
                try{
                    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    String token = "11299~nX3JnXvhLLVCMYPXWEMuQrP92MneCJea9f34D8FE8XcJWtJCnVG97kYMQMUByCFU"; // todo replace with read from file
                    if(token.equals("void")){
                        System.out.println("invalid token: " + token);
                        return;
                    }
                    con.setRequestProperty("Authorization", "Bearer " + token);
                    if(con.getResponseCode() == 200){
                        System.out.println("Printing courses: ");
                        print_courses(con);
                        //printFullCourseRequest(con);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }
    }



    private String makeCanvasRequest(boolean application, boolean sub){
        //get assignment data from canvas course
        URL url = buildurl(application, sub);
        if(url!= null){
            try{
                HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
                con.setRequestMethod("GET");
                String token = "11299~nX3JnXvhLLVCMYPXWEMuQrP92MneCJea9f34D8FE8XcJWtJCnVG97kYMQMUByCFU"; // todo replace with read from file
                if(token.equals("void")){
                    System.out.println("invalid token: " + token);
                    return "void";
                }
                con.setRequestProperty("Authorization", "Bearer " + token);
                if(con.getResponseCode() == 200){
                    if(sub){
                        System.out.println("Printing courses"); // print courses, then make another request to print the assignments from each course
                        /*String[] courseids = print_courses(con);
                        if(courseids[0].equals("void")){
                            System.out.println("connection was closed prior to reading from input stream.");
                        }

                         */
                    }
                    System.out.println("Printing content: ");
                    print_assignments(con);
                    return formatAssignments(con);
                }
                else{
                    System.out.println("Canvas request Didn't work" + "\n" + con.getResponseCode());
                }
            } catch (IOException e) {
                System.out.println("Error: Server responded with: " + e);
            }
        }
        return "failed to make request to canvas.";

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
    private String[] resize_array(String[] array){
        int newsize  = array.length * 2;
        String[] ret = new String[newsize];
        for(int i = 0;  i< array.length; i++){
            ret[i] = array[i];
        }
        return ret;

    }
    private void print_courses(HttpsURLConnection con){
        //loads course data. Should grab IDS and insert them into a Map. id:course name.
        if(con!=null){
            try {
                System.out.println("*****Course Data*****");
                // read full response from stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String input;
                StringBuilder builder = new StringBuilder();
                JSONParser parser = new JSONParser();
                while((input = reader.readLine()) != null){
                    builder.append(input);
                }
                reader.close();
                // parse the response, print relevant data
                JSONArray courses = (JSONArray) parser.parse(builder.toString());
                Iterator<JSONObject> iterator = courses.iterator();
                while(iterator.hasNext()){
                    JSONObject course = iterator.next();
                    if(course.containsKey("access_restricted_by_date"))
                        System.out.printf("Course: %d\t Access is restricted by date.\n", (long) course.get("id"));
                    else
                        System.out.println("Retrieved ID for course: " + course.get("name") + ", " + course.get("id"));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void printFullCourseRequest(HttpsURLConnection connection){
        if(connection != null){
            try{
                System.out.println("***Full Course Data Dump");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String input;
                StringBuilder builder = new StringBuilder();
                JSONParser parser = new JSONParser();
                while((input = reader.readLine()) != null)
                    builder.append(input);
                reader.close();
                JSONArray courses = (JSONArray) parser.parse(builder.toString());
                Iterator<JSONObject> iterator = courses.iterator(); // 3 seperate objects for parsing the input? Is there a better way of doing this?
                while(iterator.hasNext()){
                    JSONObject course = iterator.next();
                    System.out.println(course.toString());
                }
            }catch (IOException e){
                System.out.println("An error occurred while printing the full course data response.");
                connection.disconnect();
            } catch (ParseException e) {
                System.out.println("An error occurred while parsing the JSON response.\nSource: Main::printFullCourseRequest");
            }
        }
    }
    private void writeCourses(Long l){
        //writes course data to text file. Do i need this? Yes, to limit API calls.
        try {
            BufferedWriter writer = new BufferedWriter( new FileWriter(new File("src\\main\\resources\\courses.txt")));
            char[] seq = l.toString().toCharArray();

            for(int i = 0; i < seq.length; i++){
                writer.write(seq[i]);
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void print_assignments(HttpsURLConnection con){
        // print assignments to terminal.
        if(con!=null){
            try {
                if(con.getResponseCode() == 200){
                    System.out.println("*****Content*****");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String input;
                    StringBuilder builder = new StringBuilder();
                    JSONParser parser = new JSONParser();
                    while((input = reader.readLine()) != null){
                        builder.append(input);
                    }
                    reader.close();
                    JSONArray array = (JSONArray) parser.parse(builder.toString());
                    Iterator<JSONObject> iterator = array.iterator();
                    while(iterator.hasNext()){
                        JSONObject assignment = iterator.next();
                        System.out.println("Course id: " + assignment.get("course_id"));
                        System.out.println("Assignment ID: " + assignment.get("id"));
                        System.out.println("Assignment Name: " + assignment.get("name"));
                        System.out.println("Assignment Description: " + assignment.get("description"));
                        if(assignment.get("due_at") == null){
                            System.out.println("Due date: No due date");
                        }else{
                            System.out.println("Due date: " + assignment.get("due_at"));
                        }
                        System.out.println("~~~~~~~");
                    }
                    System.out.println("End of list");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void exit(){
        System.out.println("Tearing down.");
    }
}