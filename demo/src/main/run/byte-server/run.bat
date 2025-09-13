@echo off

::設置JAVA環境路徑
SET JAVA_PATH=%JAVA_HOME%
::設置應用環境路徑
SET APP_HOME=%cd%
::設置LOG環境路徑
SET LOG_HOME=%cd%\log
::設置應用啟動參數
SET LAUNCH_MODE=DEBUG
::設定應用啟動類
SET MAIN_CLASS="com.vscodelife.demo.DemoByteServer"
::設置JVM環境參數
SET JAVA_OPTS=-Xms128M -Xmx128M -Xmn128M -server -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.net.preferIPv4Stack=true
::SET JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=11110,server=y,suspend=n
::SET JAVA_OPTS=%JAVA_OPTS% -Dio.netty.leakDetection.level=advanced
SET JAVA_OPTS=%JAVA_OPTS% -Dapp.logPath=%LOG_HOME%
::設置依賴資源
SET CP=%APP_HOME%\lib\*;%APP_HOME%\config

::顯示訊息
echo %APP_HOME% @ %LAUNCH_MODE% start

::執行
java %JAVA_OPTS% -cp %CP% %MAIN_CLASS% %LAUNCH_MODE%
