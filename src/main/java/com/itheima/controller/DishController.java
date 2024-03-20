package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.dto.DishDto;
import com.itheima.entity.Category;
import com.itheima.entity.Dish;
import com.itheima.entity.DishFlavor;
import com.itheima.service.CategoryService;
import com.itheima.service.DishFlavorService;
import com.itheima.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info("菜品信息：{}", dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        // 清理所有菜品数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        // 清理某个菜品数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("菜品新增成功");
    }

    /**
     * 菜品分页管理
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name){
        log.info("page = {}, pageSize = {}, name = {}", page, pageSize, name);

        // 添加分页构造器
        Page<Dish> pageInfo = new Page<>(page, pageSize);

        // Dish中没有分类名称的信息
        Page<DishDto> dishDtoPage = new Page<>(page, pageSize);

        // 添加条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        // 添加过滤条件
        lambdaQueryWrapper.like(StringUtils.isNotEmpty(name), Dish::getName, name);

        // 添加排序条件
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);

        // 执行语句，查询Dish信息
        dishService.page(pageInfo, lambdaQueryWrapper);

        // 对象拷贝，忽略records，里面放的是DishDto数据
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        // 获取Dish数据
        List<Dish> records = pageInfo.getRecords();
        
        // 对DishDto进行数据扩充
        List<DishDto> dishDtoList = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            Long categoryId = item.getCategoryId(); // 获取分类id
            // 根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            String categoryName = category.getName();
            dishDto.setCategoryName(categoryName);
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(dishDtoList);

        return R.success(dishDtoPage);
    }

    @GetMapping("/{id}")
    public R<DishDto> findById(@PathVariable Long id){

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info("菜品信息：{}", dishDto.toString());

        // 清理所有菜品数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        // 清理某个菜品数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        dishService.updateWithFlavor(dishDto);

        return R.success("菜品修改成功");
    }

    /**
     * 根据菜品id修改菜品状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable int status, @RequestParam List<Long> ids){
        log.info("status = {}, ids = {}", status, ids.toString());
        dishService.updateStatusById(status, ids);
        return R.success("修改成功");
    }

    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("要删除的菜品的id：{}", ids.toString());
        dishService.delete(ids);
        return R.success("删除商品成功");
    }

    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        log.info("分类id：{}", dish.getCategoryId());
        List<DishDto> dishDto = dishService.showByCategoryId(dish);

        return R.success(dishDto);
    }
}
