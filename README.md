# Purpose
A simple application for connecting Canvas and Notion. This exists to provide an easy and lightweight means of increasing productivity. Aids in keeping track of assignments and lecture material.  
# Functionality
Dispatches requests to Canvas' API to get coursework and metadata on a per course/assignment basis. 
Compiles and sanitizes input from Canvas responses and dispatches requests to Notion's API. Course work is appended to a Notion Database.
# Requirements
Database Schema needs to match the following:
```
"properties": {
        "Dates": {
            "id": "0%23%7Dp",
            "name": "Dates",
            "type": "date",
            "date": {}
        },
        "Task": {
            "id": "2%3DL6",
            "name": "Task",
            "type": "multi_select",
            "multi_select": {
                "options": [
                    {
                        "id": "e841a454-3e7c-496b-89ae-bacf2f489030",
                        "name": "Assignment",
                        "color": "brown",
                        "description": null
                    },
                    {
                        "id": "55210694-c42f-4825-8245-eed6ddc91a88",
                        "name": "Exam",
                        "color": "red",
                        "description": null
                    },
                    {
                        "id": "6474015e-c15a-4ae9-a0a5-f6f3eb310d04",
                        "name": "Quiz",
                        "color": "orange",
                        "description": null
                    },
                    {
                        "id": "562ff0fb-293e-4b4a-8239-73e7b8f2d100",
                        "name": "Important date",
                        "color": "gray",
                        "description": null
                    },
                    {
                        "id": "05cd491f-f8c0-41d7-9cd1-bb1fdb18e64f",
                        "name": "Discussion Post",
                        "color": "green",
                        "description": null
                    },
                    {
                        "id": "52bff499-55c7-44b7-bbf0-d02b59466f2c",
                        "name": "Class Reading",
                        "color": "blue",
                        "description": null
                    }
                ]
            }
        },
        "Status": {
            "id": "u%5E%60%40",
            "name": "Status",
            "type": "checkbox",
            "checkbox": {}
        },
        "Notes": {
            "id": "v%3CK%5D",
            "name": "Notes",
            "type": "rich_text",
            "rich_text": {}
        },
        "Course": {
            "id": "ysX%5E",
            "name": "Course",
            "type": "select",
            "select": {
                "options": [
                    {
                        "id": "e4c0c1df-535a-4e94-afb9-48f612ffe45d",
                        "name": "Operating Systems Principles",
                        "color": "pink",
                        "description": null
                    },
                    {
                        "id": "3130e53e-a5c4-4e27-b51d-a8ea031c0c47",
                        "name": "Database Management Systems",
                        "color": "orange",
                        "description": null
                    },
                    {
                        "id": "5925c936-917e-41e2-abf5-5da44be5eb23",
                        "name": "Computability and Formal Languages",
                        "color": "yellow",
                        "description": null
                    },
                    {
                        "id": "3dceb5b0-fbea-4053-85ca-d62b2af40e17",
                        "name": "Archeology of Mexico",
                        "color": "blue",
                        "description": null
                    },
                    {
                        "id": "a8ab63c8-ae8a-4723-b983-9703856fb94f",
                        "name": "Career Planning",
                        "color": "red",
                        "description": null
                    },
                    {
                        "id": "7bdc4bb5-31f0-4037-be23-652058bb4b95",
                        "name": "Chicano/Latino Studies",
                        "color": "brown",
                        "description": null
                    },
                    {
                        "id": "39519646-358f-4a5d-848d-8418840c74d5",
                        "name": "CSC 130 Critical Course",
                        "color": "default",
                        "description": null
                    },
                    {
                        "id": "01ae17ad-1cd2-4286-beef-6c1467ae787c",
                        "name": "CSC Undergraduate Student Resources",
                        "color": "gray",
                        "description": null
                    },
                    {
                        "id": "29de4c00-5c37-408a-b145-867fb233c23b",
                        "name": "ANTH121 Archaeology of Mexico - SECTION 01",
                        "color": "purple",
                        "description": null
                    }
                ]
            }
        },
        "Name": {
            "id": "title",
            "name": "Name",
            "type": "title",
            "title": {}
        }
    },
```
# Limitations
1. Selection of Notion parent object is not implemented. The application assumes you wish to append entries to a Database.
2. Source code must be modified to fit requests to a Database with a different Schema. (See Notion API documentation: Create Page and Retrieve a Database for more information on how to create a page in a database and how to retrieve the Schema of a database for proper request payload formatting)
# Future 
I wish to implement a means of connecting Notes and Assignments together to aid in creating visual and spatial links between lecture material and assignments. Detecting relevant Notes to add as subpages (or create links to note pages) to relevant assignment pages. 
A real methods of installing and running this will be hashed out soon. 
Additionally, I want to figure out a way of discovering database schema and configuring the payload of a Notion POST request so that source code does not need to modified in order to facillitate page creation in databases with differing schema.
I am not sure how to algorithmically do this. Maybe I can have some sort of 
