package com.bank.product.core.adapter;

import com.bank.product.core.model.CoreSystemType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for core banking adapters.
 * Supports dynamic adapter selection based on core system type.
 */
@Slf4j
@Component
public class CoreBankingAdapterRegistry {

    private final Map<CoreSystemType, CoreBankingAdapter> adapters = new ConcurrentHashMap<>();

    /**
     * Constructor that auto-registers all available adapters.
     *
     * @param adapterList list of all CoreBankingAdapter beans
     */
    public CoreBankingAdapterRegistry(List<CoreBankingAdapter> adapterList) {
        adapterList.forEach(adapter -> {
            register(adapter);
            log.info("Registered core banking adapter: {} (version {})",
                    adapter.getType(), adapter.getAdapterVersion());
        });
    }

    /**
     * Register an adapter for a specific core system type.
     *
     * @param adapter the adapter to register
     */
    public void register(CoreBankingAdapter adapter) {
        CoreSystemType type = adapter.getType();
        if (adapters.containsKey(type)) {
            log.warn("Adapter for {} already registered. Overwriting with {}",
                    type, adapter.getClass().getSimpleName());
        }
        adapters.put(type, adapter);
    }

    /**
     * Get the adapter for a specific core system type.
     *
     * @param type the core system type
     * @return the adapter, or empty if not found
     */
    public Optional<CoreBankingAdapter> getAdapter(CoreSystemType type) {
        CoreBankingAdapter adapter = adapters.get(type);
        if (adapter == null) {
            log.error("No adapter registered for core system type: {}", type);
        }
        return Optional.ofNullable(adapter);
    }

    /**
     * Check if an adapter is registered for a core system type.
     *
     * @param type the core system type
     * @return true if adapter is registered
     */
    public boolean hasAdapter(CoreSystemType type) {
        return adapters.containsKey(type);
    }

    /**
     * Get all registered adapters.
     *
     * @return map of core system type to adapter
     */
    public Map<CoreSystemType, CoreBankingAdapter> getAllAdapters() {
        return Map.copyOf(adapters);
    }

    /**
     * Get count of registered adapters.
     *
     * @return number of registered adapters
     */
    public int getAdapterCount() {
        return adapters.size();
    }
}
