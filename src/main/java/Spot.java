package main.java;

public class Spot {
    public static final String ASSIGNMENTDESCRIPTIONREGEX = "\"|\n|<[a-zA-Z]*[/s]*[^>]*>|&nbsp;";
    public static final String ASSIGNMENTDESCRIPTIONREGEXREPLACEMENT = "";
    public static final String RegisteredFlags = "d,n,c,h"; // available options to pass.
    /*
    -d  pass a notion database ID to use in place of the ID specified in the environment variables.
    -n  pass a noton API token to use in placde of the token specified in the environment variables.
    -c  pass a canvas api token to use in place of the token specified in the environment variables.
    -h  print the help message
     */
}
