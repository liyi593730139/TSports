#!/usr/bin/env python
#coding=utf-8
""""
    @文件名:AvailableDate.py
    @作用：查看对应文件的备份时间
"""

import sys
import os
import Log

# 对应在linux下执行的shell语句
ls_command = "hadoop fs -ls /Backup/"

if __name__ == "__main__":

    #参数检测，是否满足两个参数
    if len(sys.argv) != 3:
        print("ERROR:SCD-08011.LEVEL:4.Please input like eg:'AvailableDate.py filename id'")
        exit(1)

    #建立日志文件
    log = Log.log("AvailableData",sys.argv[-1])
    log.command(" ".join(sys.argv))
    # 列出文件是否保存在hdfs之中
    filename = sys.argv[1]
    ls_outcome = os.popen(ls_command + filename).read().split("\n")

    try:
        firstdate = ls_outcome[1][-10:]
    except:
        log.error("Sorry,You didn't backup %s on the HDFS.Please check the filename!" % filename)
        print("ERROR:SCD-08002.LEVEL:4.Sorry,You didn't backup %s on the HDFS.Please check the filename!" % filename)
        log.chaLogPath()
        exit(1)

    # 显示可用的备份时间点
    log.info("======Show The available date list of %s======" % sys.argv[1])
    for record in ls_outcome[1:]:
        if record[-14:]:
            log.info(record[-14:])
            print(record[-14:])
    log.chaLogPath()
    exit(0)
