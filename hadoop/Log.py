#!/usr/bin/python
#coding=utf-8

""""
    @文件名:Log.py
    @记录对应的log文件
    @修改人:HappenLee
    @时间:2017/04/30
    @修改内容：添加注释
"""
import logging
import sys
import os
import fcntl

# log文件的大小与数目
LOG_SIZE = 10000
LOG_NUM = 3

class log():
    """
    log类的，之后执行的记录逻辑基于log类来实现
    """
    def __init__(self,type,filename):
        """
        传入log类型与文件名
        type -》 类型
        filename -》 missionid的文件名
        """
        with open(sys.path[0][:-5] + "/system.conf", mode='r') as conf:
            for line in conf:
                if "logsPath" in line:
                    self.logDir = line.split("=")[1].strip()
                    if self.logDir[-1] != "/":
                        self.logDir += "/"
                    break
            if not self.logDir:
                self.logDir = "../logs/"

        self.logDir += type + os.sep
        if os.path.exists(self.logDir):
            if not os.path.isdir(self.logDir):
                os.remove(self.logDir)
                os.mkdir(self.logDir)
                with open(self.logDir + "lock",mode = 'w') as f:
                    f.close()

        else:
            os.mkdir(self.logDir)
            with open(self.logDir + "lock",mode = 'w') as f:
                    f.close()

        self.logPath = self.logDir  + filename + ".ing"

#log文件大于logsize，新建一个log文件
#log文件数目大于lognum，删除旧的文件

        lock = open(self.logDir + "lock")
        fcntl.flock(lock,fcntl.LOCK_EX)

        if len(os.listdir(self.logDir)) >= LOG_SIZE:
            for i in range(LOG_NUM - 1,-1,-1):
                if os.path.exists(self.logDir + type + str(i)):
                    if i == LOG_NUM - 1:
                        os.remove(self.logDir + type + str(i))
                    else:
                        os.rename(self.logDir + type + str(i),self.logDir + type + str(i + 1))
            # 将对应的日志文件打包
            import zipfile
            os.chdir(self.logDir)
            log_zip = zipfile.ZipFile(self.logDir + type + "0","w")
            for f in os.listdir(self.logDir):
                if f[-1] == "g":
                    log_zip.write(self.logDir + os.sep + f)
                    os.remove(self.logDir + os.sep + f)
            log_zip.close()

        fcntl.flock(lock,fcntl.LOCK_UN)
        lock.close()

        logging.basicConfig(level=logging.DEBUG,
                format='%(asctime)s  %(levelname)s: %(message)s',
                datefmt='%a, %d %b %Y %H:%M:%S',
                filename=self.logPath,
                filemode='a+')

    def error(self,msg):
        logging.error(msg)

    def info(self,msg):
        logging.info(msg)

    def warning(self,msg):
        logging.warn(msg)

    def command(self,msg):
        command = "command: "
        logging.info(command + msg)

    def getLogPath(self):
        return self.logPath

    def chaLogPath(self):
        os.rename(self.logPath,self.logPath[:-3] + "log")
