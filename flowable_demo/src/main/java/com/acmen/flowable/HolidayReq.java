package com.acmen.flowable;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;

public class HolidayReq {


    public static void main(String[] args) {

        //实例化一个ProcessEngine实例,这是一个线程安全对象,一般全局一个就行
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:mysql://192.168.7.18:3306/flowable?autoReconnect=true&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8")
                .setJdbcUsername("root")
                .setJdbcPassword("root")
                .setJdbcDriver("com.mysql.jdbc.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine processEngine = cfg.buildProcessEngine();

    }

}

