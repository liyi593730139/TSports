#!/usr/bin/python
#coding=utf-8

""""
    @文件名:Merge.py
    @合并hdfs上存储的文件
"""

import sys
import Log
from fileinput import filename
import Util
import os

# 对应在linux下执行的shell语句
re_command = "hadoop fs -cp -f /Backup/%s/%s/* /Backup/%s/%s/"
ls_command = "hadoop fs -ls /Backup/"
rm_command = "hadoop fs -rm -r -f /Backup/%s/%s"
mv_command = "hadoop fs -mv  /Backup/%s/%s /Backup/%s/%s"
du_command = "hadoop dfs -dus /Backup/%s/%s"

help = """Please input like eg:'merge.py filename old_date merge_date taskId'(merge_date must be after old_date id)\n"""
# 参数检查，是否满足要求
if len(sys.argv) != 5:
    print len(sys.argv)
    print(help)
    exit(1)

log = Log.log("Merge",sys.argv[-1])
log.command(" ".join(sys.argv))

# 检查是否有其他进程在执行合并逻辑
if Util.check_pro(sys.argv[0].split("/")[-1] +" " + sys.argv[1]) > 3:
    log.warning("anothor pro is in running,exit myself")
    log.chaLogPath()
    exit(1)

if len(sys.argv) == 5:
    if sys.argv[2] >= sys.argv[3]:
        print("ERROR:SCD-08000.LEVEL:4." + help)
        log.chaLogPath()
        exit(1)

    # 执行对应的合并逻辑,将old 到 merge的时间内的文件
    filename,old_date,merge_date = sys.argv[1],sys.argv[2],sys.argv[3]
    log.info("===Merging the %s between %s and %s====" % (filename,old_date,merge_date))

    ls_outcome = os.popen(ls_command + filename).read().split("\n")

    # 合并从old_date到merge_date的所有文件
    firstFlag = True
    first_date = ""
    for index,record in enumerate(ls_outcome[1:]):
        mid_date = record[-14:]
        if mid_date >= old_date and mid_date <= merge_date and firstFlag:
            first_date = mid_date
            firstFlag = False
            continue
        elif mid_date >= old_date and mid_date <= merge_date:
            os.system(re_command % (filename,mid_date,filename,first_date))
            os.system(rm_command % (filename,mid_date))

    if first_date:
        os.system(mv_command % (filename,first_date,filename,merge_date))
        for line in os.popen(du_command % (filename,merge_date)):
            print line.split()[0]
        log.info("===Merge Successfully!====")
    else:
        log.warning("Maybe the datetime is wrong!")

else:
    log.error(help)
    print("ERROR:SCD-08011.LEVEL:4." + help)

log.chaLogPath()
