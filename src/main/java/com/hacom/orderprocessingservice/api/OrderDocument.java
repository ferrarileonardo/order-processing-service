package com.hacom.orderprocessingservice.api;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

import java.util.List;

@Data
@Document(collection = "orders")
public class OrderDocument {

    @Id
    private ObjectId _id;

    private String orderId;
    private String customerId;
    private String customerPhoneNumber;
    private String status;
    private List<String> items;
    private Date ts;}