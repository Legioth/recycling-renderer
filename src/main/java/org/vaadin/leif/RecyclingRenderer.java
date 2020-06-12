package org.vaadin.leif;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.Rendering;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.shared.Registration;

import elemental.json.JsonArray;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class RecyclingRenderer<T, C extends Component> extends Renderer<T> {

    private final SerializableSupplier<C> baseComponentSupplier;
    private final SerializableBiFunction<C, T, Registration> componentUpdater;

    private final Map<String, C> keyToComponent = new HashMap<>();
    private final Map<String, Registration> keyToUnregister = new HashMap<>();

    private RecyclingRenderer(SerializableSupplier<C> baseComponentSupplier,
            SerializableBiFunction<C, T, Registration> componentUpdater) {
        this.baseComponentSupplier = baseComponentSupplier;
        this.componentUpdater = componentUpdater;
    }

    public static <T, C extends Component> Renderer<T> withoutReset(SerializableSupplier<C> baseComponentSupplier,
            SerializableBiConsumer<C, T> componentUpdater) {
        return new RecyclingRenderer<>(baseComponentSupplier, (component, item) -> {
            componentUpdater.accept(component, item);
            return null;
        });
    }

    public static <T, C extends Component> Renderer<T> withReset(SerializableSupplier<C> baseComponentSupplier,
            SerializableBiFunction<C, T, Registration> componentUpdater) {
        return new RecyclingRenderer<>(baseComponentSupplier, componentUpdater);
    }

    @Override
    public Rendering<T> render(Element container, DataKeyMapper<T> keyMapper, Element contentTemplate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rendering<T> render(Element container, DataKeyMapper<T> keyMapper) {
        ReturnChannelRegistration returnChannelRegistration = container.getNode().getFeature(ReturnChannelMap.class)
                .registerChannel(arguments -> handleChannelMessage(arguments, container, keyMapper));

        container.getNode().runWhenAttached(ui -> {
            container.executeJs("this.renderer = window.recyclingRenderFactory($0, $1)", ui.getInternals().getAppId(),
                    returnChannelRegistration);
        });

        return new Rendering<T>() {
            @Override
            public Optional<DataGenerator<T>> getDataGenerator() {
                return Optional.empty();
            }

            @Override
            public Element getTemplateElement() {
                return null;
            }
        };
    }

    private void handleChannelMessage(JsonArray arguments, Element container, DataKeyMapper<T> keyMapper) {
        String oldKey = getStringOrNull(arguments, 0);
        String newKey = getStringOrNull(arguments, 1);

        C component;
        if (oldKey == null) {
            int rendererId = (int) arguments.getNumber(2);

            component = createComponent(newKey, container, rendererId);
        } else {
            component = findComponent(oldKey);
        }

        T item = keyMapper.get(newKey);

        Registration registration = componentUpdater.apply(component, item);

        keyToComponent.put(newKey, component);
        keyToUnregister.put(newKey, registration);
    }

    private C findComponent(String key) {
        C component = keyToComponent.remove(key);
        if (component == null) {
            throw new IllegalStateException("No component found for key " + key);
        }

        Registration registration = keyToUnregister.remove(key);
        if (registration != null) {
            registration.remove();
        }

        return component;
    }

    private C createComponent(String key, Element container, int rendererId) {
        if (keyToComponent.containsKey(key)) {
            throw new IllegalStateException("There is already content for key " + key);
        }

        C component = baseComponentSupplier.get();
        container.appendVirtualChild(component.getElement());

        container.executeJs("window.recyclingRenderer[$0].setNodeId($1)", rendererId,
                component.getElement().getNode().getId());

        return component;
    }

    private static String getStringOrNull(JsonArray array, int index) {
        JsonValue value = array.get(index);
        if (value.getType() == JsonType.NULL) {
            return null;
        } else {
            return value.asString();
        }
    }
}
