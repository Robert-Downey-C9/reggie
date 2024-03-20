package com.itheima.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.dto.DishDto;
import com.itheima.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    // 新增菜品，同时插入菜品对应的口味数据，需要操作两张表：Dish、Dish_Flavor
    public void saveWithFlavor(DishDto dishDto);

    // 根据id来查询菜品信息和对应的口味信息
    public DishDto getByIdWithFlavor(Long id);

    // 修改菜品菜品信息，同时更新口味信息
    public void updateWithFlavor(DishDto dishDto);

    // 更新菜品状态
    public void updateStatusById(int status, List<Long> ids);

    // 删除菜品
    public void delete(List<Long> ids);

    // 通过分类id获取菜品
    List<DishDto> showByCategoryId(Dish dish);
}
