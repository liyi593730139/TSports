#!/usr/bin/python
#coding=utf-8

import os

CHECK_PRO = "ps x | grep '%s'"

def check_pro(filename):# 检查对应的进程运行的数目
    """
    check_pro() -> int

    返回对应进程的数目,来判断是否有多进程竞争的问题。
    """
    proLine = os.popen(CHECK_PRO % filename)
    return len(proLine.readlines())

def checkSafeMode(log):# 检查对应HDFS是否在安全模式下执行
    """
    checkSafeMode() -> None

    检查对应HDFS是否在安全模式下执行，若在安全模式下退出，记录在日志之中。
    """
    HADOOP_SAFEMODE_COMMAND = "hadoop dfsadmin -safemode get"
    hadoopSafeMode = os.popen(HADOOP_SAFEMODE_COMMAND).readlines()
    for line in hadoopSafeMode:
        if "ON" in line:
            log.error("Hadoop is in safemode.Upload is failed!")
            log.chaLogPath()
            exit(1)
