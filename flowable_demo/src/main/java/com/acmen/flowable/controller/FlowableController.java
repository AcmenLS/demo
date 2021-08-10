package com.acmen.flowable.controller;

import com.acmen.flowable.entity.OrderApprovedReq;
import com.acmen.flowable.utils.R;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/flowable")
public class FlowableController {

    @Autowired
    private ProcessEngine processEngine ;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    /**
     *
     *  提交订单的审批请求
     *
     * @return
     */
    @PostMapping("/startFlow")
    public R startFlow(@PathVariable String userId, @PathVariable String purchaseOrderId){
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId",userId);
        map.put("purchaseOrderId",purchaseOrderId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("OrderApproval",map);
        String processId = processInstance.getId();
        String name = processInstance.getName();
        System.out.println(processId + ": "+ name);
        return R.ok();
    }


    /**
     *  获取用户的任务
     * @param userId
     * @return
     */
    @GetMapping("/getTasks")
    public R getTasks(@PathVariable String userId){
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();
        return R.ok(tasks.toString());
    }

    /**
     *
     * 审核通过
     * @param taskId
     * @return
     */
    @PostMapping("/success")
    public R success(@PathVariable String taskId){
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null){
            return R.error("流程不存在");
        }
        // 通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("approved",true);
        taskService.complete(taskId,map);
        return R.ok("流程审核通过！");
    }

    /**
     *
     * 审核不通过
     * @param taskId
     * @return
     */
    @PostMapping("/faile")
    public R faile(@PathVariable String taskId){
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null){
            return R.error("流程不存在");
        }
        // 通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("approved",false);
        taskService.complete(taskId,map);
        return R.ok("流程审核通过！");
    }

    @RequestMapping(value = "processDiagram")
    public void genProcessDiagram(HttpServletResponse response,String processId) throws Exception{

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();
        // 流程走完的不显示图
        if (processInstance == null){
            return;
        }
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // 使用流程实例 ID，查询正在执行的执行对象表，返回流程实例对象
        String instanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(instanceId)
                .list();

        // 得到正在执行的 Activity 的 id
        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exception:executions) {
            List<String> activeActivityIds = runtimeService.getActiveActivityIds(exception.getId());
            activityIds.addAll(activeActivityIds);

        }

        // 获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator processDiagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream inputStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(),1.0,false);

        OutputStream outputStream = null;
        byte[] buffer = new byte[1024];
        int length = 0;
        try {
            outputStream = response.getOutputStream();
            while ((length = inputStream.read(buffer))!= -1){
                outputStream.write(buffer,0,length);
            }
        }finally {
            if (inputStream != null){
                inputStream.close();
            }
            if (outputStream != null){
                outputStream.close();
            }

        }



    }








}
