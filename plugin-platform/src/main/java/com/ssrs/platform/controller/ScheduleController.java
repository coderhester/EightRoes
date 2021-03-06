package com.ssrs.platform.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronException;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.task.Task;
import com.ssrs.framework.schedule.SystemTask;
import com.ssrs.framework.schedule.SystemTaskCache;
import com.ssrs.framework.schedule.SystemTaskManager;
import com.ssrs.framework.security.annotation.Priv;
import com.ssrs.framework.util.CronExpression;
import com.ssrs.framework.web.ApiResponses;
import com.ssrs.framework.web.BaseController;
import com.ssrs.platform.code.YesOrNo;
import com.ssrs.platform.model.entity.Schedule;
import com.ssrs.platform.service.IScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.*;

/**
 * @author ssrs
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController extends BaseController {

    @Autowired
    private IScheduleService scheduleService;

    @Priv
    @GetMapping
    public ApiResponses<List<Map<String, Object>>> list() {
        List<SystemTask> taskList = SystemTaskManager.getInstance().getAllTask();
        List<Map<String, Object>> data = new ArrayList<>();
        for (SystemTask t : taskList) {
            SystemTask task = SystemTaskCache.get(t.getExtendItemID());
            Map<String, Object> map = new HashMap<>();
            map.put("id", task.getExtendItemID());
            map.put("typeCode", SystemTaskManager.ID);
            map.put("sourceId", task.getExtendItemID());
            map.put("sourceName", task.getExtendItemName());
            map.put("isUsing", task.isDisabled() ? YesOrNo.No : YesOrNo.Yes);
            map.put("cronExpression", task.getCronExpression());
            if (!task.isDisabled()) {
                try {
                    CronExpression cronExpression = new CronExpression(task.getCronExpression());
                    map.put("nextRunTime", cronExpression.getNextValidTimeAfter(new Date()));
                } catch (ParseException e) {
                    e.printStackTrace();
                    return failure("表达式解析异常！");
                }
            }
            data.add(map);
        }
        return success(data);
    }

    @Priv
    @PostMapping("/executed")
    public ApiResponses<String> excuted(String id) {
        SystemTask systemTask = SystemTaskCache.get(id);
        if (systemTask.isDisabled()) {
            return failure("已禁用的任务不能执行！");
        }
        Task task = CronUtil.getScheduler().getTask(id);
        task.execute();
        return success("任务开始执行");
    }

    @Priv
    @PutMapping
    public ApiResponses<String> update(String id, String cronExpression, String isUsing) {
        SystemTask systemTask = SystemTaskCache.get(id);
        if (StrUtil.isEmpty(cronExpression)) {
            return failure("表达式不能为空");
        }
        if (StrUtil.isEmpty(isUsing)) {
            return failure("状态不能为空");
        }
        systemTask.setCronExpression(cronExpression);
        systemTask.setDisabled(!YesOrNo.isYes(isUsing));
        try {
            if (YesOrNo.isYes(isUsing)) {
                Task task = CronUtil.getScheduler().getTaskTable().getTask(systemTask.getExtendItemID());
                if (ObjectUtil.isEmpty(task)) {
                    CronUtil.schedule(systemTask.getExtendItemID(), systemTask.getCronExpression(), systemTask);
                } else {
                    CronUtil.updatePattern(systemTask.getExtendItemID(), new CronPattern(cronExpression));
                }
            } else {
                CronUtil.remove(systemTask.getExtendItemID());
            }
            SystemTaskCache.set(systemTask);
            Schedule schedule = scheduleService.getById(id);
            if (ObjectUtil.isNotNull(schedule)) {
                schedule.setCronExpression(cronExpression);
                schedule.setIsUsing(isUsing);
                scheduleService.updateById(schedule);
            } else {
                schedule = new Schedule();
                schedule.setId(id);
                schedule.setSourceId(id);
                schedule.setIsUsing(isUsing);
                schedule.setTypeCode(SystemTaskManager.ID);
                schedule.setCronExpression(cronExpression);
                schedule.setDescription(systemTask.getExtendItemName());
                scheduleService.save(schedule);
            }
        } catch (CronException e) {
            e.printStackTrace();
            return failure("表达式解析异常！");
        }

        return success("修改成功");
    }

}
