@echo off

java -jar -Dspring.config.location=external-config/dev.properties -Dlogging.config=external-config/logback-spring.xml target/dequeuer-runtime-1.0.0.jar

echo " "
echo " "
echo "..................."
echo "      END          "
echo "..................."
echo " "
echo " "

pause