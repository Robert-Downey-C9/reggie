package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import com.itheima.entity.ShoppingCart;
import com.itheima.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     *  展示购物车数据
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){

        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartLambdaQueryWrapper.orderByAsc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(shoppingCartLambdaQueryWrapper);

        return R.success(list);
    }

    @PostMapping("/add")
    public R<String> addCart(@RequestBody ShoppingCart shoppingCart){
        log.info("添加到购物车的菜品为{}", shoppingCart.toString());

        // 设置用户id
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        // 查询菜品或者套餐是否存在购物车中
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();

        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, currentId);

        if (dishId != null){
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getDishId, dishId);
        }else {
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
        }

        ShoppingCart shoppingCartServiceOne = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);

        // 存在就在数量上加一
        if (shoppingCartServiceOne != null){
            shoppingCartServiceOne.setNumber(shoppingCartServiceOne.getNumber() + 1);
            shoppingCartService.updateById(shoppingCartServiceOne);
        }else {
            // 不存在就添加到购物车中
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
        }

        return R.success("添加购物车成功");
    }


    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> clean(){
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartService.remove(shoppingCartLambdaQueryWrapper);
        return R.success("清空购物车成功");
    }

    @PostMapping("/sub")
    public R<String> sub(@RequestBody ShoppingCart shoppingCart){
        log.info("要减少的购物车信息为{}", shoppingCart.toString());

        // 设置用户id
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        // 查询菜品或者套餐是否存在购物车中
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();

        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, currentId);

        if (dishId != null){
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getDishId, dishId);
        }else {
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getSetmealId, setmealId);
        }

        ShoppingCart shoppingCartServiceOne = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);

        // 存在并且数量大于1就在数量上减一
        if (shoppingCartServiceOne != null){
            if (shoppingCartServiceOne.getNumber()==1){
                shoppingCartService.removeById(shoppingCartServiceOne.getId());
            }else {
                shoppingCartServiceOne.setNumber(shoppingCartServiceOne.getNumber() - 1);
                shoppingCartService.updateById(shoppingCartServiceOne);
            }
            return R.success("购物车减少商品成功");
        }

        return R.error("购物车减少商品失败");
    }
}