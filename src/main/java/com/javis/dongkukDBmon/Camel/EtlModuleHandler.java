package com.javis.dongkukDBmon.Camel;

import com.javis.dongkukDBmon.model.EtlJob;
import com.javis.dongkukDBmon.model.MonitorModule;

public interface EtlModuleHandler {
    boolean supports(String moduleCode); // 이 핸들러가 처리 가능한 모듈인지
    void handle(EtlJob job, MonitorModule module, Long batchId) throws Exception;
}
