package com.itheima.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.dto.OrdersDto;
import com.itheima.entity.Orders;

public interface OrdersService extends IService<Orders> {
    // 用户下单
    void submit(Orders orders);

    // 获取订单信息
    Page<OrdersDto> getPage(int page, int pageSize);
}
