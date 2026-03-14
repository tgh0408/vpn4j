package org.ssl.vpn4j.service;

import org.ssl.common.mybatis.core.page.PageQuery;
import org.ssl.common.mybatis.core.page.TableDataInfo;
import org.ssl.vpn4j.domain.Syslog;
import org.ssl.vpn4j.domain.vo.SyslogVO;

public interface SysLogService {

    TableDataInfo<Syslog> getOperationLog(SyslogVO syslog, PageQuery pageQuery);

}
