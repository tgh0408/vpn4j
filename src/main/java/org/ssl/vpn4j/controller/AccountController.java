package org.ssl.vpn4j.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.ssl.common.core.domain.R;
import org.ssl.common.core.validate.AddGroup;
import org.ssl.common.core.validate.EditGroup;
import org.ssl.common.log.annotation.Log;
import org.ssl.common.log.enums.BusinessType;
import org.ssl.common.log.enums.OperatorType;
import org.ssl.common.mybatis.core.page.PageQuery;
import org.ssl.common.mybatis.core.page.TableDataInfo;
import org.ssl.vpn4j.domain.bo.AccountUpdateBo;
import org.ssl.vpn4j.domain.bo.DisableUserBo;
import org.ssl.vpn4j.domain.bo.DownLoadConfigBo;
import org.ssl.vpn4j.domain.bo.UserListBO;
import org.ssl.vpn4j.domain.vo.AccountVO;
import org.ssl.vpn4j.domain.vo.UserListItem;
import org.ssl.vpn4j.service.AccountService;

import java.io.IOException;

/**
 * 账号管理
 * @author TGH
 */

@Validated
@RestController
@RequiredArgsConstructor
public class AccountController {
    final AccountService accountService;

    /**
     * 获取用户列表
     * @param userListBO 用户列表查询条件
     * @param pageQuery 分页条件
     * @return 用户列表
     */
    @GetMapping("account/user/list")
    public TableDataInfo<UserListItem> userlist(UserListBO userListBO, PageQuery pageQuery) {
        return accountService.getUserList(userListBO, pageQuery);
    }

    /**
     * 创建用户
     * @param addBo 用户信息
     * @throws IOException 创建用户失败
     */
    @Log(title = "新增用户", businessType = BusinessType.INSERT, operatorType = OperatorType.MANAGE)
    @PostMapping("account/addUser")
    public R<Void> addUser(@Validated(AddGroup.class) @RequestBody AccountUpdateBo addBo) throws IOException {
        accountService.addUser(addBo);
        return R.ok();
    }

    /**
     * 复制账号密码
     */
    @GetMapping("account/copy/{id}")
    public R<AccountVO> copy(@PathVariable Long id) {
        AccountVO vo = accountService.copyUserPasswd(id);
        return R.ok(vo);
    }

    /**
     * 强制下线
     * @param ids 用户集合
     */
    @Log(title = "强制下线", businessType = BusinessType.FORCE, operatorType = OperatorType.MANAGE)
    @PostMapping("account/offline/{ids}")
    public R<Void> offline(@PathVariable Long [] ids) {
        accountService.offline(ids);
        return R.ok();
    }

    /**
     * 禁用用户
     */
    @Log(title = "禁用用户", businessType = BusinessType.FORCE, operatorType = OperatorType.MANAGE)
    @PostMapping("account/disable")
    public R<Void> disable(@Validated @RequestBody DisableUserBo disableUserBo) {
        accountService.disable(disableUserBo);
        return R.ok();
    }

    /**
     * 重新生成用户证书
     * @param addBo 用户信息
     * @throws IOException 重新生成用户证书失败
     */
    @Log(title = "重新生成用户证书", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE)
    @PostMapping("account/reCreateUserCer")
    public R<Void> reCreateUserCer(@Validated(EditGroup.class) @RequestBody AccountUpdateBo addBo) throws IOException {
        accountService.reCreateUserCer(addBo);
        return R.ok();
    }

    /**
     * 修改用户
     * @param updateBo 用户信息
     */
    @Log(title = "修改用户", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE)
    @PutMapping("account/update")
    public R<Void> update(@Validated(EditGroup.class) @RequestBody AccountUpdateBo updateBo) {
        accountService.updateUser(updateBo);
        return R.ok();
    }

    /**
     * 删除用户
     * @param ids 用户id
     */
    @Log(title = "删除用户", businessType = BusinessType.DELETE, operatorType = OperatorType.MANAGE)
    @DeleteMapping("account/delete/{ids}")
    public R<Void> delete(@PathVariable Long [] ids) {
        accountService.deleteUser(ids);
        return R.ok();
    }

    /**
     * 导出用户证书
     * @param id 用户id
     */
    @GetMapping("account/export/{id}")
    public void download(@PathVariable Long id) {
        accountService.download(new DownLoadConfigBo(id));
    }

    /**
     * 发送邮件
     * @param ids 用户id
     */
    @Log(title = "发送邮件", businessType = BusinessType.OTHER, operatorType = OperatorType.MANAGE)
    @GetMapping("account/sendEmail/{ids}")
    public R<Void> sendEmail(@PathVariable Long [] ids) {
        accountService.sendEmail(ids);
        return R.ok();
    }
}
