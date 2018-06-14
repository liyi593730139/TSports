#Recovery_once.py
#!/usr/bin/python
#coding=utf-8

import sys
import os
import Log
import Util

#Execute the download command
def downloadThread(filename,date):
    global dl_command
    os.system(dl_command % (filename,date,filename))


# The Linux command will be execute By the Script
dl_command = "hadoop fs -get /Backup/%s/%s/ ./tmph/%s/"
mv_command = "cp -arf --link ./tmph/%s/%s/. ./tmph/%s/%s/"
mvAll_command = "cp -arf --link ./tmph/%s/%s/. ./tmph/%s/"
mk_command = "mkdir ./tmph/%s"
rm_command = "rm -rf ./tmph/%s"
ls_command = "hadoop fs -ls /Backup/"

if len(sys.argv) != 4:
    print("ERROR:SCD-08000.LEVEL:4.Please input like eg:'Recovery.py filename date id'")
    exit(1)

log = Log.log("Recovery",sys.argv[-1])
log.command(" ".join(sys.argv))


# check whether anothor pro in running
if Util.check_pro(sys.argv[0].split("/")[-1] +" " + sys.argv[1]) > 3:
    log.warning("anothor pro is in running,exit the process now!")
    exit(1)

filename,redate = sys.argv[1],sys.argv[2]
log.info("===Recovering the %s on %s====" % (filename,redate))

# change the working dir
savePath = ""
with open(sys.path[0][:-5] + "/system.conf",mode="r") as conf:
    for line in conf:
        if "savePath" in line:
            savePath = line.split("=")[1].strip()
            break
if not savePath:
    savePath = sys.path[0]
os.chdir(savePath)

ls_outcome = os.popen(ls_command + filename).read().split("\n")
try:
    firstdate = ls_outcome[1][-14:]
except:
    log.error("Sorry,You didn't backup %s on the HDFS.Please check the filename!" % filename)
    print("ERROR:SCD-08002.LEVEL:4.Sorry,You didn't backup %s on the HDFS.Please check the filename!" % filename)
    exit(1)

if (os.path.exists("./tmph/%s" % filename + "-" + redate)):
    log.info("The file is recovered.")
    exit(0)

if firstdate <= redate:
    for record in ls_outcome[1:]:
        if redate == record[-14:]:
           com_date = firstdate

           os.system(mk_command % filename)
           downloadThread(filename,redate)
           os.system(mk_command % (filename + "-" + redate))
           os.system(mvAll_command % (filename,redate,filename + "-" + redate))
           os.system(rm_command % filename)

           log.info("===Recovery Successfully!====")
           exit(0)

        elif redate < record[-14:]:
            break

log.warning("Sorry,You didn't backup %s on the %s.Please try another date!" % (filename,redate))

print("The available date list:")
for record in ls_outcome[1:]:
    if record[-14:]:
        print(record[-14:])

exit(1)
