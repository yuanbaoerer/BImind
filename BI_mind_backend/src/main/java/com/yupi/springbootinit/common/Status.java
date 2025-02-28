package com.yupi.springbootinit.common;

/**
 * 任务队列，任务状态status
 * @author yuanbao
 */
public enum Status {
    WAITING("waiting"),
    RUNNING("running"),
    SUCCEED("succeed"),
    FAILED("failed");

    /**
     * 状态
     */
    private final String status;

    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
