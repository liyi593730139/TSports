<?php
/**
 * 处理虚拟机备份
*/
session_start();

// include_once("iflogin.php");
include_once("php_rw_ini.php");
include_once("conn.php");
include_once("log.php");

$ip = $_POST['ip'];
$os = $_POST['os'];
$backupnow = $_POST['backupnow'];
$vmtype = $_POST['vmtype'];
$clientname = $_POST['clientname'];
$poweron = $_POST['poweron'];
$osuser = $_POST['osuser'];

$bkperiod = $_POST['bkperiod'];
$svperiod = $_POST['svperiod'];
$remark = $_POST['missionremarks'];
$missionname = $_POST['missionname'];
$systemtype = $_POST['os'];
$grp = $_SESSION['grp'];
$userid = $_SESSION['id'];

$osversion = $_POST['osversion'];
$volumns = $_POST['volumns'];

$starttime = $_POST['starttime'];
$starttime = date("YmdHis",strtotime($starttime));
$AesKey = $_POST['AesKey'];


if ($vmtype == "vmware") {
	$target = $_POST['vmtarget'];
	$hash = md5("VM:VMWare:".$ip.':'.$target);
	$mission_type = 'VM:VMWare:'.$grp.'';
	$datatype = "backup_vm_vmware";
	$arr = explode("#", $target);
	$nametarget = $arr[0];
	$vmid = $arr[1];
}else if ($vmtype == "hyperv") {
	$target = $_POST['hypervtarget'];
	$hash = md5("VM:HyperV:".$ip.':'.$target);
	$mission_type = 'VM:HyperV:'.$grp.'';
	$datatype = "backup_vm_hyperv";
}

// $vmtype = 'hyperv';
// $ip = '219.223.1.1';
// $osuser = 'root';
// $target = 'fee';
// $bkperiod = '30 min';
// $svperiod = '30 day';
// $hash = '2312324124';
// $remark = '';
// $grp = '1';
// $userid = '18';
// $missionname = 'hrrh';
// $starttime = '20170519101010';
// $osversion = '22';
// $volumns = '1';
// $AesKey = '1';
// $systemtype = 'windows';
// $clientname ='frfer';
// $datatype = 'backup_vm_hyperv';


$filename = __DIR__.'/conf/bac_mission_vm_'.$vmtype.'_'.$_SESSION["unc"].'.ini';
$message = array('error' => 0,'data' => null,'errormessage' =>null );

$stmt = $mysqli->stmt_init();
$sql = "select 1 from mission where hash = ? and action = 'backup' and status != 0 limit 1";
if ($stmt->prepare($sql)) {
	$stmt->bind_param("s",$hash);
	if ($stmt->execute()) {
		$stmt->store_result();
		if ($stmt->num_rows) {
			$message['error'] =1;
			$message['errormessage'] ="此任务已经提交，请勿重复提交！";
			echo json_encode($message);
			exit();
		}
	}else{
		$message['error'] =1;
		$message['errormessage'] ="数据库查询出错了！";
		echo json_encode($message);
		exit();
	}

	$stmt->close();
}
$sql = "insert into mission(missionname,clientname,clientip,status,createtime,systemtype,datatype,grp,userid,action,hash) values(?,?,?,2,'".date('Y-n-j H:i:s')."',?,?,?,?,'backup',?)";
$stmt = $mysqli->stmt_init();
if ($stmt->prepare($sql)) {
	$stmt->bind_param("ssssssss",$missionname, $clientname, $ip, $systemtype,$datatype, $grp, $userid, $hash);
	$result0 = $stmt->execute();
	// $error .= $stmt->error;
	$stmt->close();
}
if ($vmtype == "vmware") {
	$sql0 = "insert into backup_vm_vmware(ip,osuser,target,poweron,bkperiod,svperiod,hash,remark,grp,userid,missionname,starttime,AesKey) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
	$stmt = $mysqli->stmt_init();
	if ($stmt->prepare($sql0)) {
		$stmt->bind_param("sssssssssssss",$ip, $osuser, $target,$poweron, $bkperiod, $svperiod,  $hash, $remark, $grp, $userid, $missionname, $starttime,$AesKey);
		$result = $stmt->execute();
		$stmt->close();
	}
}elseif ($vmtype == "hyperv") {
	$sql0 = "insert into backup_vm_hyperv(ip,osuser,target,bkperiod,svperiod,hash,remark,grp,userid,missionname,starttime,osversion,volumns,AesKey) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	$stmt = $mysqli->stmt_init();
	if ($stmt->prepare($sql0)) {
		$stmt->bind_param("sssssssssssssi",$ip, $osuser, $target, $bkperiod, $svperiod,  $hash, $remark, $grp, $userid, $missionname, $starttime,$osversion,$volumns,$AesKey);
		$result = $stmt->execute();
		// $error .= $stmt->error;
		$stmt->close();
	}
}

// $result0 = mysql_query("insert into mission(missionname,clientname,clientip,status,createtime,systemtype,datatype,grp,userid,action,hash) values('".$missionname."','".$clientname."','".$ip."','2','".date('Y-n-j H:i:s')."','".$systemtype."','backup_vm_".$vmtype."','".$grp."','".$userid."','backup','".$hash."')")or die("Invalid query: " . mysql_error());
// if ($vmtype == "vmware") {
// 	# code...
// 	$result = mysql_query("insert into backup_vm_vmware(ip,osuser,target,poweron,bkperiod,svperiod,hash,remark,grp,userid,missionname,starttime) values('".$ip."','".$osuser."','".$target."','".$poweron."','".$bkperiod."','".$svperiod."','".$hash."','".$remark."','".$grp."','".$userid."','".$missionname."','".$starttime."')")or die("Invalid query: " . mysql_error()) ;
// }else if ($vmtype == "hyperv") {
// 	# code...
// 	$result = mysql_query("insert into backup_vm_hyperv(ip,osuser,target,bkperiod,svperiod,hash,remark,grp,userid,missionname,starttime) values('".$ip."','".$osuser."','".$target."','".$bkperiod."','".$svperiod."','".$hash."','".$remark."','".$grp."','".$userid."','".$missionname."','".$starttime."')")or die("Invalid query: " . mysql_error()) ;
// }

if ($result && $result0) {
 	# code...如果成功还可以备份作业操作
	if ($backupnow == '0') {
		$L = Log::get_instance();
		$L->log(0,$_SESSION["unc"].'任务操作“保存不备份”成功！', date('Y-n-j H:i:s'));
		$L->close();
		$message['error'] =0;
		echo json_encode($message);
		exit();
	}
}else{
	$L = Log::get_instance();
	$L->log(1,$_SESSION["unc"].'任务操作“保存不备份”失败！', date('Y-n-j H:i:s'));
	$L->close();
	$message['error'] =1;
	$message['errormessage'] ="任务保存失败！";
	echo json_encode($message);
	exit;

}
if ($backupnow == '1') {
	if ($AesKey == '1') {
		$AesKey = '1234567890abcdef';
	}else{
		$AesKey = '';
	}
	if ($vmtype == "vmware") {
		$ini_array[$mission_type]=array(
			'ip' => $ip,
			'osuser' => $osuser,
			'target' => $target,
			'starttime' => $starttime,
			'sv_state' => $poweron,
			'vmid' => $vmid,
			// 'bkperiod' => '3000 min',
			'bkperiod' => $bkperiod,
			'svperiod' => $svperiod,
			'remark' => $remark,
			'action' => 'backup',
			'hash' => $hash,
			'grp' => $grp,
			'userid' => $userid,
			'AesKey' => $AesKey,
			);
	}else if ($vmtype == "hyperv") {
		$ini_array[$mission_type]=array(
			'ip' => $ip,
			'osuser' => $osuser,
			'target' => $target,
			'starttime' => $starttime,
			// 'bkperiod' => '3000 min',
			'bkperiod' => $bkperiod,
			'svperiod' => $svperiod,
			'remark' => $remark,
			'action' => 'backup',
			'hash' => $hash,
			'grp' => $grp,
			'userid' => $userid,
			'osversion' => $osversion,
			'volumns' => $volumns,
			'grp' => $grp,
			'userid' => $userid,
			'AesKey' => $AesKey,
			);
	}

	if (!write_ini_file($ini_array, $filename, true)) {
		$L = Log::get_instance();
		$L->log(1,$_SESSION["unc"].'新建mission配置失败：无法写入配置文件', date('Y-n-j H:i:s'));
		$L->close();
		$message['error'] =1;
		$message['errormessage'] ="写入配置文件失败！";
		echo json_encode($message);
		exit();
	}
	$reccmd = $GLOBALS['cmdpath']." submit ".$filename;//这是接口命令." 2>&1"
	$result = exec($reccmd);//就是最后一行"java -version 2>&1"
	if ($result == 'Success') {
		$stmt = $mysqli->stmt_init();
		$sql = "update mission set status = 1 where hash = ? and action = 'backup' and status != 0 ";
		$stmt = $mysqli->stmt_init();
		if ($stmt->prepare($sql)) {
			$stmt->bind_param("s", $hash);
			$stmt->execute();  //未对失败进行处理
			$stmt->close();
		}
		$L = Log::get_instance();
		$L->log(0,$_SESSION["unc"].'在'.$mission_type.'中进行作业'.$hash.'执行成功', date('Y-n-j H:i:s'));
		$L->close();
		$message['error'] =0;
		echo json_encode($message);
		exit();
	}else{
		$L = Log::get_instance();
		$L->log(1,$_SESSION["unc"].'在'.$mission_type.'中进行作业'.$hash.'执行失败', date('Y-n-j H:i:s'));
		$L->close();
		$message['error'] =1;
		$message['errormessage'] ="任务执行失败！";
		echo json_encode($message);
		exit();
	}

}
