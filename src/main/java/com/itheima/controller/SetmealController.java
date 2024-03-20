package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.dto.DishDto;
import com.itheima.dto.SetmealDto;
import com.itheima.entity.Category;
import com.itheima.entity.Dish;
import com.itheima.entity.Setmeal;
import com.itheima.service.CategoryService;
import com.itheima.service.SetmealDishService;
import com.itheima.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;

    /**
     * 添加套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info("套餐信息为：{}", setmealDto.toString());
        setmealService.saveWithDto(setmealDto);
        return R.success("套餐添加成功");
    }

    /**
     * 套餐分页管理
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<SetmealDto>> page(int page, int pageSize, String name){
        log.info("page = {}, pageSize = {}, name = {}", page, pageSize, name);

        // 添加分页构造器
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);

        Page<SetmealDto> setmealDtoPage = new Page<>(page, pageSize);

        // 添加条件构造器
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        // 添加过滤条件
        lambdaQueryWrapper.like(StringUtils.isNotEmpty(name), Setmeal::getName, name);

        // 添加排序条件
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);

        // 执行语句，查询Setmeal信息
        setmealService.page(pageInfo, lambdaQueryWrapper);

        // 对象拷贝，忽略records，里面放的是SetmealDto数据
        BeanUtils.copyProperties(pageInfo, setmealDtoPage, "records");

        // 获取setmeal数据
        List<Setmeal> records = pageInfo.getRecords();

        // 对SetmealDto进行数据扩充
        List<SetmealDto> setmealDtoList = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            Long categoryId = item.getCategoryId(); // 获取分类id
            // 根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            String categoryName = category.getName();
            setmealDto.setCategoryName(categoryName);
            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(setmealDtoList);

        return R.success(setmealDtoPage);
    }


    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> deltet(@RequestParam List<Long> ids){ // spring /url?id=1233,123
        log.info("删除套餐id：{}", ids.toString());
        setmealService.removeWithDish(ids);
        return R.success("套餐删除成功");
    }

    /**
     * 在修改页面展示套餐内容
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> showById(@PathVariable Long id){
        log.info("修改套餐的id为{}", id);
        SetmealDto setmealDto = setmealService.showWithDtoById(id);
        return R.success(setmealDto);
    }

    /**
     * 通过前端传回的是数据修改页面
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> updateById(@RequestBody SetmealDto setmealDto){
        log.info("修改的信息为{}", setmealDto.toString());
        setmealService.updateWithDtoById(setmealDto);
        return R.success("套餐修改成功");
    }

    /**
     * 通过套餐id修改套餐状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatusById(@PathVariable int status, @RequestParam List<Long> ids){
        log.info("要就改的套餐id为{}，状态为{}", ids.toString(), status);
        setmealService.updateStatusById(status, ids);
        return R.success("套餐状态修改成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
        log.info("套餐信息为{}", setmeal.toString());

        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        setmealLambdaQueryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        List<Setmeal> setmeals = setmealService.list(setmealLambdaQueryWrapper);

        return R.success(setmeals);
    }
}
