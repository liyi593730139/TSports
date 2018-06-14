#!/usr/bin/python
#coding=utf-8

""""
    @文件名:BackUp_Thread.py
    @作用上传文件至HDFS之上
"""

import os
import sys
import re
import threading
import time

import Log
import Util

# 对应在linux下执行的shell语句
upload_commad = "hadoop fs -put -f %s/* %s"
mk_command = "hadoop fs -mkdir %s"
mvALL_command = "cp -arf --link %s/* %s/ 2>&1"
mv_command = "cp -arf --link %s %s/"
rm_command = "rm -rf %s"


def checkUploadSize(path): # 检查上传容量是否足够
    """
    checkUploadSize() -> None

    上传文件之前先比对存储容量是否足够上传，在日志中记录容量不足的情况，管理员需介入。
    若上传文件夹tmp1为空，则不执行此次上传操作。
    """
    HADOOP_STATUS_COMMAND = "hadoop dfsadmin -report"
    sumSize = 0
    fileCount,dirCount = 0,0

    def compareSize(sumSize,num):
        return  sumSize / (1000.0 ** num)

    for root,dirs,files in os.walk(path):
        for file in files:
            sumSize += os.path.getsize(root +"/"+ file)
            fileCount += 1
        for dirs in dirs:
            dirCount += 1

    if dirCount == 0 and fileCount == 0:
        log.info("tmp1 is empty.upload over")
        log.chaLogPath()
        exit(1)

    hadoopRemaining = re.split("\(|\)",os.popen(HADOOP_STATUS_COMMAND).readlines()[2])[1]
    capacity,unit = float(hadoopRemaining[:-2]),hadoopRemaining[-2:]

    if unit == "GB":
        sumSize = compareSize(sumSize, 3)
    elif unit == "TB":
        sumSize = compareSize(sumSize, 4)
    elif unit == "MB":
        sumSize = compareSize(sumSize, 2)

    if capacity > sumSize:
        pass
    else:
        log.error("Error:The DFS capacity is not allow this time upload!Please contact with Administrator.")
        print("ERROR:SCD-08003.LEVEL:4.Error:The DFS capacity is not allow this time upload!Please contact with Administrator.")
        log.chaLogPath()
        exit(1)


def uploadThread(upload_dir,upload_path,updir):# 上传文件夹的线程
     """
     uploadThread(upload_dir,upload_path,updir) -> None

     执行上传文件的逻辑，上传完毕之后，移动文件到tmp3，删除在tmp2下的文件
     """
     os.system(mk_command % upload_path + updir)
     os.popen(upload_commad  % (upload_dir,upload_path + updir))
     os.system(mv_command % (upload_dir,"./tmp3"))

     upload_dirs = os.walk(upload_dir)
     for root,dates,files in upload_dirs:
        for date in dates:
            os.system(rm_command % "tmp1/" + updir + "/" + date)
     os.system(rm_command % upload_dir)

# 切换工作目录至savePath路径下
savePath = ""
with open(sys.path[0][:-5] + "/system.conf",mode="r") as conf:
    for line in conf:
        if "savePath" in line:
            savePath = line.split("=")[1].strip()

if not savePath:
    savePath = sys.path[0]
os.chdir(savePath)

#参数检查
if len(sys.argv) != 2:
    print("ERROR:SCD-08011.LEVEL:4.Please input like eg:'Backup.py id'")
    exit(1)

log = Log.log("Backup",sys.argv[-1])
log.command(" ".join(sys.argv))


# 检查是否有其他进程在执行上传逻辑
if Util.check_pro(sys.argv[0].split("/")[-1]) > 3:
    log.warning("anothor pro is in running,exit myself")
    log.chaLogPath()
    exit(1)

log.info("===Uploaing the data To HDFS.===")

# 目录检查
if not os.path.exists("./tmp1"):
    log.error("tmp1 not exist!Please check the script PATH ")
    print("ERROR:SCD-08002.LEVEL:4.tmp1 not exist!Please check the script PATH ")
    log.chaLogPath()
    exit(1)
if not os.path.exists("./tmp2"):
    log.info("Create a dir which named tmp2.")
    os.mkdir("./tmp2")
if not os.path.exists("./tmp3"):
    log.info("Create a dir which named tmp3.")
    os.mkdir("./tmp3")

# 执行上传逻辑之前检查是否hdfs处于安全模式，同时进行容量检查
upload_path = "/Backup/"
threads = []
Util.checkSafeMode(log)
checkUploadSize("./tmp1")

# 移动tmp1目录下的文件至tmp2目录下
mvResult = os.popen(mvALL_command % ("tmp1","tmp2"))
if len(mvResult.readlines()) > 0:
    log.error("Maybe the space Error or permission problem")
    print("ERROR:SCD-08003.LEVEL:4.Maybe the space Error or permission problem")
    log.chaLogPath()
    exit(1)


# 多线程程执行tmp2下的上传逻辑
upload_dirs = os.walk("./tmp2")
for root,dirs,files in upload_dirs:
    for updir in dirs:
        upload_dir = root + "/" +  updir
        log.info("....Uploading the %s" % (upload_dir))
        t = threading.Thread(target=uploadThread,args=(upload_dir,upload_path,updir,))
        t.setDaemon(True)
        threads.append(t)
        t.start()
    break

for t in threads:
    while t.isAlive():
        time.sleep(10)

log.info("Upload successfully!")
log.chaLogPath()
