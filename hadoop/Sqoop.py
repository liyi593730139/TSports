#Sqoop.py
#!/usr/bin/python
#coding=utf-8
import os
import sys
import Log

#The Sqoop Command
createHiveCommand = r""
importCommandMysql = r"sqoop import --connect jdbc:{database}://{ip}:{port}/{schema} --username {username} --password {password} --table {table} -m 1 --fields-terminated-by '\t' --target-dir /sqoop/{database}/{ip}/{schema}/{table}"
exportCommandMysql = r"sqoop export --connect jdbc:{database}://{ip}:{port}/{schema} --table {table}  --export-dir /sqoop/{backupDatabase}/{backupIP}/{backupSchema}/{backupTable} --username {username} --password {password} --input-fields-terminated-by '\t'"
importCommandSqlserver = r"sqoop import --connect 'jdbc:{database}://{ip}:{port};username={username};password={password};database={schema};' --table {table} -m 1 --fields-terminated-by '\t' --target-dir /sqoop/{database}/{ip}/{schema}/{table}"
exportCommandSqlserver = r"sqoop export --connect 'jdbc:{database}://{ip}:{port};username={username};password={password};database={schema};' --table {table}  --export-dir /sqoop/{backupDatabase}/{backupIP}/{backupSchema}/{backupTable} --username {username} --password {password} --input-fields-terminated-by '\t'"
importCommandOracle = r"sqoop import --connect jdbc:{database}:thin:@{ip}:{port}:{SID} --username {username} --password {password} --table {table} -m 1 --fields-terminated-by '\t' --target-dir /sqoop/{database}/{ip}/{username}/{table}"
exportCommandOracle = r"sqoop export --connect jdbc:{database}:thin:@{ip}:{port}:{SID} --table {table}  --export-dir /sqoop/{backupDatabase}/{backupIP}/{backupSchema}/{backupTable} --username {username} --password {password} --input-fields-terminated-by '\t'"
#delete the old table in HDFS
deleteCommand = r"hadoop fs -rmr /sqoop/{database}/{ip}/{schema}/{table}"
#detect whether the table in HDFS
detectCommand = r"hadoop fs -test -e /sqoop/{database}/{ip}/{schema}/{table};echo $?"
#The Help Message
help = """====The usage of Script====\n
Sqoop.py import Database(eg:mysql,oracle,sqlserver) ip port schema/SID table username password misssionID\n
Sqoop.py export Database(eg:mysql,oracle,sqlserver) ip port schema/SID table username password BackupDataBase BackupIP BackupSchema BackupTable misssionID\n
Sqoop.py delete BackupDatabase(eg:mysql,oracle,sqlserver) BackupIP BackupSchema BackupTable misssionID\n


log = Log.log("Sqoop",sys.argv[-1])
log.command(" ".join(sys.argv))

LOGPATHADD = " 2>&1| tee -a %s" % log.getLogPath()

if len(sys.argv) > 2 and sys.argv[1] == "import":
    if len(sys.argv) == 10:
        if sys.argv[2] == "mysql":
            importCommand = importCommandMysql.format(database = sys.argv[2],ip = sys.argv[3],port = sys.argv[4],schema = sys.argv[5],table = sys.argv[6],username = sys.argv[7],password = sys.argv[8])
        elif sys.argv[2] == "sqlserver":
            importCommand = importCommandSqlserver.format(database = sys.argv[2],ip = sys.argv[3],port = sys.argv[4],schema = sys.argv[5],table = sys.argv[6],username = sys.argv[7],password = sys.argv[8])
        elif sys.argv[2] == "oracle":
            importCommand = importCommandOracle.format(database = sys.argv[2],ip = sys.argv[3],port = sys.argv[4],SID = sys.argv[5],table = sys.argv[6].upper(),username = sys.argv[7],password = sys.argv[8])
        else:
            log.error("Error:Please check the database you input,only supprt oracle,sqlserver,mysql.(The database name must be lower case)")
            print("ERROR:SCD-08011.LEVEL:4.Error:Please check the database you input,only supprt oracle,sqlserver,mysql.(The database name must be lower case)")
            exit(1)

        deleteCommand = deleteCommand.format(database = sys.argv[2],ip = sys.argv[3],schema = sys.argv[5],table = sys.argv[6])
#Exc the command
        os.system(deleteCommand + LOGPATHADD)
        os.system(importCommand + LOGPATHADD)

    else:
        log.error("Error:Sorry，The args is not look up！")
        print("ERROR:SCD-08011.LEVEL:4.Error:Sorry，The args is not look up！")
        log.info(help)
        exit(1)

elif len(sys.argv) > 2 and sys.argv[1] == "export":
     #Check the args
    if len(sys.argv) != 14:
        log.info(len(sys.argv))
        log.error("Error:Sorry，The args is not look up！\n")
        print("ERROR:SCD-08011.LEVEL:4.Error:Sorry，The args is not look up！\n")
        log.info(help)
        exit(1)

#make sure the table name be upper
    if sys.argv[9] == "oracle":
        sys.argv[12] = sys.argv[12].upper()

    if sys.argv[2] == "mysql":
        exportCommand = exportCommandMysql.format(database = sys.argv[2],ip = sys.argv[3],port = sys.argv[4],schema = sys.argv[5],table = sys.argv[6],username = sys.argv[7],password = sys.argv[8],backupDatabase = sys.argv[9],backupIP = sys.argv[10],backupSchema = sys.argv[11],backupTable = sys.argv[12])
    elif sys.argv[2] == "sqlserver":
        exportCommand = exportCommandSqlserver.format(database = sys.argv[2],ip = sys.argv[3],port = sys.argv[4],schema = sys.argv[5],table = sys.argv[6],username = sys.argv[7],password = sys.argv[8],backupDatabase = sys.argv[9],backupIP = sys.argv[10],backupSchema = sys.argv[11],backupTable = sys.argv[12])
    elif sys.argv[2] == "oracle":
        exportCommand = exportCommandOracle.format(database = sys.argv[2],ip = sys.argv[3],port = sys.argv[4],SID = sys.argv[5],table = sys.argv[6].upper(),username = sys.argv[7],password = sys.argv[8],backupDatabase = sys.argv[9],backupIP = sys.argv[10],backupSchema = sys.argv[11],backupTable = sys.argv[12])
    else:
        log.error("Error:Please check the database you input,only supprt oracle,sqlserver,mysql(The database name must be lower case)")
        print("ERROR:SCD-08011.LEVEL:4.Error:Please check the database you input,only supprt oracle,sqlserver,mysql(The database name must be lower case)")
        exit(1)
    os.system(exportCommand + LOGPATHADD)

elif len(sys.argv) > 2 and sys.argv[1] == "delete":
    if len(sys.argv) == 7:
        detectCommand = detectCommand.format(database = sys.argv[2],ip = sys.argv[3],schema = sys.argv[4],table = sys.argv[5])
        if os.popen(detectCommand).readline().strip() == "0":
#Exc the delete command
            deleteCommand = deleteCommand.format(database = sys.argv[2],ip = sys.argv[3],schema = sys.argv[4],table = sys.argv[5])
            os.system(deleteCommand + LOGPATHADD)
        else:
            log.error("Error,The table not in HDFS,please check the parameters.")
            print("ERROR:SCD-08002.LEVEL:4.Error,The table not in HDFS,please check the parameters.")
            log.info(help)
            exit(1)
    else:
        log.error("Error:Sorry，The args is not look up！")
        print("ERROR:SCD-08011.LEVEL:4.Error:Sorry，The args is not look up！")
        log.info(help)
        exit(1)

else:
    log.error("Error:Wrong Args")
    print("ERROR:SCD-08011.LEVEL:4.Error:Wrong Args")
    log.info(help)
    
