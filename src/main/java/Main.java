package main.java;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Main {
    private final String[] parameters = {};
    private String canvasToken;
    private String databaseID;
    private String notionToken;
    private String[] courseIDs;
    private Map<String, String> courseMap;
    private int assignmentCount;
    private boolean debugMode = false;

    public static void main(String[] args) {
        Main main = new Main();

        if(args.length > 0){
            // parse arguments
            for (int i = 0; i < args.length; i++) {
                if(args[i].equals("-h")){
                    System.out.println(
                            "Fetch and Append Canvas coursework to a Notion Database.\n" +
                            "Usage:\nPass Notion and Canvas API tokens and target database ID or use environment variables.\n"+
                            "    -d  pass a notion database ID to use in place of the ID specified in the environment variables.\n" +
                            "    -n  pass a noton API token to use in placde of the token specified in the environment variables.\n" +
                            "    -c  pass a canvas api token to use in place of the token specified in the environment variables.\n" +
                            "    -e  use environment variables for Database ID and API tokens.\n" +
                                    "\tFormat:\n" +
                                    "\t'NOTIONTOKEN=<token>'\n" +
                                    "\t'CANVASTOKEN=<token>'\n" +
                                    "\t'DATABASEID=<database ID>'" +
                            "    -h  print the help message.");
                    return;
                }
                else if(args[i].equals("-e") && args.length == 1){
                    System.out.println("Using environment variables.");
                    main.loadSecrets(main);
                }
                else if(args[i].equals("-d") && i < args.length - 1){
                    System.out.println("Using notion database ID: " + args[i + 1]);
                    main.databaseID = args[i + 1];
                }
                else if(args[i].equals("-c") && i < args.length - 1){
                    System.out.println("Using Canvas API Token: " + args[i + 1]);
                    main.canvasToken = args[i + 1];
                }
                else if(args[i].equals("-n") && i < args.length - 1){
                    System.out.println("Using Notion API Token: " + args[i + 1]);
                    main.notionToken = args[i + 1];
                }
                else{
                    System.out.println("Flags not recognized, too many, or too failed to pass a value properly.");
                    return;
                }

            }
            main.makeCoursesRequest(false);
            for (int i = 0; i < main.courseIDs.length; i++) {
                if(main.courseIDs[i].equals("null")){
                    continue;
                }
                URL url = main.buildAssignmentRequestURL(main.courseIDs[i]);
                InputStream inputStream = main.makeAssignmentRequest(url);
                if (inputStream == null) { // feels like a shitty way to do this
                    continue;
                }
                String request = main.readAssignmentRequest(inputStream);
                String[] assignmentsProperties = main.parseAssignmentProperties(request);
                for (int j = 0; j < main.assignmentCount; j++) {
                    String notionCreatePagePayload = main.buildPageCreationPayload(assignmentsProperties[j]);
                    //System.out.println(notionCreatePagePayload);
                    main.makeNotionPageCreationRequest(notionCreatePagePayload, main.notionToken);

                }
                //System.out.println(compiledAssignments);
            }
        }else{

            //Scanner scanner = new Scanner(System.in);
            //Specifies what the user would like to do.
            // 1. "All Courses" prints a list of currently enrolled courses
            // 2. "<name of course>" prints a list of assignments for a specific course
            //String which = scanner.nextLine();
            main.makeCoursesRequest(false);
            for (int i = 0; i < main.courseIDs.length; i++) {
                if(main.courseIDs[i].equals("null")){
                    continue;
                }
                URL url = main.buildAssignmentRequestURL(main.courseIDs[i]);
                InputStream inputStream = main.makeAssignmentRequest(url);
                if (inputStream == null) { // feels like a shitty way to do this
                    continue;
                }
                String request = main.readAssignmentRequest(inputStream);
                String[] assignmentsProperties = main.parseAssignmentProperties(request);
                for (int j = 0; j < main.assignmentCount; j++) {
                    String notionCreatePagePayload = main.buildPageCreationPayload(assignmentsProperties[j]);
                    //System.out.println(notionCreatePagePayload);
                    main.makeNotionPageCreationRequest(notionCreatePagePayload, main.notionToken);

                }
                //System.out.println(compiledAssignments);
            }
        }


       // main.exit();
    }

    private void loadSecrets(Main obj) {

        obj.canvasToken = System.getenv("CANVASTOKEN");
        obj.notionToken = System.getenv("NOTIONTOKEN");
        obj.databaseID = System.getenv("DATABASEID");
    }

    private URL buildAssignmentRequestURL(String courseid) {
        URL url;
        try {
            url = new URL("https://canvas.instructure.com/api/v1/courses/" + courseid + "/assignments");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

    private InputStream makeAssignmentRequest(URL url) {

        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + canvasToken);
            if (connection.getResponseCode() == 200) {
                InputStream stream = connection.getInputStream();

                return stream;
            } else {
                System.out.println("Assignment Request: Server Responded with: " + connection.getResponseCode() + "\n" + connection.getResponseMessage());
                System.out.println(url.toString());}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private String readAssignmentRequest(InputStream is) { // research needed on how to best handle the stream.
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String input;
        StringBuilder fullinput = new StringBuilder();
        try {
            while ((input = reader.readLine()) != null) {
                fullinput.append(input);

            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Failed to read from request stream.\nReason: " + e.getMessage());
        }
        return fullinput.toString();
    }
    private String sanitizeDescription(String unsanitized){
        String sanitized = unsanitized.replaceAll(Spot.ASSIGNMENTDESCRIPTIONREGEX, Spot.ASSIGNMENTDESCRIPTIONREGEXREPLACEMENT);
        return sanitized;
    }
    private String[] parseAssignmentProperties(String unparsed) { //
        JSONParser parser = new JSONParser();
        StringBuilder assignmentOutput = new StringBuilder();

        try {
            JSONArray array = (JSONArray) parser.parse(unparsed);
            assignmentCount = array.size();
            String[] assignments = new String[assignmentCount];
            Iterator<JSONObject> iterator = array.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                // build a string. It will contain the properties.
                JSONObject assignment = iterator.next();
                // sanitize inputs
                String unsanitizedAssignmentDescription = (String) assignment.get("description");
                String sanitizedAssignmentDescription = sanitizeDescription(unsanitizedAssignmentDescription);
                //unsanitizedAssignmentDescription.replaceAll("\n", "");

                //sanitizedAssignmentDescription.replaceAll("")
                String unsanitizedAssignmentStartDate = (String) assignment.get("unlock_at");
                String sanitizedAssignmentStartDate;
                String unsanitizedAssignmentEndDate = (String) assignment.get("due_at");
                String sanitizedAssignmentEndDate;
                if (unsanitizedAssignmentStartDate == null) {
                    sanitizedAssignmentStartDate = (String) assignment.get("created_at");
                } else {
                    sanitizedAssignmentStartDate = unsanitizedAssignmentStartDate;
                }
                if (unsanitizedAssignmentEndDate == null) { // set end date to the end of the year
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime endOfYear = now.withMonth(12).withDayOfMonth(31);
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                    String formattedDate = endOfYear.format(formatter);
                    sanitizedAssignmentEndDate = formattedDate;
                } else {
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

    private String buildPageCreationPayload(String propertiesObject) {
        String payload = "{" +
                "\"parent\": { \"database_id\": \"" + this.databaseID + "\" }," + propertiesObject + "}";
        return payload;

    }

    private String resolveAssignmentType(JSONArray submissionType) {
        String subs = submissionType.toString();
        if (subs.contains("online_quiz")) {
            return "Exam";
        }
        if (subs.contains("online_upload")) {
            return "Assignment";
        }
        return "Important date";
    }

    private ArrayList<String> compileassignments(HttpsURLConnection con) {
        ArrayList<String> failure = new ArrayList<>();
        failure.add("failed");
        if (con != null) {
            try {
                if (con.getResponseCode() == 200) {
                    System.out.println("*****Assignments*****");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String input;
                    StringBuilder fullinput = new StringBuilder();
                    JSONParser parser = new JSONParser();
                    while ((input = reader.readLine()) != null) {
                        fullinput.append(input);
                    }
                    reader.close();
                    con.disconnect();
                    JSONArray array = (JSONArray) parser.parse(fullinput.toString());
                    Iterator<JSONObject> iterator = array.iterator();
                    ArrayList<String> assignments_out = new ArrayList<>();

                    while (iterator.hasNext()) { //put assignments into one JSON payload
                        JSONObject assignment = iterator.next();
                        String duedate;
                        String notes;
                        if (assignment.get("due_at") == null) {
                            duedate = "2024-10-5";
                        } else {
                            duedate = assignment.get("due_at").toString();
                        }
                        if (assignment.get("description") == null) {
                            notes = "no notes";
                        } else {
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
                                "                        \"start\": \"" + duedate + "\",\n" +
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
                                "                            \"plain_text\": \"" + notes + "\",\n" +
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

    private URL buildURLNotionDatabaseQueryRequest(String databaseID) {
        URL url;
        try {
            url = new URL("https://api.notion.com/v1/databases/" + databaseID + "/query");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

    private URL buildNotionDatabaseCreatePageRequestURL() {
        URL url;
        try {
            url = new URL("https://api.notion.com/v1/pages");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

    private void makeNotionPageCreationRequest(String payload, String authenticationToken) {
        URL url = buildNotionDatabaseCreatePageRequestURL();
        if (url != null) {
            try {
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + authenticationToken);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Notion-Version", "2022-06-28");
                connection.setDoOutput(true);
                writePayload(payload, connection);
                if (connection.getResponseCode() == 200) {
                    System.out.println("Notion Database Page Creation Request: Server Responded OK");
                    recieveResponseFromPOST(connection);
                }
                else if(connection.getResponseCode() == 401){
                    System.out.println("Notion said you were unauthorized to make that request. Is your token correct?\nToken: " + this.notionToken);
                }
                else {
                    System.out.println("Notion Page Creation Request: Server Responded: " + connection.getResponseCode() + "\t\t" + connection.getResponseMessage());
                    System.out.println("Payload: \n" + payload);
                }
            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void makeNotionDatabaseQueryRequest(String payload) {
        ////queries a database to find out information on pages
        URL url = buildURLNotionDatabaseQueryRequest(databaseID);
        if (url != null) {
            try {
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Authorization", "Bearer " + notionToken);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Notion-Version", "2022-06-28");
                con.setDoOutput(true);
                writePayload(payload, con);
                if (con.getResponseCode() == 200) {
                    System.out.println("~~~ POST request returned 200 ~~~");
                    recieveResponseFromPOST(con);
                } else {
                    System.out.println("Server responded with: " + con.getResponseCode());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println(" -- Notion Post Request was null");
        }
    }

    private void writePayload(String payload, HttpsURLConnection connection) {
        try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
            out.writeBytes(payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void recieveResponseFromPOST(HttpsURLConnection httpsURLConnection) {
        //System.out.println("~~~ Attempting to read Server's Response to POST request~~~");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String captureResponse(HttpsURLConnection connection) {
        StringBuilder builder = new StringBuilder();
        String failureSignifier = "failed to capture response";
        builder.append(failureSignifier);
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))){
            String input;

            while ((input = reader.readLine()) != null) {
                builder.append(input);
            }
            reader.close();
            connection.disconnect();
            builder.replace(0, failureSignifier.length(), "");
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
            // Something went wrong while reading, ensure everything downstream knows it. Keep the failure signifier, remove everything else
            if (builder.length() > failureSignifier.length())
                builder.replace(failureSignifier.length(), builder.length(), "");
        }
        return builder.toString();
    }

    private URL buildCoursesRequestURL() {
        URL url;
        try {
            url = new URL("https://canvas.instructure.com/api/v1/courses");

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url;
    }

    private int makeCoursesRequest(boolean writeCourseList) { // get a list of courses
        URL url = buildCoursesRequestURL();
        if (url != null) {
            HttpsURLConnection con = null;
            try {
                con = (HttpsURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                String token = canvasToken;
                con.setRequestProperty("Authorization", "Bearer " + token);
                int responseCode = con.getResponseCode();
                String response = captureResponse(con);
                if (responseCode == 200) {
                    if (writeCourseList) {
                        writeCourseToFile(response);
                    }
                    this.courseMap = mapCourseIDS(response);
                    //printFullCourseRequest(con);
                }
                else if(responseCode == 401){
                    System.out.println("The server said you are unauthorized to make this request. Is the canvas API token correct?\nToken: " + this.canvasToken);
                }
                else {
                    System.out.print("Courses Request failed. Server Responded with: " + response + " Program should exit now");
                    return -1;
                }
            } catch (IOException e) {
                System.out.println("An IO error occurred while making a request to canvas's Get Courses endpoint. The program should exit now. ");
            } finally {
                con.disconnect();
            }
        }
        return 0;
    }

    private HashMap<String, String> mapCourseIDS(String response) {
        if (!response.equals("failure")) {
            try {
                // parse the response, print relevant data
                JSONParser parser = new JSONParser();
                JSONArray courses = (JSONArray) parser.parse(response);
                int numCourses = courses.size();
                Iterator<JSONObject> iterator = courses.iterator();
                HashMap<String, String> courseIDNameMap = new HashMap<>();
                int i = 0;
                String[] courseIds = new String[numCourses];
                while (iterator.hasNext()) { // this should implicitly prevent array overruns;
                    JSONObject course = iterator.next();
                    if (course.containsKey("access_restricted_by_date")) {
                        courseIds[i] = "null";
                        i++;
                    }
                    else{
                        courseIDNameMap.put(Long.toString((long) course.get("id")), (String) course.get("name"));
                        // cache the ids
                         courseIds[i] = String.valueOf(course.get("id"));
                         i++;
                        /*
                        if(i < numCourses){
                            System.out.println(String.valueOf(course.get("id")));

                        }
                        */


                    }

                }
                this.courseIDs = courseIds;
                return courseIDNameMap;
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private void writeCourseToFile(String response) {
        //loads course data. Should grab IDS and insert them into a Map. id:course name.
        if (!response.equals("failure")) {
            try {

                // parse the response, print relevant data
                JSONParser parser = new JSONParser();
                JSONArray courses = (JSONArray) parser.parse(response);
                Iterator<JSONObject> iterator = courses.iterator();
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(System.getProperty("user.dir") + "\\src\\main\\resources\\courses.txt"))); // todo complete writing course names and IDS to file

                while (iterator.hasNext()) {
                    JSONObject course = iterator.next();
                    if (course.containsKey("access_restricted_by_date"))
                        writer.write("Access Restricted By Date:" + (long) course.get("id") + "\n");
                    else
                        writer.write((String) course.get("name") + ":" + (long) course.get("id") + "\n");
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }
}