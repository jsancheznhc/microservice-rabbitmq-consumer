@echo off

java -jar -Dlogging.config=external-config/logback-spring.xml  target/dequeuer-runtime-1.0.0.jar 

echo " "
echo " "
echo "..................."
echo "      END          "
echo "..................."
echo " "
echo " "

pause