package com.itheima.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.common.CustomException;
import com.itheima.entity.Category;
import com.itheima.entity.Dish;
import com.itheima.entity.Setmeal;
import com.itheima.mapper.CategoryMapper;
import com.itheima.service.CategoryService;
import com.itheima.service.DishService;
import com.itheima.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类，删除之前需要进行判断
     * @param id
     */
    @Override
    public void remove(Long id) {

        // 添加分类条件，根据分类id来查询
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, id);
        int dishCount = dishService.count(dishLambdaQueryWrapper);

        // 查询当前分类是否关联菜品，如果关联及抛出一个业务异常
        if (dishCount > 0){
            // 已经关联菜品，抛出一个业务异常
            throw new CustomException("当前分类项关联了菜品，不能删除");
        }

        // 添加分类条件，根据分类id来查询
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, id);
        int setmealCount = setmealService.count(setmealLambdaQueryWrapper);
        // 查询当前分类是否关联套餐，如果关联及抛出一个业务异常
        if (setmealCount > 0){
            // 已经关联套餐，抛出一个业务异常
            throw new CustomException("当前分类项关联了套餐，不能删除");
        }

        // 正常删除分类
        super.removeById(id);
    }
}
