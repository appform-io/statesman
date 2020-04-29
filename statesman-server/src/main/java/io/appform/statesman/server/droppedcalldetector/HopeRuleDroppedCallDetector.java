package io.appform.statesman.server.droppedcalldetector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Strings;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
@Slf4j
public class HopeRuleDroppedCallDetector implements DroppedCallDetector {
    private final HopeLangEngine hopeLangEngine;
    private final LoadingCache<String, Evaluatable> ruleCache;

    public HopeRuleDroppedCallDetector() {
        this.hopeLangEngine = HopeLangEngine.builder()
            .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
            .build();
        ruleCache = Caffeine.newBuilder()
            .build(new CacheLoader<String, Evaluatable>() {
                @Nullable
                @Override
                public Evaluatable load(@Nonnull String rule) throws Exception {
                    return hopeLangEngine.parse(rule);
                }
            });
    }

    @Override
    public boolean detectDroppedCall(TransformationTemplate template, JsonNode node) {
        val rule = template.getDropDetectionRule();
        if(Strings.isNullOrEmpty(rule)) {
            log.warn("No dropped call detection rule found for: {}. Node: {}", template.getProvider(), node);
            return false;
        }
        return hopeLangEngine.evaluate(ruleCache.get(rule), node);
    }
}
