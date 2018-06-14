#!/usr/bin/python
#coding=utf-8

""""
    @文件名:Dfshealth.py
    @反馈给前端存储模块的容量
    @修改人:HappenLee
    @时间:2017/04/29
    @修改内容：添加注释
"""

import os
from threading import Thread

REPORT_COMMMAND = "hadoop dfsadmin -report 2>&1"

class TimeoutException(Exception):
    pass

ThreadStop = Thread._Thread__stop
# 装饰器函数用超时机制
def timelimited(timeout):
    def decorator(function):
        def decorator2(*args,**kwargs):
            class TimeLimited(Thread):
                def __init__(self,_error= None,):
                    Thread.__init__(self)
                    self._error =  _error

                def run(self):
                    try:
                        self.result = function(*args,**kwargs)
                    except Exception,e:
                        self._error =e

                def _stop(self):
                    if self.isAlive():
                        ThreadStop(self)

            t = TimeLimited()
            t.start()
            t.join(timeout)

            if isinstance(t._error,TimeoutException):
                t._stop()
                raise TimeoutException('timeout for %s' % (repr(function)))

            if t.isAlive():
                t._stop()
                print("ERROR:SCD-08012.LEVEL:4.The hadoop is broken,Please contact with Administrator.")
                exit(1)

            if t._error is None:
                return t.result

        return decorator2
    return decorator

# 设置超时时间为20秒
@timelimited(20)
def getDfsHealth():
    dfsreport = os.popen(REPORT_COMMMAND).readlines()
    return dfsreport

dfsreport = getDfsHealth()[2:]
dfsHealth = []
for line in dfsreport:
    if line != "\n":
        dfsHealth.append(line.strip("\n"))

ansHealth = []
ansHealth.append("[DFS Health]")

# 是否处于安全模式
if "ON" in dfsHealth[0]:
    ansHealth.append(dfsHealth[0])
    dfsHealth = dfsHealth[1:]
for line in dfsHealth[:5]:
    x = line.split(":")
    property,value = x[0],x[1]
    if "(" in value:
        value = value[:value.index("(")]
    ansHealth.append(property + "=" + value)

# 可用节点的检测，包括live 和 dead节点
for lineNum,string in enumerate(dfsHealth[:15]):
    if "Live" in string:
        num = int(dfsHealth[lineNum][dfsHealth[lineNum].index('(') + 1])
        ansHealth.append("Live DataNode = " + str(num))
        i = num * 16 + lineNum + 1

        try:
            deadNum = int(dfsHealth[i][dfsHealth[i].index('(') + 1])
        except IndexError:
            deadNum = 0

        ansHealth.append("Dead DataNode = " + str(deadNum))
        break

for i in range(lineNum + 1,(num - 1) * 16 + 12,16):
    for line in dfsHealth[i:i + 9]:
        x = line.split(":")
        property,value = x[0],x[1].strip('\n')
        if "(" in value:
            value = value[:value.index("(")]
        if property == "Name":
            ansHealth.append("[" + str(value) + "]")
        else:
            ansHealth.append(property + "=" + value)

i = num * 16 + lineNum + 1

# 遍历输出每个节点的信息
try:
    num = int(dfsHealth[i][dfsHealth[i].index('(') + 1])

    for j in range(i + 1,(num - 1) * 16 + i + 2,16):
        for line in dfsHealth[j:j + 2]:
            x = line.split(":")
            property,value = x[0],x[1].strip('\n')
            if "(" in value:
                value = value[:value.index("(")]
            if property == "Name":
                ansHealth.append("[" + str(value) + "]")
            else:
                ansHealth.append(property + "=" + value)
except:
    pass

for line in ansHealth:
    print line

# with open("/home/happen/Desktop/DFS.ini","w") as file2Write:
#     for line in ansHealth:
#         file2Write.write(line + "\n")
