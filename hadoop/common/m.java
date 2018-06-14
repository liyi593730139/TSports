
public void deleteDFS(String hash,String taskId) throws IOException {
		RemoteControl.runLocal(new String[]{"python", this.pythonFilePath+"/Delete.py", "-a", taskId});
	}

	@SuppressWarnings("finally")
	public boolean deleteTaskDataFromDFS(DataRemoveMission mis) {
		try {
			String[] path = mis.getRmTimes();
			String[] cmd = new String[4 + path.length];
			String prePath = "/Backup/" + mis.getHash() + "/";
			cmd[0] = "hadoop";
			cmd[1] = "fs";
			cmd[2] = "-rm";
			cmd[3] = "-r";
			for (int i = 0; i < path.length; i++) {
				cmd[i + 4] = prePath + path[i];
			}
			//RemoteControl.runLocal(cmd);
			println(cmd);
			return true;
		} catch (Exception e) {
			System.out.println("HDFS delete data of task failed.");
			System.out.println(e.toString());
			return false;
		}
	}

	private void println(String[] s) {
		String r = "";
		for (String ss : s) {
			if (r.length() == 0) {
				r = ss;
			} else {
				r = r + " " + ss;
			}
		}
		System.out.println(r);;
	}

	public void deleteDFS(String hash, String date, String taskId) throws IOException {
		RemoteControl.runLocal(new String[]{"python", this.pythonFilePath + "/Delete.py", date, taskId});
	}

	public void MergeDFS(String hash,String sdate,String edate,String taskId) throws IOException {
		RemoteControl.runLocal(new String[]{"python", this.pythonFilePath + "/merge.py", sdate, edate, taskId});
	}

	public void MergeDFS(String hash,String taskId) throws IOException {
		RemoteControl.runLocal(new String[]{"python", this.pythonFilePath + "/merge.py", "-a", taskId});
	}

	public void toDFS(String taskId) throws Exception{
		RemoteControl.runLocal(new String[]{"python", this.pythonFilePath + "/BackUp_Thread.py", taskId});
	}

	/**
	* 从HDFS上下载文件(调用Recovery_once.py)
	* 当执行恢复任务时，根据参数中的timeStr（starttime）把指定时间点对应的文件夹内容下载到服务器端
	* @param idName: 指定的备份任务对应的hash值
	* @param timeStr: 指定恢复到的时间点
	* @param taskId: 执行该次下载所生成的日志名
	*/
	private void fromDFS(String idName, String timeStr, String taskId) throws Exception{
		RemoteControl.runLocal(new String[]{"python",
		this.pythonFilePath+"/Recovery_once.py", idName, timeStr, taskId});
		MergeDir(tmph,tmpi);
	}

	/**
	* 从HDFS上下载文件(调用Recovery_once.py)
	* 当执行恢复任务时，根据参数中的timeStr（starttime）把指定时间点对应的文件夹内容下载到服务器端
	* @param idName: 指定的备份任务对应的hash值
	* @param timeStr: 指定恢复到的时间点
	* @param taskId: 执行该次下载所生成的日志名
	*/
	private void fromDFSDatabase(String idName, String timeStr, String taskId, String dbhash) throws Exception {

		// 将时间转换为数据库识别的格式
		String timeformatStr = timeStr.substring(0, 4) + "-" + timeStr.substring(4, 6) + "-" + timeStr.substring(6, 8)
				+ " " + timeStr.substring(8, 10) + ":" + timeStr.substring(10, 12) + ":" + timeStr.substring(12, 14);

		// 从数据库中查询备份作业对应的全备份时间点
		String sqlStr = String.format("select dependency from schedule_be where hash = '%s' and starttime = '%s'", dbhash, timeformatStr);
		MysqlCtrl mysql_ = MysqlCtrl.instance();
		ResultSet rs = mysql_.execQuery(sqlStr);
		rs.next();
		String dependency = rs.getString("dependency");

		// 将全备份时间点转换为时间戳格式
		String dependStr = dependency.substring(0, 4) + dependency.substring(5, 7) + dependency.substring(8, 10)
				+ dependency.substring(11, 13) + dependency.substring(14, 16) + dependency.substring(17, 19);

		RemoteControl.runLocal(
				new String[] { "python", this.pythonFilePath + "/Recovery_thread.py", idName, dependStr, timeStr, taskId });
		MergeDir(tmph, tmpi);
	}

	private void fromDFSMysqlSelected(String idName, String timeStr, String taskId) throws Exception{
		RemoteControl.runLocal(new String[]{"python",
								this.pythonFilePath+"/Recovery_once.py", idName, timeStr, taskId});
		MergeDir(tmph, tmpi);
	}



	/*
	* 将数据传输到HDFS上.
	* 将tmp2中的文件上传到HDFS
	*/
	public void toDFS() throws Exception {
		MergeDir(tmp1, tmp2);
		String nameInHdfs = HdfsPath;
		RemoteControl.runLocal(new String[]{"hadoop", "fs", "-put", tmp2, nameInHdfs});
		MergeDir(tmp2, tmp3);
	}
