package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import com.itheima.entity.AddressBook;
import com.itheima.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/addressBook")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增地址
     * @param addressBook
     * @return
     */
    @PostMapping
    public R<AddressBook> add(@RequestBody AddressBook addressBook){
        // 通过线程缓存区获取用户id
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("新添加的地址为{}", addressBook.toString());

        addressBookService.save(addressBook);
        return R.success(addressBook);
    }

    /**
     * 将地址展示出来
     * @return
     */
    @GetMapping("list")
    public R<List<AddressBook>> list(){
        LambdaQueryWrapper<AddressBook> addressBookLambdaQueryWrapper = new LambdaQueryWrapper<>();
        addressBookLambdaQueryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        addressBookLambdaQueryWrapper.orderByDesc(AddressBook::getIsDefault).orderByDesc(AddressBook::getUpdateTime);
        List<AddressBook> addressBooks = addressBookService.list(addressBookLambdaQueryWrapper);
        return R.success(addressBooks);

    }

    /**
     * 设置默认地址
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    public R<String> setDefault(@RequestBody AddressBook addressBook){
        log.info("接收的用户id为{}", addressBook.toString());
        LambdaQueryWrapper<AddressBook> addressBookLambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 搜索出当前用户的收货地址
        addressBookLambdaQueryWrapper.eq(AddressBook::getUserId,BaseContext.getCurrentId());
        List<AddressBook> addressBooks = addressBookService.list(addressBookLambdaQueryWrapper);

        // 将默认地址置为一，其他都置为0
        addressBooks = addressBooks.stream().map((item) -> {
            if (item.getId().equals(addressBook.getId())){
                item.setIsDefault(1);
            }else {
                item.setIsDefault(0);
            }
            return item;
        }).collect(Collectors.toList());
        addressBookService.updateBatchById(addressBooks);
        return R.success("修改默认地址成功");
    }

    /**
     * 查询默认地址
     */
    @GetMapping("default")
    public R<AddressBook> getDefault() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        queryWrapper.eq(AddressBook::getIsDefault, 1);

        //SQL:select * from address_book where user_id = ? and is_default = 1
        AddressBook addressBook = addressBookService.getOne(queryWrapper);

        if (null == addressBook) {
            return R.error("没有找到该对象");
        } else {
            return R.success(addressBook);
        }
    }

    @GetMapping("/{id}")
    public R<AddressBook> show(@PathVariable Long id){
        log.info("需要编辑的收货地址id为{}", id);

        AddressBook addressBook = addressBookService.getById(id);

        return R.success(addressBook);
    }

    /**
     * 修改地址
     * @param addressBook
     * @return
     */
    @PutMapping
    public R<AddressBook> update(@RequestBody AddressBook addressBook){
        log.info("修改的地址为{}", addressBook.toString());
        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }

    /**
     * 删除地址
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam Long ids){
        log.info("要删除的地址为{}", ids);

        addressBookService.removeById(ids);

        return R.success("删除地址成功");
    }
}
