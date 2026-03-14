package org.ssl.vpn4j.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.core.utils.StringUtils;
import org.ssl.vpn4j.domain.Admin;
import org.ssl.vpn4j.domain.bo.AdminBo;
import org.ssl.vpn4j.mapper.AdminMapper;
import org.ssl.vpn4j.service.AdminService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    final AdminMapper adminMapper;

    @Override
    public List<Admin> getAdminList(String keyword) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(keyword)) {
            queryWrapper.like(Admin::getAccount, keyword);
        }
        return adminMapper.selectList(queryWrapper);
    }

    @Override
    public void updatePasswd(AdminBo bo) {
        if (!Strings.CS.equals(bo.getNewPassword(), bo.getConfirmPassword())) {
            throw new ServiceException("密码不一致");
        }
        if (StringUtils.isEmpty(bo.getNewPassword())) {
            throw new ServiceException("密码不能为空");
        }
        LambdaUpdateWrapper<Admin> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Admin::getId, bo.getId())
                .set(Admin::getPassword, bo.getNewPassword());
        adminMapper.update(updateWrapper);
    }

    @Override
    public void updateInfo(AdminBo bo) {
        LambdaUpdateWrapper<Admin> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Admin::getId, bo.getId());
        updateWrapper.set(StringUtils.isNotBlank(bo.getUsername()), Admin::getAccount, bo.getUsername());
        updateWrapper.set(StringUtils.isNotBlank(bo.getNickname()), Admin::getNickname, bo.getNickname());
        updateWrapper.set(StringUtils.isNotBlank(bo.getEmail()), Admin::getEmail, bo.getEmail());
        updateWrapper.set(StringUtils.isNotBlank(bo.getPhone()), Admin::getPhone, bo.getPhone());
        updateWrapper.set(StringUtils.isNotBlank(bo.getAddress()), Admin::getAddress, bo.getAddress());
        updateWrapper.set(StringUtils.isNotBlank(bo.getDescription()), Admin::getDescription, bo.getDescription());
        updateWrapper.set(StringUtils.isNotBlank(bo.getSex()), Admin::getSex, bo.getSex());
        adminMapper.update(updateWrapper);
    }

    @Override
    public Admin getAdminInfo(Long adminId) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getId, adminId);
        queryWrapper.select(
                Admin::getId,
                Admin::getAccount,
                Admin::getNickname,
                Admin::getEmail,
                Admin::getPhone,
                Admin::getAddress,
                Admin::getDescription,
                Admin::getSex
        );
        return adminMapper.selectOne(queryWrapper);
    }
}
