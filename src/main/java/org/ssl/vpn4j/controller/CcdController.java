package org.ssl.vpn4j.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.ssl.common.core.domain.R;
import org.ssl.common.core.validate.AddGroup;
import org.ssl.common.core.validate.DelGroup;
import org.ssl.common.core.validate.EditGroup;
import org.ssl.common.log.annotation.Log;
import org.ssl.common.log.enums.BusinessType;
import org.ssl.common.log.enums.OperatorType;
import org.ssl.common.mybatis.core.page.PageQuery;
import org.ssl.common.mybatis.core.page.TableDataInfo;
import org.ssl.vpn4j.domain.bo.CcdBo;
import org.ssl.vpn4j.domain.vo.CcdVO;
import org.ssl.vpn4j.service.CcdService;
import org.ssl.vpn4j.service.ClientAuthService;

@RestController
@RequiredArgsConstructor
public class CcdController {
    final CcdService ccdService;
    final ClientAuthService clientAuthService;

    @GetMapping("/ccd")
    public TableDataInfo<CcdVO> ccd(CcdBo bo, PageQuery pageQuery) {
        return ccdService.getCcd(bo, pageQuery);
    }

    @GetMapping("/ccd/{id}")
    public R<String> ccdInfo(@PathVariable Long id) {
        return R.ok(null, clientAuthService.generateCcdConfig(id));
    }

    @Log(title = "创建ccd配置", businessType = BusinessType.INSERT, operatorType = OperatorType.MANAGE)
    @PostMapping("/ccd/addccdCfg")
    public R<String> addCcd(@Validated(AddGroup.class) @RequestBody CcdBo bo) {
        ccdService.addCcd(bo);
        return R.ok();
    }

    @Log(title = "更新ccd配置", businessType = BusinessType.UPDATE, operatorType = OperatorType.MANAGE)
    @PostMapping("/ccd/update")
    public R<Void> updateCcd(@Validated(EditGroup.class) @RequestBody CcdBo bo) {
        ccdService.updateCcd(bo);
        return R.ok();
    }

    @Log(title = "删除ccd配置", businessType = BusinessType.DELETE, operatorType = OperatorType.MANAGE)
    @DeleteMapping("/ccd/remove")
    public R<Void> removeCcd(@Validated(DelGroup.class) @RequestBody CcdBo bo) {
        ccdService.removeCcd(bo);
        return R.ok();
    }
}
