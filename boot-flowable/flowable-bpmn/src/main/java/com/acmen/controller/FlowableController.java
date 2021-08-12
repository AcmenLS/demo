package com.acmen.controller;


import com.acmen.utils.R;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/flowable")
public class FlowableController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProcessEngine processEngine ;

    /**
     *
     * 提交采购订单的审批请求
     *
     * @param userId
     * @param purchaseOrderId
     * @return
     */
    @PostMapping("/start/{userId}/{purchaseOrderId}")
    public R startFlow(@PathVariable String userId,@PathVariable String purchaseOrderId){
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId",userId);
        map.put("purchaseOrderId",purchaseOrderId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("OrderApproval",map);
        String processId = processInstance.getId();
        String name = processInstance.getName();
        System.out.println(processId + ":" + name);
        return R.ok(processId + ":" + name);
    }

    /**
     * 获取用户任务
     *
     * @param userId
     * @return
     */
    @GetMapping("/getTasks/{userId}")
    public R getTasks(@PathVariable String userId){
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
        return R.ok(tasks.toString());
    }


    /**
     * 审核通过
     *
     * @param taskId
     * @return
     */
    @PostMapping("/success/{taskId}")
    public R success(@PathVariable String taskId){
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null){
            return R.error("流程不存在");
        }
        //审核通过
        HashMap<String, Object> map = new HashMap<>();
        map.put("approved",true);
        taskService.complete(taskId,map);
        return R.ok("流程审核通过！！");
    }

    /**
     * 审核不通过
     *
     * @param taskId
     * @return
     */
    @PostMapping("/faile/{taskId}")
    public R faile(@PathVariable String taskId){
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null){
            return R.error("流程不存在");
        }
        //审核不通过
        HashMap<String, Object> map = new HashMap<>();
        map.put("approved",false);
        taskService.complete(taskId,map);
        return R.ok("流程审核不通过！！");
    }

    @RequestMapping(value ="processDiagram")
    public void getProcessDiagram(HttpServletResponse response,String processId) throws IOException {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        // 流程走完的不显示图
        if (processInstance == null){
            return;
        }

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        //使用流程实例 ID，查询正在执行的执行对象表，返回流程实例对象
        String processInstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list();
        //得到正在执行的 Activity的 Id
        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exec:executions) {
            List<String> activeActivityIds = runtimeService.getActiveActivityIds(exec.getId());
            activityIds.addAll(activeActivityIds);
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator processDiagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
        InputStream inputStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, processEngineConfiguration.getActivityFontName(), processEngineConfiguration.getLabelFontName(), processEngineConfiguration.getAnnotationFontName(), processEngineConfiguration.getClassLoader(), 1.0, false);
        OutputStream outputStream = null;
        byte[] buffer = new byte[1024];
        int length = 0;
        try{
            outputStream = response.getOutputStream();
            while ((length = inputStream.read(buffer)) != -1){
                outputStream.write(buffer,0,length);
            }
        }finally {
            if (inputStream!=null){
                inputStream.close();
            }
            if (outputStream != null){
                outputStream.close();
            }

        }


    }



}
