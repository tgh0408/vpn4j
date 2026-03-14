package org.ssl.vpn4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ssl.vpn4j.domain.Syslog;

/**
 * <p>
 * 系统日志表 Mapper 接口
 * </p>
 *
 * @author TGH
 * @since 2026-01-08
 */
@Mapper
public interface SyslogMapper extends BaseMapper<Syslog> {

}
