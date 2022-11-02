package com.kiona.analysis.google.service;

import com.kiona.analysis.entity.GoogleEvent;
import com.kiona.analysis.entity.repository.GoogleEventRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author yangshuaichao
 * @date 2022/08/15 15:58
 * @description TODO
 */
@Service
public class GoogleEventService {
    private final GoogleEventRepository repository;
    private final Map<String, Double> purchaseValueCache = new HashMap<>();

    public GoogleEventService(GoogleEventRepository repository) {this.repository = repository;}

    public double getPurchaseValue(String category, int[] eventCounts){
        return IntStream.range(0, eventCounts.length)
                .filter(no -> isPurchaseEvent(category, String.valueOf(no)))
                .mapToDouble(eventNo -> getPurchaseValueByCategoryAndNo(category, String.valueOf(eventNo)) * eventCounts[eventNo])
                .sum();
    }

    public int getPurchaseCount(String category, int[] eventCounts) {
        return IntStream.range(0, eventCounts.length)
                .filter(no -> isPurchaseEvent(category, String.valueOf(no)))
                .map(eventNo -> eventCounts[eventNo])
                .sum();
    }

    private double getPurchaseValueByCategoryAndNo(String category, String no){
        return repository.findByCategoryAndNo(category, no).map(GoogleEvent::getPurchaseValue).orElse(0d);
    }

    private boolean isPurchaseEvent(String category, String no){
        return repository.findByCategoryAndNo(category, no).map(GoogleEvent::isPurchaseEvent).orElse(false);
    }
}
