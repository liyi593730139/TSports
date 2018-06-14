#isSaveMode.py
import os

HADOOP_SAFEMODE_COMMAND = "hadoop dfsadmin -safemode get"
hadoopSafeMode = os.popen(HADOOP_SAFEMODE_COMMAND).readlines()
for line in hadoopSafeMode:
    if "ON" in line:
        print("SafeMode is ON!")
        exit(1)

print("SafeMode is OFF!")
