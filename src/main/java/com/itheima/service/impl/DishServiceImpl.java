package com.itheima.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.common.CustomException;
import com.itheima.dto.DishDto;
import com.itheima.entity.Category;
import com.itheima.entity.Dish;
import com.itheima.entity.DishFlavor;
import com.itheima.entity.SetmealDish;
import com.itheima.mapper.DishMapper;
import com.itheima.service.CategoryService;
import com.itheima.service.DishFlavorService;
import com.itheima.service.DishService;
import com.itheima.service.SetmealDishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    @Transactional
    @Override
    public void saveWithFlavor(DishDto dishDto) {
        // 保存菜品基本信息到菜品表Dish
        this.save(dishDto);

        Long dishId = dishDto.getId(); // 菜品id

        // 菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        // 保存菜品口味数据到菜品口味表
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id来查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        // 查询菜品基本信息，从dish查询
        Dish dish = this.getById(id);
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        // 查询菜品基本信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);

        dishDto.setFlavors(flavors);
        return dishDto;
    }

    @Transactional // 如果方法执行期间发生异常，事务会被回滚，确保数据的完整性和一致性。
    @Override
    public void updateWithFlavor(DishDto dishDto) {
        // 更新dish表
        this.updateById(dishDto);

        // 更新dish_flavor表
        // 1、先delete dish_flavor表
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());

        dishFlavorService.remove(queryWrapper);

        // 2、添加当前提交过来的口味数据表
        // 菜品口味，存入dishid，修改口味会没有dishid
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据菜品id修改菜品状态
     * @param status
     * @param ids
     */
    @Override
    @Transactional
    public void updateStatusById(int status, List<Long> ids) {

        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new  LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(ids != null, Dish::getId, ids);
        List<Dish> list = this.list(lambdaQueryWrapper);

        for (Dish dish : list){
            if (dish != null){
                dish.setStatus(status);
                this.updateById(dish);
            }
        }
    }

    /**
     * 菜品的批量删除和单个删除
     * @param ids
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.in(Dish::getId, ids);
        List<Dish> list = this.list(dishLambdaQueryWrapper);
        LambdaQueryWrapper<DishFlavor> flavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        flavorLambdaQueryWrapper.in(DishFlavor::getDishId, ids);
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getDishId, ids);

        int count = setmealDishService.count(setmealDishLambdaQueryWrapper);

        for (Dish dish : list){
            Integer status = dish.getStatus();
            // 1、商品是否在售，不在售且没有套餐绑定，直接删除
            if (status == 0 && count == 0){
                this.removeById(dish.getId());
                dishFlavorService.remove(flavorLambdaQueryWrapper);
            }else {
                //此时应该回滚,因为可能前面的删除了，但是后面的是正在售卖
                throw new CustomException("删除菜品中有正在售卖菜品或有包含本商品的套餐存在,无法删除");
            }

        }

    }

    /**
     * 通过分类id获取菜品
     *
     * @param dish
     * @return
     */
    @Override
    public List<DishDto> showByCategoryId(Dish dish) {
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        lambdaQueryWrapper.eq(Dish::getStatus, dish.getStatus());
        // 通过分类id获取菜品信息
        List<Dish> list = this.list(lambdaQueryWrapper);
        List<DishDto> dishDtos = list.stream().map((item)->{
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            Long id = item.getCategoryId();
            Category category = categoryService.getById(id);
            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            // 获得当前菜品id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DishFlavor::getDishId, dishId);
            // 获取口味信息
            List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
            // 将口味信息添加到dto中
            dishDto.setFlavors(flavors);

            return dishDto;
        }).collect(Collectors.toList());

        return dishDtos;
    }


}
