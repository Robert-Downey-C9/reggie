package com.itheima.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.common.CustomException;
import com.itheima.dto.SetmealDto;
import com.itheima.entity.Category;
import com.itheima.entity.Dish;
import com.itheima.entity.Setmeal;
import com.itheima.entity.SetmealDish;
import com.itheima.mapper.SetmealMapper;
import com.itheima.service.CategoryService;
import com.itheima.service.DishService;
import com.itheima.service.SetmealDishService;
import com.itheima.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private DishService dishService;

    @Override
    @Transactional
    public void saveWithDto(SetmealDto setmealDto) {
        this.save(setmealDto);
        Long setmealId = setmealDto.getId();
        // 套餐菜品关系
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealId);
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 删除套餐，同时需要删除套餐和菜品的关联数据
     * @param ids
     */
    @Transactional
    @Override
    public void removeWithDish(List<Long> ids) {
        // select count(*) from setmeal where id in (1,2,3) and status = 1;
        // 查询套餐状态，确认是否可以删除
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.in(Setmeal::getId, ids);
        setmealLambdaQueryWrapper.eq(Setmeal::getStatus, 1);

        int count = this.count(setmealLambdaQueryWrapper);
        // 如果不能删除，抛出一个业务异常
        if (count > 0){
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        // 如果可以删除，先删除套餐表中的数据--setmeal
        this.removeByIds(ids);

        // 删除关系表中的数据--setmealdish
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(setmealDishLambdaQueryWrapper);
    }


    @Override
    public SetmealDto showWithDtoById(Long id) {
        //获取Setmeal
        Setmeal setmeal = this.getById(id);
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        // 获取SetmealDish信息
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> list = setmealDishService.list(setmealDishLambdaQueryWrapper);

        // 将SetmealDish添加到SetmealDto中
        setmealDto.setSetmealDishes(list);
        return setmealDto;
    }

    /**
     * 通过前端传回的是数据修改页面
     * @param setmealDto
     */
    @Transactional
    @Override
    public void updateWithDtoById(SetmealDto setmealDto) {
        // 先更新Setmeal表
        this.updateById(setmealDto);

        // 更新SetmealDish表
        // 1、先删除套餐的菜品数据
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(setmealDishLambdaQueryWrapper);
        // 2、在添加套餐的菜品数据
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 通过套餐id修改套餐状态
     * @param status
     * @param ids
     */
    @Transactional
    @Override
    public void updateStatusById(int status, List<Long> ids) {
        // 通过id获取Setmeal
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.in(Setmeal::getId, ids);
        List<Setmeal> setmealList = this.list(setmealLambdaQueryWrapper);


        // 通过id查找setmealdish 菜品id
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        List<SetmealDish> setmealDishList = setmealDishService.list(setmealDishLambdaQueryWrapper);

        // 通过菜品id查找菜品状态
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Dish::getId, setmealDishList.stream().map(SetmealDish::getDishId).collect(Collectors.toList()));
        List<Dish> dishList = dishService.list(lambdaQueryWrapper);

        // 在停售状态下，判断套餐中菜品是否停售
        if (status == 1){
            for (Dish dish : dishList){
                // 判断套餐中菜品停售,抛出业务异常
                if (dish.getStatus() == 0){
                    throw new CustomException("套餐中有菜品已停售");
                }
            }
        }

        // 修改套餐状态
        setmealList = setmealList.stream().map((item) -> {
            item.setStatus(status);
            return item;
        }).collect(Collectors.toList());

        this.updateBatchById(setmealList);
    }
}
