#!/usr/bin/env python
#coding=utf-8
""""
    @文件名:Dynamic_add_del_node
    @作用：动态增加删除hadoop集群中的datanode
"""

import os
import sys
import Log

# 对应在linux下执行的shell语句
ADD_COMMAND = """ssh -t %s 'echo %s >> %s'"""
SCP_COMMAND = """scp %s %s:%s"""

if __name__ == "__main__":
    #日志模块记录对应的日志信息
    log = Log.log("DynamicNode",sys.argv[-1])
    log.command(" ".join(sys.argv))

    #获取当前的工作目录
    now_path = os.getcwd()
    #检测是否hadoop安装妥当
    hadoop_path = os.getenv("HADOOP_INSTALL")
    if not hadoop_path:
        errorMsg = "'Please check the setting of 'HADOOP_HOME' path in env.'"
        log.error(errorMsg)
        print("ERROR:SCD-08012.LEVEL:4." + errorMsg)
        log.chaLogPath()
        exit(1)

    #检查执行脚本的节点是否为active状态
    hostname = os.popen("hostname").read().strip("\n")
    isActive = os.popen(hadoop_path + "/bin/hdfs haadmin -getServiceState %s" % hostname).read().strip("\n")
    if isActive != "active":
        errorMsg = "'Sorry,please exec the script in the Active namenode.'"
        log.error(errorMsg)
        print("ERROR:SCD-08012.LEVEL:4." + errorMsg)
        log.chaLogPath()
        exit(1)
    #初始化需要修改配置文件的路径
    deny_path = hadoop_path + "/etc/hadoop/datanode-deny.list"
    add_path = hadoop_path + "/etc/hadoop/slaves"
    master_path = hadoop_path + "/etc/hadoop/masters"
    num_datanode = len(sys.argv)
    if os.path.exists(master_path) and os.path.exists(deny_path) and os.path.exists(add_path) and num_datanode > 2:
        with open(master_path) as file:
            master0 = file.readline().strip("\n")
            master1 = file.readline().strip("\n")
        if not master0 or not master1:
            log.error("%s" % master_path)
            print("ERROR:SCD-08012.LEVEL:4.%s" % master_path)
        #确认是否添加的节点ip记录在/etc/hosts文件中，可以选择不记录
        hosts = open("/etc/hosts","r")
        content = hosts.readlines()
        content = [i.strip("\n").split()[1] for i in content if i[0].isdigit()]

        for i in range(2,num_datanode):
            datanode = sys.argv[i]
            if datanode not in content:
                warnMsg = "'Sorry,the %s ip not in /etc/hosts,Please revise the /etc/hosts.'" % datanode
                log.warning(warnMsg)
        #没有合法的节点，结束脚本
        if len([i for i in sys.argv[2:] if i]) == 0:
            log.warning("All input datanode is illeagl.")
            log.chaLogPath()
            exit(1)
        hostName = os.popen("hostname").readlines()[0].strip()

        #处理删除的的节点
        if sys.argv[1] == "del":
            isInAddPath = []

            with open(add_path) as file:
                while True:
                    line = file.readline().strip("\n")
                    if not line:
                        break
                    isInAddPath.append(line)

            for i in range(2,num_datanode):
                if sys.argv[i] not in isInAddPath:
                    sys.argv[i] = ""
                else:
                    isInAddPath.remove(sys.argv[i])

            with open(add_path,"w") as file:
                for node in isInAddPath:
                    file.write(node + "\n")
            with open(deny_path) as file:
                while True:
                    line = file.readline().strip("\n")
                    if not line:
                        break
                    for i in range(2,num_datanode):
                        if sys.argv[i] == line:
                            sys.argv[i] = ""

            for i in range(2,num_datanode):
                if sys.argv[i] != "":
                    os.system(ADD_COMMAND % (master0,sys.argv[i],deny_path))
                    os.system(ADD_COMMAND % (master1,sys.argv[i],deny_path))

            if hostName == master0:
                os.system(SCP_COMMAND % (add_path,master1,add_path))
            else:
                os.system(SCP_COMMAND % (add_path,master1,add_path))

        # 处理添加的节点
        elif sys.argv[1] == "add":
            for datanode in sys.argv[2:]:
                if datanode:
                    os.system("ssh %s -t '%s/sbin/hadoop-daemon.sh start datanode'" % (datanode,hadoop_path))
        #要添加的节点在deny_path中，删除对应记录
            with open(deny_path) as file:
                lines = file.readlines()
                for j in range(len(lines)):
                    for i in range(2,num_datanode):
                        if sys.argv[i] == lines[j].strip():
                            lines[j] = ""

            with open(deny_path,"w") as file:
                for line in lines:
                    if line != "":
                        file.write(line)

            #唤醒对应的添加的节点
            with open(add_path,"a+") as file:
                while True:
                    line = file.readline().strip("\n")
                    if not line:
                        break
                    for i in range(2,num_datanode):
                        if sys.argv[i] == line:
                            sys.argv[i] = ""
                for i in range(2,num_datanode):
                    if sys.argv[i] != "":
                        file.write(sys.argv[i] + "\n")
            #同步文件内容到standby节点
            if hostName == master0:
                os.system(SCP_COMMAND % (deny_path,master1,deny_path))
                os.system(SCP_COMMAND % (add_path,master1,add_path))
            else:
                os.system(SCP_COMMAND % (deny_path,master0,deny_path))
                os.system(SCP_COMMAND % (add_path,master1,add_path))

        #更新hadoop的状态
        output = os.system("hadoop dfsadmin -refreshNodes")
        infoMsg = "'Ok,the datanode is refresh!'"
        log.info(infoMsg)
        log.chaLogPath()

    else:
        #错误记录
        if not os.path.exists(deny_path):
            errorMsg = "'Sorry,Don't find %s'" % deny_path
        elif not os.path.exists(master_path):
            errorMsg = "'Sorry,Don't find %s'" % master_path
        else:
            errorMsg = "'Please input the Dynamic_add_del_node.py (del or add) and datanode's IP'"
        log.error(errorMsg)
        print("ERROR:SCD-08012.LEVEL:4." + errorMsg)
        log.chaLogPath()
