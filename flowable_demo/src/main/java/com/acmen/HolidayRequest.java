package com.acmen;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HolidayRequest {

    public static void main(String[] args) {
        /*
        * 首先实例化 ProcessEngine，线程安全对象，一般全局只有一个即可，从 ProcessEngineConfiguration 创建的话，
        * 可以调整一些配置，通常我们会从 Xml中创建，至少要配置一个 JDBC 连接
        *
        * 如果是在spring的配置中，使用 SpringProcessEngineConfiguration
        *
        * */
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration().setJdbcUrl("jdbc:mysql://192.168.7.18:3306/flowable?autoReconnect=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8")
                .setJdbcUsername("root")
                .setJdbcPassword("root")
                .setJdbcDriver("com.mysql.jdbc.Driver")
                // 如果数据表不存在则自动创建数据表
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        // 执行完成之后就可以开始创建流程
        ProcessEngine processEngine = cfg.buildProcessEngine();

        // 使用 BPMN 2.0 定义 process。存储为 Xml，同时也是可以可视化的。
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment =repositoryService.createDeployment()
                .addClasspathResource("holiday-request.bpmn20.xml")
                .deploy();

        ProcessDefinition processDefinition =repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        System.out.println("Found process definition : " + processDefinition.getName());

        // 启动 process的实例，需要一些初始化的变量，示例用scanner 获取，开发中会通过接口去获取
        Scanner scanner= new Scanner(System.in);

        System.out.println("Who are you?");
        String employee = scanner.nextLine();

        System.out.println("How many holidays do you want to request?");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());

        System.out.println("Why do you need them?");
        String description = scanner.nextLine();

        RuntimeService runtimeService = processEngine.getRuntimeService();

        // 使用 Map 来接收申请的信息
        Map<String, Object> variables = new HashMap<>();
        variables.put("employee",employee);
        variables.put("nrOfHolidays",nrOfHolidays);
        variables.put("description",description);

        //当创建实例的时候，execution 就被创建了，然后放在启动的事件中，这个事件可以从数据库中获取
        // 用户后续等待这个状态即可
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holidayRequest",variables);

        // 在 Flowable 中数据库的事务对数据一致性起着关键性的作用。

        // 查询和完成任务
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println((i+1)+") " + tasks.get(i).getName());
        }

        System.out.println("Which task would you like to complete?");
        int taskIndex = Integer.valueOf(scanner.nextLine());
        Task task = tasks.get(taskIndex - 1);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " + processVariables.get("nrOfHolidays") + "of holidays. Do you approve this?");

        boolean approved = scanner.nextLine().toLowerCase().equals("y");
        variables = new HashMap<String, Object>();
        variables.put("approved",approved);
        taskService.complete(task.getId(),variables);

        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc()
                .list();

        for (HistoricActivityInstance activity:activities) {
            System.out.println(activity.getActivityId() + " took " + activity.getDurationInMillis()+ " milliseconds");

        }

    }
}
