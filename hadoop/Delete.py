#!/usr/bin/python
#coding=utf-8

""""
    @文件名:Delete.py
    @删除对应hdfs上的备份文件
"""
import sys
import os
import Log
import Util

# 对应在linux下执行的shell语句
rm_command = "hadoop fs -rmr /Backup/%s/%s"
rm_Allcommand = "hadoop fs -rmr /Backup/%s/"

#参数数目的检查,是否满足hashname date -a/-d and missionid.
#-a 删除对应目录下所有文件
#-d 删除指定日期的文件
if len(sys.argv) != 5:
    print("ERROR:SCD-08011.LEVEL:4.Please input like eg:'Delete.py hashname date -a,-d id'")
    exit(1)

# 创建日志目录
log = Log.log("Delete",sys.argv[-1])
log.command(" ".join(sys.argv))


# 删除安全模式检查
Util.checkSafeMode(log)
hashname,date,filename = sys.argv[1],sys.argv[2],sys.argv[3]
filename = filename.replace("'","")

# 若-a则删除全部文件
if filename == "-a":
    rm_command = rm_Allcommand % hashname
else:
    rm_command = rm_command % (hashname,date)

# 执行删除逻辑
rm_output = os.popen(rm_command)
for line in rm_output:
    if line.startswith("Deleted"):
        log.info("Delete successfully!")
        log.chaLogPath()
        exit(0)

log.info("Delete failed.Please check the DFS status.")
log.chaLogPath()
