package common.monitor;

/**
* 报警模块接口。
*/
public interface Alert {
    /**
    * 作业执行异常报警。
    * @param taskID 任务id。
    * @param type 作业报警类型。
    * @param errorCode 作业报警错误码。
    * @param errorInfo 作业报警信息。
    */
    public void taskAlert(String taskID, String type, String errorCode, String errorInfo);
    /**
    * 总调度执行异常报警。
    * @param type 作业报警类型。
    * @param errorCode 作业报警错误码。
    * @param errorInfo 作业报警信息。
    */
    public void systemAlert(String type, String errorCode, String errorInfo);
    /**
    * 监控结果异常报警。
    * @param taskID 任务id。
    * @param type 作业报警类型。
    * @param errorCode 作业报警错误码。
    * @param errorInfo 作业报警信息。
    */
    public void monitorAlert(String taskID, String type, String errorCode ,String errorInfo);
}
