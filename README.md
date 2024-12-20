# Purpose
A simple application for connecting Canvas and Notion. This exists to provide an easy and lightweight means of increasing productivity. Aids in keeping track of assignments and lecture material.  
# Functionality
Dispatches requests to Canvas' API to get coursework and metadata on a per course/assignment basis. 
Compiles and sanitizes input from Canvas responses and dispatches requests to Notion's API. Course work entries are appended to a Notion Database. 
# Limitations
1. Canvas uses HTML tags to format various attributes of an assignment. These are invisible to a normal visitor but make their way into a Canvas endpoint response. Currently only rudimentary sanitization is done to prevent double qoutation marks from breaking JSON payloads. Additional input sanitization is needed to remove paragraph tags, anchor tags, and other tags.
2. Selection of Notion parent object is not implemented. The application assumes you wish to append entries to a Database.
3. Source code must be modified to fit requests to a Database with a different Schema. (See Notion API documentation: Create Page for more details)
# Future 
I wish to implement a means of connecting Notes and Assignments together to aid in creating visual and spatial links between lecture material and assignments. Detecting relevant Notes to add as subpages (or create links to note pages) to relevant assignment pages.
