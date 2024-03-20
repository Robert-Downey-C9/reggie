package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import com.itheima.dto.OrdersDto;
import com.itheima.entity.Orders;
import com.itheima.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    /**
     * 接收用户下单的数据
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单信息为{}", orders.toString());
        ordersService.submit(orders);
        return R.success("下单成功");
    }

    @GetMapping("/userPage")
    public R<Page<OrdersDto>> page(int page, int pageSize){
        log.info("page = {}, pageSize = {}", page, pageSize);

        Page<OrdersDto> ordersDtoPage = ordersService.getPage(page, pageSize);

        return R.success(ordersDtoPage);
    }
}
