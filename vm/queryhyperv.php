<?php
/**
 * 读取Hyperv的实例，iconv处理服务器终端是gbk编码的问题，
 * 一般来说不需要iconv函数，因为都会指定编码方式，不能跟随pc端的编码而变化
*/

//session_start();
header('content-type:text/html;charset=utf-8');

include_once('iflogin.php');
$ip = $_POST['ip'];

class datamessage
{
	public $data = array();
	public $error = 0;
	public $errormessage = "";
}

$ii = 0;
$datamessage = new datamessage();
$command = $GLOBALS['javapath'].'java -jar jar/HypervNameQuery.jar '.$ip;
$file = popen($command,"r");

while (( $buffer  = fgets ( $file ,  4096 )) !==  false ) {
	$buffer =trim( $buffer);

	if(preg_match("/([0-9]+)/",$buffer) ){
		$info=preg_split ("/ +/",$buffer);
		$datamessage->data[$ii] = array('name' =>$info[0]);
		$ii++;

	}

}
pclose($file);
echo json_encode($datamessage);
