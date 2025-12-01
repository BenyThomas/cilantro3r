# Use a Tomcat image with a Java 8 JRE
FROM tomcat:9.0-jre8-openjdk

# (Optional) Remove the default ROOT webapp
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copy your WAR into Tomcat's webapps folder as ROOT.war
COPY target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
