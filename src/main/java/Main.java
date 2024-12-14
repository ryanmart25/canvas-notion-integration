package main.java;

import javax.net.ssl.HttpsURLConnection;
import javax.print.attribute.standard.MediaSize;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.cert.Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
private Map<String, String> courseMap;
private int assignmentCount;
private boolean debugMode = false;
    public static void main(String[] args) {
        if(args != null){
            System.out.println("args isn't null");
        }
        Main main = new Main();
        //Scanner scanner = new Scanner(System.in);
        //Specifies what the user would like to do.
        // 1. "All Courses" prints a list of currently enrolled courses
        // 2. "<name of course>" prints a list of assignments for a specific course
        //String which = scanner.nextLine();
        main.makeCoursesRequest();
        for (int i = 0; i < courseIDs.length; i++) {
            URL url = main.buildAssignmentRequestURL(courseIDs[i]);
            InputStream inputStream = main.makeAssignmentRequest(url);
            if(inputStream == null){ // feels like a shitty way to do this
                continue;
            }
            String request =  main.readAssignmentRequest(inputStream);
            String[] assignmentsProperties = main.parseAssignmentProperties(request);
            for (int j = 0; j < main.assignmentCount; j++) {
                String notionCreatePagePayload = main.buildPageCreationPayload(assignmentsProperties[j]);
                //System.out.println(notionCreatePagePayload);
                main.makeNotionPageCreationRequest(main.databaseID, notionCreatePagePayload, main.notionToken);

            }
            //System.out.println(compiledAssignments);
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
    private InputStream  makeAssignmentRequest(URL url){

        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + canvasToken);
            if(connection.getResponseCode() == 200){
                InputStream stream = connection.getInputStream();

                return stream;
            }else{
                System.out.println("Assignment Request: Server Responded with: " + connection.getResponseCode() + "\n" + connection.getResponseMessage());
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    private String readAssignmentRequest(InputStream is)  { // research needed on how to best handle the stream.
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String input;
      StringBuilder fullinput = new StringBuilder();
      try {
          while ((input = reader.readLine()) != null) {
              fullinput.append(input);

          }
          reader.close();
      }catch (IOException e){
          System.out.println("Failed to read from request stream.\nReason: " + e.getMessage());
      }
      return fullinput.toString();
    }
    private String[] parseAssignmentProperties(String unparsed){ //
        JSONParser parser = new JSONParser();
        StringBuilder assignmentOutput = new StringBuilder();

        try {
            JSONArray array = (JSONArray) parser.parse(unparsed);
            assignmentCount = array.size();
            String[] assignments = new String[assignmentCount];
            Iterator<JSONObject> iterator = array.iterator();
            int i = 0;
            while(iterator.hasNext()){

                // build a string. It will contain the properties.
                JSONObject assignment = iterator.next();
                // sanitize inputs
                String unsanitizedAssignmentDescription = (String) assignment.get("description");
                String sanitizedAssignmentDescription = unsanitizedAssignmentDescription.replaceAll("\n", "");
                if(sanitizedAssignmentDescription.contains("\"")){
                    sanitizedAssignmentDescription =sanitizedAssignmentDescription.replaceAll("\"", "");
                }

                String unsanitizedAssignmentStartDate = (String) assignment.get("unlock_at");
                String sanitizedAssignmentStartDate;
                String unsanitizedAssignmentEndDate = (String) assignment.get("due_at");
                String sanitizedAssignmentEndDate;
                if(unsanitizedAssignmentStartDate == null){
                    sanitizedAssignmentStartDate = (String) assignment.get("created_at");
                }else{
                    sanitizedAssignmentStartDate = unsanitizedAssignmentStartDate;
                }
                if(unsanitizedAssignmentEndDate == null){ // set end date to the end of the year
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime endOfYear = now.withMonth(12).withDayOfMonth(31);
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                    String formattedDate = endOfYear.format(formatter);
                    sanitizedAssignmentEndDate = formattedDate;
                }else{
                    sanitizedAssignmentEndDate = unsanitizedAssignmentEndDate;
                }
                //System.out.println("Assignment Description: \n\n" + assignment.get("description"));
                assignmentOutput.append("\"properties\": {\n\t" + "\"Name\": {\n\t\t" + "\"title\": [\n\t\t\t" + "{\n\t\t\t\t" + "           \"text\": {\n\t\t\t\t\t\t" + "\"content\": \"").append(assignment.get("name")).append("\"\n\t\t\t\t\t").append("}\n\t\t\t\t").append("        }").append("       ]\n\t\t\t").append("},").append("\"Notes\": {").append("\"rich_text\": [").append("{").append("\"text\": {").append("\"content\": \"").append(sanitizedAssignmentDescription).append("\"").append("}").append("}").append("]").append("},").append("\"Course\": {").append("\"select\": {").append("\"name\": \"").append(courseMap.get(Long.toString((long) assignment.get("course_id")))).append("\"").append("}").append("},").append("\"Dates\": {").append("\"date\": {").append("\"start\": \"").append(sanitizedAssignmentStartDate).append("\",").append("\"end\": \"").append(sanitizedAssignmentEndDate).append("\"").append("}").append("},").append("\"Task\": {").append("\"multi_select\": [").append("{").append("\"name\": \"").append(resolveAssignmentType((JSONArray) assignment.get("submission_types"))).append("\"").append( // i am pretty sure i need to fix this to ensure it parses the array properly.
                        "}").append("]").append("}").append("}");

                        assignments[i] = assignmentOutput.toString();
                        assignmentOutput.delete(0, assignmentOutput.length());
                        i++;
            }
            return assignments;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    private String buildPageCreationPayload(String propertiesObject){
        String payload = "{" +
                "\"parent\": { \"database_id\": \""+this.databaseID+"\" },"+ propertiesObject + "}";
        return payload;

    }
private String resolveAssignmentType(JSONArray submissionType){
        String subs = submissionType.toString();
        if(subs.contains("online_quiz")){
            return "Exam";
        }
        if(subs.contains("online_upload")){
            return "Assignment";
        }
        return "Important date";
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
                    con.disconnect();
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
    private URL buildURLNotionDatabaseQueryRequest(String databaseID){
        URL url;
        try{
            url = new URL("https://api.notion.com/v1/databases/"+databaseID+"/query");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }
    private URL buildNotionDatabaseCreatePageRequestURL(String databaseID){
        URL url;
        try{
            url = new URL("https://api.notion.com/v1/pages");
        }catch (MalformedURLException e){
            throw new RuntimeException(e);
        }
        return url;
    }
    private void makeNotionPageCreationRequest(String databaseID, String payload, String authenticationToken){
        URL url = buildNotionDatabaseCreatePageRequestURL(databaseID);
        if(url != null){
            try{
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + authenticationToken);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Notion-Version", "2022-06-28");
                connection.setDoOutput(true);
                writePayload(payload, connection);
                if(connection.getResponseCode() == 200){
                    System.out.println("Notion Database Page Creation Request: Server Responded OK");
                    recieveResponseFromPOST(connection);
                }else{
                    System.out.println("Notion Page Creation Request: Server Responded: " + connection.getResponseCode() + "\t\t" + connection.getResponseMessage());
                    System.out.println("Payload: " + payload);
                }
            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void makeNotionDatabaseQueryRequest(String payload){
        ////queries a database to find out information on pages
        URL url = buildURLNotionDatabaseQueryRequest(databaseID);
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
                makeNotionDatabaseQueryRequest(list.get(j));
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
        //System.out.println("~~~ Attempting to read Server's Response to POST request~~~");
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()))){
            String line;
            while((line = reader.readLine()) != null){
                //System.out.println(line);
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
                   this.courseMap =  mapCourseIDS(con);
                //printFullCourseRequest(con);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private HashMap<String, String> mapCourseIDS(HttpsURLConnection connection){
        if(connection!=null){
            try {
                System.out.println("*****Course Data*****");
                // read full response from stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String input;
                StringBuilder builder = new StringBuilder();
                JSONParser parser = new JSONParser();
                while((input = reader.readLine()) != null){
                    builder.append(input);
                }
                reader.close();
                connection.disconnect();
                // parse the response, print relevant data
                JSONArray courses = (JSONArray) parser.parse(builder.toString());
                Iterator<JSONObject> iterator = courses.iterator();
                HashMap<String, String> courseIDNameMap= new HashMap<>();
                while(iterator.hasNext()){
                    JSONObject course = iterator.next();
                    if(course.containsKey("access_restricted_by_date"))
                        continue;
                    else
                        courseIDNameMap.put(Long.toString((long)course.get("id")), (String)course.get("name"));
                }
                return courseIDNameMap;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
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
        /*
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
    */
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
}