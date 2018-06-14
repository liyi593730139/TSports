<?php
/**
 * 读取VMWare的实例，iconv处理服务器终端是gbk编码的问题，
 * 一般来说不需要iconv函数，因为都会指定编码方式，不能跟随pc端的编码而变化
*/
header('content-type:text/html;charset=utf-8');
$ip = $_POST['ip'];
$osuser = $_POST['osuser'];
class datamessage
{
	public $data = array();
	public $error = 0;
	public $errormessage = "";
}

$datamessage = new datamessage();

$command = 'SCDEngine/bksys.sh -agentinfo VM:VMWare '.$ip.' '.$osuser;
$out = '';
$file = popen($command,"r");
while (( $buffer  = fgets ( $file ,  4096 )) !==  false ) {
	$buffer =trim( $buffer);
	// $buffer = iconv('GBK', 'UTF-8', $buffer);
	$out = $out.$buffer;
}
$datamessage->data = json_decode($out , true);
fclose($file);
echo json_encode($datamessage);





//恢复任务类型是虚拟机VMware
		}else if($type=="vmware"){
			$sip=$value[1];
			$sosuser=$value[2];
			$target=$value[3];
			$osuser=$_POST["dosuser4"];
			$action="recover";
			$remark=$_POST["remarks"];
		   //将该恢复任务插入re_vm_vmware数据表中
			$sql = "select * from backup_vm_vmware where hash = ? and status = 1";
			$stmt = $mysqli->stmt_init();
			$stmt->prepare($sql);
			$stmt->bind_param("s",$hash);
			$stmt->execute();
			$meta = $stmt->result_metadata();
			while ($field = $meta->fetch_field())
			{
				$params[] = &$row[$field->name];
			}
			call_user_func_array(array($stmt, 'bind_result'), $params);
			while ($stmt->fetch()) {
				foreach($row as $key => $val)
				{
					$c[$key] = $val;
				}
				$result[] = $c;
			}
			$stmt->close();
			$AesKey = $result[0]['AesKey'];
			if ($AesKey == '1') {
				$AesKeyValue = '1234567890abcdef';
			}else{
				$AesKeyValue = '';
			}

			$stmt = $mysqli->stmt_init();
			$sql = "INSERT INTO re_vm_vmware (grp,userid, sip, sosuser, target, bktime, ip, osuser, action, hash, remark, AesKey,missionname) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
			$stmt = $mysqli->stmt_init();
			$stmt->prepare($sql) ;
			$stmt->bind_param("sssssssssssss", $_SESSION["grp"],$userid,$sip,$sosuser,$target,$bktime,$ip,$osuser,$action,$hash,$remark,$AesKey,$missionname);
			$stmt->execute();
			$stmt->close();
			if($_POST["actionmethod"]=="dorecover"){
				$createtime = date('Y-m-d H:i:s');
			  //插入总mission数据表中，状态status设置为1
				$stmt = $mysqli->stmt_init();
				$sql = "INSERT INTO mission (missionname, clientname, status, createtime, systemtype, datatype, grp,userid, action, hash, clientip) VALUES (?,?,1,?,?,'re_vm_vmware',?,?,'recover',?,?)";
				$stmt = $mysqli->stmt_init();
				$stmt->prepare($sql) ;
				$stmt->bind_param("ssssssss", $missionname,$clientname,$createtime,$clientos,$_SESSION["grp"],$userid,$hash,$ip);
				$stmt->execute();
				$stmt->close();
				$filename = __DIR__.'/conf/rec_mission_vm_vmware_'.$_SESSION["unc"].'.ini';
				if (!file_exists($filename)) {
					# code...如果不存在则创建新文件
					$counter_file  =fopen($filename, 'x');
					fclose($counter_file );
				}else{
					unlink($filename);
				}
				$mission_type = "VM:VMWare:".$_SESSION['grp'];
				$rec_ini_array[$mission_type]=array(
					'sip' => $sip,
					'sosuser' => $sosuser,
					'target' => $target,
					'bktime' => $bktime,
					'ip' => $ip,
					'osuser' => $osuser,
					'action' => $action,
					'hash' => $hash,
					'remark' => $remark,
					'grp' => $grp,
					'userid' => $userid,
					'AesKey' => $AesKeyValue,
					);
				if (!write_ini_file($rec_ini_array, $filename, true)) {
					echo '配置文件填写失败！';
				}else{
				  //执行接口命令
				  $reccmd=$GLOBALS['cmdpath']." submit ".$filename;//这是接口命令
				  exec($reccmd,$result_arr,$result_val);//exec执行系统外部命令时不会输出结果，而是返回结果的最后一行。如果想得到结果，可以使用第二个参数，让其输出到指定的数组。
				  $result = $result_arr[count($result_arr)-1];//获得结果的倒数第二行，最后一行是空行
				  if ($result == "Success") {
				  	$L = Log::get_instance();
				  	$L->log(0,'用户'.$_SESSION["unc"].'恢复'.$mission_type.'成功', date('Y-n-j H:m:s'));
				  	$L->close();
				  	echo "<script>alert('作业进行中!');window.location.href='recover.php';</script>";
				  }else{
				  	$L = Log::get_instance();
				  	$L->log(1,'用户'.$_SESSION["unc"].'恢复'.$mission_type.'失败', date('Y-n-j H:m:s'));
				  	$L->close();
				  	echo "<script>alert('恢复失败，请重试，或联系管理员');</script>";
				  }
				}
			}else{
				$createtime = date('Y-m-d H:i:s');
			  //插入总mission数据表中，状态status设置为2
				$stmt = $mysqli->stmt_init();
				$sql = "INSERT INTO mission (missionname, clientname, status, createtime, systemtype, datatype, grp,userid, action, hash, clientip) VALUES (?,?,2,?,?,'re_vm_vmware',?,?,'recover',?,?)";
				$stmt = $mysqli->stmt_init();
				$stmt->prepare($sql) ;
				$stmt->bind_param("ssssssss", $missionname,$clientname,$createtime,$clientos,$_SESSION["grp"],$userid,$hash,$ip);
				$stmt->execute();
				$stmt->close();
			}
      //恢复任务类型是虚拟机hyperv
		}else if($type=="hyperv"){
			$sip=$value[1];
			$sosuser=$value[2];
			$target=$value[3];
			$osuser=$_POST["dosuser5"];
			$action="recover";
			$remark=$_POST["remarks"];
		   //将该恢复任务插入re_vm_hyperv数据表中

			$sql = "select * from backup_vm_hyperv where hash = ? and status = 1";
			$stmt = $mysqli->stmt_init();
			$stmt->prepare($sql);
			$stmt->bind_param("s",$hash);
			$stmt->execute();
			$meta = $stmt->result_metadata();
			while ($field = $meta->fetch_field())
			{
				$params[] = &$row[$field->name];
			}
			call_user_func_array(array($stmt, 'bind_result'), $params);
			while ($stmt->fetch()) {
				foreach($row as $key => $val)
				{
					$c[$key] = $val;
				}
				$result[] = $c;
			}
			$stmt->close();
			$AesKey = $result[0]['AesKey'];
			if ($AesKey == '1') {
				$AesKeyValue = '1234567890abcdef';
			}else{
				$AesKeyValue = '';
			}
			$stmt = $mysqli->stmt_init();
			$sql = "INSERT INTO re_vm_hyperv (grp,userid, sip, sosuser, target, bktime, ip, osuser, action, hash, remark,osversion,volumns,AesKey,missionname) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			$stmt = $mysqli->stmt_init();
			$stmt->prepare($sql) ;
			$stmt->bind_param("sssssssssssssss", $_SESSION["grp"],$userid,$sip,$sosuser,$target,$bktime,$ip,$osuser,$action,$hash,$remark,$result[0]['osversion'],$result[0]['volumns'],$AesKey,$missionname);
			$stmt->execute();
			$stmt->close();
			if($_POST["actionmethod"]=="dorecover"){
				$createtime = date('Y-m-d H:i:s');
			  //插入总mission数据表中，状态status设置为1
				$stmt = $mysqli->stmt_init();
				$sql = "INSERT INTO mission (missionname, clientname, status, createtime, systemtype, datatype, grp,userid, action, hash, clientip) VALUES (?,?,1,?,?,'re_vm_hyperv',?,?,'recover',?,?)";
				$stmt = $mysqli->stmt_init();
				$stmt->prepare($sql) ;
				$stmt->bind_param("ssssssss", $missionname,$clientname,$createtime,$clientos,$_SESSION["grp"],$userid,$hash,$ip);
				$stmt->execute();
				$stmt->close();
				$filename = __DIR__.'/conf/rec_mission_vm_hyperv_'.$_SESSION["unc"].'.ini';
				if (!file_exists($filename)) {
					# code...如果不存在则创建新文件
					$counter_file  =fopen($filename, 'x');
					fclose($counter_file );
				}else{
					unlink($filename);
				}
				$mission_type = "VM:HyperV:".$_SESSION['grp'];
				$rec_ini_array[$mission_type]=array(
					'sip' => $sip,
					'sosuser' => $sosuser,
					'target' => $target,
					'bktime' => $bktime,
					'ip' => $ip,
					'osuser' => $osuser,
					'action' => $action,
					'hash' => $hash,
					'remark' => $remark,
					'grp' => $grp,
					'userid' => $userid,
					'AesKey' => $AesKeyValue,
					'osversion' => $result[0]['osversion'],
					'volumns' => $result[0]['volumns']
					);
				if (!write_ini_file($rec_ini_array, $filename, true)) {
					echo '配置文件填写失败！';
				}else{
				  //执行接口命令
				  $reccmd=$GLOBALS['cmdpath']." submit ".$filename;//这是接口命令
				  exec($reccmd,$result_arr,$result_val);//exec执行系统外部命令时不会输出结果，而是返回结果的最后一行。如果想得到结果，可以使用第二个参数，让其输出到指定的数组。
				  $result = $result_arr[count($result_arr)-1];//获得结果的倒数第二行，最后一行是空行
				  if ($result == "Success") {
				  	$L = Log::get_instance();
				  	$L->log(0,'用户'.$_SESSION["unc"].'恢复'.$mission_type.'成功', date('Y-n-j H:m:s'));
				  	$L->close();
				  	echo "<script>alert('作业进行中!');window.location.href='recover.php';</script>";
				  }else{
				  	$L = Log::get_instance();
				  	$L->log(1,'用户'.$_SESSION["unc"].'恢复'.$mission_type.'失败', date('Y-n-j H:m:s'));
				  	$L->close();
				  	echo "<script>alert('恢复失败，请重试，或联系管理员');</script>";
				  }
				}
			}else{
				$createtime = date('Y-m-d H:i:s');
			  //插入总mission数据表中，状态status设置为2
				$stmt = $mysqli->stmt_init();
				$sql = "INSERT INTO mission (missionname, clientname, status, createtime, systemtype, datatype, grp,userid, action, hash, clientip) VALUES (?,?,2,?,?,'re_vm_hyperv',?,?,'recover',?,?)";
				$stmt = $mysqli->stmt_init();
				$stmt->prepare($sql) ;
				$stmt->bind_param("ssssssss", $missionname,$clientname,$createtime,$clientos,$_SESSION["grp"],$userid,$hash,$ip);
				$stmt->execute();
				$stmt->close();
			}
