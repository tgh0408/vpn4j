package org.ssl.vpn4j.service;

import org.ssl.common.mybatis.core.page.PageQuery;
import org.ssl.common.mybatis.core.page.TableDataInfo;
import org.ssl.vpn4j.domain.bo.CcdBo;
import org.ssl.vpn4j.domain.vo.CcdVO;

import java.util.List;

public interface CcdService {

    TableDataInfo<CcdVO> getCcd(CcdBo bo, PageQuery pageQuery);

    void addCcd(CcdBo bo);

    void updateCcd(CcdBo bo);

    void removeCcd(CcdBo bo);

    void removeCcds(Long [] ids);

    void removeCcds(List<String> usernames);
}
