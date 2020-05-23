package com.ssrs.framework.task;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.ssrs.framework.schedule.SystemTask;

/**
 * @author ssrs
 */
public class TestTask2 extends SystemTask {
    private Log log = LogFactory.get(TestTask2.class);

    @Override
    public void execute() {
        log.info("------------------正在执行task2---------------");
    }

    @Override
    public String getCronExpression() {
        return "0/10 * * * * ?";
    }

    @Override
    public String getExtendItemID() {
        return "com.ssrs.framework.task.TestTask2";
    }

    @Override
    public String getExtendItemName() {
        return "测试任务2";
    }
}
