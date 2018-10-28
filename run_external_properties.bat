@echo off

java -jar -Dspring.config.location=external-config/localhost.properties target/dequeuer-runtime-1.0.0.jar

echo " "
echo " "
echo "..................."
echo "      END          "
echo "..................."
echo " "
echo " "

pause