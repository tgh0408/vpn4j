package org.ssl.vpn4j.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ssl.common.core.exception.ServiceException;
import org.ssl.common.mybatis.core.page.PageQuery;
import org.ssl.common.mybatis.core.page.TableDataInfo;
import org.ssl.vpn4j.domain.Syslog;
import org.ssl.vpn4j.domain.vo.SyslogVO;
import org.ssl.vpn4j.mapper.SyslogMapper;
import org.ssl.vpn4j.service.SysLogService;

@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements SysLogService {
    final SyslogMapper syslogMapper;
    @Override
    public TableDataInfo<Syslog> getOperationLog(SyslogVO syslog, PageQuery pageQuery) {
        LambdaQueryWrapper<Syslog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(syslog.getOperId() != null, Syslog::getOperId, syslog.getOperId());
        queryWrapper.eq(syslog.getBusinessType() != null, Syslog::getBusinessType, syslog.getBusinessType());
        if (syslog.getOperTime() != null){
            if (syslog.getOperTime().length !=2){
                throw new ServiceException("时间格式错误, 请使用yyyy-MM-dd格式");
            }else {
                // 判断大小
                if (syslog.getOperTime()[0].compareTo(syslog.getOperTime()[1]) > 0){
                    throw new ServiceException("时间格式错误, 请使用yyyy-MM-dd格式");
                }
                queryWrapper.between(Syslog::getOperTime, syslog.getOperTime()[0] + " 00:00:00", syslog.getOperTime()[1] + " 23:59:59");
            }
        }
        queryWrapper.orderByDesc(Syslog::getOperId);
        Page<Syslog> syslogPage = syslogMapper.selectPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(syslogPage);
    }
}
