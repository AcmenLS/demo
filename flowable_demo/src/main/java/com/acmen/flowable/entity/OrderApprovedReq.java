package com.acmen.flowable.entity;

import lombok.Data;

@Data
public class OrderApprovedReq {

    private String userId;
    private String purchaseOrderId;

}
