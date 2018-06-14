package main

/**  FileName: client_win_v1.5.go
 *  Description: 在之前版本之上增加了以json格式的服务器和Agent之间的通信方式；
 				 增加了文件传输过程中的加密和解密*/

import (
	//"bufio"
	"bytes"
	"fmt"
	"io"
	"io/ioutil"
	//"encoding/binary"
	"bufio"
	"crypto/md5"
	"encoding/hex"
	//"mahonia"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	//"runtime"
	"strconv"
	"strings"
	//"time"
	"client2"
	"encoding/json"
	"log"
	"logger"
	"time"
)

// 文件信息包含（文件名、最新修改时间、文件大小）
type fileinfo struct {
	filename  string
	timestamp int64
	size      int64
}

//接受文件时buffer的大小
const bufferSize = 1024

/** 接收文件
 * @param conn:connection
 * @param path: 数据所写的文件路径及文件名
 */
func recvFile(path string, conn net.Conn) {
	defer func() {
		if r := recover(); r != nil {
			print("Runtime error caught : %v \n", r)
		}
	}()
	strs := strings.Split(path, "@-%-@")
	path = strs[0]
	filesize, err := strconv.ParseInt(strs[1], 10, 64)
	if err != nil {
		panic(err)
	}
	fi2, err := os.OpenFile(path, os.O_TRUNC|os.O_CREATE|os.O_RDWR, 0777)
	if err != nil {
		print("Creat fail \"%s\"\n", err)
		return
	}
	defer fi2.Close()
	conn.Write([]byte("1"))
	buf := make([]byte, bufferSize)
	tmpSize := filesize
	for tmpSize = 0; tmpSize < filesize; {
		n, err := conn.Read(buf) //fi1.Read(buf)
		if err != nil && err != io.EOF {
			conn.Write([]byte("Error"))
			print("%s\n", err)
			return
		}
		if 0 == n {
			break
		}
		fi2.Write(buf[:n])
		tmpSize += int64(n)
	}
	if tmpSize == filesize {
		conn.Write([]byte("Success"))
		print("%s\n", "Success")
	} else {
		conn.Write([]byte("Error"))
		print("%s\n", "Error")
	}
}

/** 发送文件
 * @param conn:connection
 * @param path: 待发送文件的路径名称
 */
func sendFile(path string, conn net.Conn) {
	defer func() {
		if r := recover(); r != nil {
			print("Runtime error caught : %v \n", r)
		}
	}()
	path = strings.Trim(path, " \r\n") //
	fi, err := os.OpenFile(path, os.O_RDONLY, 0777)
	if err != nil {
		print("%s\n", err)
		conn.Write([]byte("There is an error : " + err.Error()))
		return
	}
	defer fi.Close()
	tmpStat, err := fi.Stat()
	filesize := tmpStat.Size()
	tmp := filesize
	print("File size : %d\n", filesize)
	buf := make([]byte, bufferSize)
	for i := 0; i < 8; i++ {
		buf[i] = byte(tmp & 0xff)
		tmp = tmp >> 8
	}
	conn.Write(buf[:8])
	n, err := conn.Read(buf)
	if n == 0 {
		print("%s\n", "Error")
		return
	}
	for {
		n, err := fi.Read(buf)
		if err != nil && err != io.EOF {
			print("%s\n", err)
			return
		}
		if 0 == n {
			break
		}
		conn.Write([]byte(buf[:n]))
	}
}

/** 本地新建文件夹
 * @param conn:connection
 * @param path: 本地新建目录路径
 */
func makeDir(path string, conn net.Conn) {
	defer func() {
		if r := recover(); r != nil {
			print("Runtime error caught : %v \n", r)
		}
	}()
	err := os.MkdirAll(path, 0777)
	if err != nil {
		conn.Write([]byte(err.Error()))
	} else {
		conn.Write([]byte("Success"))
	}
}

/** 远端机器获取本地机器目录
 * @param conn:connection
 * @param path: 本地新建目录路径
 */
func getDir(path string, conn net.Conn) {
	strs := strings.Split(path, "@-%-@")
	remoteDir := strs[0]
	localDir := strs[1]
	dirs, err := ioutil.ReadDir(remoteDir)
	if err != nil {
		conn.Write([]byte("error : " + err.Error()))
		return
	}
	if len(dirs) == 0 {
		conn.Write([]byte("makeDir " + localDir))
		buf := make([]byte, bufferSize)
		conn.Read(buf)
		conn.Write([]byte("end"))
		return
	} else {
		err := checkDir(remoteDir, localDir, conn)
		if err != nil {
			print("%s\n", err.Error())
		}
		conn.Write([]byte("end"))
		print("%s\n", "end")
	}
}

/** 递归调用，将本地目录传输到远端
 * @param conn:connection
 * @param remoteDir: 本地路径
 * @param localDir: 远端路径
 */
func checkDir(remoteDir, localDir string, conn net.Conn) error {
	defer func() {
		if r := recover(); r != nil {
			print("Runtime error caught : %v \n", r)
		}
	}()
	dirs, err := ioutil.ReadDir(remoteDir)
	if err != nil {
		conn.Write([]byte("error : " + err.Error()))
		return err
	}
	buf := make([]byte, bufferSize)
	for _, dir := range dirs {
		if dir.IsDir() {
			remoteStr := remoteDir + "/" + dir.Name()
			localStr := localDir + "/" + dir.Name() //
			tmpdirs, err := ioutil.ReadDir(remoteStr)
			if err != nil {
				conn.Write([]byte("error : " + err.Error()))
				return err
			}
			if len(tmpdirs) == 0 {
				conn.Write([]byte("makeDir " + localStr))
				_, err := conn.Read(buf)
				if err != nil {
					return err
				}
			} else {
				checkDir(remoteStr, localStr, conn)
			}
		} else {
			conn.Write([]byte("get " + remoteDir + "/" + dir.Name() + "@-%-@" + localDir + "/" + dir.Name()))
			n, err := conn.Read(buf)
			if err != nil {
				return err
			}
			str := string(buf[:n])
			if strings.HasPrefix(str, "get ") {
				sendFile(str[4:], conn)
			}
			_, err = conn.Read(buf)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

/* * 判断是否是增量文件，再选择是否传输到远端机器
 * @param conn:connection
 * @param logMap: 上次备份时该目录的所有文件信息
 * @param newLogMap: 这次备份时该目录的所有文件信息
 * @param remoteDir: 本地目录
 * @param localDir: 本地目录对应的远端目录
 */
func wsyncCheckDir(logMap map[string]fileinfo, newLogMap [3]map[string]fileinfo, remoteDir, localDir string, conn net.Conn) error {
	defer func() {
		if r := recover(); r != nil {
			print("Runtime error caught : %v \n", r)
		}
	}()
	dirs, err := ioutil.ReadDir(remoteDir)
	if err != nil {
		conn.Write([]byte("error : " + err.Error()))
		return err
	}
	buf := make([]byte, bufferSize)
	for _, dir := range dirs {
		if dir.IsDir() {
			remoteStr := remoteDir + "/" + dir.Name()
			localStr := localDir + "/" + dir.Name() //
			tmpdirs, err := ioutil.ReadDir(remoteStr)
			if err != nil {
				conn.Write([]byte("error : " + err.Error()))
				return err
			}
			if len(tmpdirs) == 0 {
				tmpInfo, _ := os.Stat(remoteStr)
				_, ok := logMap[remoteStr]
				if !ok {
					newLogMap[0][remoteStr] = fileinfo{remoteStr, tmpInfo.ModTime().Unix(), tmpInfo.Size()}
					conn.Write([]byte("makeDir " + localStr))
					_, err := conn.Read(buf)
					if err != nil {
						return err
					}
				} else {
					newLogMap[2][remoteStr] = fileinfo{remoteStr, tmpInfo.ModTime().Unix(), tmpInfo.Size()}
				}
			} else {
				wsyncCheckDir(logMap, newLogMap, remoteStr, localStr, conn)
			}
		} else {
			remoteStr := remoteDir + "/" + dir.Name()
			localStr := localDir + "/" + dir.Name()
			tmpInfo, _ := os.Stat(remoteStr)
			_, ok := logMap[remoteStr]
			if !ok {
				newLogMap[0][remoteStr] = fileinfo{remoteStr, tmpInfo.ModTime().Unix(), tmpInfo.Size()}
				conn.Write([]byte("get " + remoteStr + "@-%-@" + localStr))
				n, err := conn.Read(buf)
				if err != nil {
					return err
				}
				str := string(buf[:n])
				if strings.HasPrefix(str, "get ") {
					sendFile(str[4:], conn)
				}
				_, err = conn.Read(buf)
				if err != nil {
					return err
				}
			} else if logMap[remoteStr].timestamp < tmpInfo.ModTime().Unix() {
				newLogMap[1][remoteStr] = fileinfo{remoteStr, tmpInfo.ModTime().Unix(), tmpInfo.Size()}
				conn.Write([]byte("get " + remoteStr + "@-%-@" + localStr))
				n, err := conn.Read(buf)
				if err != nil {
					return err
				}
				str := string(buf[:n])
				if strings.HasPrefix(str, "get ") {
					sendFile(str[4:], conn)
				}
				_, err = conn.Read(buf)
				if err != nil {
					return err
				}
			} else {
				newLogMap[2][remoteStr] = fileinfo{remoteStr, tmpInfo.ModTime().Unix(), tmpInfo.Size()}
			}
		}
	}
	return nil
}

/** 将本地目录增量备份到远端
 * @param conn:connection
 * @param path: 需要增量备份的目录
 */
func wsync(path string, conn net.Conn) {
	strs := strings.Split(path, "@-%-@")
	remoteDir := strs[0]
	localDir := strs[1]
	logDir := strs[2]
	curTime := strs[3]
	md5Ctx := md5.New()
	md5Ctx.Write([]byte(remoteDir))
	cipherStr := md5Ctx.Sum(nil)
	tmpfile, err := exec.LookPath(os.Args[0])
	if err != nil {
		fmt.Println(err.Error())
		return
	}
	tmpabspath, err := filepath.Abs(tmpfile)
	if err != nil {
		fmt.Println(err.Error())
		return
	}
	strss := strings.Split(tmpabspath, "/")
	var abspath string
	for i := 0; i < len(strss)-1; i++ {
		abspath += strss[i] + "/"
	}
	os.MkdirAll(abspath+"wsyncLog", 0777)
	logFileName := abspath + "wsyncLog/" + hex.EncodeToString(cipherStr) + ".log"
	logFile, err := os.OpenFile(logFileName, os.O_CREATE|os.O_RDWR, 0666)
	if err != nil {
		print("%s\n", err.Error())
		conn.Write([]byte("error : " + err.Error()))
		return
	}
	defer logFile.Close()
	bufferReader := bufio.NewReader(logFile)
	var (
		logMap    map[string]fileinfo
		newLogMap [3]map[string]fileinfo
	)
	logMap = make(map[string]fileinfo)
	for i := 0; i < 3; i++ {
		newLogMap[i] = make(map[string]fileinfo)
	}
	for line, err := bufferReader.ReadString('\n'); err == nil; line, err = bufferReader.ReadString('\n') {
		line = strings.Trim(line, "\n")
		if line == "Add:" || line == "Modify:" || line == "Others:" || line == "" {
			continue
		}
		strs := strings.Split(line, "-%20-")
		num1, _ := strconv.ParseInt(strs[1], 10, 64)
		num2, _ := strconv.ParseInt(strs[2], 10, 64)
		logMap[strs[0]] = fileinfo{strs[0], num1, num2}
	}
	dirs, err := ioutil.ReadDir(remoteDir)
	if err != nil {
		conn.Write([]byte("error : " + err.Error()))
		return
	}
	if len(dirs) == 0 {
		tmpInfo, _ := os.Stat(remoteDir)
		_, ok := logMap[remoteDir]
		if !ok {
			newLogMap[0][remoteDir] = fileinfo{remoteDir, tmpInfo.ModTime().Unix(), tmpInfo.Size()}
			conn.Write([]byte("makeDir " + localDir))
			buf := make([]byte, bufferSize)
			conn.Read(buf)
		} else {
			newLogMap[2][remoteDir] = fileinfo{remoteDir, tmpInfo.ModTime().Unix(), tmpInfo.Size()}
		}
	} else {
		err := wsyncCheckDir(logMap, newLogMap, remoteDir, localDir, conn)
		if err != nil {
			print("%s\n", err.Error())
		}
	}
	//*******************************8
	newlogFile, err := os.OpenFile(logFileName, os.O_CREATE|os.O_RDWR|os.O_TRUNC, 0666)
	if err != nil {
		print("%s\n", err.Error())
		return
	}
	defer newlogFile.Close()
	tmpStrs := [3]string{"Add:\n", "Modify:\n", "Others:\n"}
	for i := 0; i < 3; i++ {
		_, err := newlogFile.WriteString(tmpStrs[i])
		if err != nil {
			print("%s\n", err.Error())
			return
		}
		for _, tmp := range newLogMap[i] {
			_, err = newlogFile.WriteString(tmp.filename + "-%20-" + fmt.Sprintf("%d", tmp.timestamp) + "-%20-" + fmt.Sprintf("%d", tmp.size) + "\n")
			if err != nil {
				print("%s\n", err.Error())
				return}}}
	statusName := abspath + "wsyncLog/" + hex.EncodeToString(cipherStr) + "_status.log"
	statusFile, err := os.OpenFile(statusName, os.O_CREATE|os.O_APPEND|os.O_RDWR, 0666)
	if err != nil {
		print("%s\n", err.Error())
		return
	}
	defer statusFile.Close()
	_, err = statusFile.Write([]byte(curTime + ".log\n"))
	if err != nil {
		print("%s\n", err.Error())
		return
	}
	buf := make([]byte, bufferSize)
	conn.Write([]byte("get " + logFileName + "@-%-@" + logDir + "/" + curTime + ".log")) //+
	n, err := conn.Read(buf)
	if err != nil {
		return
	}
	str := string(buf[:n])
	if strings.HasPrefix(str, "get ") {
		sendFile(str[4:], conn)
	}
	_, err = conn.Read(buf)
	if err != nil {
		return
	}
	conn.Write([]byte("get " + statusName + "@-%-@" + logDir + "/" + "meta.log"))
	n, err = conn.Read(buf)
	if err != nil {
		return
	}
	str = string(buf[:n])
	if strings.HasPrefix(str, "get ") {
		sendFile(str[4:], conn)
	}
	_, err = conn.Read(buf)
	if err != nil {
		return
	}
	conn.Write([]byte("end"))
}
func heartBeat(conn net.Conn) {
	conn.Write([]byte("heartbeat"))
	buf := make([]byte, bufferSize)
	mes := make(chan int)
	go heartBeating(conn, mes, 60*20)
	for {
		size, err := conn.Read(buf)
		if err != nil {
			print("%s\n", "Read socket Error : "+err.Error())
			return
		}
		tmp := strings.Trim(string(buf[0:size]), " \r\n")
		if strings.HasPrefix(tmp, "heartbeat") {
			conn.Write([]byte("heartbeat"))
		}
		mes <- 1
	}
}
func heartBeating(conn net.Conn, mes chan int, timeout int) {
	timer := time.NewTimer(time.Second * time.Duration(timeout))
	for {
		select {
		case <-mes:
			timer.Reset(time.Second * time.Duration(timeout))
		case <-timer.C:
			close(mes)
			conn.Close()
			print("Shutdown\n")
			return
		}
	}
}
func runLocal(cmd string, conn net.Conn) {
	defer func() {
		if r := recover(); r != nil {
			print("Runtime error caught : %v \n", r)
		}
	}()
	tmps := strings.Split(cmd, "@-%-@")
	cmd = tmps[1]
	cmd = strings.Replace(cmd, "%20", "\\ ", -1)
	print("%s\n", cmd)
	var out bytes.Buffer
	exeCmd := exec.Command("/bin/bash", "-c", cmd)
	exeCmd.Stdout = &out
	exeCmd.Stderr = &out
	exeCmd.Start()
	exeCmd.Wait()
	conn.Write(out.Bytes())
	return
}

/** 新建连接，并解析字符串判断所要执行的操作
 * @param conn:connection
 */
func doServerStuff(conn net.Conn) {
	defer func() {
		if r := recover(); r != nil {
			print("Runtime error caught : %v \n", r)
		}
	}()
	remote := conn.RemoteAddr().String()
	print("%s\n", remote+" connected!")
	buf := make([]byte, bufferSize)
	size, err := conn.Read(buf)
	if err != nil {
		print("%s\n", "Read socket Error : "+err.Error())
		return
	}
	var msg client2.Message
	err = json.Unmarshal(buf[0:size], &msg)
	if err == nil {
		if msg.MSGNum == client2.MSG_RUN_LOCAL_COMMAND {
			print("RunLocal2...")
			client2.RunLocal2(msg, conn)
		} else if msg.MSGNum == client2.MSG_SEND_DATA {
			print("SendFile2...")
			client2.SendFile2(msg, conn)
		} else if msg.MSGNum == client2.MSG_GET_DATA {
			print("RecvFile2...")
			client2.RecvFile2(msg, conn)
		} else if msg.MSGNum == client2.MSG_MAKE_DIR {
			print("MakeDir2...")
			client2.MakeDir2(msg, conn)
		} else if msg.MSGNum == client2.MSG_GET_DIR {
			print("GetDir2...")
			client2.GetDir2(msg, conn)
		} else if msg.MSGNum == client2.MSG_WSYNCDIR {
			print("Wsync2...")
			client2.Wsync2(msg, conn)
		} else {
			print("unvalid json message!")
		}
		conn.Close()
	} else {
		tmp := strings.Trim(string(buf[0:size]), " \r\n")
		if strings.HasPrefix(tmp, "heartbeat") {
			heartBeat(conn)
		} else {
			if strings.HasPrefix(tmp, "get ") {
				sendFile(tmp[4:], conn)
			} else if strings.HasPrefix(tmp, "put ") {
				recvFile(tmp[4:], conn)
			} else if strings.HasPrefix(tmp, "makeDir ") {
				makeDir(tmp[8:], conn)
			} else if strings.HasPrefix(tmp, "getDir ") {
				getDir(tmp[7:], conn)
			} else if strings.HasPrefix(tmp, "wsyncDir ") {
				wsync(tmp[9:], conn)
			} else {
				runLocal(tmp, conn)
			}
			conn.Close()
		}
	}
}
func print(format string, a ...interface{}) {
	logger.Printf(format, a...)
	log.Printf(format, a...) //print log on terminal
}

/** 监听某个端口，对每个新建连接用一个goroutine去处理
 */
func main() {
	tmpfile, err := exec.LookPath(os.Args[0])
	if err != nil {
		fmt.Println(err.Error())
		return
	}
	tmpabspath := filepath.Dir(tmpfile)
	if err != nil {
		fmt.Println(err.Error())
		return
	}
	logger.SetRollingFile(tmpabspath+"/log", "log", 4, 12*1024, 1024)
	print("%s\n", "Starting the server...")
	listener, err := net.Listen("tcp", "0.0.0.0:50000")
	if err != nil {
		print("Listen Error : %s\n", err)
		return
	}
	for {
		conn, err := listener.Accept()
		if err != nil {
			print("Accept Error : %s\n", err)
			return
		}
		go doServerStuff(conn)
	}
}
