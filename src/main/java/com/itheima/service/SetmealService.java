package com.itheima.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.dto.SetmealDto;
import com.itheima.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    void saveWithDto(SetmealDto setmealDto);

    // 删除套餐，同时需要删除套餐和菜品的关联数据
    void removeWithDish(List<Long> ids);

    // 通过套餐id展示页面
    SetmealDto showWithDtoById(Long id);

    // 通过前端传回的是数据修改页面
    void updateWithDtoById(SetmealDto setmealDto);

    // 通过套餐id修改套餐状态
    void updateStatusById(int status, List<Long> ids);
}
