# !!!!!! UNDER DEVELOPMENT !!!!!!

# Library Management System

A project implementing a Library Management System with admin dashboard and student functionalities, using Java servlets, MongoDB and SQL.

## Tech stack
- Java (servlets)
- Maven (\`mvnw.cmd\` for Windows)
- MongoDB
- (Optional - not favoured) Docker / Docker Compose

## Prerequisites
- JDK 21.0.3 => https://download.oracle.com/java/21/archive/jdk-21.0.3_windows-x64_bin.zip
- Maven (or use \`.\mvnw.cmd\`)
- MongoDB running at \`mongodb://localhost:27017\`
- IntelliJ IDEA (recommended for running/deploying)
- Apache Tomcat (latest version)

## Quick start

1. Build:
    - Windows (wrapper):  
      \`\`\`powershell
      .\mvnw.cmd clean package -DskipTests
      \`\`\`
    - Or: \`mvn clean package\`

2. Run in IDE:
    - Deploy built WAR to Tomcat or run via your IDE run configuration as follows:
![img_1.png](img_1.png)

    - Make sure to set the correct artifact and server settings:
   
![img_2.png](img_2.png)

3. (Optional) Docker:
    - If a \`Dockerfile\` / \`docker-compose.yml\` exist:  
      \`\`\`bash
      docker compose up --build
      \`\`\`

## Configuration
- Default MongoDB connection is in code: \`mongodb://localhost:27017\`
- DB name: \`dbLibraryMS\`
- At least the \`Admin\` collection should be imported in dbLibraryMS for login (refer to ReadTHIS.txt file in MongoDB-data-Json directory).
- Collections: \`BorrowReturnHist\`, \`Fines\`... Find the rest in MongoDB-data-Json directory.

## Usage
- Access the application at: \`http://localhost:8080/LibraryMS/src/main/webapp/common/index.jsp`
- Login as Admin (refer to ReadTHIS.txt file in MongoDB-data-Json directory for default credentials).
- Student registration/login pages are available on the landing page via call-to-action buttons.
- Explore admin dashboard and student functionalities.
- Admin can;
1. Manage books
2. View reports
4. Handle fines

- Students can;
1. Register/Login
2. Search/Borrow/Return/Extend books
3. View borrowing history and fines
4. Update profile


## Notes
- Ensure MongoDB is running before starting the application.
- Modify MongoDB connection settings in the code if necessary.
- Ensure Tomcat server is properly configured in your IDE.
- Ensure required collections (i.e. Admin) and data are present in MongoDB for full functionality.
- For any questions on functionality, refer to "Watch Demo" button on the landing page for a demo video.
