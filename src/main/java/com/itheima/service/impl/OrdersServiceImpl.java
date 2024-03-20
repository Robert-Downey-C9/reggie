package com.itheima.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.common.BaseContext;
import com.itheima.common.CustomException;
import com.itheima.dto.OrdersDto;
import com.itheima.entity.*;
import com.itheima.mapper.OrdersMapper;
import com.itheima.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private UserService userService;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 提交用户下单的数据
     * @param orders
     */
    @Override
    public void submit(Orders orders) {
        // 获得当前用户的id
        Long userId = BaseContext.getCurrentId();

        // 查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartLambdaQueryWrapper);

        // 如果购物车数据不存在就抛出业务异常
        if (shoppingCartList == null || shoppingCartList.isEmpty()){
            throw new CustomException("购物车为空，不能下单");
        }

        // 查询用户数据
        User user = userService.getById(userId);

        // 查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null){
            throw new CustomException("地址信息为空，不能下单");
        }

        // 向订单表插入数据，一条数据
        // 生成订单id
        long orderId = IdWorker.getId();

        // 原子整型，保证线程安全
        AtomicInteger amount = new AtomicInteger(0);

        List<OrderDetail> orderDetailList = shoppingCartList.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setAmount(item.getAmount());
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;

        }).collect(Collectors.toList());

        orders.setNumber(String.valueOf(orderId));
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));
        orders.setUserId(userId);
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        this.save(orders);

        // 向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetailList);

        // 清空购物车数据
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
    }

    /**
     * 获取订单信息
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public Page<OrdersDto> getPage(int page, int pageSize) {

        // 添加分页构造器
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();

        // 获取当前用户id
        User user = userService.getById(BaseContext.getCurrentId());

        // 查询订单信息
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        ordersLambdaQueryWrapper.eq(Orders::getUserId, user.getId());
        ordersLambdaQueryWrapper.orderByAsc(Orders::getOrderTime);

        this.page(ordersPage, ordersLambdaQueryWrapper);

        BeanUtils.copyProperties(ordersPage, ordersDtoPage);

//        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");
//
//        List<Orders> orders = ordersPage.getRecords();
//
//        List<OrdersDto> collect = orders.stream().map((item) -> {
//            OrdersDto ordersDto = new OrdersDto();
//            BeanUtils.copyProperties(item, ordersDto);
//            ordersDto.setUserName(user.getName());
//            ordersDto.setPhone(item.getPhone());
//            ordersDto.setConsignee(item.getConsignee());
//            return ordersDto;
//        }).collect(Collectors.toList());
//
//        ordersDtoPage.setRecords(collect);

        return ordersDtoPage;
    }
}
