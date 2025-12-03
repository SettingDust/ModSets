package settingdust.mod_sets.util;

import com.google.common.base.Supplier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public interface SavingData {
    Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    SavingData invoker = new SavingData() {
        private final Supplier<Iterable<SavingData>> services = () -> ServiceLoaderUtil.findServices(SavingData.class);

        @Override
        public void load() {
            for (final var data : services.get()) {
                data.load();
            }
        }

        @Override
        public void save() {
            for (final var data : services.get()) {
                data.save();
            }
        }
    };

    void load();

    void save();
}
