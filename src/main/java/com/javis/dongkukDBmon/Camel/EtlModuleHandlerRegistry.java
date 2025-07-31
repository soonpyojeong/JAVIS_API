package com.javis.dongkukDBmon.Camel;



import com.javis.dongkukDBmon.Camel.AbstractEtlModuleHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EtlModuleHandlerRegistry {

    private final List<AbstractEtlModuleHandler> handlers; // 모든 핸들러 주입
    private final Map<String, AbstractEtlModuleHandler> handlerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (AbstractEtlModuleHandler handler : handlers) {
            // 각 핸들러가 지원하는 모듈 코드 등록
            String moduleCode = getModuleCode(handler);
            handlerMap.put(moduleCode.toUpperCase(), handler);
        }
    }

    private String getModuleCode(AbstractEtlModuleHandler handler) {
        // 각 핸들러가 supports("모듈코드") 구현하므로 이를 활용
        // 초기 등록을 위해 전수 조사 방식 사용
        List<String> knownModules = List.of("HEALTH", "INVALID_OBJECT", "SESSION", "SELECT", "ETC","PROC");
        for (String code : knownModules) {
            if (handler.supports(code)) return code;
        }
        throw new IllegalStateException("등록할 수 없는 핸들러: " + handler.getClass().getName());
    }

    public AbstractEtlModuleHandler find(String moduleCode) {
        AbstractEtlModuleHandler handler = handlerMap.get(moduleCode.toUpperCase());
        if (handler == null) {
            throw new IllegalArgumentException("지원하지 않는 모듈 코드: " + moduleCode);
        }
        return handler;
    }
}
