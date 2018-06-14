#!/usr/bin/python
#coding=utf-8

""""
    @文件名:Recovery_thread.py
    @还原hdfs的文件到本地
    @修改内容：修改接口，添加定期还原参数
"""

import sys
import os
import Log
import Util

# 执行的下载文件的逻辑
def downloadThread(filename,date):
    global dl_command
    os.system(dl_command % (filename,date,filename))

# 对应在linux下执行的shell语句
dl_command = "hadoop fs -get /Backup/%s/%s/ ./tmph/%s/"
mv_command = "cp -arf --link ./tmph/%s/%s/. ./tmph/%s/%s/"
mvAll_command = "cp -arf --link ./tmph/%s/%s/. ./tmph/%s/"
mk_command = "mkdir ./tmph/%s"
rm_command = "rm -rf ./tmph/%s"
ls_command = "hadoop fs -ls /Backup/"

if len(sys.argv) != 5:
    print("ERROR:SCD-08000.LEVEL:4.Please input like eg:'Recovery.py filename startdate recovrydate id'")
    exit(1)

log = Log.log("Recovery",sys.argv[-1])
log.command(" ".join(sys.argv))

# 检查是否有其他进程在执行还原逻辑
if Util.check_pro(sys.argv[0].split("/")[-1] +" " + sys.argv[1]) > 3:
    log.warning("anothor pro is in running,exit the process now!")
    log.chaLogPath()
    exit(1)

filename,strdate,redate = sys.argv[1],sys.argv[2],sys.argv[3]
log.info("===Recovering the %s on %s====" % (filename,redate))

# 切换工作目录
savePath = ""
with open(sys.path[0][:-5] + "/system.conf",mode="r") as conf:
    for line in conf:
        if "savePath" in line:
            savePath = line.split("=")[1].strip()

if not savePath:
    savePath = sys.path[0]
os.chdir(savePath)

# 执行命令列出文件保存日期的，是否保存了对应日期的备份
ls_outcome = os.popen(ls_command + filename).read().split("\n")
try:
    firstdate = ls_outcome[1][-14:]
except:
    log.error("Sorry,You didn't backup %s on the HDFS.Please check the filename!" % filename)
    print("ERROR:SCD-08002.LEVEL:4.Sorry,You didn't backup %s on the HDFS.Please check the filename!" % filename)
    log.chaLogPath()
    exit(1)

# 若文件已经还原，则脚本运行结束
if (os.path.exists("./tmph/%s" % filename + "-" + redate)):
    log.info("The file is recovered.")
    log.chaLogPath()
    exit(0)

if firstdate <= redate:
    for record in ls_outcome[1:]:
        if redate == record[-14:]:
           com_date = firstdate

        #  还原备份的文件
           os.system(mk_command % filename)
           downloadThreads = []
           downloadDate = []
           for mid_date in ls_outcome[1:]:
                com_date = mid_date[-14:]
                if strdate <=com_date <= redate and com_date:
                    downloadDate.append(com_date)
                    downloadThread(filename,com_date)
                    downloadThreads.append(1)
                elif com_date > redate:
                    break

           for num,_ in enumerate(downloadThreads):
               if num != 0:
                   os.system(mv_command % (filename,downloadDate[num],filename,strdate))
                   os.system(rm_command % filename + "/" + downloadDate[num])

           os.system(mk_command % (filename + "-" + redate))
           os.system(mvAll_command % (filename,strdate,filename + "-" + redate))
           os.system(rm_command % filename)


           log.info("===Recovery Successfully!====")
           log.chaLogPath()
           exit(0)
        elif redate < record[-14:]:
            break

log.warning("Sorry,You didn't backup %s on the %s.Please try another date!" % (filename,redate))
# 打印可用的还原日期
print("The available date list:")
for record in ls_outcome[1:]:
    if record[-14:]:
        print(record[-14:])

log.chaLogPath()
exit(1)
